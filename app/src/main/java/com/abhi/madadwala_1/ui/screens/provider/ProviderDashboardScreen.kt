package com.abhi.madadwala_1.ui.screens.provider

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.abhi.madadwala_1.R
import com.abhi.madadwala_1.ui.components.*
import com.abhi.madadwala_1.data.PreferenceManager
import com.abhi.madadwala_1.data.remote.*
import com.abhi.madadwala_1.ui.theme.MadadwalaColors
import com.abhi.madadwala_1.ui.viewmodel.LocationViewModel
import com.abhi.madadwala_1.ui.viewmodel.ProviderDashboardState
import com.abhi.madadwala_1.ui.viewmodel.ProviderViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

import com.abhi.madadwala_1.ui.viewmodel.CallViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ProviderDashboardScreen(
    onLogout: () -> Unit,
    onNavigateToActiveJob: (String) -> Unit,
    onNavigate: (String) -> Unit = {},
    viewModel: ProviderViewModel = viewModel(),
    locationViewModel: LocationViewModel = viewModel(),
    callViewModel: CallViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity
    )
) {
    val context = LocalContext.current
    val dashboardState by viewModel.dashboardState.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var subScreen by rememberSaveable { mutableStateOf<String?>(null) }
    
    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val headerBgColor = if (selectedTab == 0) {
        when {
            hour in 6..16 -> Color(0xFFFFFAEC)
            hour in 17..19 -> Color(0xFFFFEBE3)
            else -> Color(0xFF111827) // Dark Night
        }
    } else if (selectedTab == 1) Color.White else MadadwalaColors.Cream

    val isNightMode = (hour !in 6..19) && selectedTab == 0
    val topBarContentColor = if (isNightMode) Color.White else MadadwalaColors.Ink
    val topBarSubColor = if (isNightMode) Color.White.copy(alpha = 0.7f) else MadadwalaColors.Gray

    BackHandler(enabled = selectedTab != 0 || subScreen != null) {
        if (subScreen != null) {
            subScreen = null
        } else {
            selectedTab = 0
        }
    }
    var activeChatBookingId by rememberSaveable { mutableStateOf<String?>(null) }
    val isOnline by viewModel.isOnline.collectAsState()
    
    val address by locationViewModel.address.collectAsState()
    val lat by locationViewModel.latitude.collectAsState()
    val lng by locationViewModel.longitude.collectAsState()

    val scope = rememberCoroutineScope()
    val preferenceManager = remember { PreferenceManager(context) }
    val preferredOnlineStatus by preferenceManager.preferredOnlineStatus.collectAsState(initial = true)
    val unreadNotificationsCount by preferenceManager.unreadNotificationsCount.collectAsState(initial = 0)
    val lifecycleOwner = LocalLifecycleOwner.current

    // Location Permission
    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            while (true) {
                locationViewModel.fetchLocation(context)
                delay(30000) // Update every 30 seconds
            }
        } else {
            locationPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(lat, lng) {
        if (lat != 0.0 && lng != 0.0) {
            viewModel.updateLocation(lat, lng)
        }
    }

    var operationalCities by remember { mutableStateOf<List<String>>(emptyList()) }
    var isCityOperational by remember { mutableStateOf(false) }
    var cityCheckLoading by remember { mutableStateOf(true) }
    var isCitiesLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while(true) {
            try {
                val res = com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.getOperationalCities()
                if (res.isSuccessful) {
                    operationalCities = res.body()?.map { it.name } ?: emptyList()
                    isCitiesLoaded = true
                }
            } catch (_: Exception) {
                if (!isCitiesLoaded) isCitiesLoaded = true
            }
            delay(10000) // Refresh every 10 seconds
        }
    }

    LaunchedEffect(address, operationalCities, isCitiesLoaded) {
        if (!isCitiesLoaded) {
            cityCheckLoading = true
            return@LaunchedEffect
        }

        if (address.isNotEmpty() && address != "Locating..." && address != "Fetching location...") {
            isCityOperational = operationalCities.any { city ->
                address.contains(city, ignoreCase = true)
            }
            cityCheckLoading = false
        } else {
            cityCheckLoading = true
        }
    }

    if (cityCheckLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MadadwalaColors.Teal)
        }
        return
    }

    if (!isCityOperational) {
        com.abhi.madadwala_1.ui.screens.ComingSoonScreen(
            cityName = address.split(",").firstOrNull()?.trim() ?: "Your City",
            address = address,
            onNotifyMe = {
                scope.launch {
                    try {
                        val cityName = address.split(",").firstOrNull()?.trim() ?: "Your City"
                        val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                        val user = (dashboardState as? ProviderDashboardState.Success)?.user
                        val res = com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.registerLocationInterest(
                            mapOf(
                                "uid" to (user?.uid ?: ""),
                                "cityName" to cityName,
                                "fcmToken" to token
                            )
                        )
                        if (res.isSuccessful) {
                            Toast.makeText(context, "We'll notify you when we launch in $cityName!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "You're already on our waitlist for $cityName!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Added to waitlist!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        return
    }

    // Handle Online/Offline based on App Lifecycle
    DisposableEffect(lifecycleOwner, preferredOnlineStatus) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (preferredOnlineStatus) {
                        viewModel.setOnlineStatus(true)
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Alert Sound Logic
    val isNotificationSoundEnabled by preferenceManager.isNotificationSoundEnabled.collectAsState(initial = true)
    val mediaPlayer = remember { MediaPlayer.create(context, R.raw.alert_bell).apply { isLooping = true } }
    
    LaunchedEffect(dashboardState, isOnline, isNotificationSoundEnabled) {
        val state = dashboardState
        if (isOnline && isNotificationSoundEnabled && state is ProviderDashboardState.Success && state.customRequests.isNotEmpty()) {
            if (!mediaPlayer.isPlaying) mediaPlayer.start()
        } else {
            if (mediaPlayer.isPlaying) mediaPlayer.pause()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }

    Scaffold(
        topBar = {
            val user = (dashboardState as? ProviderDashboardState.Success)?.user ?: return@Scaffold
            Surface(
                shadowElevation = 0.dp,
                color = headerBgColor
            ) {
                TopAppBar(
                    title = {
                        Column(modifier = Modifier.padding(start = 0.dp)) {
                            Text(
                                text = when(selectedTab) {
                                    0 -> stringResource(R.string.partner_dashboard)
                                    1 -> stringResource(R.string.my_bookings)
                                    2 -> stringResource(R.string.payouts_wallet)
                                    else -> stringResource(R.string.partner_profile)
                                },
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black, 
                                    color = topBarContentColor,
                                    fontSize = if (selectedTab == 1 || selectedTab == 2) 22.sp else 18.sp,
                                    letterSpacing = (-0.5).sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Visible
                            )
                            when (selectedTab) {
                                0 -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = if (isNightMode) Color(0xFF10B981) else MadadwalaColors.Green,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = address.ifEmpty { "Location not available" },
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = topBarSubColor,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 140.dp)
                                        )
                                    }
                                }
                                1 -> {
                                    Text(
                                        text = "Manage and track all your bookings",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MadadwalaColors.Gray,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                                2 -> {
                                    Text(
                                        text = "Manage your earnings and payouts",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MadadwalaColors.Gray,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                                3 -> {
                                    Text(
                                        text = "Manage your account and business",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MadadwalaColors.Gray,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        if (selectedTab == 0) {
                            Surface(
                                modifier = Modifier.padding(end = 4.dp),
                                shape = RoundedCornerShape(24.dp),
                                color = if (isOnline) MadadwalaColors.Green.copy(alpha = 0.12f) else MadadwalaColors.LightGray.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically, 
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isOnline) stringResource(R.string.online) else stringResource(R.string.offline),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOnline) MadadwalaColors.Green else MadadwalaColors.Gray,
                                            fontSize = 11.sp
                                        )
                                    )
                                    Switch(
                                        checked = isOnline,
                                        onCheckedChange = { online ->
                                            viewModel.setOnlineStatus(online)
                                            scope.launch {
                                                preferenceManager.setPreferredOnlineStatus(online)
                                            }
                                        },
                                        modifier = Modifier.scale(0.55f).padding(start = 0.dp),
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = MadadwalaColors.Green,
                                            uncheckedThumbColor = Color.White,
                                            uncheckedTrackColor = Color.LightGray
                                        )
                                    )
                                }
                            }
                        }
                        
                        BadgedBox(
                            badge = { 
                                if (unreadNotificationsCount > 0) {
                                    Badge(
                                        containerColor = MadadwalaColors.Red,
                                        modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                                    ) { 
                                        Text(unreadNotificationsCount.toString(), fontSize = 10.sp, color = Color.White) 
                                    }
                                }
                            },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            IconButton(onClick = { onNavigate(com.abhi.madadwala_1.ui.navigation.Screen.Notifications.route) }) {
                                Icon(
                                    Icons.Default.NotificationsNone, 
                                    contentDescription = "Notifications", 
                                    tint = topBarContentColor
                                )
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MadadwalaColors.Ink)
                                .clickable { selectedTab = 3 },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!user.profileImage.isNullOrEmpty()) {
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                                        .data(user.profileImage)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = (user.name ?: "A").take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                val items = listOf(
                    Triple(0, if (selectedTab == 0) Icons.Default.GridView else Icons.Outlined.GridView, stringResource(R.string.home)),
                    Triple(1, if (selectedTab == 1) Icons.Default.Assignment else Icons.Outlined.Assignment, stringResource(R.string.bookings)),
                    Triple(2, if (selectedTab == 2) Icons.Default.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet, stringResource(R.string.payouts)),
                    Triple(3, if (selectedTab == 3) Icons.Default.Person else Icons.Outlined.Person, stringResource(R.string.profile))
                )
                items.forEach { (index, icon, label) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(26.dp)) },
                        label = { 
                            Text(
                                text = label, 
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium),
                                color = if (selectedTab == index) Color(0xFF2E7D32) else Color.Gray
                            ) 
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF2E7D32),
                            selectedTextColor = Color(0xFF2E7D32),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color(0xFF2E7D32).copy(alpha = 0.1f)
                        )
                    )
                }
            }
        },
        containerColor = if (selectedTab == 1) Color(0xFFFBFBFB) else MadadwalaColors.Cream
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = dashboardState) {
                is ProviderDashboardState.Loading -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = MadadwalaColors.Teal) }
                }
                is ProviderDashboardState.Error -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { Text(state.message, color = MadadwalaColors.Red) }
                }
                is ProviderDashboardState.Success -> {
                    AnimatedContent(targetState = selectedTab, label = "TabTransition") { tab ->
                        when(tab) {
                            0 -> ProviderHomeTab(
                                user = state.user,
                                isOnline = isOnline,
                                bookings = state.bookings,
                                requests = state.customRequests,
                                onViewDetails = { bookingId -> 
                                    val booking = state.bookings.find { it._id == bookingId }
                                    if (booking?.status?.lowercase() == "done" || booking?.status?.lowercase() == "completed") {
                                        onNavigate(com.abhi.madadwala_1.ui.navigation.Screen.WorkCompleted.createRoute(bookingId, isPartner = true))
                                    } else {
                                        onNavigateToActiveJob(bookingId)
                                    }
                                },
                                onSendBid = { id, price -> 
                                    val request = state.customRequests.find { it._id == id }
                                    if (request?.isAutoPrice == true) {
                                        viewModel.submitBid(id, price)
                                    } else {
                                        viewModel.directAccept(id)
                                    }
                                },
                                onRejectRequest = { viewModel.updateCustomRequestStatus(it, "rejected") },
                                onCancelBooking = { id, reason -> viewModel.cancelBooking(id, reason) },
                                onAcceptDirect = { bookingId, time, comment, custId -> 
                                    viewModel.updateBookingStatus(bookingId, "accepted", time, comment)
                                },
                                onNavigateToWallet = { selectedTab = 2 },
                                onNavigateToRatings = { selectedTab = 3 },
                                onNavigateToBookings = { selectedTab = 1 },
                                onChat = { activeChatBookingId = it },
                                callViewModel = callViewModel
                            )
                            1 -> ProviderBookingsTab(
                                bookings = state.bookings,
                                onViewDetails = { bookingId -> 
                                    val booking = state.bookings.find { it._id == bookingId }
                                    if (booking?.status?.lowercase() == "done" || booking?.status?.lowercase() == "completed") {
                                        onNavigate(com.abhi.madadwala_1.ui.navigation.Screen.WorkCompleted.createRoute(bookingId, isPartner = true))
                                    } else {
                                        onNavigateToActiveJob(bookingId)
                                    }
                                },
                                onAcceptDirect = { id, time, comment, custId -> viewModel.updateBookingStatus(id, "accepted", time, comment) },
                                onCancelBooking = { id, reason -> viewModel.cancelBooking(id, reason) },
                                onChat = { activeChatBookingId = it },
                                callViewModel = callViewModel
                            )
                            2 -> ProviderPayoutsTab(
                                balance = state.user.walletBalance,
                                totalEarned = state.user.totalEarnings ?: 0.0,
                                transactions = state.transactions,
                                onRefresh = { viewModel.loadDashboardData(false) },
                                onViewWithdrawalHistory = { selectedTab = 3; subScreen = "withdrawals" },
                                onSupportClick = { onNavigate(com.abhi.madadwala_1.ui.navigation.Screen.HelpSupport.route) }
                            )
                            3 -> ProviderProfileTab(
                                user = state.user,
                                services = state.services,
                                reviews = state.reviews,
                                preferenceManager = preferenceManager,
                                subScreen = subScreen,
                                onSubScreenChange = { subScreen = it },
                                onLogout = onLogout,
                                onUpdateServicePrice = { id, price -> viewModel.updateServicePrice(id, price) },
                                onAddService = { name, price -> viewModel.addService(name, price) },
                                onUploadImage = { viewModel.uploadProfileImage(it, context) },
                                onRefresh = { viewModel.loadDashboardData(false) },
                                onSupportClick = { onNavigate(com.abhi.madadwala_1.ui.navigation.Screen.HelpSupport.route) }
                            )
                        }
                    }
                }
            }
        }
    }

    activeChatBookingId?.let { bookingId ->
        com.abhi.madadwala_1.ui.components.BookingChatBottomSheet(
            bookingId = bookingId,
            onDismiss = { activeChatBookingId = null }
        )
    }

    // Negative Balance Warning Dialog
    val user = (dashboardState as? ProviderDashboardState.Success)?.user
    if (user != null && user.walletBalance <= -100) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = Color.White,
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MadadwalaColors.Red, modifier = Modifier.size(48.dp)) },
            title = { 
                Text(
                    "Low Wallet Balance", 
                    fontWeight = FontWeight.Bold, 
                    color = MadadwalaColors.Ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                ) 
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Your wallet balance is ₹${user.walletBalance.toInt()}. Please maintain a minimum balance of -₹100 to continue accepting new bookings.",
                        textAlign = TextAlign.Center,
                        color = MadadwalaColors.Ink
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "You will not be able to accept any new or instant bookings until the balance is cleared.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MadadwalaColors.Red,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedTab = 2 },
                    colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Teal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Recharge Wallet", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun ProviderHomeTab(
    user: UserResponse,
    isOnline: Boolean,
    bookings: List<BookingResponse>,
    requests: List<CustomRequestResponse>,
    onViewDetails: (String) -> Unit,
    onSendBid: (String, Double) -> Unit,
    onRejectRequest: (String) -> Unit,
    onCancelBooking: (String, String) -> Unit,
    onAcceptDirect: (String, String, String?, String?) -> Unit,
    onNavigateToWallet: () -> Unit = {},
    onNavigateToRatings: () -> Unit = {},
    onNavigateToBookings: () -> Unit = {},
    onChat: (String) -> Unit,
    callViewModel: CallViewModel
) {
    val scrollState = rememberLazyListState()
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    Box(modifier = Modifier.fillMaxSize().background(MadadwalaColors.Cream)) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Time-based Sky Background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = when {
                                        hour in 6..16 -> listOf(
                                            Color(0xFFFFFAEC),
                                            Color(0xFFFFF1C1),
                                            MadadwalaColors.Cream
                                        )
                                        hour in 17..19 -> listOf(
                                            Color(0xFFFFEBE3),
                                            Color(0xFFFFCCBC),
                                            MadadwalaColors.Cream
                                        )
                                        else -> listOf(
                                            Color(0xFF0F172A),
                                            Color(0xFF1E293B),
                                            MadadwalaColors.Cream
                                        )
                                    }
                                )
                            )
                    ) {
                        when {
                            hour in 6..16 -> {
                                // Simple Sun Illustration (Morning/Afternoon)
                                Canvas(modifier = Modifier.size(120.dp).align(Alignment.Center).offset(x = (-20).dp, y = (-20).dp)) {
                                    drawCircle(
                                        color = Color(0xFFFFD54F).copy(alpha = 0.15f),
                                        radius = size.minDimension / 1.1f
                                    )
                                    drawCircle(
                                        color = Color(0xFFFFD54F).copy(alpha = 0.4f),
                                        radius = size.minDimension / 1.6f
                                    )
                                }
                            }
                            hour in 17..19 -> {
                                // Sunset Sun Illustration (Evening)
                                Canvas(modifier = Modifier.size(160.dp).align(Alignment.Center).offset(x = 0.dp, y = 35.dp)) {
                                    drawCircle(
                                        color = Color(0xFFFFB74D).copy(alpha = 0.2f),
                                        radius = size.minDimension / 0.9f
                                    )
                                    drawCircle(
                                        color = Color(0xFFFFB74D).copy(alpha = 0.5f),
                                        radius = size.minDimension / 1.4f
                                    )
                                }
                                
                                // Simple birds approximation
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val birdColor = Color(0xFF455A64).copy(alpha = 0.3f)
                                    // Bird 1
                                    drawArc(
                                        color = birdColor,
                                        startAngle = 200f,
                                        sweepAngle = 140f,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
                                        topLeft = androidx.compose.ui.geometry.Offset(100f, 60f),
                                        size = androidx.compose.ui.geometry.Size(20f, 10f)
                                    )
                                    // Bird 2
                                    drawArc(
                                        color = birdColor,
                                        startAngle = 200f,
                                        sweepAngle = 140f,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
                                        topLeft = androidx.compose.ui.geometry.Offset(140f, 80f),
                                        size = androidx.compose.ui.geometry.Size(24f, 12f)
                                    )
                                }
                            }
                            else -> {
                                // Night Illustration (Crescent Moon & Stars)
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    // Crescent Moon
                                    val moonColor = Color(0xFFFDE68A)
                                    drawCircle(
                                        color = moonColor,
                                        radius = 35f,
                                        center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2.5f)
                                    )
                                    drawCircle(
                                        color = Color(0xFF0F172A), // Same as background start color to create crescent
                                        radius = 32f,
                                        center = androidx.compose.ui.geometry.Offset(size.width / 2f + 12f, size.height / 2.5f - 8f)
                                    )
                                    
                                    // Stars
                                    val starColor = Color.White.copy(alpha = 0.6f)
                                    val random = java.util.Random(42)
                                    repeat(25) {
                                        drawCircle(
                                            color = starColor,
                                            radius = random.nextFloat() * 2f + 0.5f,
                                            center = androidx.compose.ui.geometry.Offset(
                                                random.nextFloat() * size.width,
                                                random.nextFloat() * (size.height * 0.7f)
                                            )
                                        )
                                    }
                                }
                                
                                // City Skyline approximation at bottom of background
                                Canvas(modifier = Modifier.fillMaxWidth().height(80.dp).align(Alignment.BottomCenter)) {
                                    val buildingColor = Color(0xFF1E293B).copy(alpha = 0.8f)
                                    val windowColor = Color(0xFFFDE68A).copy(alpha = 0.4f)
                                    
                                    val widths = listOf(40f, 60f, 35f, 70f, 50f, 80f, 45f)
                                    val heights = listOf(60f, 90f, 40f, 110f, 70f, 100f, 80f)
                                    var currentX = 0f
                                    
                                    var idx = 0
                                    while (currentX < size.width) {
                                        val w = widths[idx % widths.size]
                                        val h = heights[idx % heights.size]
                                        drawRect(
                                            color = buildingColor,
                                            topLeft = androidx.compose.ui.geometry.Offset(currentX, size.height - h),
                                            size = androidx.compose.ui.geometry.Size(w, h)
                                        )
                                        
                                        // Some windows
                                        if (h > 40f) {
                                            drawRect(
                                                color = windowColor,
                                                topLeft = androidx.compose.ui.geometry.Offset(currentX + 5f, size.height - h + 10f),
                                                size = androidx.compose.ui.geometry.Size(6f, 6f)
                                            )
                                            drawRect(
                                                color = windowColor,
                                                topLeft = androidx.compose.ui.geometry.Offset(currentX + w - 11f, size.height - h + 25f),
                                                size = androidx.compose.ui.geometry.Size(6f, 6f)
                                            )
                                        }
                                        
                                        currentX += w + 2f
                                        idx++
                                    }
                                }
                            }
                        }
                    }

                    // Helper Illustration repositioned
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(1000)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 10.dp, y = 10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.helper_person),
                            contentDescription = null,
                            modifier = Modifier.size(200.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Greeting Section
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 32.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val greeting = when(hour) {
                                    in 6..11 -> "Good Morning! ☀️"
                                    in 12..16 -> "Good Afternoon! ☀️"
                                    in 17..19 -> "Good Evening! 🌇"
                                    else -> "Good Night! 🌙"
                                }
                                Text(
                                    text = greeting,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = if (hour !in 6..19) Color.White.copy(alpha = 0.8f) else MadadwalaColors.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                val rawName = user.name
                                val displayName = if (rawName.isNullOrEmpty() || rawName == "undefined") "Partner" else rawName
                                val greetingName = if (displayName.contains("- Prov", ignoreCase = true)) displayName else "$displayName - Prov"
                                Text(
                                    text = greetingName,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = if (hour !in 6..19) Color.White else MadadwalaColors.Ink,
                                        fontSize = 30.sp,
                                        letterSpacing = (-1).sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ready to help more customers today",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (hour !in 6..19) Color.White.copy(alpha = 0.6f) else MadadwalaColors.Gray,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                            
                            // Reserved space for the image overlap to prevent text clashing
                            Spacer(modifier = Modifier.width(100.dp))
                        }

                        // Stat Cards Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                label = stringResource(R.string.wallet_balance), 
                                value = "₹${user.walletBalance.toInt()}.00", 
                                icon = Icons.Default.AccountBalanceWallet,
                                buttonText = stringResource(R.string.view_wallet),
                                onClick = onNavigateToWallet,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                label = "${user.reviewCount ?: 0} Ratings", 
                                value = String.format(Locale.getDefault(), "%.1f", user.rating ?: 0.0),
                                icon = Icons.Default.Star,
                                buttonText = stringResource(R.string.view_ratings),
                                onClick = onNavigateToRatings,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                label = stringResource(R.string.total_jobs), 
                                value = "${user.totalJobs ?: 0}", 
                                icon = Icons.Default.Work,
                                buttonText = stringResource(R.string.view_jobs),
                                onClick = onNavigateToBookings,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                if (user.kycRejected) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable { onNavigateToRatings() }, // Ratings points to profile tab where KYC is
                        shape = RoundedCornerShape(24.dp),
                        color = MadadwalaColors.Red.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MadadwalaColors.Red.copy(alpha = 0.2f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = MadadwalaColors.Red, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Registration Rejected", fontWeight = FontWeight.Bold, color = MadadwalaColors.Red)
                                Text("Click here to update your KYC details and re-apply.", style = MaterialTheme.typography.bodySmall, color = MadadwalaColors.Ink)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = MadadwalaColors.Red)
                        }
                    }
                } else {
                    PromotionCard()
                }
            }

            if (!isOnline) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn() + expandVertically()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MadadwalaColors.Red.copy(alpha = 0.05f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MadadwalaColors.Red.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.CloudOff, 
                                    contentDescription = null, 
                                    tint = MadadwalaColors.Red, 
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "You are currently Offline", 
                                    fontWeight = FontWeight.Bold, 
                                    color = MadadwalaColors.Red
                                )
                                Text(
                                    "Go online to start receiving new job requests", 
                                    style = MaterialTheme.typography.bodySmall, 
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            if (isOnline && requests.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        stringResource(R.string.new_job_requests), 
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        ),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
                items(requests) { request ->
                    Box(modifier = Modifier.padding(vertical = 8.dp)) {
                        CustomJobRequestNotification(
                            request = request,
                            onAccept = { price: Double -> onSendBid(request._id, price) },
                            onReject = { onRejectRequest(request._id) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.upcoming_jobs), 
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onNavigateToBookings() }) {
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MadadwalaColors.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MadadwalaColors.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            val upcoming = bookings.filter { it.status != "done" && it.status != "cancelled" }
            if (upcoming.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.AutoMirrored.Filled.EventNote, 
                                contentDescription = null, 
                                tint = MadadwalaColors.LightGray, 
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No upcoming jobs yet.", color = Color.Gray)
                        }
                    }
                }
            } else {
                items(upcoming) { booking ->
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(800, delayMillis = 400)) + slideInVertically(initialOffsetY = { 60 })
                    ) {
                        BookingCard(
                            booking = booking,
                            onViewDetails = { onViewDetails(booking._id) },
                            onAccept = { time, comment -> onAcceptDirect(booking._id, time, comment, booking.customerUid) },
                            onReject = { reason -> onCancelBooking(booking._id, reason) },
                            onChat = { onChat(booking._id) },
                            callViewModel = callViewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PromotionCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        color = MadadwalaColors.GreenDark
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Stars, null, tint = MadadwalaColors.Lime, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Complete more jobs", 
                    color = Color.White, 
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Great job! Keep it up and grow your business with Madadwala",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 16.sp
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.White)
        }
    }
}

