package com.abhi.madadwala_1.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhi.madadwala_1.data.remote.BookingResponse
import com.abhi.madadwala_1.data.remote.RetrofitClient
import com.abhi.madadwala_1.ui.components.PrimaryButton
import com.abhi.madadwala_1.ui.theme.MadadwalaColors
import com.abhi.madadwala_1.ui.viewmodel.PaymentResult
import com.abhi.madadwala_1.ui.viewmodel.PaymentViewModel
import com.abhi.madadwala_1.ui.viewmodel.WalletState
import com.abhi.madadwala_1.ui.viewmodel.WalletViewModel
import com.razorpay.Checkout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    bookingId: String,
    onBack: () -> Unit,
    onPaymentSuccess: (String, String) -> Unit,
    paymentViewModel: PaymentViewModel = viewModel(),
    walletViewModel: WalletViewModel = viewModel()
) {
    var selectedMethod by remember { mutableStateOf("UPI") }
    var isPaying by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var booking by remember { mutableStateOf<BookingResponse?>(null) }
    var isLoadingBooking by remember { mutableStateOf(true) }
    var isPaymentDone by remember { mutableStateOf(false) }

    var couponCode by remember { mutableStateOf("") }
    var couponDiscount by remember { mutableDoubleStateOf(0.0) }
    var isCouponApplied by remember { mutableStateOf(false) }
    var currentTotal by remember { mutableDoubleStateOf(0.0) }

    val walletState by walletViewModel.uiState.collectAsState()
    var walletBalance by remember { mutableStateOf(0.0) }

    LaunchedEffect(walletState) {
        if (walletState is WalletState.Success) {
            walletBalance = (walletState as WalletState.Success).balance
        }
    }

    LaunchedEffect(Unit) {
        try {
            Checkout.preload(context.applicationContext)
        } catch (e: Exception) {
            android.util.Log.e("PaymentScreen", "Razorpay preload failed", e)
        }
    }

    // Listen for Payment Results from MainActivity
    LaunchedEffect(Unit) {
        paymentViewModel.paymentResult.collectLatest { result ->
            when (result) {
                is PaymentResult.Success -> {
                    isPaying = true
                    try {
                        val response = RetrofitClient.apiService.completePayment(bookingId, mapOf("amount" to currentTotal.toString()))
                        if (response.isSuccessful) {
                            isPaymentDone = true
                            delay(1500)
                            onPaymentSuccess(bookingId, booking?.providerUid ?: "")
                        } else {
                            onPaymentSuccess(bookingId, booking?.providerUid ?: "")
                        }
                    } catch (e: Exception) {
                        onPaymentSuccess(bookingId, booking?.providerUid ?: "")
                    } finally {
                        isPaying = false
                    }
                }
                is PaymentResult.Error -> {
                    isPaying = false
                    Toast.makeText(context, "Payment Cancelled/Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(bookingId) {
        try {
            val response = RetrofitClient.apiService.getBookingDetails(bookingId)
            if (response.isSuccessful) {
                val b = response.body()
                booking = b
                if (!isCouponApplied) {
                    currentTotal = b?.totalAmount ?: 0.0
                }
                if (b?.paymentStatus == "paid") {
                    onPaymentSuccess(bookingId, b.providerUid)
                }
            }
        } catch (e: Exception) {}
        isLoadingBooking = false
    }

    fun startRazorpay() {
        val activity = context as? Activity ?: return
        
        scope.launch {
            isPaying = true
            try {
                // 1. Create Order on Backend
                val orderResponse = RetrofitClient.apiService.createRazorpayOrder(
                    mapOf(
                        "amount" to currentTotal,
                        "bookingId" to bookingId
                    )
                )

                if (orderResponse.isSuccessful && orderResponse.body() != null) {
                    val order = orderResponse.body()!!
                    val checkout = Checkout()
                    checkout.setKeyID(order.keyId)

                    val options = JSONObject()
                    options.put("name", "Madadwala")
                    options.put("description", "Booking Payment: ${booking?.serviceName}")
                    options.put("theme.color", "#1E5631")
                    options.put("currency", order.currency)
                    options.put("amount", order.amount)
                    options.put("order_id", order.id) // IMPORTANT: Use order_id from backend
                    
                    val prefill = JSONObject()
                    prefill.put("name", booking?.customerName ?: "Customer")
                    prefill.put("email", FirebaseAuth.getInstance().currentUser?.email ?: "support@madadwala.com")
                    prefill.put("contact", FirebaseAuth.getInstance().currentUser?.phoneNumber ?: "9879338393")
                    options.put("prefill", prefill)

                    options.put("send_sms_hash", true)
                    
                    val retryObj = JSONObject()
                    retryObj.put("enabled", true)
                    retryObj.put("max_count", 4)
                    options.put("retry", retryObj)


                    try {
                        checkout.open(activity, options)
                    } catch (e: Exception) {
                        val msg = e.message ?: ""
                        if (msg.contains("webview", ignoreCase = true) || msg.contains("package", ignoreCase = true)) {
                            Toast.makeText(context, "WebView Error: Please update 'Android System WebView' and 'Google Chrome' from Play Store.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Error opening payment: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                        isPaying = false
                    }
                } else {
                    Toast.makeText(context, "Failed to create payment order", Toast.LENGTH_SHORT).show()
                    isPaying = false
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                isPaying = false
            }
        }
    }

    fun payUsingWallet() {
        if (walletBalance < currentTotal) {
            Toast.makeText(context, "Insufficient wallet balance", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            isPaying = true
            try {
                val response = RetrofitClient.apiService.payFromWallet(
                    bookingId,
                    mapOf(
                        "customerUid" to (FirebaseAuth.getInstance().currentUser?.uid ?: ""),
                        "amount" to currentTotal.toString()
                    )
                )
                if (response.isSuccessful) {
                    isPaymentDone = true
                    delay(1500)
                    onPaymentSuccess(bookingId, booking?.providerUid ?: "")
                } else {
                    Toast.makeText(context, "Wallet payment failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isPaying = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment", fontWeight = FontWeight.Bold, color = MadadwalaColors.Ink) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MadadwalaColors.Ink)
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = MadadwalaColors.Green,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Secure Checkout",
                            style = MaterialTheme.typography.labelSmall,
                            color = MadadwalaColors.Green,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (isPaymentDone) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color(0xFFE8F5E9), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MadadwalaColors.Green,
                        modifier = Modifier.size(64.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Payment Successful!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MadadwalaColors.Ink
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Finalizing your booking...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MadadwalaColors.Gray
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF9F9F9))
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ListAlt,
                                        contentDescription = null,
                                        tint = MadadwalaColors.Green,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Order Summary",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MadadwalaColors.Green
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            if (isLoadingBooking) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = MadadwalaColors.Green)
                            } else {
                                val total = booking?.totalAmount ?: 0.0
                                val baseCharge = total / 1.05
                                val platformFee = total - baseCharge
                                var couponMessage by remember { mutableStateOf<String?>(null) }
                                var isValidatingCoupon by remember { mutableStateOf(false) }

                                BillRowV2("Base Charge", "₹${String.format("%.2f", baseCharge)}")
                                BillRowV2("Extra Work", "₹0.00")
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Platform Fee (5%)", style = MaterialTheme.typography.bodyMedium, color = MadadwalaColors.Gray)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Info, contentDescription = null, tint = MadadwalaColors.Gray, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text("₹${String.format("%.2f", platformFee)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                                
                                if (isCouponApplied) {
                                    BillRowV2("Discount", "-₹${String.format("%.2f", couponDiscount)}", color = MadadwalaColors.Green)
                                } else {
                                    BillRowV2("Discount", "-₹0.00", color = MadadwalaColors.Green)
                                }
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.5f))

                                // Coupon Section
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = couponCode,
                                        onValueChange = { couponCode = it.uppercase() },
                                        placeholder = { Text("Enter Coupon Code", fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true,
                                        enabled = !isCouponApplied && !isValidatingCoupon,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MadadwalaColors.Green,
                                            unfocusedBorderColor = Color.LightGray
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (isCouponApplied) {
                                                isCouponApplied = false
                                                couponDiscount = 0.0
                                                currentTotal = total
                                                couponCode = ""
                                                couponMessage = null
                                            } else {
                                                scope.launch {
                                                    isValidatingCoupon = true
                                                    try {
                                                        val res = RetrofitClient.apiService.validateCoupon(mapOf(
                                                            "code" to couponCode,
                                                            "amount" to total
                                                        ))
                                                        if (res.isSuccessful && res.body()?.valid == true) {
                                                            val data = res.body()!!
                                                            val discount = data.discountAmount ?: 0.0
                                                            if (discount > 0) {
                                                                couponDiscount = discount
                                                                currentTotal = data.finalAmount ?: total
                                                                isCouponApplied = true
                                                                couponMessage = data.message
                                                            } else {
                                                                couponMessage = "Coupon not valid"
                                                                isCouponApplied = false
                                                                currentTotal = total
                                                            }
                                                        } else {
                                                            couponMessage = res.body()?.message ?: "Coupon not valid"
                                                            isCouponApplied = false
                                                            currentTotal = total
                                                        }
                                                    } catch (e: Exception) {
                                                        couponMessage = "Error validating coupon"
                                                    } finally {
                                                        isValidatingCoupon = false
                                                    }
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isCouponApplied) MadadwalaColors.Red else MadadwalaColors.Green
                                        ),
                                        enabled = (couponCode.isNotBlank() || isCouponApplied) && !isValidatingCoupon
                                    ) {
                                        if (isValidatingCoupon) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                        } else {
                                            Text(if (isCouponApplied) "Remove" else "Apply", fontSize = 12.sp)
                                        }
                                    }
                                }
                                
                                if (couponMessage != null) {
                                    Text(
                                        text = couponMessage!!,
                                        color = if (isCouponApplied) MadadwalaColors.Green else MadadwalaColors.Red,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Total Amount",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MadadwalaColors.Ink
                                    )
                                    Text(
                                        "₹${String.format("%.2f", currentTotal)}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MadadwalaColors.Green
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFFF1F8E9),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            tint = MadadwalaColors.Green,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "You won't be charged until the service is completed",
                                            fontSize = 11.sp,
                                            color = MadadwalaColors.Green,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        "Select Payment Method",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MadadwalaColors.Ink
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    PaymentMethodItem(
                        title = "Online Payment",
                        subtitle = "UPI, Cards, NetBanking",
                        icon = Icons.Default.QrCode,
                        isSelected = selectedMethod == "UPI",
                        logos = true
                    ) { selectedMethod = "UPI" }
                    
                    PaymentMethodItem(
                        title = "Wallet",
                        subtitle = "Available Balance: ₹${String.format("%.2f", walletBalance)}",
                        icon = Icons.Default.AccountBalanceWallet,
                        isSelected = selectedMethod == "Wallet"
                    ) { selectedMethod = "Wallet" }

                    PaymentMethodItem(
                        title = "Cash",
                        subtitle = "Pay after service",
                        icon = Icons.Default.Payments,
                        isSelected = selectedMethod == "Cash"
                    ) { selectedMethod = "Cash" }

                    Spacer(modifier = Modifier.height(24.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFFF9E6),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFFFEEB3), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = MadadwalaColors.Amber,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Safe & Secure Payments",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF856404)
                                )
                                Text(
                                    "Your payment details are encrypted and 100% secure",
                                    fontSize = 12.sp,
                                    color = Color(0xFF856404).copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // Bottom Button
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = Color.White
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        PrimaryButton(
                            text = if (selectedMethod == "Cash") "Confirm Cash Payment" else "Pay ₹${String.format("%.2f", currentTotal)}",
                            onClick = {
                                when (selectedMethod) {
                                    "UPI" -> startRazorpay()
                                    "Wallet" -> payUsingWallet()
                                    "Cash" -> {
                                        scope.launch {
                                            isPaying = true
                                            try {
                                                val response = RetrofitClient.apiService.completePayment(bookingId, mapOf("amount" to currentTotal.toString()))
                                                if (response.isSuccessful) {
                                                    isPaymentDone = true
                                                    delay(1500)
                                                    onPaymentSuccess(bookingId, booking?.providerUid ?: "")
                                                } else {
                                                    Toast.makeText(context, "Failed to confirm payment", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                            } finally {
                                                isPaying = false
                                            }
                                        }
                                    }
                                }
                            },
                            isLoading = isPaying,
                            leadingIcon = {
                                Icon(
                                    if (selectedMethod == "Cash") Icons.Default.CheckCircle else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            trailingIcon = if (selectedMethod == "UPI") {
                                {
                                    Text(
                                        "via Razorpay",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BillRowV2(label: String, value: String, color: Color = MadadwalaColors.Ink) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MadadwalaColors.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun PaymentMethodItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    logos: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MadadwalaColors.Green) else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MadadwalaColors.Gray)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MadadwalaColors.Ink)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MadadwalaColors.Gray)
                if (logos && isSelected) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        PaymentLogo("UPI")
                        Spacer(modifier = Modifier.width(4.dp))
                        PaymentLogo("VISA")
                        Spacer(modifier = Modifier.width(4.dp))
                        PaymentLogo("MC")
                        Spacer(modifier = Modifier.width(4.dp))
                        PaymentLogo("Rupay")
                        Spacer(modifier = Modifier.width(4.dp))
                        PaymentLogo("Paytm")
                    }
                }
            }
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = MadadwalaColors.Green)
            )
        }
    }
}

@Composable
fun PaymentLogo(text: String) {
    Box(
        modifier = Modifier
            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
    }
}
