package com.abhi.madadwala_1.ui.screens.provider

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.abhi.madadwala_1.ui.components.OtpBoxRow
import com.abhi.madadwala_1.ui.theme.MadadwalaColors
import com.abhi.madadwala_1.data.remote.RetrofitClient
import com.abhi.madadwala_1.data.remote.BookingResponse
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.content.Intent
import android.widget.Toast
import com.abhi.madadwala_1.services.LocationService
import com.abhi.madadwala_1.services.SocketHandler
import io.socket.client.Socket
import org.json.JSONObject
import com.abhi.madadwala_1.utils.PolylineDecoder
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

import com.abhi.madadwala_1.ui.viewmodel.CallViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

enum class JobStatus {
    IDLE, ON_THE_WAY, REACHED, OTP_VERIFICATION, OTP_VERIFIED, IN_PROGRESS, COMPLETED
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderActiveJobScreen(
    bookingId: String,
    onBack: () -> Unit,
    callViewModel: CallViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity
    )
) {
    var status by remember { mutableStateOf(JobStatus.IDLE) }
    var otpValue by remember { mutableStateOf("") }
    var booking by remember { mutableStateOf<BookingResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showChatSheet by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showSosDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = showChatSheet) {
        showChatSheet = false
    }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Socket Initialization
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
    }

    DisposableEffect(Unit) {
        onDispose {
            // SocketHandler.closeConnection() // Keep open if needed
        }
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    var providerLocation by remember { mutableStateOf<LatLng?>(null) }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var distanceText by remember { mutableStateOf("Calculating...") }
    var durationText by remember { mutableStateOf("...") }
    var isFirstLoadCamera by remember { mutableStateOf(true) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(21.6264, 73.0152), 15f)
    }

    // Polling booking status and locations
    LaunchedEffect(bookingId) {
        var isFirstLoad = true
        while(true) {
            try {
                val response = RetrofitClient.apiService.getBookingDetails(bookingId)
                if (response.isSuccessful) {
                    booking = response.body()
                    android.util.Log.d("Madadwala", "Booking loaded from Vercel: ${booking?._id}")
                    
                    // Update Status from Backend
                    val backendStatus = booking?.status?.lowercase()
                    status = when(backendStatus) {
                        "accepted" -> JobStatus.IDLE
                        "on_the_way" -> JobStatus.ON_THE_WAY
                        "arrived" -> JobStatus.OTP_VERIFICATION
                        "otp_verified" -> JobStatus.OTP_VERIFIED
                        "in_progress" -> JobStatus.IN_PROGRESS
                        "done" -> JobStatus.COMPLETED
                        else -> status
                    }
                } else {
                    android.util.Log.e("Madadwala", "Failed to load booking: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("Madadwala", "Error fetching booking", e)
            }
            if (isFirstLoad) {
                isLoading = false
                isFirstLoad = false
            }
            delay(5000)
        }
    }

    // Partner Location Updates and Route Calculation
    LaunchedEffect(status, booking) {
        if ((status == JobStatus.ON_THE_WAY || status == JobStatus.IDLE) && booking != null) {
            val socket = SocketHandler.getSocket()
            var lastDbUpdateTime = 0L
            var lastMapUpdateTime = 0L

            while(true) {
                try {
                    val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                    if (location != null) {
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        providerLocation = currentLatLng
                        
                        // 1. Instant update via Socket (Every 3-5 seconds)
                        val socketData = JSONObject()
                        socketData.put("bookingId", bookingId)
                        socketData.put("lat", location.latitude)
                        socketData.put("lng", location.longitude)
                        socketData.put("role", "provider")
                        socket?.emit("update_location", socketData)

                        val currentTime = System.currentTimeMillis()
                        
                        // 2. Throttled persistent update to MongoDB (every 30 seconds)
                        if (currentTime - lastDbUpdateTime >= 30000) {
                            try {
                                android.util.Log.d("Madadwala", "Attempting Render Tracking update (Persistent)...")
                                RetrofitClient.trackingApiService.updateBookingLocation(
                                    bookingId, 
                                    mapOf("lat" to location.latitude, "lng" to location.longitude, "role" to "provider")
                                )
                                lastDbUpdateTime = currentTime
                            } catch (e: Exception) {
                                android.util.Log.e("Madadwala", "Render location update failed", e)
                            }
                        }
                        
                        // 3. Fetch Real Road Route and Distance (Throttled to every 15 seconds)
                        if (currentTime - lastMapUpdateTime >= 15000 || lastMapUpdateTime == 0L) {
                            val destination = "${booking?.customerLat},${booking?.customerLng}"
                            val origin = "${location.latitude},${location.longitude}"
                                
                            val directionsResponse = try {
                                RetrofitClient.mapsApiService.getDirections(
                                    origin = origin,
                                    destination = destination,
                                    apiKey = "AIzaSyCmBUpQgoSWPSXcC0DpxilJzJGTXJtke-8"
                                )
                            } catch (e: Exception) { null }
                            
                            if (directionsResponse != null && directionsResponse.isSuccessful && directionsResponse.body()?.status == "OK") {
                                val route = directionsResponse.body()?.routes?.firstOrNull()
                                route?.let { r ->
                                    routePoints = PolylineDecoder.decode(r.overview_polyline.points)
                                    distanceText = r.legs.firstOrNull()?.distance?.text ?: "Calculating..."
                                    durationText = r.legs.firstOrNull()?.duration?.text?.replace(" mins", " min") ?: "..."
                                }
                            } else {
                                booking?.let { b ->
                                    val results = FloatArray(1)
                                    android.location.Location.distanceBetween(
                                        location.latitude, location.longitude,
                                        b.customerLat ?: 0.0, b.customerLng ?: 0.0,
                                        results
                                    )
                                    val km = results[0] / 1000
                                    distanceText = String.format("%.1f km", km)
                                }
                            }
                            lastMapUpdateTime = currentTime
                        }

                        if (isFirstLoadCamera) {
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(currentLatLng, 15f)
                            isFirstLoadCamera = false
                        } else {
                            cameraPositionState.animate(
                                update = com.google.android.gms.maps.CameraUpdateFactory.newLatLng(currentLatLng),
                                durationMs = 1000
                            )
                        }
                    }
                } catch (e: Exception) {}
                delay(3000) // Fast updates for Socket/UI
            }
        }
    }

    fun updateStatus(newApiStatus: String, nextUiStatus: JobStatus) {
        scope.launch {
            try {
                val pName = booking?.providerName
                val safePartnerName = if (pName.isNullOrEmpty() || pName == "undefined") "Partner" else pName
                
                val response = RetrofitClient.apiService.updateBookingStatus(
                    bookingId, 
                    mapOf("status" to newApiStatus, "providerName" to safePartnerName)
                )
                if (response.isSuccessful) {
                    status = nextUiStatus
                    
                    // Service Handling
                    if (newApiStatus == "on_the_way") {
                        val intent = Intent(context, LocationService::class.java)
                        intent.putExtra("bookingId", bookingId)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    } else if (newApiStatus == "done") {
                        context.stopService(Intent(context, LocationService::class.java))
                    }
                }
            } catch (e: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Job", fontWeight = FontWeight.Bold, color = MadadwalaColors.Ink) },
                navigationIcon = {
                    IconButton(onClick = onBack) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MadadwalaColors.Ink)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSosDialog = true },
                        modifier = Modifier.padding(end = 4.dp).background(Color(0xFFFFEBEE), CircleShape).size(40.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "SOS", tint = Color.Red, modifier = Modifier.size(24.dp))
                    }
                    IconButton(
                        onClick = {
                            booking?.let { b ->
                                callViewModel.startCall(
                                    bookingId = bookingId,
                                    customerId = b.customerUid,
                                    partnerId = b.providerUid,
                                    partnerName = b.customerName ?: "Customer"
                                )
                            }
                        }, 
                        modifier = Modifier.padding(end = 4.dp).background(Color(0xFFF9FAFB), CircleShape).size(40.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Call", tint = MadadwalaColors.Green, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { /* Chat */ }, modifier = Modifier.padding(end = 16.dp).background(Color(0xFFF9FAFB), CircleShape).size(40.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", tint = MadadwalaColors.Green, modifier = Modifier.size(20.dp))
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Map Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            rotationGesturesEnabled = false,
                            tiltGesturesEnabled = false
                        )
                    ) {
                        booking?.let { b ->
                            val customerLoc = LatLng(b.customerLat ?: 21.6264, b.customerLng ?: 73.0152)
                            Marker(
                                state = MarkerState(position = customerLoc),
                                title = "Customer Location"
                            )

                            providerLocation?.let { loc ->
                                Marker(
                                    state = MarkerState(position = loc),
                                    title = "You",
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                                )

                                if (routePoints.isNotEmpty()) {
                                    Polyline(
                                        points = routePoints,
                                        color = MadadwalaColors.Green,
                                        width = 12f,
                                        jointType = JointType.ROUND
                                    )
                                }
                            }
                        }
                    }

                    // Distance Info Box
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 4.dp,
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = MadadwalaColors.Green)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Distance: $distanceText away",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Estimated time: $durationText",
                                    fontSize = 12.sp,
                                    color = MadadwalaColors.Gray
                                )
                            }
                        }
                    }
                }

                // Customer Info Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF3F4F6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MadadwalaColors.Green)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = booking?.customerName ?: "Customer",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = booking?.serviceName ?: "Service",
                                    fontSize = 14.sp,
                                    color = MadadwalaColors.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Collect: ₹${booking?.totalAmount ?: "0.00"}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MadadwalaColors.GreenDark
                                )
                            }
                            
                            IconButton(
                                onClick = {
                                    booking?.let { b ->
                                        callViewModel.startCall(
                                            bookingId = bookingId,
                                            customerId = b.customerUid,
                                            partnerId = b.providerUid,
                                            partnerName = b.customerName ?: "Customer"
                                        )
                                    }
                                },
                                modifier = Modifier.size(40.dp).background(Color(0xFFF9FAFB), CircleShape).border(1.dp, Color(0xFFE5E7EB), CircleShape)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Call", tint = MadadwalaColors.Green, modifier = Modifier.size(20.dp))
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = { showChatSheet = true },
                                modifier = Modifier.size(40.dp).background(Color(0xFFF9FAFB), CircleShape).border(1.dp, Color(0xFFE5E7EB), CircleShape)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", tint = MadadwalaColors.Green, modifier = Modifier.size(20.dp))
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = { showReportDialog = true },
                                modifier = Modifier.size(40.dp).background(Color(0xFFF9FAFB), CircleShape).border(1.dp, Color(0xFFE5E7EB), CircleShape)
                            ) {
                                Icon(Icons.Default.Flag, contentDescription = "Report", tint = Color.Red, modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MadadwalaColors.Green.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MadadwalaColors.Green, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Customer Location", fontSize = 12.sp, color = MadadwalaColors.Gray)
                                Text(
                                    text = booking?.address ?: "Address",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MadadwalaColors.Ink
                                )
                            }
                            Button(
                                onClick = {
                                    booking?.customerLat?.let { lat ->
                                        booking?.customerLng?.let { lng ->
                                            val uri = Uri.parse("google.navigation:q=$lat,$lng")
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                            intent.setPackage("com.google.android.apps.maps")
                                            context.startActivity(intent)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Navigation, contentDescription = null, tint = MadadwalaColors.Green, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Navigate", color = MadadwalaColors.Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        // Issue Images Section
                        if (!booking?.issueImages.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color(0xFFF3F4F6))
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                "Issue Photos",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MadadwalaColors.Ink
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(booking!!.issueImages!!) { imageUrl ->
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = "Issue Image",
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, Color.LightGray.copy(0.3f), RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }

                // Progress Indicator Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ActiveJobStatusIndicator(status)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Be on time Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Be on time, every time!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MadadwalaColors.Green
                            )
                            Text(
                                text = "Your customer is expecting you.",
                                fontSize = 12.sp,
                                color = MadadwalaColors.Gray
                            )
                        }
                        // Mock scooter illustration space
                        Icon(Icons.Default.TwoWheeler, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Job Details Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    JobDetailBox(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Schedule,
                        title = "Job Time",
                        line1 = booking?.scheduledTime ?: "11:00 AM - 01:00 PM",
                        line2 = "2 hrs"
                    )
                    JobDetailBox(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CalendarToday,
                        title = "Job ID",
                        line1 = booking?._id?.takeLast(10)?.uppercase() ?: "JOB123456",
                        line2 = "Booked on ${booking?.createdAt?.take(10) ?: "Today"}"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Action Button / OTP Input
                Box(modifier = Modifier.padding(16.dp)) {
                    AnimatedContent(targetState = status, label = "StatusAction") { currentStatus ->
                        when (currentStatus) {
                            JobStatus.IDLE -> {
                                Button(
                                    onClick = { updateStatus("on_the_way", JobStatus.ON_THE_WAY) },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Green),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Start Trip to Customer", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                            JobStatus.ON_THE_WAY -> {
                                Button(
                                    onClick = { updateStatus("arrived", JobStatus.OTP_VERIFICATION) },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Green),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("I Have Reached", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                            JobStatus.OTP_VERIFICATION -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MadadwalaColors.Green.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Enter 4-digit Service OTP", fontWeight = FontWeight.Bold, color = MadadwalaColors.Ink)
                                        Text("Ask the customer for the OTP to start the job", fontSize = 12.sp, color = MadadwalaColors.Gray)
                                        Spacer(modifier = Modifier.height(20.dp))
                                        OtpBoxRow(otpCount = 4, otpValue = otpValue, onOtpChange = {
                                            otpValue = it
                                            if (it.length == 4) {
                                                scope.launch {
                                                    try {
                                                        val pName = booking?.providerName
                                                        val safePartnerName = if (pName.isNullOrEmpty() || pName == "undefined") "Partner" else pName
                                                        val response = RetrofitClient.apiService.verifyBookingOtp(
                                                            bookingId, 
                                                            mapOf("otp" to it, "providerName" to safePartnerName)
                                                        )
                                                        if (response.isSuccessful) {
                                                            status = JobStatus.OTP_VERIFIED
                                                        } else {
                                                            otpValue = ""
                                                        }
                                                    } catch (e: Exception) {}
                                                }
                                            }
                                        })
                                    }
                                }
                            }
                            JobStatus.OTP_VERIFIED -> {
                                Button(
                                    onClick = { updateStatus("in_progress", JobStatus.IN_PROGRESS) },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Green),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Start Work", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                            JobStatus.IN_PROGRESS -> {
                                Button(
                                    onClick = { updateStatus("done", JobStatus.COMPLETED) },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Green),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Mark Job as Completed", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                            JobStatus.COMPLETED -> {
                                Button(
                                    onClick = onBack,
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Green),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Back to Dashboard", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                            else -> {}
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showChatSheet) {
        com.abhi.madadwala_1.ui.components.BookingChatBottomSheet(
            bookingId = bookingId,
            onDismiss = { showChatSheet = false }
        )
    }

    if (showSosDialog) {
        AlertDialog(
            onDismissRequest = { showSosDialog = false },
            title = { Text("EMERGENCY SOS", color = Color.Red, fontWeight = FontWeight.Black) },
            text = { Text("Are you in danger? Clicking 'Send SOS' will alert our emergency team with your live location and job details immediately.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val res = RetrofitClient.trackingApiService.sendSOS(mapOf(
                                    "bookingId" to bookingId,
                                    "uid" to (booking?.providerUid ?: ""),
                                    "name" to (booking?.providerName ?: "Partner"),
                                    "location" to mapOf(
                                        "lat" to (providerLocation?.latitude ?: 0.0),
                                        "lng" to (providerLocation?.longitude ?: 0.0)
                                    )
                                ))
                                if (res.isSuccessful) {
                                    Toast.makeText(context, "SOS Sent! Emergency team is on the way.", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to send SOS. Please call police.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showSosDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("SEND SOS", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSosDialog = false }) { 
                    Text("Cancel", color = MadadwalaColors.Gray, fontWeight = FontWeight.Bold) 
                }
            }
        )
    }

    if (showReportDialog) {
        var reason by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var isReporting by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { if (!isReporting) showReportDialog = false },
            title = { Text("Report Issue", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Reporting customer: ${booking?.customerName}")
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason (e.g. Misbehavior, Location issue)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Detailed Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isReporting = true
                            try {
                                val reporterUid = (booking?.providerUid ?: "").toRequestBody("text/plain".toMediaTypeOrNull())
                                val reportedUid = (booking?.customerUid ?: "").toRequestBody("text/plain".toMediaTypeOrNull())
                                val reasonBody = reason.toRequestBody("text/plain".toMediaTypeOrNull())
                                val descBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
                                
                                val res = RetrofitClient.apiService.submitReport(
                                    reporterUid, reportedUid, reasonBody, descBody, null
                                )
                                if (res.isSuccessful) {
                                    Toast.makeText(context, "Report submitted. We will investigate.", Toast.LENGTH_LONG).show()
                                    showReportDialog = false
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to submit report", Toast.LENGTH_SHORT).show()
                            } finally {
                                isReporting = false
                            }
                        }
                    },
                    enabled = reason.isNotBlank() && !isReporting,
                    colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Red)
                ) {
                    if (isReporting) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else Text("Submit Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }, enabled = !isReporting) { 
                    Text("Cancel", color = MadadwalaColors.Gray, fontWeight = FontWeight.Bold) 
                }
            }
        )
    }
}

@Composable
fun ActiveJobStatusIndicator(status: JobStatus) {
    val steps = listOf("Start", "Reach", "Work", "Done")
    val subtexts = listOf("Trip Started", "Reached Customer", "Job in Progress", "Job Completed")
    val icons = listOf(Icons.Default.PlayArrow, Icons.Default.LocationOn, Icons.Default.Build, Icons.Default.Check)

    val currentStep = when(status) {
        JobStatus.IDLE -> -1
        JobStatus.ON_THE_WAY -> 0
        JobStatus.REACHED, JobStatus.OTP_VERIFICATION -> 1
        JobStatus.IN_PROGRESS -> 2
        JobStatus.COMPLETED -> 3
        else -> -1
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        // Dashed horizontal line
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 20.dp, start = 40.dp, end = 40.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.LightGray) // Simplified dashed line
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
                            .background(if (isActive) MadadwalaColors.Green else Color(0xFFF3F4F6))
                            .border(
                                width = if (isActive) 0.dp else 1.dp,
                                color = if (isActive) Color.Transparent else Color.LightGray,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icons[index],
                            contentDescription = null,
                            tint = if (isActive) Color.White else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = step,
                        fontSize = 12.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) MadadwalaColors.Green else Color.Gray
                    )
                    Text(
                        text = subtexts[index],
                        fontSize = 10.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun JobDetailBox(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    line1: String,
    line2: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MadadwalaColors.Green.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MadadwalaColors.Green, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontSize = 10.sp, color = MadadwalaColors.Gray)
                Text(text = line1, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MadadwalaColors.Ink)
                Text(text = line2, fontSize = 10.sp, color = MadadwalaColors.Gray)
            }
        }
    }
}