@Composable
fun ProviderBookingsTab(
    bookings: List<BookingResponse>,
    onViewDetails: (String) -> Unit,
    onAcceptDirect: (String, String, String?, String?) -> Unit,
    onCancelBooking: (String, String) -> Unit,
    onChat: (String) -> Unit,
    callViewModel: CallViewModel
) {
    var selectedFilter by rememberSaveable { mutableStateOf("All") }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val filteredBookings = when (selectedFilter) {
        "Pending" -> bookings.filter { it.status.lowercase() == "pending" || it.status.lowercase() == "requested" || it.status.lowercase() == "upcoming" }
        "Accepted" -> bookings.filter { it.status.lowercase() == "accepted" }
        "Completed" -> bookings.filter { it.status.lowercase() == "done" || it.status.lowercase() == "completed" }
        else -> bookings
    }

    val counts = mapOf(
        "All" to bookings.size,
        "Pending" to bookings.count { it.status.lowercase() == "pending" || it.status.lowercase() == "requested" || it.status.lowercase() == "upcoming" },
        "Accepted" to bookings.count { it.status.lowercase() == "accepted" },
        "Completed" to bookings.count { it.status.lowercase() == "done" || it.status.lowercase() == "completed" }
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    FilterChipV2(
                        label = "All",
                        count = counts["All"] ?: 0,
                        isSelected = selectedFilter == "All",
                        onClick = { selectedFilter = "All" },
                        icon = Icons.Default.GridView,
                        iconColor = Color(0xFF1E5631)
                    )
                }
                item {
                    FilterChipV2(
                        label = "Pending",
                        count = counts["Pending"] ?: 0,
                        isSelected = selectedFilter == "Pending",
                        onClick = { selectedFilter = "Pending" },
                        icon = Icons.Default.HourglassEmpty,
                        iconColor = Color(0xFFFF9800)
                    )
                }
                item {
                    FilterChipV2(
                        label = "Accepted",
                        count = counts["Accepted"] ?: 0,
                        isSelected = selectedFilter == "Accepted",
                        onClick = { selectedFilter = "Accepted" },
                        icon = Icons.Default.CheckCircle,
                        iconColor = Color(0xFF4CAF50)
                    )
                }
                item {
                    FilterChipV2(
                        label = "Completed",
                        count = counts["Completed"] ?: 0,
                        isSelected = selectedFilter == "Completed",
                        onClick = { selectedFilter = "Completed" },
                        icon = Icons.Default.CalendarToday,
                        iconColor = Color(0xFF2196F3)
                    )
                }
            }
        }

        if (filteredBookings.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = MadadwalaColors.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No $selectedFilter bookings found", color = MadadwalaColors.Gray)
                    }
                }
            }
        } else {
                items(filteredBookings) { booking ->
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(600)) + slideInVertically(initialOffsetY = { 40 })
                    ) {
                        ModernBookingCard(
                            booking = booking,
                            onViewDetails = { onViewDetails(booking._id) },
                            onReject = { reason -> onCancelBooking(booking._id, reason) },
                            onChat = { onChat(booking._id) },
                            callViewModel = callViewModel
                        )
                    }
                }
        }
    }
}

