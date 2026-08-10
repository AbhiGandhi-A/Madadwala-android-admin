package com.abhi.madadwala_1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.abhi.madadwala_1.data.remote.BookingResponse
import com.abhi.madadwala_1.data.remote.ProviderDetailResponse
import com.abhi.madadwala_1.data.remote.RetrofitClient
import com.abhi.madadwala_1.ui.theme.MadadwalaColors
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.launch

@Composable
fun WorkCompletedScreen(
    bookingId: String,
    onHome: () -> Unit,
    onRate: (String, String) -> Unit,
    onInvoice: (BookingResponse) -> Unit
) {
    var booking by remember { mutableStateOf<BookingResponse?>(null) }
    var providerDetail by remember { mutableStateOf<ProviderDetailResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(bookingId) {
        scope.launch {
            try {
                val response = RetrofitClient.apiService.getBookingDetails(bookingId)
                if (response.isSuccessful) {
                    val b = response.body()
                    booking = b
                    b?.providerUid?.let { uid ->
                        val pResponse = RetrofitClient.apiService.getProviderDetails(uid)
                        if (pResponse.isSuccessful) {
                            providerDetail = pResponse.body()
                        }
                    }
                }
            } catch (e: Exception) {
            } finally {
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF7FCF9))) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF2E7D4F))
            }
        } else if (booking != null) {
            WorkCompletedUI(
                booking = booking!!,
                providerDetail = providerDetail,
                onHome = onHome,
                onRate = { onRate(bookingId, booking!!.providerUid) },
                onInvoice = { onInvoice(booking!!) }
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Failed to load booking details")
                Button(onClick = onHome) { Text("Back to Home") }
            }
        }
    }
}

@Composable
fun WorkCompletedUI(
    booking: BookingResponse,
    providerDetail: ProviderDetailResponse?,
    onHome: () -> Unit,
    onRate: () -> Unit,
    onInvoice: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        // Success Animation
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(com.abhi.madadwala_1.R.raw.upload_complete))
        LottieAnimation(
            composition = composition,
            iterations = 1,
            modifier = Modifier.size(160.dp)
        )

        Text(
            text = "Work Completed!",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MadadwalaColors.Green
        )
        
        Text(
            text = "Your service for ${booking.serviceName} has been successfully completed.",
            textAlign = TextAlign.Center,
            color = Color.Gray,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        // Booking Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (providerDetail?.provider?.profileImage?.isNotEmpty() == true) {
                        AsyncImage(
                            model = providerDetail.provider.profileImage,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFE8F5E9)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, tint = MadadwalaColors.Green, modifier = Modifier.size(30.dp), contentDescription = null)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(providerDetail?.provider?.name ?: booking.providerName ?: "Partner", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(booking.serviceName, color = Color.Gray, fontSize = 14.sp)
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFF0F0F0))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Total Amount Paid", color = Color.Gray, fontSize = 12.sp)
                        Text("₹${booking.totalAmount}", fontWeight = FontWeight.Black, fontSize = 20.sp, color = MadadwalaColors.Green)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Status", color = Color.Gray, fontSize = 12.sp)
                        Surface(color = Color(0xFFE3F2FD), shape = RoundedCornerShape(8.dp)) {
                            Text("JOB DONE", color = Color(0xFF2196F3), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Actions
        Button(
            onClick = onRate,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Green),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Star, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Rate & Review Partner", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onInvoice,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MadadwalaColors.Green)
        ) {
            Icon(Icons.Default.Download, contentDescription = null, tint = MadadwalaColors.Green)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Download Invoice", color = MadadwalaColors.Green, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onHome) {
            Text("Back to Home", color = Color.Gray, fontWeight = FontWeight.Medium)
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}
