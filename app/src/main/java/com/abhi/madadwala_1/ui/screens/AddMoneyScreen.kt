package com.abhi.madadwala_1.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhi.madadwala_1.data.remote.RetrofitClient
import com.abhi.madadwala_1.ui.theme.MadadwalaColors
import com.abhi.madadwala_1.ui.viewmodel.PaymentResult
import com.abhi.madadwala_1.ui.viewmodel.PaymentViewModel
import com.google.firebase.auth.FirebaseAuth
import com.razorpay.Checkout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMoneyScreen(
    onBack: () -> Unit,
    paymentViewModel: PaymentViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )
) {
    var amount by remember { mutableStateOf("") }
    var isPaying by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()

    val quickAmounts = listOf("100", "200", "500", "1000", "2000", "5000")

    LaunchedEffect(Unit) {
        try {
            Checkout.preload(context.applicationContext)
        } catch (e: Exception) {
            android.util.Log.e("AddMoneyScreen", "Razorpay preload failed", e)
        }
    }

    LaunchedEffect(Unit) {
        paymentViewModel.paymentResult.collectLatest { result ->
            if (!isPaying) return@collectLatest
            
            when (result) {
                is PaymentResult.Success -> {
                    try {
                        val response = RetrofitClient.apiService.verifyWalletPayment(
                            mapOf(
                                "razorpay_order_id" to (result.orderId ?: ""),
                                "razorpay_payment_id" to (result.paymentId ?: ""),
                                "razorpay_signature" to (result.signature ?: ""),
                                "uid" to (auth.currentUser?.uid ?: ""),
                                "amount" to amount
                            )
                        )

                        if (response.isSuccessful) {
                            Toast.makeText(context, "Wallet Updated Successfully!", Toast.LENGTH_LONG).show()
                            onBack()
                        } else {
                            Toast.makeText(context, "Verification Failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isPaying = false
                    }
                }
                is PaymentResult.Error -> {
                    isPaying = false
                    val errorMsg = if (result.code == 0) "Payment Cancelled" else (result.message ?: "Payment Failed")
                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun startRazorpay() {
        val activity = context as? Activity ?: return
        val amountDouble = amount.toDoubleOrNull() ?: return
        
        if (amountDouble < 10.0) {
            Toast.makeText(context, "Minimum amount is ₹10", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            isPaying = true
            try {
                val amountDouble = amount.toDoubleOrNull() ?: 0.0
                val response = RetrofitClient.apiService.createRazorpayOrder(
                    mapOf(
                        "amount" to amountDouble,
                        "type" to "wallet",
                        "uid" to (auth.currentUser?.uid ?: "")
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val order = response.body()!!
                    val checkout = Checkout()
                    checkout.setKeyID(order.keyId)

                    val options = JSONObject()
                    options.put("name", "Madadwala")
                    options.put("description", "Wallet Top-up")
                    options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.png")
                    options.put("theme.color", "#1E5631")
                    options.put("currency", order.currency)
                    options.put("amount", order.amount)
                    options.put("order_id", order.id)
                    
                    val prefill = JSONObject()
                    prefill.put("name", auth.currentUser?.displayName ?: "Customer")
                    prefill.put("contact", auth.currentUser?.phoneNumber ?: "")
                    prefill.put("email", auth.currentUser?.email ?: "support@madadwala.com")
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
                    Toast.makeText(context, "Failed to initiate payment", Toast.LENGTH_SHORT).show()
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
                title = { Text("Add Money", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = MadadwalaColors.Green,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Enter amount to add",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.length <= 6) amount = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isPaying,
                placeholder = { Text("₹0.00") },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                prefix = { Text("₹", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MadadwalaColors.Green,
                    unfocusedBorderColor = MadadwalaColors.LightGray
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(quickAmounts) { qAmount ->
                    Surface(
                        modifier = Modifier
                            .clickable(enabled = !isPaying) { amount = qAmount }
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (amount == qAmount) MadadwalaColors.Green else Color.Transparent,
                        border = if (amount == qAmount) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "₹$qAmount",
                                fontWeight = FontWeight.Bold,
                                color = if (amount == qAmount) Color.White else Color.Black
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = { startRazorpay() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Green),
                enabled = amount.isNotBlank() && !isPaying
            ) {
                if (isPaying) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Proceed to Pay", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}