@Composable
fun ProviderPayoutsTab(balance: Double, totalEarned: Double, transactions: List<TransactionResponse>, onRefresh: () -> Unit, onViewWithdrawalHistory: () -> Unit, onSupportClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    var showWithdrawDialog by rememberSaveable { mutableStateOf(false) }
    var withdrawAmount by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var isBalanceVisible by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9F9))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            item {
                PartnerWalletBalanceCard(
                    balance = balance,
                    walletId = "MW${(auth.currentUser?.uid ?: "000000").takeLast(6).uppercase()}",
                    isVisible = isBalanceVisible,
                    onToggleVisibility = { 
                        isBalanceVisible = !isBalanceVisible
                        if (isBalanceVisible) {
                            onRefresh()
                        }
                    },
                    onAddMoney = { /* TODO: Add money logic */ },
                    onTransactions = { /* Already in transactions list */ }
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .background(Color.White, RoundedCornerShape(24.dp))
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    WalletQuickActionItem(
                        icon = Icons.Default.Download,
                        title = "Withdraw",
                        subtitle = "Request your\npayout",
                        onClick = { showWithdrawDialog = true }
                    )
                    WalletQuickActionItem(
                        icon = Icons.Default.History,
                        title = "History",
                        subtitle = "Withdrawal\nstatus",
                        onClick = onViewWithdrawalHistory
                    )
                    WalletQuickActionItem(
                        icon = Icons.Default.VerifiedUser,
                        title = "Secure",
                        subtitle = "Your money is\n100% safe"
                    )
                    WalletQuickActionItem(
                        icon = Icons.Default.HeadsetMic,
                        title = "Support",
                        subtitle = "Get help with\npayouts",
                        onClick = onSupportClick
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { /* TODO */ }
                    ) {
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (transactions.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) {
                        Text("No transactions yet.", color = Color.Gray)
                    }
                }
            } else {
                items(transactions) { tx ->
                    PartnerTransactionItem(tx, balance)
                }
            }

            item {
                InviteEarnBanner(
                    onInvite = {
                        val walletId = "MW${(auth.currentUser?.uid ?: "000000").takeLast(6).uppercase()}"
                        val sendIntent: android.content.Intent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, "Join Madadwala as a Partner and start earning! Use my referral code: $walletId. Download now: https://madadwala.com/download")
                            type = "text/plain"
                        }
                        val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showWithdrawDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showWithdrawDialog = false },
            containerColor = Color.White,
            title = { Text("Withdraw Money", fontWeight = FontWeight.Bold, color = MadadwalaColors.Ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Maximum available: ₹$balance", style = MaterialTheme.typography.bodySmall, color = MadadwalaColors.Gray)
                    OutlinedTextField(
                        value = withdrawAmount,
                        onValueChange = { if (it.all { char -> char.isDigit() }) withdrawAmount = it },
                        label = { Text("Enter Amount") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        prefix = { Text("₹", color = MadadwalaColors.Ink) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MadadwalaColors.Ink,
                            unfocusedTextColor = MadadwalaColors.Ink,
                            focusedBorderColor = MadadwalaColors.Teal,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                    Text("Note: It takes 3-4 hours to credit in your account.", fontSize = 12.sp, color = MadadwalaColors.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = withdrawAmount.toDoubleOrNull() ?: 0.0
                        if (amt < 500) {
                            Toast.makeText(context, "Minimum withdrawal is ₹500", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (amt > balance) {
                            Toast.makeText(context, "Insufficient balance", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        scope.launch {
                            isSubmitting = true
                            try {
                                val res = RetrofitClient.apiService.requestWithdrawal(mapOf(
                                    "providerUid" to (auth.currentUser?.uid ?: ""),
                                    "amount" to amt
                                ))
                                if (res.isSuccessful) {
                                    Toast.makeText(context, res.body()?.message ?: "Success", Toast.LENGTH_LONG).show()
                                    showWithdrawDialog = false
                                } else {
                                    val errorJson = res.errorBody()?.string()
                                    val errorMsg = if (errorJson?.contains("bank account") == true) "Please add your bank account first in Profile > Bank Details" else "Failed to request withdrawal"
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    enabled = !isSubmitting && withdrawAmount.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MadadwalaColors.Teal,
                        contentColor = Color.White,
                        disabledContainerColor = Color.LightGray,
                        disabledContentColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Request Withdrawal", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showWithdrawDialog = false }, 
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.textButtonColors(contentColor = MadadwalaColors.Red)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderProfileTab(
    user: UserResponse,
    services: List<ServiceResponse>,
    reviews: List<ReviewResponse>,
    preferenceManager: PreferenceManager,
    subScreen: String?,
    onSubScreenChange: (String?) -> Unit,
    onLogout: () -> Unit,
    onUpdateServicePrice: (String, Double) -> Unit,
    onAddService: (String, Double) -> Unit,
    onUploadImage: (Uri) -> Unit,
    onRefresh: () -> Unit,
    onSupportClick: () -> Unit
) {
    var legalContentType by rememberSaveable { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            onUploadImage(it)
        }
    }

    BackHandler(enabled = subScreen != null) {
        onSubScreenChange(null)
    }

    AnimatedContent(targetState = subScreen, label = "ProfileTransition") { screen ->
        when (screen) {
            null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MadadwalaColors.Cream),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        // Profile Header Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White
                        ) {
                            Box(modifier = Modifier.padding(20.dp)) {
                                // Background Medal Decoration
                                Icon(
                                    Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 10.dp, y = (-10).dp),
                                    tint = MadadwalaColors.LightGray.copy(alpha = 0.3f)
                                )
                                
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(100.dp)
                                                .clip(CircleShape)
                                                .background(MadadwalaColors.Ink)
                                                .clickable { photoPicker.launch("image/*") },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val pName = user.name
                                            val safeName = if (pName.isNullOrEmpty() || pName == "undefined") "Partner" else pName
                                            if (!user.profileImage.isNullOrEmpty()) {
                                                AsyncImage(
                                                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                                                        .data(user.profileImage)
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = "Profile Photo",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Text(
                                                    text = safeName.take(1).uppercase(),
                                                    color = Color.White,
                                                    fontSize = 40.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .size(32.dp)
                                                    .background(Color.White, CircleShape)
                                                    .padding(2.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(MadadwalaColors.Green, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.width(20.dp))
                                        
                                        Column {
                                            val pName = user.name
                                            val safeName = if (pName.isNullOrEmpty() || pName == "undefined") "Partner" else pName
                                            Text(
                                                text = safeName,
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MadadwalaColors.Ink
                                                )
                                            )
                                            
                                            Surface(
                                                modifier = Modifier.padding(vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                color = MadadwalaColors.Green.copy(alpha = 0.1f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Default.Verified,
                                                        contentDescription = null,
                                                        tint = MadadwalaColors.Green,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Verified Partner",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = MadadwalaColors.Green,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
                                                }
                                            }
                                            
                                            Text(
                                                text = user.profession ?: user.category ?: "Professional",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MadadwalaColors.Gray
                                            )
                                            
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                                Icon(Icons.Default.Call, null, tint = MadadwalaColors.Gray, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("+91 ${user.phoneNumber}", style = MaterialTheme.typography.bodySmall, color = MadadwalaColors.Gray)
                                            }
                                            
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                                Icon(Icons.Default.Email, null, tint = MadadwalaColors.Gray, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(user.email ?: "noemail@example.com", style = MaterialTheme.typography.bodySmall, color = MadadwalaColors.Gray)
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(24.dp))
                                    HorizontalDivider(color = MadadwalaColors.LightGray.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Stats Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        StatMiniItem(
                                            icon = Icons.Default.Work,
                                            value = "${user.rating ?: 4.8}",
                                            label = "Rating",
                                            subtext = "(${user.reviewCount ?: 128} Reviews)",
                                            showStars = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        VerticalDivider(modifier = Modifier.height(40.dp).padding(horizontal = 4.dp), color = MadadwalaColors.LightGray.copy(alpha = 0.5f))
                                        StatMiniItem(
                                            icon = Icons.Default.AssignmentTurnedIn,
                                            value = "${user.totalJobs ?: 0}",
                                            label = "Jobs Completed",
                                            subtext = "Lifetime",
                                            modifier = Modifier.weight(1f)
                                        )
                                        VerticalDivider(modifier = Modifier.height(40.dp).padding(horizontal = 4.dp), color = MadadwalaColors.LightGray.copy(alpha = 0.5f))
                                        StatMiniItem(
                                            icon = Icons.Default.AccountBalanceWallet,
                                            value = "₹${user.walletBalance.toInt()}.00",
                                            label = "Wallet Balance",
                                            link = "View Wallet",
                                            onLinkClick = { /* Navigate to wallet tab */ },
                                            modifier = Modifier.weight(1f)
                                        )
                                        VerticalDivider(modifier = Modifier.height(40.dp).padding(horizontal = 4.dp), color = MadadwalaColors.LightGray.copy(alpha = 0.5f))
                                        
                                        val experienceText = remember(user.createdAt) {
                                            if (user.createdAt == null) "New Joiner"
                                            else {
                                                try {
                                                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                                    val created = sdf.parse(user.createdAt.take(10))
                                                    val now = java.util.Date()
                                                    val diff = now.time - created.time
                                                    val days = diff / (1000 * 60 * 60 * 24)
                                                    when {
                                                        days < 30 -> "New Joiner"
                                                        days < 365 -> "${days / 30} Months"
                                                        else -> "${String.format("%.1f", days / 365.0)} Years"
                                                    }
                                                } catch (e: Exception) { "New Joiner" }
                                            }
                                        }

                                        val experienceValue = if (!user.profession.isNullOrEmpty()) user.profession 
                                                            else experienceText

                                        StatMiniItem(
                                            icon = Icons.Default.Stars,
                                            value = experienceValue,
                                            label = "Experience",
                                            subtext = "On Madadwala",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White
                        ) {
                            Column {
                                ProfileOptionV2(
                                    label = stringResource(R.string.kyc_details),
                                    subtext = "View and update your KYC information",
                                    icon = Icons.Default.Badge,
                                    onClick = { onSubScreenChange("kyc") }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MadadwalaColors.LightGray.copy(alpha = 0.3f))
                                ProfileOptionV2(
                                    label = stringResource(R.string.service_categories),
                                    subtext = "Manage your service categories",
                                    icon = Icons.Default.Category
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MadadwalaColors.LightGray.copy(alpha = 0.3f))
                                ProfileOptionV2(
                                    label = stringResource(R.string.edit_service_charges),
                                    subtext = "Set and update your service charges",
                                    icon = Icons.Default.Payments,
                                    onClick = { onSubScreenChange("charges") }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MadadwalaColors.LightGray.copy(alpha = 0.3f))
                                ProfileOptionV2(
                                    label = stringResource(R.string.bank_details),
                                    subtext = "Manage your bank account details",
                                    icon = Icons.Default.AccountBalance,
                                    onClick = { onSubScreenChange("bank") }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MadadwalaColors.LightGray.copy(alpha = 0.3f))
                                ProfileOptionV2(
                                    label = stringResource(R.string.performance),
                                    subtext = "View your performance and analytics",
                                    icon = Icons.Default.Assessment,
                                    onClick = { onSubScreenChange("performance") }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MadadwalaColors.LightGray.copy(alpha = 0.3f))
                                ProfileOptionV2(
                                    label = stringResource(R.string.my_reviews),
                                    subtext = "View all your customer reviews and ratings",
                                    icon = Icons.Default.Star,
                                    onClick = { onSubScreenChange("reviews") }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MadadwalaColors.LightGray.copy(alpha = 0.3f))
                                ProfileOptionV2(
                                    label = stringResource(R.string.settings),
                                    subtext = "Manage app preferences and account settings",
                                    icon = Icons.Default.Settings,
                                    onClick = { onSubScreenChange("settings") }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MadadwalaColors.LightGray.copy(alpha = 0.3f))
                                ProfileOptionV2(
                                    label = stringResource(R.string.help_support),
                                    subtext = "Get help and contact support",
                                    icon = Icons.Default.SupportAgent,
                                    onClick = onSupportClick
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MadadwalaColors.LightGray.copy(alpha = 0.3f))
                                ProfileOptionV2(
                                    label = "About Madadwala",
                                    subtext = "Terms, policies and app information",
                                    icon = Icons.Default.Info,
                                    onClick = { legalContentType = "About" }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MadadwalaColors.LightGray.copy(alpha = 0.3f))
                                ProfileOptionV2(
                                    label = "Terms & Conditions",
                                    subtext = "Usage terms and legal rules",
                                    icon = Icons.Default.Description,
                                    onClick = { legalContentType = "Terms" }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MadadwalaColors.LightGray.copy(alpha = 0.3f))
                                ProfileOptionV2(
                                    label = "Privacy Policy",
                                    subtext = "How we handle your data",
                                    icon = Icons.Default.PrivacyTip,
                                    onClick = { legalContentType = "Privacy" }
                                )
                            }
                        }
                    }

                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable { onLogout() },
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MadadwalaColors.LightGray.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MadadwalaColors.Red, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(stringResource(R.string.logout), fontWeight = FontWeight.Bold, color = MadadwalaColors.Red)
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
            "kyc" -> KycSubScreen(user) { onSubScreenChange(null) }
            "bank" -> BankDetailsSubScreen(user.uid, user.bankDetails, onRefresh) { onSubScreenChange(null) }
            "performance" -> PerformanceSubScreen(user.uid) { onSubScreenChange(null) }
            "withdrawals" -> WithdrawalHistorySubScreen(user.uid) { onSubScreenChange(null) }
            "reviews" -> ReviewsSubScreen(reviews) { onSubScreenChange(null) }
            "settings" -> SettingsSubScreen(preferenceManager) { onSubScreenChange(null) }
            "charges" -> ChargesEditor(
                services = services,
                onBack = { onSubScreenChange(null) },
                onUpdate = onUpdateServicePrice,
                onAdd = onAddService
            )
        }
    }

    if (legalContentType != null) {
        PartnerLegalContentDialog(
            type = legalContentType!!,
            onDismiss = { legalContentType = null }
        )
    }
}

@Composable
fun PartnerLegalContentDialog(type: String, onDismiss: () -> Unit) {
    val title = when(type) {
        "About" -> "About Madadwala"
        "Terms" -> "Terms & Conditions"
        "Privacy" -> "Privacy Policy"
        else -> "Information"
    }

    val content = when(type) {
        "About" -> """
            Madadwala is your one-stop solution for all home service needs. We connect you with verified, background-checked professionals for services ranging from plumbing and electrical work to deep cleaning and beauty services.
            
            Our mission is to empower local service providers while ensuring homeowners receive top-quality, reliable, and safe services at fair prices.
            
            Version: 1.0.4 (Stable)
            Developed by: Abhi Gandhi
        """.trimIndent()
        "Terms" -> """
            1. Acceptance of Terms: By joining Madadwala as a partner, you agree to follow our professional code of conduct.
            
            2. Service Quality: Partners are expected to provide high-quality service. Low ratings may lead to account suspension.
            
            3. Commission: A standard commission of 15% is deducted from all booking amounts processed through the platform.
            
            4. Payouts: Withdrawal requests are processed within 3-4 hours after approval.
            
            5. Safety: Background verification is mandatory for all partners. Any fraudulent activity will be reported to the authorities.
        """.trimIndent()
        "Privacy" -> """
            1. Data Collection: We collect your professional details, Aadhaar information, and live location to assign jobs.
            
            2. Data Usage: Your location and profile are shared with customers only when you accept their booking.
            
            3. Security: Your sensitive documents and banking details are encrypted and stored securely.
            
            4. Third Parties: We do not share your private data with external agencies except for verification purposes.
            
            5. Controls: You can manage your profile visibility and availability status directly from the dashboard.
        """.trimIndent()
        else -> ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(title, fontWeight = FontWeight.Bold, color = MadadwalaColors.Teal) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MadadwalaColors.Ink,
                    lineHeight = 22.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Teal),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun ProfileOptionV2(
    label: String,
    subtext: String,
    icon: ImageVector,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MadadwalaColors.Green.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MadadwalaColors.Green, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MadadwalaColors.Ink)
            Text(subtext, style = MaterialTheme.typography.labelSmall, color = MadadwalaColors.Gray)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun StatMiniItem(
    icon: ImageVector,
    value: String,
    label: String,
    subtext: String? = null,
    link: String? = null,
    onLinkClick: () -> Unit = {},
    showStars: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MadadwalaColors.Green.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MadadwalaColors.Green, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            color = MadadwalaColors.Ink,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MadadwalaColors.Gray,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            lineHeight = 10.sp
        )
        
        if (showStars) {
            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                val rating = value.toDoubleOrNull() ?: 0.0
                repeat(5) { i ->
                    Icon(
                        Icons.Default.Star,
                        null,
                        tint = if (i < rating.toInt()) MadadwalaColors.Green else MadadwalaColors.LightGray,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
        
        if (subtext != null) {
            Text(subtext, style = MaterialTheme.typography.labelSmall, color = MadadwalaColors.Gray, fontSize = 9.sp, textAlign = TextAlign.Center)
        }
        
        if (link != null) {
            Text(
                text = link,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MadadwalaColors.Green,
                    fontWeight = FontWeight.Bold,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                    fontSize = 9.sp
                ),
                modifier = Modifier.clickable { onLinkClick() }
            )
        }
    }
}


@Composable
fun KycSubScreen(user: UserResponse, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var newAadhaarNumber by remember { mutableStateOf(user.aadhaarNumber ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selectedImageUri = it }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MadadwalaColors.Ink) }
            Text("KYC Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MadadwalaColors.Ink)
        }
        Spacer(modifier = Modifier.height(24.dp))

        if (user.kycRejected) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                color = MadadwalaColors.Red.copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MadadwalaColors.Red.copy(alpha = 0.1f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = MadadwalaColors.Red, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Registration Rejected", fontWeight = FontWeight.Bold, color = MadadwalaColors.Red)
                    }
                    Text(
                        text = "Reason: ${user.kycRejectionReason ?: "Information provided was incorrect or blurry."}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MadadwalaColors.Ink.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "Please update your Aadhaar details below to re-apply.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MadadwalaColors.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), Color.White, tonalElevation = 2.dp) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (user.kycRejected) {
                    OutlinedTextField(
                        value = newAadhaarNumber,
                        onValueChange = { if (it.length <= 12 && it.all { c -> c.isDigit() }) newAadhaarNumber = it },
                        label = { Text("Aadhaar Number") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MadadwalaColors.Teal,
                            unfocusedBorderColor = MadadwalaColors.LightGray
                        )
                    )

                    Surface(
                        onClick = { photoPicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MadadwalaColors.Cream.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedImageUri != null) MadadwalaColors.Teal else Color.LightGray)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (selectedImageUri != null) {
                                AsyncImage(model = selectedImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddPhotoAlternate, null, tint = MadadwalaColors.Teal, modifier = Modifier.size(32.dp))
                                    Text("Re-upload Aadhaar Image", fontSize = 12.sp, color = MadadwalaColors.Teal)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (newAadhaarNumber.length < 12) {
                                Toast.makeText(context, "Invalid Aadhaar Number", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSubmitting = true
                            scope.launch {
                                try {
                                    val aadhaarNoBody = newAadhaarNumber.toRequestBody("text/plain".toMediaTypeOrNull())
                                    var aadhaarPart: MultipartBody.Part? = null
                                    
                                    selectedImageUri?.let { uri ->
                                        val inputStream = context.contentResolver.openInputStream(uri)
                                        val file = java.io.File(context.cacheDir, "reupload_aadhaar.jpg")
                                        inputStream?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                                        aadhaarPart = MultipartBody.Part.createFormData("aadhaarImage", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
                                    }

                                    val res = RetrofitClient.apiService.updateKyc(user.uid, aadhaarNoBody, aadhaarPart)
                                    if (res.isSuccessful) {
                                        Toast.makeText(context, "KYC details updated. Under review.", Toast.LENGTH_LONG).show()
                                        onBack()
                                    } else {
                                        Toast.makeText(context, "Update failed: ${res.message()}", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Teal),
                        enabled = !isSubmitting && (newAadhaarNumber != user.aadhaarNumber || selectedImageUri != null)
                    ) {
                        if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        else Text("Update & Re-apply", fontWeight = FontWeight.Bold)
                    }
                } else {
                    val displayAadhaar = if (user.aadhaarNumber.isNullOrEmpty()) "Verified" 
                                        else "**** **** ${user.aadhaarNumber.takeLast(4)}"
                                        
                    val displayDate = if (user.verificationDate.isNullOrEmpty()) {
                        if (user.isVerified) user.createdAt?.take(10) ?: "Member" else "Pending"
                    } else user.verificationDate

                    KycRow("Aadhaar Number", displayAadhaar)
                    KycRow("Verification Date", displayDate)
                    KycRow("Status", if (user.isVerified) "Verified" else "Under Review")
                    
                    if (!user.isVerified && user.aadhaarImage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text("Submitted Aadhaar Card:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        AsyncImage(
                            model = user.aadhaarImage,
                            contentDescription = "Aadhaar",
                            modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BankDetailsSubScreen(uid: String, initialDetails: BankDetailsResponse?, onRefresh: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var accNo by remember(initialDetails) { mutableStateOf(initialDetails?.accountNumber ?: "") }
    var reAccNo by remember(initialDetails) { mutableStateOf(initialDetails?.accountNumber ?: "") }
    var ifsc by remember(initialDetails) { mutableStateOf(initialDetails?.ifscCode ?: "") }
    var holderName by remember(initialDetails) { mutableStateOf(initialDetails?.accountHolderName ?: "") }

    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(initialDetails) {
        if (initialDetails != null) {
            accNo = initialDetails.accountNumber ?: ""
            reAccNo = initialDetails.accountNumber ?: ""
            ifsc = initialDetails.ifscCode ?: ""
            holderName = initialDetails.accountHolderName ?: ""
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Bank Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = accNo, 
                onValueChange = { accNo = it }, 
                label = { Text("Account Number") }, 
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
            OutlinedTextField(
                value = reAccNo, 
                onValueChange = { reAccNo = it }, 
                label = { Text("Re-enter Account Number") }, 
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
            OutlinedTextField(
                value = ifsc, 
                onValueChange = { ifsc = it.uppercase() }, 
                label = { Text("IFSC Code") }, 
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = holderName, 
                onValueChange = { holderName = it }, 
                label = { Text("Account Holder Name") }, 
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (accNo != reAccNo) {
                        Toast.makeText(context, "Account numbers do not match", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (accNo.isEmpty() || ifsc.isEmpty() || holderName.isEmpty()) {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    scope.launch {
                        isSaving = true
                        try {
                            val res = com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.updateBankDetails(
                                uid, 
                                mapOf(
                                    "accountNumber" to accNo, 
                                    "ifscCode" to ifsc, 
                                    "accountHolderName" to holderName
                                )
                            )
                            if (res.isSuccessful) {
                                onRefresh()
                                Toast.makeText(context, "Saved Successfully", Toast.LENGTH_SHORT).show()
                                delay(500) // Small delay to allow state refresh
                                onBack()
                            } else {
                                Toast.makeText(context, "Failed to save: ${res.message()}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Teal),
                enabled = !isSaving
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                else Text("Save Details", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PerformanceSubScreen(uid: String, onBack: () -> Unit) {
    var report by remember { mutableStateOf<com.abhi.madadwala_1.data.remote.PerformanceReportResponse?>(null) }
    LaunchedEffect(Unit) {
        try {
            val res = com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.getPerformanceReport(uid)
            if (res.isSuccessful) report = res.body()
        } catch (e: Exception) {}
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                Text("Performance Report", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatCard(
                    label = "Total Work", 
                    value = report?.totalWork?.toString() ?: "0", 
                    icon = Icons.Default.Work, 
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                StatCard(
                    label = "Earned", 
                    value = "₹${report?.totalEarned ?: 0}", 
                    icon = Icons.Default.Payments, 
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            val joinDate = report?.joinDate?.take(10) ?: "..."
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), Color.White, tonalElevation = 1.dp) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EventAvailable, null, tint = MadadwalaColors.Teal)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Partner Since", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(joinDate, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("Monthly Attendance", fontWeight = FontWeight.Bold, color = MadadwalaColors.Teal)
            Spacer(modifier = Modifier.height(8.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), Color.White, tonalElevation = 1.dp) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Online (Green)", style = MaterialTheme.typography.labelSmall, color = MadadwalaColors.GreenDark)
                        Text("Offline (Red)", style = MaterialTheme.typography.labelSmall, color = MadadwalaColors.Red)
                    }
                    Spacer(Modifier.height(12.dp))
                    
                    // Simple Calendar Grid
                    val activity = report?.dailyActivity ?: emptyList()
                    Box(modifier = Modifier.height(200.dp)) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(7),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(activity) { log ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .background(if (log.isOnline) MadadwalaColors.Green.copy(0.2f) else MadadwalaColors.Red.copy(0.1f))
                                        .border(1.dp, if (log.isOnline) MadadwalaColors.Green else MadadwalaColors.Red, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = log.date.takeLast(2),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (log.isOnline) MadadwalaColors.GreenDark else MadadwalaColors.Red
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("Weekly Earnings Graph", fontWeight = FontWeight.Bold, color = MadadwalaColors.Teal)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().height(150.dp).background(Color.White, RoundedCornerShape(16.dp)).padding(20.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (report?.weekly ?: listOf(0.0)).forEach {
                    val h = (it / 10).coerceIn(4.0, 110.0).dp
                    Box(Modifier.width(24.dp).height(h).background(MadadwalaColors.Teal, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                }
            }
        }

        item {
            Text("Provider Insights", fontWeight = FontWeight.Bold, color = MadadwalaColors.Teal)
            Surface(Modifier.fillMaxWidth().padding(top = 8.dp), RoundedCornerShape(16.dp), Color.White) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    KycRow("Average Rating", "${String.format("%.1f", report?.averageRating ?: 0.0)} ★")
                    KycRow("Completion Rate", "${report?.completionRate ?: 100}%")
                    KycRow("On-time Arrival", "${report?.onTimeArrival ?: 95}%")
                }
            }
        }
    }
}

@Composable
fun SettingsSubScreen(preferenceManager: PreferenceManager, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isNotificationSoundEnabled by preferenceManager.isNotificationSoundEnabled.collectAsState(initial = true)
    val appLanguage by preferenceManager.appLanguage.collectAsState(initial = "en")
    
    var showLanguagePicker by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = Color.White
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MadadwalaColors.Teal, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(stringResource(R.string.notification_sound), modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                Switch(
                    checked = isNotificationSoundEnabled,
                    onCheckedChange = { 
                        scope.launch { preferenceManager.setNotificationSoundEnabled(it) }
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = MadadwalaColors.Teal)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        ProfileOption(stringResource(R.string.app_language), Icons.Default.Language) { 
            showLanguagePicker = true
        }
    }

    if (showLanguagePicker) {
        val languages = listOf("English" to "en", "Hindi" to "hi", "Gujarati" to "gu")
        AlertDialog(
            onDismissRequest = { showLanguagePicker = false },
            containerColor = Color.White,
            title = { Text(stringResource(R.string.select_language), color = MadadwalaColors.Ink, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    languages.forEach { (name, code) ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                scope.launch {
                                    preferenceManager.setAppLanguage(code)
                                    val locale = java.util.Locale(code)
                                    java.util.Locale.setDefault(locale)
                                    val config = context.resources.configuration
                                    config.setLocale(locale)
                                    context.resources.updateConfiguration(config, context.resources.displayMetrics)
                                    
                                    showLanguagePicker = false
                                    Toast.makeText(context, "Language changed to $name", Toast.LENGTH_SHORT).show()
                                }
                            }.padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = appLanguage == code, 
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = MadadwalaColors.Teal)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(name, color = MadadwalaColors.Ink, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = { 
                TextButton(onClick = { showLanguagePicker = false }) { 
                    Text(stringResource(R.string.cancel), color = MadadwalaColors.Teal, fontWeight = FontWeight.Bold) 
                } 
            }
        )
    }
}

@Composable
fun ChargesEditor(
    services: List<ServiceResponse>,
    onBack: () -> Unit,
    onUpdate: (String, Double) -> Unit,
    onAdd: (String, Double) -> Unit
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Edit Service Charges", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, null, tint = MadadwalaColors.Teal) }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(services) { service ->
                ServicePriceCard(service, onUpdate)
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var price by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New Service") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Service Name") })
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAdd(name, price.toDoubleOrNull() ?: 0.0)
                    showAddDialog = false
                }) { Text("Add") }
            }
        )
    }
}

@Composable
fun ServicePriceCard(service: ServiceResponse, onUpdate: (String, Double) -> Unit) {
    var price by remember { mutableStateOf(service.price.toString()) }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        tonalElevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(service.name, fontWeight = FontWeight.Bold)
                Text("Current: ₹${service.price}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                modifier = Modifier.width(80.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(onClick = { onUpdate(service._id, price.toDoubleOrNull() ?: 0.0) }) {
                Icon(Icons.Default.Check, null, tint = MadadwalaColors.Green)
            }
        }
    }
}

@Composable
fun ProfileOption(label: String, icon: ImageVector, onClick: () -> Unit = {}) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = Color.White
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MadadwalaColors.Teal, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun KycRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MadadwalaColors.Gray, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun StatCard(
    label: String, 
    value: String, 
    icon: ImageVector, 
    buttonText: String? = null,
    onClick: () -> Unit = {},
    modifier: Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MadadwalaColors.LightGray.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MadadwalaColors.Green.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MadadwalaColors.Green,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    value, 
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = MadadwalaColors.Ink,
                        fontSize = 22.sp
                    )
                )
                
                if (label.contains("Ratings")) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        val ratingValue = value.toDoubleOrNull() ?: 0.0
                        repeat(5) { index ->
                            Icon(
                                Icons.Default.Star,
                                null,
                                tint = if (index < ratingValue.toInt()) MadadwalaColors.Green else MadadwalaColors.LightGray,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
                
                Text(
                    label, 
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MadadwalaColors.Gray,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                )
            }
            
            if (buttonText != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    modifier = Modifier
                        .clickable { onClick() },
                    shape = RoundedCornerShape(10.dp),
                    color = MadadwalaColors.Green.copy(alpha = 0.05f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            buttonText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = MadadwalaColors.Green,
                            maxLines = 1
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            null,
                            tint = MadadwalaColors.Green,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookingCard(
    booking: BookingResponse, 
    onViewDetails: () -> Unit,
    onAccept: (String, String) -> Unit,
    onReject: (String) -> Unit,
    onChat: () -> Unit,
    callViewModel: CallViewModel
) {
    val status = booking.status.uppercase()
    var showAcceptDialog by rememberSaveable { mutableStateOf(false) }
    var showRejectDialog by rememberSaveable { mutableStateOf(false) }
    var selectedTime by remember { mutableStateOf(booking.scheduledTime) }
    var partnerComment by remember { mutableStateOf("") }
    var rejectReason by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MadadwalaColors.LightGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(MadadwalaColors.Green.copy(alpha = 0.08f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                booking.serviceName.contains("Electric", ignoreCase = true) -> Icons.Default.FlashOn
                                booking.serviceName.contains("Plumb", ignoreCase = true) -> Icons.Default.Opacity
                                else -> Icons.Default.Build
                            },
                            contentDescription = null,
                            tint = MadadwalaColors.Green,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            booking.customerName ?: "Customer", 
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold, 
                                color = MadadwalaColors.Ink,
                                fontSize = 18.sp
                            )
                        )
                        Text(
                            booking.serviceName, 
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MadadwalaColors.Gray
                            )
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when(status) {
                            "COMPLETED", "DONE" -> MadadwalaColors.Green.copy(alpha = 0.1f)
                            "CANCELLED" -> MadadwalaColors.Red.copy(alpha = 0.1f)
                            "ACCEPTED" -> MadadwalaColors.Green.copy(alpha = 0.1f)
                            else -> MadadwalaColors.LightGray.copy(alpha = 0.5f)
                        }
                    ) {
                        Text(
                            text = when(status) {
                                "DONE" -> stringResource(R.string.status_done)
                                "ACCEPTED" -> "ACCEPTED"
                                "PENDING" -> stringResource(R.string.status_pending)
                                "CANCELLED" -> stringResource(R.string.status_cancelled)
                                "UPCOMING" -> stringResource(R.string.status_upcoming)
                                else -> status
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = when(status) {
                                    "COMPLETED", "DONE" -> MadadwalaColors.Green
                                    "CANCELLED" -> MadadwalaColors.Red
                                    "ACCEPTED" -> MadadwalaColors.Green
                                    else -> MadadwalaColors.Gray
                                }
                            )
                        )
                    }
                    if (!booking.paymentStatus.isNullOrEmpty()) {
                        val isPaid = booking.paymentStatus.lowercase() == "paid"
                        Text(
                            text = if (isPaid) "PAID" else "UNPAID",
                            modifier = Modifier.padding(top = 6.dp, end = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isPaid) MadadwalaColors.Green else MadadwalaColors.Red
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccessTime, 
                    contentDescription = null, 
                    modifier = Modifier.size(16.dp), 
                    tint = MadadwalaColors.Gray
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = booking.scheduledTime, 
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MadadwalaColors.Gray,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.clickable {
                    booking.customerLat?.let { lat ->
                        booking.customerLng?.let { lng ->
                            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng")
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                            intent.setPackage("com.google.android.apps.maps")
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")))
                            }
                        }
                    }
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MadadwalaColors.Green
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (booking.address.contains("Customer Location", ignoreCase = true)) "View Customer Location" else booking.address,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (booking.address.contains("Customer Location", ignoreCase = true)) MadadwalaColors.Green else MadadwalaColors.Gray,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (status == "PENDING" || status == "REQUESTED" || status == "UPCOMING") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { showRejectDialog = true },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MadadwalaColors.Red),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MadadwalaColors.Red)
                    ) {
                        Text(stringResource(R.string.reject), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Button(
                        onClick = { showAcceptDialog = true },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Green)
                    ) {
                        Text(stringResource(R.string.accept), color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onViewDetails,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.GreenDark)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "View Job Details", 
                                color = Color.White, 
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(16.dp))
                        }
                    }
                    
                    Surface(
                        modifier = Modifier
                            .height(52.dp)
                            .weight(0.42f)
                            .clickable { 
                                callViewModel.startCall(
                                    bookingId = booking._id,
                                    customerId = booking.customerUid,
                                    partnerId = booking.providerUid,
                                    partnerName = booking.customerName ?: "Customer"
                                )
                            },
                        color = MadadwalaColors.LightGray.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Call, null, tint = MadadwalaColors.Green, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Call", 
                                color = MadadwalaColors.Green,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .height(52.dp)
                            .weight(0.42f)
                            .clickable { onChat() },
                        color = MadadwalaColors.LightGray.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Chat, null, tint = MadadwalaColors.Green, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Chat",
                                color = MadadwalaColors.Green,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAcceptDialog) {
        AlertDialog(
            onDismissRequest = { showAcceptDialog = false },
            title = { Text("Accept Booking", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Provide arrival time and any comment for the customer.")
                    OutlinedTextField(
                        value = selectedTime,
                        onValueChange = { selectedTime = it },
                        label = { Text("Arrival Time") },
                        placeholder = { Text("e.g., 10 mins, 4:30 PM") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = partnerComment,
                        onValueChange = { partnerComment = it },
                        label = { Text("Comment (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAccept(selectedTime, partnerComment)
                        showAcceptDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Teal)
                ) {
                    Text("Confirm & Accept", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAcceptDialog = false }) {
                    Text("Cancel", color = MadadwalaColors.Gray, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Reject Booking", fontWeight = FontWeight.Bold, color = MadadwalaColors.Ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Please provide a reason for rejecting this booking. This will be shared with the customer.", color = MadadwalaColors.Ink)
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("Reason (Optional)") },
                        placeholder = { Text("e.g. Too far, already busy") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReject(rejectReason)
                        showRejectDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Red)
                ) {
                    Text("Confirm & Reject", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Go Back", color = MadadwalaColors.Gray, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun CustomRequestCard(
    request: CustomRequestResponse,
    onAccept: (Double) -> Unit,
    onReject: () -> Unit
) {
    var bidPrice by remember { mutableStateOf("") }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(request.customerName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MadadwalaColors.Ink))
                    Text(request.category, style = MaterialTheme.typography.bodySmall.copy(color = MadadwalaColors.Teal))
                }
                
                val priceText = if (request.isAutoPrice) "Auto Price" 
                               else if (request.maxPrice != null) "₹${request.minPrice} - ${request.maxPrice}"
                               else "₹${request.minPrice}"
                
                Text(
                    text = priceText,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, color = MadadwalaColors.GreenDark)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = request.problem,
                style = MaterialTheme.typography.bodyMedium,
                color = MadadwalaColors.Gray,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MadadwalaColors.LightGray.copy(alpha = 0.8f))
            
            if (request.isAutoPrice) {
                OutlinedTextField(
                    value = bidPrice,
                    onValueChange = { bidPrice = it },
                    label = { Text("Your Price (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                
                val currentPrice = bidPrice.toDoubleOrNull() ?: 0.0
                if (currentPrice > 0) {
                    val finalPrice = currentPrice * 1.05

                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Job Value", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MadadwalaColors.Ink)
                        Text("₹${String.format("%.2f", currentPrice)}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MadadwalaColors.Ink)
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Amount to Collect from Customer", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("₹${String.format("%.2f", finalPrice)}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MadadwalaColors.GreenDark)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MadadwalaColors.Red),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MadadwalaColors.Red)
                ) {
                    Text("Reject", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
                Button(
                    onClick = { 
                        if (request.isAutoPrice) {
                            bidPrice.toDoubleOrNull()?.let { onAccept(it) }
                        } else {
                            onAccept(request.minPrice ?: 0.0)
                        }
                    },
                    modifier = Modifier.weight(1f).height(40.dp),
                    enabled = !request.isAutoPrice || bidPrice.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Teal)
                ) {
                    Text(if (request.isAutoPrice) "Send Bid" else "Accept", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
fun FilterChipV2(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    iconColor: Color = MadadwalaColors.Ink
) {
    Surface(
        modifier = Modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF1A302A) else Color.White,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F0F0))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else iconColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$label ($count)",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else Color(0xFF1A1A1A),
                    fontSize = 12.sp
                )
            )
        }
    }
}

@Composable
fun ModernBookingCard(
    booking: BookingResponse,
    onViewDetails: () -> Unit,
    onReject: (String) -> Unit,
    onChat: () -> Unit,
    callViewModel: CallViewModel
) {
    val status = booking.status.uppercase()
    val isDone = status == "DONE" || status == "COMPLETED"
    val isAccepted = status == "ACCEPTED"
    val isPending = status == "PENDING" || status == "REQUESTED" || status == "UPCOMING"
    val isArrived = status == "ARRIVED"
    val isCancelled = status == "CANCELLED"
    val isPaid = booking.paymentStatus?.lowercase() == "paid"

    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }

    val statusColor = when {
        isDone -> Color(0xFF4CAF50)      // Green
        isArrived -> Color(0xFFFF9800)   // Orange
        isAccepted -> Color(0xFF4CAF50)  // Green
        isPending -> Color(0xFFFBC02D)   // Amber
        isCancelled -> Color(0xFFDC4A3E) // Red
        else -> Color.Gray
    }

    val statusBgColor = statusColor.copy(alpha = 0.1f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F0F0))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = statusBgColor,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                booking.serviceName.contains("Electrical", ignoreCase = true) -> Icons.Default.Bolt
                                booking.serviceName.contains("Light", ignoreCase = true) -> Icons.Default.Lightbulb
                                isDone -> Icons.Default.CheckCircle
                                isArrived -> Icons.Default.Bolt
                                else -> Icons.Default.Info
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = booking.customerName ?: "Customer",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A1A),
                                fontSize = 18.sp
                            )
                        )
                        Text(
                            text = booking.serviceName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = statusBgColor
                    ) {
                        Text(
                            text = status,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = statusColor,
                                fontSize = 10.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isPaid) "PAID" else "UNPAID",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = if (isPaid) Color(0xFF4CAF50) else Color(0xFFDC4A3E),
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = booking.scheduledTime,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = booking.address,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Booking ID",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 11.sp)
                    )
                    Text(
                        text = "#${booking._id.takeLast(8).uppercase()}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isDone && !isCancelled) {
                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { onChat() },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF5F5F5)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Chat",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    
                    Button(
                        onClick = onViewDetails,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDone) Color.White else Color(0xFF1B3C2D)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = if (isDone) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)) else null,
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isDone) "View Summary" else "View Details",
                                color = if (isDone) Color.Black else Color.White,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (isDone) Color.Black else Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Reject Booking", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Please provide a reason for rejecting this booking.")
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("Reason (Optional)") },
                        placeholder = { Text("e.g. Too far, already busy") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReject(rejectReason)
                        showRejectDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Red)
                ) {
                    Text("Confirm & Reject", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Go Back")
                }
            }
        )
    }
}

@Composable
fun CustomJobRequestNotification(request: CustomRequestResponse, onAccept: (Double) -> Unit, onReject: () -> Unit) {
    var timeLeft by remember { mutableFloatStateOf(1.0f) }
    var bidPrice by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        val duration = 30000L // 30 seconds to accept
        val startTime = System.currentTimeMillis()
        while (timeLeft > 0) {
            val elapsed = System.currentTimeMillis() - startTime
            timeLeft = (1.0f - (elapsed.toFloat() / duration)).coerceAtLeast(0f)
            if (timeLeft <= 0) onReject()
            kotlinx.coroutines.delay(50)
        }
    }

    Surface(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        tonalElevation = 12.dp,
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { timeLeft },
                    modifier = Modifier.size(80.dp),
                    color = MadadwalaColors.Teal,
                    strokeWidth = 6.dp,
                    trackColor = MadadwalaColors.LightGray
                )
                Text(
                    text = (timeLeft * 30).toInt().toString(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = MadadwalaColors.Teal
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "NEW CUSTOM REQUEST",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = MadadwalaColors.Gray,
                    letterSpacing = 2.sp
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                request.category,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MadadwalaColors.Cream, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("PROBLEM", style = MaterialTheme.typography.labelSmall.copy(color = MadadwalaColors.Gray))
                Text(request.problem, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), textAlign = TextAlign.Center)
                
                Spacer(modifier = Modifier.height(12.dp))
                
                val priceText = if (request.isAutoPrice) "Auto Price" 
                               else if (request.maxPrice != null) "₹${request.minPrice} - ₹${request.maxPrice}"
                               else "₹${request.minPrice}"
                               
                Text("BUDGET", style = MaterialTheme.typography.labelSmall.copy(color = MadadwalaColors.Gray))
                Text(priceText, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MadadwalaColors.GreenDark))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (request.isAutoPrice) {
                OutlinedTextField(
                    value = bidPrice,
                    onValueChange = { bidPrice = it },
                    label = { Text("Your Price (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                
                val currentPrice = bidPrice.toDoubleOrNull() ?: 0.0
                if (currentPrice > 0) {
                    val finalPrice = currentPrice * 1.05

                    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Job Value", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MadadwalaColors.Ink)
                        Text("₹${String.format("%.2f", currentPrice)}", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MadadwalaColors.Ink)
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Amount to Collect from Customer", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("₹${String.format("%.2f", finalPrice)}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MadadwalaColors.GreenDark)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.LightGray),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Reject", color = MadadwalaColors.Ink, fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = { 
                        if (request.isAutoPrice) {
                            bidPrice.toDoubleOrNull()?.let { onAccept(it) }
                        } else {
                            onAccept(request.minPrice ?: 0.0)
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    enabled = !request.isAutoPrice || bidPrice.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Teal),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (request.isAutoPrice) "Send Bid" else "Accept", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ReviewsSubScreen(reviews: List<ReviewResponse>, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MadadwalaColors.Ink) }
            Text(stringResource(R.string.my_reviews), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MadadwalaColors.Ink)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (reviews.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Star, null, modifier = Modifier.size(64.dp), tint = MadadwalaColors.LightGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No reviews yet", color = MadadwalaColors.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    ProviderRatingSummary(reviews = reviews)
                }
                
                item {
                    Text(
                        text = "Customer Feedback",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MadadwalaColors.Ink,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(reviews) { review ->
                    ProviderReviewCard(review)
                }
            }
        }
    }
}

@Composable
fun ProviderRatingSummary(reviews: List<ReviewResponse>) {
    val totalReviews = reviews.size
    val averageRating = if (totalReviews > 0) reviews.map { it.rating }.average() else 0.0
    val ratingCounts = reviews.groupingBy { it.rating }.eachCount()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, MadadwalaColors.LightGray.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(end = 32.dp)
            ) {
                Text(
                    text = String.format("%.1f", averageRating),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = MadadwalaColors.Ink
                )
                Row {
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (index < averageRating.toInt()) MadadwalaColors.Amber else MadadwalaColors.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$totalReviews Reviews",
                    style = MaterialTheme.typography.labelSmall,
                    color = MadadwalaColors.Gray
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                for (i in 5 downTo 1) {
                    val count = ratingCounts[i] ?: 0
                    val progress = if (totalReviews > 0) count.toFloat() / totalReviews else 0f
                    ProviderRatingBar(label = "$i", progress = progress)
                }
            }
        }
    }
}

@Composable
fun ProviderRatingBar(label: String, progress: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = label, 
            fontSize = 12.sp, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(12.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(CircleShape),
            color = MadadwalaColors.Green,
            trackColor = MadadwalaColors.LightGray.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun ProviderReviewCard(review: ReviewResponse) {
    val formattedDate = remember(review.createdAt) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date = sdf.parse(review.createdAt)
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date!!)
        } catch (e: Exception) {
            review.createdAt.split("T").firstOrNull() ?: ""
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, MadadwalaColors.LightGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val avatarColor = remember(review.customerName) {
                    val colors = listOf(
                        Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFF9C27B0),
                        Color(0xFFFF9800), Color(0xFFE91E63), Color(0xFF00BCD4)
                    )
                    colors[review.customerName.length % colors.size]
                }
                
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(avatarColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = review.customerName.take(1).uppercase(),
                        color = avatarColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = review.customerName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MadadwalaColors.Ink)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (index < review.rating) MadadwalaColors.Amber else MadadwalaColors.LightGray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formattedDate,
                            fontSize = 12.sp,
                            color = MadadwalaColors.Gray
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = review.comment,
                fontSize = 14.sp,
                color = MadadwalaColors.Ink.copy(alpha = 0.8f),
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun WithdrawalHistorySubScreen(uid: String, onBack: () -> Unit) {
    var history by remember { mutableStateOf<List<WithdrawalResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uid) {
        try {
            val res = RetrofitClient.apiService.getWithdrawalHistory(uid)
            if (res.isSuccessful) history = res.body() ?: emptyList()
        } catch (e: Exception) {} finally { isLoading = false }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MadadwalaColors.Ink) }
            Text("Withdrawal History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MadadwalaColors.Ink)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = MadadwalaColors.Teal) }
        } else if (history.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("No payout history yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(history) { req ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MadadwalaColors.LightGray.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Amount", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text("₹${req.amount}", fontWeight = FontWeight.Black, fontSize = 20.sp, color = MadadwalaColors.Ink)
                                }
                                
                                val statusColor = when(req.status.lowercase()) {
                                    "pending" -> MadadwalaColors.Amber
                                    "paid", "approved" -> MadadwalaColors.Green
                                    "rejected", "failed" -> MadadwalaColors.Red
                                    else -> Color.Gray
                                }
                                
                                Surface(
                                    color = statusColor.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = req.status.uppercase(),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = statusColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MadadwalaColors.LightGray.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalance, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("A/C: ${req.accountNumber}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                Icon(Icons.Default.Event, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = req.createdAt.take(10), 
                                    style = MaterialTheme.typography.bodySmall, 
                                    color = Color.Gray
                                )
                            }

                            if (!req.rejectionReason.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Rejection Reason: ${req.rejectionReason}",
                                    color = MadadwalaColors.Red,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
