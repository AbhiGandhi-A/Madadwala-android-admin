package com.abhi.madadwala_1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.abhi.madadwala_1.data.remote.BookingResponse
import com.abhi.madadwala_1.data.remote.ProviderDetailResponse
import com.abhi.madadwala_1.data.remote.RetrofitClient
import com.abhi.madadwala_1.ui.theme.MadadwalaColors
import com.abhi.madadwala_1.ui.viewmodel.CallViewModel
import com.abhi.madadwala_1.utils.NotificationHelper
import kotlinx.coroutines.launch
import com.airbnb.lottie.compose.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BookingConfirmationScreen(
    bookingId: String,
    onTrackLive: (String) -> Unit,
    onHome: () -> Unit,
    callViewModel: CallViewModel = viewModel(
        viewModelStoreOwner = androidx.compose.ui.platform.LocalContext.current as androidx.activity.ComponentActivity
    )
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
                    
                    // Fetch provider details to show real name, image, and rating
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
            ConfirmedBookingUI(
                booking = booking!!,
                providerDetail = providerDetail,
                onTrackLive = { onTrackLive(bookingId) }, 
                onHome = onHome,
                onCall = {
                    booking?.let { b ->
                        callViewModel.startCall(
                            bookingId = bookingId,
                            customerId = b.customerUid,
                            partnerId = b.providerUid,
                            partnerName = b.providerName ?: "Partner"
                        )
                    }
                },
                onCancel = {
                    scope.launch {
                        val res = RetrofitClient.apiService.cancelBooking(bookingId, mapOf("cancelledBy" to "customer"))
                        if (res.isSuccessful) {
                            onHome()
                        }
                    }
                },
                onReschedule = { newTime ->
                    scope.launch {
                        val res = RetrofitClient.apiService.rescheduleBooking(bookingId, mapOf("scheduledTime" to newTime))
                        if (res.isSuccessful) {
                            val response = RetrofitClient.apiService.getBookingDetails(bookingId)
                            if (response.isSuccessful) booking = response.body()
                        }
                    }
                }
            )
        } else {
            // Error state
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
fun ConfirmedBookingUI(
    booking: BookingResponse,
    providerDetail: ProviderDetailResponse?,
    onTrackLive: () -> Unit,
    onHome: () -> Unit,
    onCall: () -> Unit,
    onCancel: () -> Unit,
    onReschedule: (String) -> Unit
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    var showRescheduleDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Success Checkmark (Lottie)
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(com.abhi.madadwala_1.R.raw.upload_complete))
            val progress by animateLottieCompositionAsState(
                composition,
                iterations = 1
            )

            Box(
                modifier = Modifier.size(150.dp),
                contentAlignment = Alignment.Center
            ) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(140.dp)
                )
            }

            Text(
                text = "Booking Confirmed!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MadadwalaColors.Green
            )
            Text(
                text = "Your service provider is assigned and will reach you soon.",
                textAlign = TextAlign.Center,
                color = MadadwalaColors.Gray,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Booking ID Badge
            Surface(
                color = Color(0xFFE8F5EC),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Booking ID: #${booking._id.takeLast(7).uppercase()}",
                    color = Color(0xFF2E7D4F),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
        }

        // Provider Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (providerDetail?.provider?.profileImage?.isNotEmpty() == true) {
                        AsyncImage(
                            model = providerDetail.provider.profileImage,
                            contentDescription = null,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDFF3E5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, tint = Color(0xFF2E7D4F), modifier = Modifier.size(25.dp), contentDescription = null)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = providerDetail?.provider?.name ?: booking.providerName ?: "Partner",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1E1E1E)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB400), modifier = Modifier.size(12.dp))
                            Text(
                                text = " ${providerDetail?.provider?.rating ?: 4.0} (${providerDetail?.provider?.reviewCount ?: 1} reviews)",
                                fontSize = 12.sp,
                                color = Color(0xFF757575)
                            )
                        }
                        Surface(
                            color = Color(0xFFE8F5EC),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF2E7D4F), modifier = Modifier.size(10.dp))
                                Text(" Verified Provider", color = Color(0xFF2E7D4F), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    IconButton(
                        onClick = onCall,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFF3FAF6), CircleShape)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF2E7D4F), modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color(0xFFF0F0F0))
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    BookingInfoItem(Modifier.weight(1f), Icons.Default.Security, "Service", booking.serviceName)
                    BookingInfoItem(Modifier.weight(1f), Icons.Default.CalendarToday, "Date", formatDate(booking.scheduledTime))
                    BookingInfoItem(Modifier.weight(1f), Icons.Default.AccessTime, "Time", formatTime(booking.scheduledTime))
                    BookingInfoItem(Modifier.weight(1f), Icons.Default.LocationOn, "Address", booking.address.split(",").firstOrNull() ?: "Location")
                }
            }
        }

        // What's Next & Track
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3FAF6)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(32.dp).background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFF2E7D4F), modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("What's Next?", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D4F), fontSize = 13.sp)
                        Text(
                            "${booking.providerName ?: "Partner"} has been notified.",
                            fontSize = 11.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }
            }

            Button(
                onClick = onTrackLive,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D4F)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Track Live Status", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.White)
                }
            }
        }

        // Footer Badges
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            FooterBadge(Icons.Default.Security, "Safe & Secure", "Verified pros")
            FooterBadge(Icons.Default.ThumbUp, "Quality Service", "Skilled pros")
            FooterBadge(Icons.Default.HeadsetMic, "24/7 Support", "We help you")
        }

        TextButton(onClick = onHome, modifier = Modifier.height(36.dp)) {
            Text("Back to Home", color = MadadwalaColors.Green, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { showRescheduleDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reschedule", color = Color.Gray)
            }
            OutlinedButton(
                onClick = { showCancelDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Red)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel", color = Color.Red)
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Booking?", fontWeight = FontWeight.Bold) },
            text = { 
                Column {
                    Text("Are you sure you want to cancel this booking?")
                    if (booking.paymentStatus == "paid") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Since you've already paid, the full amount (₹${booking.totalAmount}) will be refunded to your wallet instantly.",
                            color = Color(0xFF2E7D4F),
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        onCancel()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Confirm Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("No, keep it")
                }
            }
        )
    }

    if (showRescheduleDialog) {
        RescheduleDialog(
            currentSlot = booking.scheduledTime,
            onDismiss = { showRescheduleDialog = false },
            onConfirm = { newTime ->
                showRescheduleDialog = false
                onReschedule(newTime)
            }
        )
    }
}

