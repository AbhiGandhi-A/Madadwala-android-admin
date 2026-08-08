package com.abhi.madadwala_1.ui.screens

import android.location.Location
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.abhi.madadwala_1.data.remote.BookingResponse
import com.abhi.madadwala_1.data.remote.ProviderResponse
import com.abhi.madadwala_1.data.remote.RetrofitClient
import com.abhi.madadwala_1.ui.theme.MadadwalaColors
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.abhi.madadwala_1.services.SocketHandler
import io.socket.client.Socket
import org.json.JSONObject
import com.abhi.madadwala_1.utils.PolylineDecoder
import com.abhi.madadwala_1.utils.NotificationHelper

import com.abhi.madadwala_1.ui.viewmodel.CallViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTrackingScreen(
    bookingId: String,
    onBack: () -> Unit,
    onComplete: (String) -> Unit,
    onPayNow: (String) -> Unit,
    callViewModel: CallViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity
    )
) {
    val context = LocalContext.current
    var booking by remember { mutableStateOf<BookingResponse?>(null) }
    var provider by remember { mutableStateOf<ProviderResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var durationText by remember { mutableStateOf("Partner has not started the ride") }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var partnerLocation by remember { mutableStateOf<LatLng?>(null) }
    var lastKnownOtp by remember { mutableStateOf<String?>(null) }
    var showDetailsSheet by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(21.6264, 73.0152), 15f)
    }

    // Socket Setup
    LaunchedEffect(bookingId) {
        SocketHandler.setSocket("https://madadwala-backend.onrender.com")
        SocketHandler.establishConnection()
        val socket = SocketHandler.getSocket()
        
        socket?.on(io.socket.client.Socket.EVENT_CONNECT) {
            socket.emit("join_booking", bookingId)
        }
        
        if (socket?.connected() == true) {
            socket.emit("join_booking", bookingId)
        }
        
        socket?.on("location_update") { args ->
            android.util.Log.d("Madadwala", "Socket location_update received")
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val lat = data.getDouble("lat")
                val lng = data.getDouble("lng")
                android.util.Log.d("Madadwala", "New Partner Location: $lat, $lng")
                val newLoc = LatLng(lat, lng)
                
                partnerLocation = newLoc
                
                if (!cameraPositionState.isMoving) {
                    scope.launch {
                        cameraPositionState.animate(
                            update = com.google.android.gms.maps.CameraUpdateFactory.newLatLng(newLoc),
                            durationMs = 1000
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(bookingId) {
        while (true) {
            try {
                // Use trackingApiService (Render) for most up-to-date location
                val response = RetrofitClient.trackingApiService.getBookingDetails(bookingId)
                if (response.isSuccessful) {
                    val b = response.body()
                    
                    // Show notification when provider has arrived
                    if (b?.otp != null && b.status == "arrived" && b.otp != lastKnownOtp) {
                        NotificationHelper(context).showNotification(
                            title = "Service OTP: ${b.otp}",
                            message = "Share this OTP with your partner for ${b.serviceName} only when they arrive at your location."
                        )
                        lastKnownOtp = b.otp
                    }

                    booking = b
                    
                    if (b?.status == "done") {
                        onComplete(bookingId)
                    }

                    b?.providerUid?.let { uid ->
                        if (provider == null) {
                            val pResponse = RetrofitClient.apiService.getProviderDetails(uid)
                            if (pResponse.isSuccessful) {
                                provider = pResponse.body()?.provider
                            }
                        }
                    }

                    val pLat = partnerLocation?.latitude ?: b?.providerLat
                    val pLng = partnerLocation?.longitude ?: b?.providerLng
                    
                    // Check for valid coordinates (not 0.0)
                    if (pLat != null && pLng != null && pLat != 0.0 && b?.customerLat != null && b?.customerLat != 0.0) {
                        val origin = "$pLat,$pLng"
                        val destination = "${b.customerLat},${b.customerLng}"
                        
                        val directionsResponse = try {
                            RetrofitClient.mapsApiService.getDirections(
                                origin = origin,
                                destination = destination,
                                apiKey = "AIzaSyCmBUpQgoSWPSXcC0DpxilJzJGTXJtke-8"
                            )
                        } catch (e: Exception) { 
                            android.util.Log.e("Madadwala", "Directions error", e)
                            null 
                        }

                        if (directionsResponse != null && directionsResponse.isSuccessful && directionsResponse.body()?.status == "OK") {
                            val route = directionsResponse.body()?.routes?.firstOrNull()
                            route?.let { r ->
                                routePoints = PolylineDecoder.decode(r.overview_polyline.points)
                                val leg = r.legs.firstOrNull()
                                durationText = leg?.duration?.text?.replace(" mins", " min")?.replace(" min", " min") ?: "12 min"
                            }
                        } else {
                            // Fallback to straight line distance
                            val results = FloatArray(1)
                            Location.distanceBetween(
                                pLat, pLng,
                                b.customerLat!!, b.customerLng!!,
                                results
                            )
                            val km = results[0] / 1000
                            durationText = String.format("%.1f km", km)
                        }

                        val providerLoc = LatLng(pLat, pLng)
                        if (cameraPositionState.position.target.latitude == 21.6264) {
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(providerLoc, 15f)
                        } else if (!cameraPositionState.isMoving) {
                            cameraPositionState.animate(
                                update = com.google.android.gms.maps.CameraUpdateFactory.newLatLng(providerLoc),
                                durationMs = 1000
                            )
                        }
                    }
                }
            } catch (e: Exception) {}
            isLoading = false
            delay(30000) // Poll every 30 seconds for persistence fallback
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Track Partner", fontWeight = FontWeight.Bold, color = MadadwalaColors.Ink) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MadadwalaColors.Ink)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Help */ }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Help", tint = MadadwalaColors.Ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        if (isLoading && booking == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MadadwalaColors.Green)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Partner Info Card
                    PartnerInfoCard(provider, bookingId, booking, callViewModel, onChatClick = { showChatSheet = true })

                    // Map View
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            uiSettings = MapUiSettings(
                                zoomControlsEnabled = false,
                                scrollGesturesEnabled = false,
                                tiltGesturesEnabled = false,
                                rotationGesturesEnabled = false
                            )
                        ) {
                            booking?.let { b ->
                                val customerLoc = LatLng(b.customerLat ?: 21.6264, b.customerLng ?: 73.0152)
                                val providerLoc = partnerLocation ?: if (b.providerLat != null && b.providerLng != null) LatLng(b.providerLat, b.providerLng) else null

                                Marker(
                                    state = MarkerState(position = customerLoc),
                                    title = "Your Location"
                                )

                                providerLoc?.let { loc ->
                                    Marker(
                                        state = MarkerState(position = loc),
                                        title = b.providerName ?: "Partner",
                                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                                    )

                                    if (routePoints.isNotEmpty()) {
                                        Polyline(
                                            points = routePoints,
                                            color = Color(0xFFF59E0B), // Orange to match theme
                                            width = 12f,
                                            jointType = JointType.ROUND
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Time overlay on map
                        Surface(
                            modifier = Modifier.align(Alignment.Center).padding(bottom = 40.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            shadowElevation = 4.dp,
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                val awaySuffix = if (durationText != "Partner has not started the ride") " away" else ""
                                Text(text = "$durationText$awaySuffix", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFF59E0B), textAlign = TextAlign.Center)
                            }
                        }
                    }

                    // Bottom Content
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Partner Status Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val isUnpaidDone = booking?.status == "done" && booking?.paymentStatus != "paid"
                                Text(
                                    text = when {
                                        isUnpaidDone -> "Service Completed"
                                        booking?.status == "accepted" -> "Partner Accepted Your Request"
                                        booking?.status == "on_the_way" -> "Partner is on the way"
                                        booking?.status == "arrived" -> "Partner Has Arrived"
                                        booking?.status == "in_progress" -> "Work In Progress"
                                        booking?.status == "done" -> "Job Completed"
                                        else -> "Connecting..."
                                    },
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (isUnpaidDone) "Please complete the payment to proceed" else "Your partner is getting ready to arrive",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MadadwalaColors.Gray
                                )
                            }
                            
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFF7ED), // Light orange background
                                modifier = Modifier.size(width = 80.dp, height = 70.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    val displayTime = remember(durationText) {
                                        if (durationText == "Partner has not started the ride") {
                                            "Not Started"
                                        } else if (durationText.contains("hour")) {
                                            val parts = durationText.split(" ")
                                            // Format "1 hour 27 min" -> "1.5" approx or just "1h 27m"
                                            val h = parts.getOrNull(0) ?: ""
                                            val m = parts.getOrNull(2) ?: ""
                                            "${h}h ${m}m"
                                        } else {
                                            durationText.split(" ")[0]
                                        }
                                    }
                                    
                                    val unitText = remember(durationText) {
                                        if (durationText.contains("hour")) ""
                                        else if (durationText.contains("min")) "min" 
                                        else if (durationText.contains("km")) "km" 
                                        else ""
                                    }

                                    Text(
                                        text = displayTime,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold, 
                                            color = Color(0xFFF59E0B),
                                            fontSize = if (displayTime.length > 3) 16.sp else 20.sp
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                    if (unitText.isNotEmpty()) {
                                        Text(
                                            text = unitText,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                            color = Color(0xFFF59E0B)
                                        )
                                    }
                                    if (durationText != "Partner has not started the ride") {
                                        Text(
                                            text = "away",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = Color(0xFFF59E0B)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Progress Bar
                        TrackingProgressBar(
                            currentStep = when(booking?.status) {
                                "accepted" -> 0
                                "on_the_way" -> 1
                                "arrived", "in_progress" -> 2
                                "done" -> 3
                                else -> 0
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Notification Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MadadwalaColors.Green),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "We'll notify you once your partner is nearby",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "You can contact your partner for any help.",
                                        fontSize = 12.sp,
                                        color = MadadwalaColors.Gray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Order Details
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                DetailItem(
                                    icon = Icons.Default.LocationOn,
                                    title = "Service Address",
                                    subtitle = booking?.address ?: "Address not available",
                                    iconColor = MadadwalaColors.Green,
                                    actionText = "View",
                                    onActionClick = {
                                        booking?.let { b ->
                                            if (b.customerLat != null && b.customerLng != null) {
                                                val uri = android.net.Uri.parse("geo:0,0?q=${b.customerLat},${b.customerLng}(Service Location)")
                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                                intent.setPackage("com.google.android.apps.maps")
                                                context.startActivity(intent)
                                            }
                                        }
                                    }
                                )
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF3F4F6))

                                DetailItem(
                                    icon = Icons.Default.Flag,
                                    title = "Service Type",
                                    subtitle = booking?.serviceName ?: "Service",
                                    iconColor = Color(0xFFF59E0B)
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF3F4F6))

                                val isPaymentPending = booking?.paymentStatus?.lowercase() == "pending"
                                DetailItem(
                                    icon = Icons.Default.Payments,
                                    title = "Order Value",
                                    subtitle = "₹${booking?.totalAmount ?: 0.0}",
                                    iconColor = MadadwalaColors.Green,
                                    extraInfo = if (isPaymentPending) "Pending" else "Paid",
                                    extraInfoColor = if (isPaymentPending) Color(0xFFF59E0B) else MadadwalaColors.Green,
                                    actionText = if (isPaymentPending) "Pay Now" else null,
                                    onActionClick = {
                                        if (isPaymentPending) {
                                            onPayNow(bookingId)
                                        }
                                    }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF3F4F6))

                                DetailItem(
                                    icon = Icons.Default.Receipt,
                                    title = "Order ID",
                                    subtitle = "#${booking?._id?.takeLast(10)?.uppercase() ?: "ID"}",
                                    iconColor = Color(0xFF3B82F6),
                                    actionText = "View Details",
                                    onActionClick = { showDetailsSheet = true }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Safety Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MadadwalaColors.Green),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Your safety is our priority",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MadadwalaColors.Green
                                    )
                                    Text(
                                        text = "All partners are background verified for your safety.",
                                        fontSize = 12.sp,
                                        color = MadadwalaColors.Gray
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(80.dp)) // Extra space for floating button
                    }
                }
            }
        }
    }

    if (showDetailsSheet && booking != null) {
        ModalBottomSheet(
            onDismissRequest = { showDetailsSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFE5E7EB)) }
        ) {
            OrderDetailsSheetContent(booking!!, onDismiss = { showDetailsSheet = false })
        }
    }

    if (showChatSheet) {
        com.abhi.madadwala_1.ui.components.BookingChatBottomSheet(
            bookingId = bookingId,
            onDismiss = { showChatSheet = false }
        )
    }
}

