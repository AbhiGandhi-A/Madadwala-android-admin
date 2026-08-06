package com.abhi.madadwala_1.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abhi.madadwala_1.data.remote.BookingResponse
import com.abhi.madadwala_1.data.remote.RetrofitClient
import com.abhi.madadwala_1.ui.components.PrimaryButton
import com.abhi.madadwala_1.ui.theme.MadadwalaColors
import kotlinx.coroutines.launch
import com.razorpay.Checkout
import org.json.JSONObject
import android.app.Activity

import com.abhi.madadwala_1.ui.viewmodel.PaymentViewModel
import com.abhi.madadwala_1.ui.viewmodel.PaymentResult
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    bookingId: String,
    onBack: () -> Unit,
    onPaymentSuccess: (String, String) -> Unit,
    paymentViewModel: PaymentViewModel = viewModel()
) {
    var selectedMethod by remember { mutableStateOf("UPI") }
    var isPaying by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var booking by remember { mutableStateOf<BookingResponse?>(null) }
    var isLoadingBooking by remember { mutableStateOf(true) }
    var isPaymentDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Checkout.preload(context.applicationContext)
    }

    // Listen for Payment Results from MainActivity
    LaunchedEffect(Unit) {
        paymentViewModel.paymentResult.collectLatest { result ->
            when (result) {
                is PaymentResult.Success -> {
                    isPaying = true
                    try {
                        val response = RetrofitClient.apiService.completePayment(bookingId)
                        if (response.isSuccessful) {
                            isPaymentDone = true
                            kotlinx.coroutines.delay(1500)
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
                        "amount" to (booking?.totalAmount ?: 0.0),
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
                    prefill.put("email", "support@madadwala.com")
                    prefill.put("contact", "9879338393")
                    options.put("prefill", prefill)

                    val upi = JSONObject()
                    upi.put("allow_grouped", true)
                    options.put("upi", upi)

                    val config = JSONObject()
                    val display = JSONObject()
                    val preferences = JSONObject()
                    preferences.put("show_default_blocks", true)
                    display.put("preferences", preferences)
                    config.put("display", display)
                    options.put("config", config)

                    checkout.open(activity, options)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isPaymentDone) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("✅", style = MaterialTheme.typography.displayLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Payment Successful!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Finalizing your booking...", style = MaterialTheme.typography.bodyMedium, color = MadadwalaColors.Gray)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)
            ) {
                Text("Order Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isLoadingBooking) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = MadadwalaColors.Teal)
                } else {
                    val total = booking?.totalAmount ?: 0.0
                    BillRow("Base Charge", "₹${String.format("%.2f", total * 0.9)}")
                    BillRow("Extra Work", "₹0.00")
                    BillRow("Taxes & Fees", "₹${String.format("%.2f", total * 0.1)}")
                    BillRow("Discount", "-₹0.00", color = MadadwalaColors.Green)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    BillRow("Total Amount", "₹${String.format("%.2f", total)}", isTotal = true)
                }

                Spacer(modifier = Modifier.height(48.dp))
                
                Text("Select Payment Method", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                PaymentMethodItem("Online Payment", "UPI, Cards, NetBanking", Icons.Default.QrCode, selectedMethod == "UPI") { selectedMethod = "UPI" }
                PaymentMethodItem("Cash", "Pay after service", Icons.Default.Money, selectedMethod == "Cash") { selectedMethod = "Cash" }

                Spacer(modifier = Modifier.weight(1f))

                PrimaryButton(
                    text = if (selectedMethod == "Cash") "Confirm Cash Payment" else "Pay via Razorpay",
                    onClick = {
                        if (selectedMethod == "UPI") {
                            startRazorpay()
                        } else {
                            scope.launch {
                                isPaying = true
                                try {
                                    val response = RetrofitClient.apiService.completePayment(bookingId)
                                    if (response.isSuccessful) {
                                        isPaymentDone = true
                                        kotlinx.coroutines.delay(1500)
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
                    },
                    isLoading = isPaying
                )
            }
        }
    }
}

@Composable
fun BillRow(label: String, value: String, color: Color = MadadwalaColors.Ink, isTotal: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (isTotal) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge, fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal)
        Text(value, style = if (isTotal) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge, fontWeight = if (isTotal) FontWeight.Black else FontWeight.Bold, color = color)
    }
}

@Composable
fun PaymentMethodItem(title: String, subtitle: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MadadwalaColors.Teal.copy(alpha = 0.1f) else MadadwalaColors.Cream,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MadadwalaColors.Teal) else null
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(MadadwalaColors.Teal.copy(alpha = 0.1f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MadadwalaColors.Teal)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MadadwalaColors.Gray)
            }
            RadioButton(selected = isSelected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = MadadwalaColors.Teal))
        }
    }
}