@Composable
fun RescheduleDialog(
    currentSlot: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val calendar = Calendar.getInstance()
    val todayDate = calendar.time
    
    val dates = (0..6).map { 
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, it)
        cal.time
    }
    
    val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.ENGLISH)
    val dayNumFormat = SimpleDateFormat("dd", Locale.ENGLISH)
    val dayNameFormat = SimpleDateFormat("EEE", Locale.ENGLISH)

    var selectedDate by remember { mutableStateOf(todayDate) }
    var selectedTime by remember { mutableStateOf("09:00 AM") }

    val timeSlots = listOf(
        "09:00 AM", "10:00 AM", "11:00 AM",
        "12:00 PM", "01:00 PM", "02:00 PM",
        "03:00 PM", "04:00 PM", "05:00 PM",
        "06:00 PM", "07:00 PM", "08:00 PM"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reschedule Service", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text("Select a new date and time", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(dates) { date ->
                        val isSelected = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).format(date) == 
                                       SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).format(selectedDate)
                        
                        Surface(
                            onClick = { selectedDate = date },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF2E7D4F) else Color.White,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                            modifier = Modifier.size(width = 50.dp, height = 60.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(dayNumFormat.format(date), fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else Color.Black)
                                Text(dayNameFormat.format(date), fontSize = 10.sp, color = if (isSelected) Color.White else Color.Gray)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Available Slots", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    timeSlots.chunked(3).forEach { rowSlots ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowSlots.forEach { slot ->
                                val isSelected = selectedTime == slot
                                Surface(
                                    onClick = { selectedTime = slot },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF2E7D4F).copy(alpha = 0.1f) else Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF2E7D4F) else Color.LightGray)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(slot, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) Color(0xFF2E7D4F) else Color.Black)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm("${dateFormat.format(selectedDate)} | $selectedTime")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D4F))
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
fun BookingInfoItem(modifier: Modifier, icon: ImageVector, label: String, value: String) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(32.dp).background(Color(0xFFF3FAF6), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF2E7D4F), modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, color = Color(0xFF757575))
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E1E), textAlign = TextAlign.Center)
    }
}

@Composable
fun FooterBadge(icon: ImageVector, title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(40.dp).background(Color(0xFFF3FAF6), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF2E7D4F), modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D4F))
        Text(subtitle, fontSize = 10.sp, color = Color(0xFF757575), textAlign = TextAlign.Center, lineHeight = 12.sp)
    }
}

fun formatDate(dateStr: String): String {
    if (dateStr.contains("|")) {
        return dateStr.split("|")[0].trim()
    }
    if (dateStr.equals("ASAP", ignoreCase = true) || dateStr.equals("Urgent", ignoreCase = true)) return "Urgent"
    
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(dateStr)
        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        date?.let { outputFormat.format(it) } ?: dateStr
    } catch (e: Exception) {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(dateStr)
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
            date?.let { outputFormat.format(it) } ?: dateStr
        } catch (e2: Exception) {
            dateStr.split("T").firstOrNull() ?: "02 Aug 2025"
        }
    }
}

fun formatTime(dateStr: String): String {
    if (dateStr.contains("|")) {
        return dateStr.split("|").last().trim()
    }
    if (dateStr.equals("ASAP", ignoreCase = true) || dateStr.equals("Urgent", ignoreCase = true)) return "Urgent"

    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(dateStr)
        val outputFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
        date?.let { outputFormat.format(it) } ?: "04:00 PM"
    } catch (e: Exception) {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(dateStr)
            val outputFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
            date?.let { outputFormat.format(it) } ?: "04:00 PM"
        } catch (e2: Exception) {
            "04:00 PM"
        }
    }
}