@Composable
fun OrderDetailsSheetContent(booking: BookingResponse, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Order Details",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MadadwalaColors.Ink
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        DetailSheetItem("Order ID", "#${booking._id.uppercase()}", Icons.Default.Fingerprint)
        DetailSheetItem("Service", booking.serviceName, Icons.Default.Build)
        DetailSheetItem("Scheduled For", booking.scheduledTime.replace("|", "at"), Icons.Default.Event)
        DetailSheetItem("Address", booking.address, Icons.Default.LocationOn)
        DetailSheetItem("Total Amount", "₹${booking.totalAmount}", Icons.Default.Payments)
        DetailSheetItem("Payment Status", booking.paymentStatus?.replaceFirstChar { it.uppercase() } ?: "Pending", Icons.Default.Info)
        
        if (!booking.partnerComment.isNullOrEmpty()) {
            DetailSheetItem("Notes", booking.partnerComment!!, Icons.Default.Notes)
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Green),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Close", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun DetailSheetItem(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFF3F4F6)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MadadwalaColors.Green, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, fontSize = 12.sp, color = MadadwalaColors.Gray)
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MadadwalaColors.Ink)
        }
    }
}

@Composable
fun PartnerInfoCard(
    provider: ProviderResponse?,
    bookingId: String,
    booking: BookingResponse?,
    callViewModel: CallViewModel,
    onChatClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Fallback logic for better data display
    val displayName = provider?.name ?: booking?.providerName ?: "Partner"
    val displayImage = provider?.profileImage ?: booking?.providerImage
    val displayRating = if (provider != null && provider.rating > 0.0) provider.rating else 4.8
    val displayJobs = if (provider != null && provider.totalJobs > 0) provider.totalJobs else (provider?.reviewCount ?: 15)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!displayImage.isNullOrEmpty()) {
                AsyncImage(
                    model = displayImage,
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MadadwalaColors.Green, modifier = Modifier.size(30.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$displayRating",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB400), modifier = Modifier.size(14.dp))
                    Text(
                        text = " ($displayJobs+ orders)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MadadwalaColors.Gray
                    )
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MadadwalaColors.Green, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Verified Partner", fontSize = 10.sp, color = MadadwalaColors.Green, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {
                            if (booking != null) {
                                callViewModel.startCall(
                                    bookingId = bookingId,
                                    customerId = booking.customerUid,
                                    partnerId = booking.providerUid,
                                    partnerName = booking.providerName ?: "Partner"
                                )
                            }
                        },
                        modifier = Modifier.size(40.dp).background(Color(0xFFF3F4F6), CircleShape)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Call", tint = MadadwalaColors.Green, modifier = Modifier.size(20.dp))
                    }
                    Text("Call", fontSize = 10.sp, color = MadadwalaColors.Gray)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onChatClick,
                        modifier = Modifier.size(40.dp).background(Color(0xFFF3F4F6), CircleShape)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = "Chat", tint = MadadwalaColors.Green, modifier = Modifier.size(20.dp))
                    }
                    Text("Chat", fontSize = 10.sp, color = MadadwalaColors.Gray)
                }
            }
        }
    }
}

@Composable
fun TrackingProgressBar(currentStep: Int) {
    val steps = listOf("Preparing", "On the way", "Arrived", "Completed")
    val icons = listOf(Icons.Default.ShoppingBag, Icons.Default.TwoWheeler, Icons.Default.LocationOn, Icons.Default.Check)

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        HorizontalDivider(
            modifier = Modifier.align(Alignment.CenterStart).padding(top = 20.dp, start = 20.dp, end = 20.dp),
            thickness = 2.dp,
            color = Color(0xFFE5E7EB)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEachIndexed { index, step ->
                val isActive = index <= currentStep
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isActive) MadadwalaColors.Green else Color(0xFFE5E7EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icons[index],
                            contentDescription = null,
                            tint = if (isActive) Color.White else Color(0xFF9CA3AF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = step,
                        fontSize = 10.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) MadadwalaColors.Green else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun DetailItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    extraInfo: String? = null,
    extraInfoColor: Color = MadadwalaColors.Green
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 12.sp, color = MadadwalaColors.Gray)
            Text(text = subtitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MadadwalaColors.Ink)
        }
        
        Column(horizontalAlignment = Alignment.End) {
            if (extraInfo != null) {
                Text(text = extraInfo, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = extraInfoColor)
            }
            if (actionText != null) {
                Row(
                    modifier = Modifier
                        .clickable(enabled = onActionClick != null) { onActionClick?.invoke() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = actionText, fontSize = 12.sp, color = MadadwalaColors.Green, fontWeight = FontWeight.Bold)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MadadwalaColors.Green, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
