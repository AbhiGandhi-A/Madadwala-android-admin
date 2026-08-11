package com.abhi.madadwala_1.ui.screens

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.location.Location
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.abhi.madadwala_1.ui.components.TransactionItemV2
import com.abhi.madadwala_1.ui.navigation.Screen
import com.abhi.madadwala_1.ui.theme.MadadwalaColors
import com.abhi.madadwala_1.ui.viewmodel.AuthState
import com.abhi.madadwala_1.ui.viewmodel.AuthViewModel
import com.abhi.madadwala_1.ui.viewmodel.LocationViewModel
import com.abhi.madadwala_1.data.PreferenceManager
import com.google.accompanist.permissions.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import android.location.Geocoder
import com.airbnb.lottie.compose.*
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import com.abhi.madadwala_1.ui.viewmodel.CallViewModel
import com.abhi.madadwala_1.utils.InvoiceGenerator

@Composable
fun Modifier.bounceClick(onClick: () -> Unit = {}) = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "BounceScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                },
                onTap = { onClick() }
            )
        }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    val authViewModel: AuthViewModel = viewModel()
    val callViewModel: CallViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )
    val authState by authViewModel.authState.collectAsState()
    val locationViewModel: LocationViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )
    val address by locationViewModel.address.collectAsState()
    val userLat by locationViewModel.latitude.collectAsState()
    val userLng by locationViewModel.longitude.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        locationViewModel.init(context)
    }

    val preferenceManager = remember { PreferenceManager(context) }
    val unreadCount by preferenceManager.unreadNotificationsCount.collectAsState(initial = 0)

    val user = (authState as? AuthState.Authenticated)?.user
    val userName = user?.name ?: "User"
    val uid = user?.uid ?: ""

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showLocationPicker by rememberSaveable { mutableStateOf(false) }
    var activeChatBookingId by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler(enabled = selectedTab != 0 || showLocationPicker) {
        if (showLocationPicker) {
            showLocationPicker = false
        } else {
            selectedTab = 0
        }
    }

    // Hoisted Home Tab Data
    var categories by remember { mutableStateOf<List<com.abhi.madadwala_1.data.remote.CategoryResponse>>(emptyList()) }
    var recentBookings by remember { mutableStateOf<List<com.abhi.madadwala_1.data.remote.BookingResponse>>(emptyList()) }
    var banners by remember { mutableStateOf<List<com.abhi.madadwala_1.data.remote.BannerResponse>>(emptyList()) }
    var nearbyProviders by remember { mutableStateOf<List<com.abhi.madadwala_1.data.remote.ProviderResponse>>(emptyList()) }
    var isProvidersLoading by remember { mutableStateOf(true) }
    var operationalCities by remember { mutableStateOf<List<String>>(emptyList()) }
    var isCityOperational by remember { mutableStateOf(false) }
    var cityCheckLoading by remember { mutableStateOf(true) }
    var isCitiesLoaded by remember { mutableStateOf(false) }

    // Global Request Tracking
    val sessionStartTime = remember { System.currentTimeMillis() }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var myRequests by remember { mutableStateOf<List<com.abhi.madadwala_1.data.remote.CustomRequestResponse>>(emptyList()) }

    LaunchedEffect(myRequests.isNotEmpty()) {
        if (myRequests.isNotEmpty()) {
            while (true) {
                currentTime = System.currentTimeMillis()
                delay(1000)
            }
        }
    }
    var acceptanceData by remember { mutableStateOf<AcceptanceData?>(null) }
    var prevPendingIds by remember { mutableStateOf(setOf<String>()) }
    var processedIds by remember { mutableStateOf(setOf<String>()) }
    val sdf = remember { java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") } }

    LaunchedEffect(uid) {
        if (uid.isNotBlank()) {
            while(true) {
                try {
                    val response = com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.getCustomerCustomRequests(uid)
                    if (response.isSuccessful) {
                        val allRequests = response.body() ?: emptyList()

                        // 1. Filter for REAL-TIME pending requests (created during this session)
                        val currentPending = allRequests.filter { req ->
                            try {
                                val createdTime = sdf.parse(req.createdAt)?.time ?: 0L
                                req.status == "pending" &&
                                        !processedIds.contains(req._id) &&
                                        createdTime >= (sessionStartTime - 30000L) // 30s buffer
                            } catch (_: Exception) { false }
                        }
                        myRequests = currentPending
                        val currentPendingIds = currentPending.map { it._id }.toSet()

                        // 2. Check for requests that shifted to 'accepted' status (only from this session)
                        val recentlyAccepted = allRequests.find { req ->
                            try {
                                val createdTime = sdf.parse(req.createdAt)?.time ?: 0L
                                req.status == "accepted" &&
                                        !processedIds.contains(req._id) &&
                                        createdTime >= (sessionStartTime - 30000L)
                            } catch (_: Exception) { false }
                        }

                        if (recentlyAccepted != null) {
                            val nameFromRequest = recentlyAccepted.acceptedProviderName
                                ?: recentlyAccepted.bids.find { it.providerUid == recentlyAccepted.acceptedProviderUid }?.providerName

                            // If we still don't have a name, try to find it in the nearby providers list
                            val finalProviderName = nameFromRequest ?: nearbyProviders.find { it.uid == recentlyAccepted.acceptedProviderUid }?.name ?: "Partner"

                            acceptanceData = AcceptanceData(
                                id = recentlyAccepted._id,
                                category = recentlyAccepted.category,
                                providerName = finalProviderName
                            )
                            processedIds = processedIds + recentlyAccepted._id
                        } else {
                            // 3. Detect if a request from THIS session disappeared (likely accepted and converted to booking)
                            val disappearedIds = (prevPendingIds - allRequests.map { it._id }.toSet())

                            if (disappearedIds.isNotEmpty()) {
                                val bookingsRes = com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.getCustomerBookings(uid)
                                if (bookingsRes.isSuccessful) {
                                    val bookings = bookingsRes.body() ?: emptyList()
                                    // Find a booking created recently in this session
                                    val newBooking = bookings.find { b ->
                                        try {
                                            val createdTime = sdf.parse(b.createdAt)?.time ?: 0L
                                            createdTime >= (sessionStartTime - 30000L) && !processedIds.contains(b._id)
                                        } catch (_: Exception) { false }
                                    }
                                    if (newBooking != null) {
                                        var realProviderName = if (newBooking.providerName.isNullOrBlank()) {
                                            // Try to get name from the bids of the request that disappeared
                                            val lastKnownReq = allRequests.find { disappearedIds.contains(it._id) }
                                            lastKnownReq?.bids?.find { it.providerUid == newBooking.providerUid }?.providerName
                                        } else newBooking.providerName

                                        // Final fallback to nearby providers
                                        if (realProviderName.isNullOrBlank()) {
                                            realProviderName = nearbyProviders.find { it.uid == newBooking.providerUid }?.name ?: "Partner"
                                        }

                                        acceptanceData = AcceptanceData(
                                            id = newBooking._id,
                                            category = newBooking.serviceName,
                                            providerName = realProviderName
                                        )
                                        processedIds = processedIds + newBooking._id
                                    }
                                }
                            }
                        }
                        prevPendingIds = currentPendingIds
                    }
                } catch (_: Exception) {}
                kotlinx.coroutines.delay(5000)
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val res = com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.getOperationalCities()
            if (res.isSuccessful) {
                operationalCities = res.body()?.map { it.name } ?: emptyList()
                isCitiesLoaded = true
            }
        } catch (_: Exception) {
            isCitiesLoaded = true
        }
    }

    // Fetch data in background
    LaunchedEffect(uid) {
        if (uid.isBlank()) return@LaunchedEffect

        while(true) {
            try {
                val catResponse = com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.getCategories()
                if (catResponse.isSuccessful) {
                    categories = catResponse.body() ?: emptyList()
                }

                val bookingResponse = com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.getCustomerBookings(uid)
                if (bookingResponse.isSuccessful) {
                    recentBookings = bookingResponse.body() ?: emptyList()
                }

                val bannerResponse = com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.getBanners()
                if (bannerResponse.isSuccessful) {
                    banners = bannerResponse.body() ?: emptyList()
                }

                val citiesResponse = com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.getOperationalCities()
                if (citiesResponse.isSuccessful) {
                    operationalCities = citiesResponse.body()?.map { it.name } ?: emptyList()
                    isCitiesLoaded = true
                }

                val providerResponse = com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.getProviders(
                    lat = if (userLat != 0.0) userLat else null,
                    lng = if (userLng != 0.0) userLng else null
                )
                if (providerResponse.isSuccessful) {
                    nearbyProviders = providerResponse.body() ?: emptyList()
                }
            } catch (_: Exception) {} finally {
                isProvidersLoading = false
            }
            delay(10000) // Refresh every 10 seconds
        }
    }

    // Location Permission
    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    LaunchedEffect(locationPermissionState.status) {
        if (locationPermissionState.status is PermissionStatus.Granted) {
            locationViewModel.fetchLocation(context)
        } else {
            locationPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(address, operationalCities, isCitiesLoaded) {
        if (!isCitiesLoaded) {
            cityCheckLoading = true
            return@LaunchedEffect
        }

        if (address.isNotEmpty() && address != "Locating...") {
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
            CircularProgressIndicator(color = MadadwalaColors.Green)
        }
        return
    }

    if (!isCityOperational) {
        Box(modifier = Modifier.fillMaxSize()) {
            ComingSoonScreen(
                cityName = address.split(",").firstOrNull()?.trim() ?: "Your City",
                address = address,
                onLocationClick = { showLocationPicker = true },
                onNotifyMe = {
                    scope.launch {
                        try {
                            val cityName = address.split(",").firstOrNull()?.trim() ?: "Your City"
                            val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                            val res = com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.registerLocationInterest(
                                mapOf(
                                    "uid" to uid,
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

            if (showLocationPicker) {
                LocationPickerOverlay(
                    uid = user?.uid ?: "",
                    onDismiss = { showLocationPicker = false },
                    onLocationSelected = { lat, lng, name ->
                        locationViewModel.setLocation(context, lat, lng, name)
                        showLocationPicker = false
                    },
                    onUseLiveLocation = {
                        locationViewModel.fetchLocation(context, force = true)
                        showLocationPicker = false
                    }
                )
            }
        }
        return
    }

    Scaffold(
        topBar = {
            if (selectedTab == 0) { // Only show top bar on Home tab
                TopAppBar(
                    title = {
                        Column(modifier = Modifier.clickable { showLocationPicker = true }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Hi, $userName 👋",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = MadadwalaColors.Green
                                    )
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MadadwalaColors.Green,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = address,
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { onNavigate(Screen.Notifications.route) }) {
                            if (unreadCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = MadadwalaColors.Red) {
                                            Text(unreadCount.toString(), color = Color.White)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications", tint = MadadwalaColors.Green)
                                }
                            } else {
                                Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications", tint = MadadwalaColors.Green)
                            }
                        }
                        IconButton(onClick = { selectedTab = 3 }) {
                            if (!user?.profileImage.isNullOrEmpty()) {
                                AsyncImage(
                                    model = user?.profileImage,
                                    contentDescription = "Profile",
                                    modifier = Modifier.size(36.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MadadwalaColors.Green.copy(0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = MadadwalaColors.Green)
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                val items = listOf(
                    Triple("Home", Icons.Default.Home, 0),
                    Triple("Instant", Icons.Default.FlashOn, 4),
                    Triple("Bookings", Icons.AutoMirrored.Filled.Assignment, 1),
                    Triple("Wallet", Icons.Default.AccountBalanceWallet, 2),
                    Triple("Profile", Icons.Default.Person, 3)
                )
                items.forEach { (label, icon, index) ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            selectedTab = index
                            if (index == 2 || index == 3) {
                                authViewModel.refresh()
                            }
                        },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MadadwalaColors.Green else MadadwalaColors.Gray
                            )
                        },
                        icon = {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MadadwalaColors.Green.copy(alpha = 0.1f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(icon, contentDescription = label, tint = MadadwalaColors.Green)
                                }
                            } else {
                                Icon(icon, contentDescription = label, tint = MadadwalaColors.Gray)
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MadadwalaColors.Green,
                            selectedTextColor = MadadwalaColors.Green,
                            unselectedIconColor = MadadwalaColors.Gray,
                            unselectedTextColor = MadadwalaColors.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        },
        containerColor = Color.White
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    (fadeIn(tween(300, easing = FastOutSlowInEasing)) + slideInHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing), initialOffsetX = { it / 4 })) togetherWith
                            (fadeOut(tween(300, easing = FastOutSlowInEasing)) + slideOutHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing), targetOffsetX = { -it / 4 }))
                },
                label = "TabTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> HomeTabContent(
                        uid = uid,
                        categories = categories,
                        recentBookings = recentBookings,
                        banners = banners,
                        nearbyProviders = nearbyProviders,
                        onCategoryClick = onCategoryClick,
                        onNavigate = onNavigate,
                        userLat = userLat,
                        userLng = userLng,
                        onTabSelect = { selectedTab = it },
                        walletBalance = user?.walletBalance ?: 0.0,
                        callViewModel = callViewModel,
                        isProvidersLoading = isProvidersLoading,
                        onChat = { activeChatBookingId = it }
                    )
                    4 -> CategoriesTabContent(
                        user = user,
                        address = address,
                        recentBookings = recentBookings,
                        onNavigate = onNavigate,
                        myRequests = myRequests,
                        onBidAccepted = { bookingId, category, providerName ->
                            processedIds = processedIds + bookingId
                            acceptanceData = AcceptanceData(bookingId, category, providerName)
                            onNavigate(Screen.LiveTracking.createRoute(bookingId))
                        },
                        onTabSelect = { selectedTab = it },
                        userLat = userLat,
                        userLng = userLng,
                        onLocationClick = { showLocationPicker = true },
                        callViewModel = callViewModel,
                        nearbyProviders = nearbyProviders
                    )
                    1 -> BookingsTabContent(user?.uid ?: "", onNavigate, callViewModel, onChat = { activeChatBookingId = it })
                    2 -> WalletTabContent(user, onNavigate)
                    3 -> {
                        val activeBooking = recentBookings.find { it.status in listOf("accepted", "on_the_way", "arrived", "in_progress") }
                        ProfileTabContent(
                            name = userName,
                            phone = user?.phoneNumber ?: "",
                            profileImage = user?.profileImage,
                            uid = user?.uid ?: "",
                            userLat = userLat,
                            userLng = userLng,
                            totalBookings = recentBookings.size,
                            activeBookingId = activeBooking?._id,
                            onLogout = onLogout,
                            onNavigate = onNavigate
                        )
                    }
                }
            }

            // Global Waiting Overlay
            if (myRequests.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MadadwalaColors.Green.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val currentRequest = myRequests.first()
                        val createdTime = try { sdf.parse(currentRequest.createdAt)?.time ?: 0L } catch(e: Exception) { 0L }
                        val timeLeftSeconds = ((createdTime + 30000L) - currentTime) / 1000
                        val isTimeout = timeLeftSeconds <= 0 && currentRequest.bids.isEmpty()

                        val availableAndNotRejected = nearbyProviders.filter {
                            it.category == currentRequest.category && it.uid !in currentRequest.rejectedBy
                        }
                        val isNoPartnerFound = (currentRequest.bids.isEmpty() && availableAndNotRejected.isEmpty() && currentRequest.rejectedBy.isNotEmpty()) || isTimeout

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isNoPartnerFound) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MadadwalaColors.Green)
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Text(
                                if (isNoPartnerFound) "No Service Found" else "Waiting for Partner to Accept...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = if (isNoPartnerFound) MadadwalaColors.Red else MadadwalaColors.Green)
                            )

                            if (!isNoPartnerFound && currentRequest.bids.isEmpty() && timeLeftSeconds > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MadadwalaColors.Red.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${timeLeftSeconds}s",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MadadwalaColors.Red,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        RunningCharacter()

                        Text(
                            if (isNoPartnerFound) "Try again later or choose another category" else "Finding the best match for your ${currentRequest.category} request",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )

                        if (isNoPartnerFound) {
                            TextButton(onClick = {
                                processedIds = processedIds + currentRequest._id
                                myRequests = myRequests.filter { it._id != currentRequest._id }
                            }) {
                                Text("Close", color = MadadwalaColors.Gray, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (currentRequest.bids.isNotEmpty()) {
                            if (currentRequest.isAutoPrice) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        "View ${currentRequest.bids.size} Bids Received",
                                        color = MadadwalaColors.Lime,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    val latestBidTime = currentRequest.bids.map {
                                        try { sdf.parse(it.createdAt)?.time ?: 0L } catch(e: Exception) { 0L }
                                    }.maxOrNull() ?: 0L

                                    if (latestBidTime > 0) {
                                        BidTimer(
                                            expiryTimeMillis = latestBidTime + 30000L,
                                            onExpire = {
                                                scope.launch {
                                                    try {
                                                        com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.updateCustomRequestStatus(
                                                            currentRequest._id,
                                                            mapOf("status" to "cancelled")
                                                        )
                                                        processedIds = processedIds + currentRequest._id
                                                    } catch (_: Exception) {}
                                                }
                                            }
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 250.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    currentRequest.bids.forEach { bid ->
                                        BidItem(currentRequest, bid) { bookingId, providerName ->
                                            processedIds = processedIds + bookingId
                                            acceptanceData = AcceptanceData(bookingId, currentRequest.category, providerName)
                                            onNavigate(Screen.LiveTracking.createRoute(bookingId))
                                        }
                                    }
                                }
                            } else {
                                TextButton(onClick = { selectedTab = 4 }) {
                                    Text("View ${currentRequest.bids.size} Bids Received", color = MadadwalaColors.Lime, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            if (showLocationPicker) {
                LocationPickerOverlay(
                    uid = user?.uid ?: "",
                    onDismiss = { showLocationPicker = false },
                    onLocationSelected = { lat, lng, name ->
                        locationViewModel.setLocation(context, lat, lng, name)
                        showLocationPicker = false
                    },
                    onUseLiveLocation = {
                        locationViewModel.fetchLocation(context, force = true)
                        showLocationPicker = false
                    }
                )
            }

            // Success Popup
            if (acceptanceData != null) {
                Dialog(onDismissRequest = { acceptanceData = null }) {
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = Color.White,
                        modifier = Modifier.padding(16.dp).fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = MadadwalaColors.Green, modifier = Modifier.size(64.dp))
                            Text("Request Accepted!", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black))
                            Text(
                                "Your ${acceptanceData?.category} request has been accepted by ${acceptanceData?.providerName}.",
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                            Button(
                                onClick = {
                                    acceptanceData = null
                                    selectedTab = 1
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Green),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("View Booking", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    activeChatBookingId?.let { bookingId ->
        com.abhi.madadwala_1.ui.components.BookingChatBottomSheet(
            bookingId = bookingId,
            onDismiss = { activeChatBookingId = null },
            onViewBookingDetails = { id -> onNavigate(com.abhi.madadwala_1.ui.navigation.Screen.LiveTracking.createRoute(id)) },
            onViewPartnerProfile = { uid -> onNavigate(com.abhi.madadwala_1.ui.navigation.Screen.ProviderProfile.createRoute(uid)) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTabContent(
    uid: String,
    categories: List<com.abhi.madadwala_1.data.remote.CategoryResponse>,
    recentBookings: List<com.abhi.madadwala_1.data.remote.BookingResponse>,
    banners: List<com.abhi.madadwala_1.data.remote.BannerResponse>,
    nearbyProviders: List<com.abhi.madadwala_1.data.remote.ProviderResponse>,
    onCategoryClick: (String) -> Unit,
    onNavigate: (String) -> Unit = {},
    userLat: Double = 0.0,
    userLng: Double = 0.0,
    onTabSelect: (Int) -> Unit = {},
    walletBalance: Double = 0.0,
    callViewModel: CallViewModel,
    isProvidersLoading: Boolean = false,
    onChat: (String) -> Unit = {}
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortOrder by rememberSaveable { mutableStateOf("Default") }
    val scope = rememberCoroutineScope()

    val filteredCategories = remember(categories, searchQuery, sortOrder) {
        val list = if (searchQuery.isBlank()) {
            categories
        } else {
            categories.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        when (sortOrder) {
            "A-Z" -> list.sortedBy { it.name }
            "Z-A" -> list.sortedByDescending { it.name }
            else -> list
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Active Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search for a service...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.LightGray.copy(0.5f),
                unfocusedBorderColor = Color.LightGray.copy(0.5f),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            singleLine = true
        )

        // Instant Search Results Section
        if (searchQuery.isNotEmpty()) {
            val demoList = listOf("Cleaning", "Plumbing", "Electrical", "Painting", "Appliances")
            val matches = if (categories.isEmpty()) {
                demoList.filter { it.contains(searchQuery, ignoreCase = true) }
            } else {
                categories.filter { it.name.contains(searchQuery, ignoreCase = true) }.map { it.name }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(1.dp, MadadwalaColors.LightGray, RoundedCornerShape(16.dp))
            ) {
                if (matches.isEmpty()) {
                    Text(
                        "No services found for \"$searchQuery\"",
                        modifier = Modifier.padding(20.dp),
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    matches.forEach { match ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCategoryClick(match) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(match, fontWeight = FontWeight.Medium, color = MadadwalaColors.Green)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowOutward, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                        }
                        if (match != matches.last()) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MadadwalaColors.LightGray.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        // Hide other content when searching to focus on results
        if (searchQuery.isEmpty()) {
            // Quick Actions Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                QuickActionItem("Instant Help", "Quick", Icons.Default.FlashOn, MadadwalaColors.Green) { onTabSelect(4) }
                QuickActionItem("Bookings", "My Bookings", Icons.Default.CalendarToday, MadadwalaColors.Green) { onTabSelect(1) }
                QuickActionItem("Wallet", "₹${String.format(Locale.getDefault(), "%.0f", walletBalance)}", Icons.Default.AccountBalanceWallet, MadadwalaColors.Green) { onTabSelect(2) }
                QuickActionItem("Offers", "Save More", Icons.Default.LocalOffer, MadadwalaColors.Green) { onNavigate(Screen.Offers.route) }
                QuickActionItem("Support", "Help Center", Icons.Default.HeadsetMic, MadadwalaColors.Green) { onNavigate(Screen.HelpSupport.route) }
            }

            // Instant Help Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .bounceClick { onTabSelect(4) },
                shape = RoundedCornerShape(16.dp),
                color = MadadwalaColors.Green
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        RunningCharacter() // Reusing the lottie for visual
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "In a hurry? Need help instantly?",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "Get a verified professional at your doorstep in minutes!",
                            color = Color.White.copy(0.8f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = { onTabSelect(4) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Instant Help ⚡", color = MadadwalaColors.Green, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val minPrice = remember(nearbyProviders) {
                            nearbyProviders.minByOrNull { it.startingPrice }?.startingPrice?.toInt() ?: 149
                        }
                        Text("From ₹$minPrice", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Live Tracking Card (if any active booking)
            val activeBooking = recentBookings.find { it.status == "accepted" || it.status == "on_the_way" || it.status == "arrived" }
            if (activeBooking != null) {
                val provider = nearbyProviders.find { it.uid == activeBooking.providerUid }
                LiveTrackingCard(activeBooking, provider, onNavigate, onChat, callViewModel)
            }

            // Nearby Available Partners
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Nearby Available Partners",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MadadwalaColors.Green)
                )
                Text(
                    "View All >",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.Gray),
                    modifier = Modifier.clickable { onNavigate(Screen.CategoryListing.createRoute("All")) }
                )
            }

            if (isProvidersLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MadadwalaColors.Green,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Loading nearby partners...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            } else if (nearbyProviders.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(nearbyProviders.filter { it.isAvailable }) { provider ->
                        val distanceStr = remember(userLat, userLng, provider.lat, provider.lng) {
                            if (userLat != 0.0 && provider.lat != null) {
                                val results = FloatArray(1)
                                android.location.Location.distanceBetween(userLat, userLng, provider.lat, provider.lng!!, results)
                                val km = results[0] / 1000
                                String.format("%.1f km away", km)
                            } else {
                                "Near you"
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .width(160.dp)
                                .bounceClick {
                                    onNavigate(Screen.ProviderProfile.createRoute(provider.uid))
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(0.3f)),
                            shadowElevation = 1.dp
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (!provider.profileImage.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = provider.profileImage,
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
                                            .background(MadadwalaColors.Cream),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = MadadwalaColors.Green)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = provider.name,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB400), modifier = Modifier.size(14.dp))
                                    Text(
                                        text = " ${provider.rating} (${provider.reviewCount})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                                Text(distanceStr, style = MaterialTheme.typography.labelSmall, color = Color.Gray)

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = Color.LightGray.copy(0.3f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Starts at ₹${provider.startingPrice.toInt()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MadadwalaColors.Green,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MadadwalaColors.Green, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            } else {
                Text("No partners available nearby", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "What do you need help with?",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Categories Grid
            val visibleCategoriesData = if (filteredCategories.isEmpty()) {
                listOf(
                    "Plumbing" to Icons.Default.Handyman,
                    "Electrical" to Icons.Default.FlashOn,
                    "Carpentry" to Icons.Default.Handyman,
                    "AC Repair" to Icons.Default.Air,
                    "Appliance" to Icons.Default.Kitchen,
                    "More" to Icons.Default.GridView
                )
            } else {
                filteredCategories.map { it.name to getIconForCategory(it.icon) }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.height(230.dp).padding(horizontal = 16.dp),
                userScrollEnabled = false
            ) {
                items(visibleCategoriesData) { item ->
                    ServiceCardV2(item.first, item.second) {
                        if (item.first == "More") {
                            onNavigate(Screen.AllCategories.route)
                        } else {
                            onCategoryClick(item.first)
                        }
                    }
                }
            }
        }

        // Book Again Section (from original but redesigned)
        if (searchQuery.isEmpty() && recentBookings.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Book Again",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MadadwalaColors.Green)
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(recentBookings.take(5)) { booking ->
                    Surface(
                        modifier = Modifier
                            .width(200.dp)
                            .bounceClick { onCategoryClick(booking.serviceName) },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(0.1f)),
                        shadowElevation = 2.dp
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(MadadwalaColors.Green.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = MadadwalaColors.Green, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(booking.serviceName.replace("(Custom)", "").trim(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                                Text("Book again", color = MadadwalaColors.Green, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }


}

@Composable
fun QuickActionItem(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(75.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MadadwalaColors.Green, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.Black),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 10.sp),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
fun LiveTrackingCard(
    booking: com.abhi.madadwala_1.data.remote.BookingResponse,
    provider: com.abhi.madadwala_1.data.remote.ProviderResponse?,
    onNavigate: (String) -> Unit,
    onChat: (String) -> Unit,
    callViewModel: CallViewModel
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Real-time duration calculation
    var durationText by remember { mutableStateOf("Not Started") }

    LaunchedEffect(booking.providerLat, booking.providerLng, booking.customerLat, booking.customerLng) {
        if (booking.customerLat != null && booking.customerLng != null &&
            booking.providerLat != null && booking.providerLng != null) {
            try {
                val origin = "${booking.providerLat},${booking.providerLng}"
                val dest = "${booking.customerLat},${booking.customerLng}"

                val response = try {
                    com.abhi.madadwala_1.data.remote.RetrofitClient.mapsApiService.getDirections(
                        origin = origin,
                        destination = dest,
                        apiKey = "AIzaSyCmBUpQgoSWPSXcC0DpxilJzJGTXJtke-8"
                    )
                } catch (_: Exception) { null }

                if (response != null && response.isSuccessful && response.body()?.status == "OK") {
                    val route = response.body()?.routes?.firstOrNull()
                    durationText = route?.legs?.firstOrNull()?.duration?.text?.replace(" mins", " min") ?: "Not Started"
                } else {
                    val results = FloatArray(1)
                    android.location.Location.distanceBetween(
                        booking.providerLat, booking.providerLng,
                        booking.customerLat, booking.customerLng,
                        results
                    )
                    val distanceInMeters = results[0]
                    val minutes = (distanceInMeters / 400).toInt().coerceAtLeast(1)
                    durationText = "$minutes min"
                }
            } catch (_: Exception) {
                durationText = "Not Started"
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onNavigate(Screen.LiveTracking.createRoute(booking._id)) },
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(0.3f)),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Track Your Service Partner",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "LIVE",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                // Left: Provider Info
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    if (!provider?.profileImage.isNullOrEmpty()) {
                        AsyncImage(
                            model = provider?.profileImage,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.LightGray)) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.align(Alignment.Center), tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(booking.providerName ?: "Partner", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFFFB400), modifier = Modifier.size(12.dp))
                            Text(
                                text = " ${provider?.rating ?: 4.8} (${provider?.reviewCount ?: 128})",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                        Surface(color = MadadwalaColors.Green.copy(0.1f), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = booking.serviceName.replace("(Custom)", "").trim(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MadadwalaColors.Green,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Divider
                Box(modifier = Modifier.width(1.dp).height(50.dp).background(Color.LightGray.copy(0.3f)))

                // Right Part: Status + Map
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp), horizontalAlignment = Alignment.Start) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (booking.status == "arrived") Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.DirectionsRun,
                                contentDescription = null,
                                tint = MadadwalaColors.Green,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = when(booking.status) {
                                    "accepted" -> " Assigned"
                                    "on_the_way" -> " On the way"
                                    "arrived" -> " Arrived"
                                    else -> " On the way"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MadadwalaColors.Green,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = when {
                                booking.status == "arrived" -> "Partner is here"
                                durationText == "Not Started" -> "Partner"
                                else -> "Arriving in"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Text(
                            text = if (booking.status == "arrived") "At location" else durationText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }

                    // Mini Map Tracking
                    val customerPos = remember(booking.customerLat, booking.customerLng) {
                        LatLng(booking.customerLat ?: 0.0, booking.customerLng ?: 0.0)
                    }
                    val providerPos = remember(booking.providerLat, booking.providerLng) {
                        LatLng(booking.providerLat ?: 0.0, booking.providerLng ?: 0.0)
                    }
                    val mapCameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(providerPos, 14f)
                    }

                    LaunchedEffect(customerPos, providerPos) {
                        if (booking.customerLat != null && booking.providerLat != null) {
                            try {
                                val bounds = LatLngBounds.builder()
                                    .include(customerPos)
                                    .include(providerPos)
                                    .build()
                                mapCameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 40))
                            } catch (_: Exception) {}
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(110.dp, 80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF0F0F0))
                            .border(1.dp, Color.LightGray.copy(0.3f), RoundedCornerShape(12.dp))
                    ) {
                        GoogleMap(
                            modifier = Modifier.height(120.dp).fillMaxWidth(),
                            cameraPositionState = mapCameraPositionState,
                            uiSettings = MapUiSettings(
                                zoomControlsEnabled = false,
                                tiltGesturesEnabled = false,
                                rotationGesturesEnabled = false,
                                scrollGesturesEnabled = false,
                                zoomGesturesEnabled = false,
                                compassEnabled = false,
                                myLocationButtonEnabled = false,
                                mapToolbarEnabled = false
                            ),
                            properties = MapProperties(
                                mapStyleOptions = null
                            )
                        ) {
                            if (booking.customerLat != null && booking.customerLng != null) {
                                Marker(
                                    state = remember(customerPos) { MarkerState(position = customerPos) },
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                                )
                            }
                            if (booking.providerLat != null && booking.providerLng != null) {
                                Marker(
                                    state = remember(providerPos) { MarkerState(position = providerPos) },
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                                )
                                Polyline(
                                    points = listOf(customerPos, providerPos),
                                    color = MadadwalaColors.Green,
                                    width = 8f
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            callViewModel.startCall(
                                bookingId = booking._id,
                                customerId = booking.customerUid,
                                partnerId = booking.providerUid,
                                partnerName = booking.providerName ?: "Partner"
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Call, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = { onChat(booking._id) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Chat", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    "View Details >",
                    style = MaterialTheme.typography.labelMedium,
                    color = MadadwalaColors.Green,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigate(Screen.LiveTracking.createRoute(booking._id)) }
                )
            }
        }
    }


}

@Composable
fun ServiceCardV2(name: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().bounceClick { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(0.1f)),
        shadowElevation = 1.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MadadwalaColors.Green.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MadadwalaColors.Green,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.Black),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesTabContent(
    user: com.abhi.madadwala_1.data.remote.UserResponse?,
    address: String,
    recentBookings: List<com.abhi.madadwala_1.data.remote.BookingResponse>,
    onNavigate: (String) -> Unit,
    myRequests: List<com.abhi.madadwala_1.data.remote.CustomRequestResponse> = emptyList(),
    onBidAccepted: (String, String, String) -> Unit = { _, _, _ -> },
    onTabSelect: (Int) -> Unit = {},
    userLat: Double = 0.0,
    userLng: Double = 0.0,
    onLocationClick: () -> Unit = {},
    callViewModel: CallViewModel,
    nearbyProviders: List<com.abhi.madadwala_1.data.remote.ProviderResponse> = emptyList()
) {
    var categories by remember { mutableStateOf<List<com.abhi.madadwala_1.data.remote.CategoryResponse>>(emptyList()) }
    var selectedCategoryName by rememberSaveable { mutableStateOf<String?>(null) }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortOrder by rememberSaveable { mutableStateOf("Default") }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    val unreadCount by preferenceManager.unreadNotificationsCount.collectAsState(initial = 0)

    LaunchedEffect(Unit) {
        try {
            val response = com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.getCategories()
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                categories = response.body()!!
            }
        } catch (_: Exception) {
        } finally {
            isLoading = false
        }
    }

    val displayCategories = if (categories.isEmpty()) {
        listOf("Cleaning", "Plumbing", "Electrical", "Appliance Repair", "Carpentry", "Painting", "AC Service", "Pest Control", "Gardening")
    } else {
        categories.map { it.name }
    }

    val sortedCategories = remember(displayCategories, sortOrder) {
        when (sortOrder) {
            "A-Z" -> displayCategories.sorted()
            "Z-A" -> displayCategories.sortedDescending()
            else -> displayCategories
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF9F9F9))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Instant Help",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.FlashOn, null, tint = MadadwalaColors.Green, modifier = Modifier.size(28.dp))
                    }
                    Text(
                        text = "What can we help you with today?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Location Chip
                    Surface(
                        onClick = onLocationClick,
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocationOn, null, tint = MadadwalaColors.Green, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = address.split(",").firstOrNull() ?: "Location",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    // Notification Bell
                    Box {
                        IconButton(onClick = { onNavigate(Screen.Notifications.route) }) {
                            Icon(Icons.Default.NotificationsNone, null, tint = Color.Black, modifier = Modifier.size(28.dp))
                        }
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(Color.Red, CircleShape)
                                    .align(Alignment.TopEnd)
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(unreadCount.toString(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Search & Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search for a service...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier.height(56.dp).clickable { showFilterSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Tune, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Filters", fontWeight = FontWeight.Bold)
                    }
                }
            }

            val realPrices = remember(nearbyProviders, displayCategories) {
                displayCategories.associateWith { catName ->
                    val minPrice = nearbyProviders.filter {
                        it.category.equals(catName, ignoreCase = true) && it.isAvailable
                    }.minByOrNull { it.startingPrice }?.startingPrice?.toInt()

                    minPrice?.toString() ?: when(catName) {
                        "Cleaning", "Painting", "Gardening" -> "149"
                        "AC Service", "AC Repair" -> "249"
                        else -> "199"
                    }
                }
            }

            val bannerMinPrice = remember(realPrices) {
                realPrices.values.mapNotNull { it.toIntOrNull() }.minOrNull() ?: 149
            }

            // Instant Search Results Section
            if (searchQuery.isNotEmpty()) {
                val matches = displayCategories.filter { it.contains(searchQuery, ignoreCase = true) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .border(1.dp, Color.LightGray.copy(0.2f), RoundedCornerShape(16.dp))
                ) {
                    if (matches.isEmpty()) {
                        Text(
                            "No services found for \"$searchQuery\"",
                            modifier = Modifier.padding(20.dp),
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        matches.forEach { match ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCategoryName = match
                                        showDialog = true
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val categoryIcon = categories.find { it.name == match }?.icon ?: match
                                Icon(
                                    imageVector = getIconForCategory(categoryIcon),
                                    contentDescription = null,
                                    tint = MadadwalaColors.Green,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(match, fontWeight = FontWeight.Medium, color = Color.Black)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                            }
                            if (match != matches.last()) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }

            if (searchQuery.isEmpty()) {
                // Green Promo Banner
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MadadwalaColors.Green
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Small Character placeholder
                        Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                            RocketCharacter()
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Need Help in a Hurry?", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Get verified professionals at your doorstep in minutes!",
                                color = Color.White.copy(0.8f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(
                                onClick = {
                                    if (displayCategories.isNotEmpty()) {
                                        selectedCategoryName = displayCategories[0]
                                        showDialog = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Book Instant ⚡", color = MadadwalaColors.Green, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                            Text("From ₹$bannerMinPrice", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }

                // Popular Categories
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Popular Categories", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "View All >",
                        color = MadadwalaColors.Green,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.clickable { onNavigate(Screen.InstantCategories.route) }
                    )
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier
                        .height(240.dp)
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = false
                ) {
                    items(sortedCategories.take(9)) { catName ->
                        val category = categories.find { it.name == catName }
                            ?: com.abhi.madadwala_1.data.remote.CategoryResponse("", catName, catName, null)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCategoryName = catName
                                    showDialog = true
                                }
                        ) {
                            Surface(
                                modifier = Modifier.size(60.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(0.2f)),
                                shadowElevation = 1.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = getIconForCategory(category.icon ?: catName),
                                        contentDescription = null,
                                        tint = MadadwalaColors.Green,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = catName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "From ₹${realPrices[catName] ?: "149"}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = Color.Gray),
                                maxLines = 1
                            )
                        }
                    }
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onNavigate(Screen.InstantCategories.route) }
                        ) {
                            Surface(
                                modifier = Modifier.size(60.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                shadowElevation = 1.dp
                            ) {
                                Icon(Icons.Default.GridView, null, tint = MadadwalaColors.Green, modifier = Modifier.padding(16.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("More Services", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
                        }
                    }
                }

                // Trust Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TrustItem(Icons.Default.VerifiedUser, "Verified Professionals", "Trusted & Background Verified")
                    TrustItem(Icons.Default.Schedule, "Quick Response", "Experts arrive on time")
                    TrustItem(Icons.Default.AccountBalanceWallet, "Affordable Pricing", "Transparent & No Hidden Charges")
                    TrustItem(Icons.Default.ThumbUp, "Satisfaction Guaranteed", "Quality Service Assured")
                }

                // Recently Booked
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recently Booked", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "View All >",
                        color = MadadwalaColors.Green,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.clickable { onTabSelect(1) }
                    )
                }

                if (recentBookings.isEmpty()) {
                    Text("No recent bookings", color = Color.Gray, modifier = Modifier.padding(16.dp))
                } else {
                    recentBookings.take(2).forEach { booking ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clickable { onNavigate(Screen.LiveTracking.createRoute(booking._id)) },
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(0.2f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                // Avatar Placeholder
                                Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.LightGray)) {
                                    Icon(Icons.Default.Person, null, modifier = Modifier.align(Alignment.Center), tint = Color.White)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${booking.providerName ?: "Partner"} - ${booking.serviceName.replace("(Custom)", "").trim()}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Today, ${if (booking.scheduledTime.contains("|")) booking.scheduledTime.split("|").last().trim() else booking.scheduledTime}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    val statusColor = when(booking.status) {
                                        "accepted", "on_the_way", "arrived" -> Color(0xFF4CAF50)
                                        "pending" -> Color(0xFFFFA000)
                                        "completed" -> MadadwalaColors.Green
                                        else -> Color.Gray
                                    }
                                    Surface(color = statusColor.copy(0.1f), shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            text = booking.status.replace("_", " ").uppercase(),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = statusColor,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (booking.status == "on_the_way" || booking.status == "accepted") {
                                        Text("Tracking Available", style = MaterialTheme.typography.labelSmall, color = MadadwalaColors.Green, modifier = Modifier.padding(top = 4.dp))
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    "Sort Categories",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                val options = listOf("Default", "A-Z", "Z-A")
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                sortOrder = option
                                showFilterSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = sortOrder == option,
                            onClick = {
                                sortOrder = option
                                showFilterSheet = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = MadadwalaColors.Green)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(option)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showDialog && selectedCategoryName != null && user != null) {
        CustomRequestDialog(
            category = selectedCategoryName!!,
            user = user,
            onDismiss = { showDialog = false },
            userLat = userLat,
            userLng = userLng
        )
    }

    // Requests/Bids at bottom
    if (myRequests.isNotEmpty()) {
        // This is handled by the global overlay in HomeScreen, but if specific tab content is needed:
        val manualRequests = myRequests.filter { !it.isAutoPrice }
        if (manualRequests.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .background(Color.White, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text("Partner Bids", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MadadwalaColors.Green))
                    }
                    items(manualRequests) { req ->
                        req.bids.forEach { bid ->
                            BidItem(req, bid) { bookingId, providerName ->
                                onBidAccepted(bookingId, req.category, providerName)
                            }
                        }
                    }
                }
            }
        }
    }


}

@Composable
fun TrustItem(icon: ImageVector, title: String, subtitle: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(0.1f)),
        modifier = Modifier.width(180.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).background(Color(0xFFE8F5E9), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MadadwalaColors.Green, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = Color.Gray), maxLines = 1)
            }
        }
    }


}

@Composable
fun BidItem(
    request: com.abhi.madadwala_1.data.remote.CustomRequestResponse,
    bid: com.abhi.madadwala_1.data.remote.BidResponse,
    onAccept: (String, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // bid.price is the Job Value (₹1,000)
    // bid.totalPrice is the customer pay amount (₹1,050)
    val jobValue = bid.price
    val platformFee = jobValue * 0.05
    val totalCustomerPay = jobValue + platformFee

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MadadwalaColors.Cream,
        border = androidx.compose.foundation.BorderStroke(1.dp, MadadwalaColors.Lime.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(bid.providerName, fontWeight = FontWeight.Bold, color = MadadwalaColors.Green)
                Text("Requested: ${request.category}", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "Service Price",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "₹${String.format("%.2f", jobValue)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "Platform Fee (5%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "₹${String.format("%.2f", platformFee)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.LightGray.copy(alpha = 0.5f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "Total Payable",
                        fontWeight = FontWeight.Black,
                        color = MadadwalaColors.Green,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "₹${String.format("%.2f", totalCustomerPay)}",
                        fontWeight = FontWeight.Black,
                        color = MadadwalaColors.Green,
                        fontSize = 18.sp
                    )
                }
            }
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val response = com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.acceptBid(
                                request._id,
                                mapOf(
                                    "providerUid" to bid.providerUid,
                                    "price" to String.format("%.2f", totalCustomerPay),
                                    "providerName" to bid.providerName
                                )
                            )
                            if (response.isSuccessful) {
                                response.body()?._id?.let { bookingId ->
                                    onAccept(bookingId, bid.providerName)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Green),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Accept")
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun CustomRequestDialog(
    category: String,
    user: com.abhi.madadwala_1.data.remote.UserResponse,
    onDismiss: () -> Unit,
    userLat: Double = 0.0,
    userLng: Double = 0.0
) {
    var problem by remember { mutableStateOf("") }
    var minPrice by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }
    var isPriceRange by remember { mutableStateOf(false) }
    var isAutoPrice by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .wrapContentHeight()
                .animateContentSize(),
            shape = RoundedCornerShape(32.dp),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(MadadwalaColors.Lime.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when (category.lowercase()) {
                            "cleaning" -> Icons.Default.Home
                            "plumbing" -> Icons.Default.Build
                            "electrical" -> Icons.Default.ElectricBolt
                            else -> Icons.Default.Engineering
                        }
                        Icon(icon, contentDescription = null, tint = MadadwalaColors.Green, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Request $category",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = MadadwalaColors.Green
                        )
                    )
                    Text(
                        text = "Let partners know what you need",
                        style = MaterialTheme.typography.bodySmall,
                        color = MadadwalaColors.Gray
                    )
                }

                // Problem Description
                OutlinedTextField(
                    value = problem,
                    onValueChange = { problem = it },
                    placeholder = { Text("Describe your problem in detail...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MadadwalaColors.Green,
                        unfocusedBorderColor = MadadwalaColors.LightGray,
                        focusedContainerColor = MadadwalaColors.Cream.copy(alpha = 0.3f),
                        unfocusedContainerColor = MadadwalaColors.Cream.copy(alpha = 0.3f)
                    )
                )

                // Price Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MadadwalaColors.Cream.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Auto Price Toggle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoMode, contentDescription = null, tint = MadadwalaColors.Green, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Automatic Price", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Switch(
                            checked = isAutoPrice,
                            onCheckedChange = {
                                isAutoPrice = it
                                if (it) {
                                    minPrice = ""
                                    maxPrice = ""
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = MadadwalaColors.Green, checkedTrackColor = MadadwalaColors.Lime)
                        )
                    }

                    if (!isAutoPrice) {
                        HorizontalDivider(color = MadadwalaColors.LightGray.copy(alpha = 0.5f))

                        // Price Range Toggle
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = MadadwalaColors.Green, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Price Range", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                            Switch(
                                checked = isPriceRange,
                                onCheckedChange = { isPriceRange = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = MadadwalaColors.Green, checkedTrackColor = MadadwalaColors.Lime)
                            )
                        }

                        // Price Input Fields
                        AnimatedVisibility(visible = true) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (isPriceRange) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        PriceField(value = minPrice, onValueChange = { minPrice = it }, label = "Min ₹", modifier = Modifier.weight(1f))
                                        PriceField(value = maxPrice, onValueChange = { maxPrice = it }, label = "Max ₹", modifier = Modifier.weight(1f))
                                    }
                                } else {
                                    PriceField(value = minPrice, onValueChange = { minPrice = it }, label = "Price ₹", modifier = Modifier.fillMaxWidth())
                                }

                                val minP = minPrice.toDoubleOrNull() ?: 0.0
                                if (minP > 0) {
                                    val gst = minP * 0.075
                                    val tax = minP * 0.075
                                    val total = minP + gst + tax
                                    Text(
                                        text = "Total Incl. GST & Tax (15%): ₹${String.format("%.2f", total)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MadadwalaColors.Green,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = MadadwalaColors.Gray, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    if (userLat != 0.0 && userLng != 0.0) {
                                        val minP = minPrice.toDoubleOrNull()
                                        val maxP = if (isPriceRange) maxPrice.toDoubleOrNull() else null

                                        // Calculate inclusive totals
                                        val finalMin = if (minP != null) minP + (minP * 0.15) else null
                                        val finalMax = if (maxP != null) maxP + (maxP * 0.15) else null

                                        val request = com.abhi.madadwala_1.data.remote.CustomRequest(
                                            customerUid = user.uid,
                                            customerName = user.name ?: "Customer",
                                            category = category,
                                            problem = problem,
                                            minPrice = if (!isAutoPrice) finalMin else null,
                                            maxPrice = if (!isAutoPrice && isPriceRange) finalMax else null,
                                            isAutoPrice = isAutoPrice,
                                            lat = userLat,
                                            lng = userLng
                                        )
                                        val response = com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.createCustomRequest(request)
                                        if (response.isSuccessful) {
                                            Toast.makeText(context, "Request Sent Successfully!", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        } else {
                                            Toast.makeText(context, "Failed to send request: ${response.message()}", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                            scope.launch {
                                                val request = com.abhi.madadwala_1.data.remote.CustomRequest(
                                                    customerUid = user.uid,
                                                    customerName = user.name ?: "Customer",
                                                    category = category,
                                                    problem = problem,
                                                    minPrice = if (!isAutoPrice) minPrice.toDoubleOrNull() else null,
                                                    maxPrice = if (!isAutoPrice && isPriceRange) maxPrice.toDoubleOrNull() else null,
                                                    isAutoPrice = isAutoPrice,
                                                    lat = location?.latitude,
                                                    lng = location?.longitude
                                                )
                                                val response = com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.createCustomRequest(request)
                                                if (response.isSuccessful) {
                                                    Toast.makeText(context, "Request Sent Successfully!", Toast.LENGTH_SHORT).show()
                                                    onDismiss()
                                                } else {
                                                    Toast.makeText(context, "Failed to send request: ${response.message()}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }.addOnFailureListener {
                                            scope.launch {
                                                val request = com.abhi.madadwala_1.data.remote.CustomRequest(
                                                    customerUid = user.uid,
                                                    customerName = user.name ?: "Customer",
                                                    category = category,
                                                    problem = problem,
                                                    minPrice = if (!isAutoPrice) minPrice.toDoubleOrNull() else null,
                                                    maxPrice = if (!isAutoPrice && isPriceRange) maxPrice.toDoubleOrNull() else null,
                                                    isAutoPrice = isAutoPrice,
                                                    lat = null,
                                                    lng = null
                                                )
                                                val response = com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.createCustomRequest(request)
                                                if (response.isSuccessful) {
                                                    Toast.makeText(context, "Request Sent!", Toast.LENGTH_SHORT).show()
                                                    onDismiss()
                                                } else {
                                                    Toast.makeText(context, "Error: ${response.message()}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier.weight(1.5f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Green),
                        shape = RoundedCornerShape(12.dp),
                        enabled = problem.isNotBlank() && (isAutoPrice || minPrice.isNotBlank())
                    ) {
                        Text("Request Now", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }


}

@Composable
fun PriceField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MadadwalaColors.Green,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}

@Composable
fun PromoBannerCarousel(banners: List<com.abhi.madadwala_1.data.remote.BannerResponse> = emptyList()) {
    if (banners.isEmpty()) return

    var currentBanner by remember { mutableIntStateOf(0) }
    val bannerCount = banners.size

    LaunchedEffect(bannerCount) {
        if (bannerCount > 0) {
            while (true) {
                delay(4000)
                currentBanner = (currentBanner + 1) % bannerCount
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        AnimatedContent(
            targetState = currentBanner,
            transitionSpec = {
                fadeIn(tween(1000)) togetherWith fadeOut(tween(1000))
            },
            label = "BannerCarousel"
        ) { index ->
            val banner = banners[index]
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(24.dp),
                color = MadadwalaColors.Green,
                border = androidx.compose.foundation.BorderStroke(3.dp, MadadwalaColors.Green)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Background Image - Occupies full background
                    if (banner.image.isNotEmpty()) {
                        AsyncImage(
                            model = banner.image,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Vertical Gradient at bottom for text
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.5f),
                                            Color.Black.copy(alpha = 0.8f)
                                        ),
                                        startY = 100f
                                    )
                                )
                        )
                    }

                    // Content positioned at the bottom with a small translucent background if needed
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = banner.title,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = banner.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }

        // Indicators
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(bannerCount) { index ->
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            if (currentBanner == index) Color.White else Color.White.copy(alpha = 0.4f),
                            CircleShape
                        )
                )
            }
        }
    }


}

@Composable
fun CategoryCard(category: com.abhi.madadwala_1.data.remote.CategoryResponse, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().bounceClick { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MadadwalaColors.Lime.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getIconForCategory(category.icon),
                contentDescription = null,
                tint = MadadwalaColors.Green,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MadadwalaColors.Green),
            textAlign = TextAlign.Center
        )
    }
}

fun getIconForCategory(iconName: String?): ImageVector {
    return when (iconName) {
        "Cleaning" -> Icons.Default.CleaningServices
        "Plumbing" -> Icons.Default.Handyman
        "Electrical" -> Icons.Default.ElectricBolt
        "Painting" -> Icons.Default.Brush
        "AC", "AC Service" -> Icons.Default.Air
        "Appliance", "Appliance Repair" -> Icons.Default.Kitchen
        "Carpentry" -> Icons.Default.Handyman
        "Pest Control" -> Icons.Default.BugReport
        "Gardening" -> Icons.Default.Yard
        "Home" -> Icons.Default.Home
        "Work" -> Icons.Default.Work
        "Car" -> Icons.Default.DirectionsCar
        "Health" -> Icons.Default.MedicalServices
        "Pets" -> Icons.Default.Pets
        else -> Icons.Default.Category
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsTabContent(uid: String, onNavigate: (String) -> Unit, callViewModel: CallViewModel, onChat: (String) -> Unit) {
    var bookings by remember { mutableStateOf<List<com.abhi.madadwala_1.data.remote.BookingResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by rememberSaveable { mutableStateOf("All Bookings") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    val unreadCount by preferenceManager.unreadNotificationsCount.collectAsState(initial = 0)

    LaunchedEffect(uid) {
        if (uid.isBlank()) return@LaunchedEffect
        scope.launch {
            try {
                val response = com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.getCustomerBookings(uid)
                if (response.isSuccessful) {
                    bookings = response.body() ?: emptyList()
                }
            } catch (_: Exception) {}
            finally { isLoading = false }
        }
    }

    val filteredBookings = remember(bookings, selectedFilter) {
        when (selectedFilter) {
            "Ongoing" -> bookings.filter { it.status in listOf("pending", "accepted", "on_the_way", "arrived") }
            "Completed" -> bookings.filter { it.status == "done" }
            "Cancelled" -> bookings.filter { it.status == "cancelled" }
            else -> bookings
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAFA))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "My Bookings",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = MadadwalaColors.Green,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Text(
                    "Track and manage all your service bookings",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                )
            }
            IconButton(onClick = { onNavigate(Screen.Notifications.route) }) {
                if (unreadCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge(containerColor = MadadwalaColors.Red) {
                                Text(unreadCount.toString(), color = Color.White)
                            }
                        }
                    ) {
                        Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications", tint = MadadwalaColors.Green)
                    }
                } else {
                    Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications", tint = MadadwalaColors.Green)
                }
            }
        }

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BookingFilterChip("All Bookings", Icons.Default.GridView, selectedFilter == "All Bookings") { selectedFilter = "All Bookings" }
            BookingFilterChip("Ongoing", Icons.Default.Schedule, selectedFilter == "Ongoing") { selectedFilter = "Ongoing" }
            BookingFilterChip("Completed", Icons.Default.CheckCircleOutline, selectedFilter == "Completed") { selectedFilter = "Completed" }
            BookingFilterChip("Cancelled", Icons.Default.Cancel, selectedFilter == "Cancelled") { selectedFilter = "Cancelled" }
        }

        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MadadwalaColors.Green)
            }
        } else if (filteredBookings.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, modifier = Modifier.size(64.dp), tint = MadadwalaColors.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Text("No bookings found", style = MaterialTheme.typography.titleMedium, color = MadadwalaColors.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredBookings) { booking ->
                    BookingCardV2(
                        booking = booking,
                        onViewDetails = {
                            if (booking.status == "done") {
                                onNavigate(Screen.WorkCompleted.createRoute(booking._id, isPartner = false))
                            } else {
                                onNavigate(Screen.BookingConfirmation.createRoute(booking._id))
                            }
                        },
                        onTrackPartner = { onNavigate(Screen.LiveTracking.createRoute(booking._id)) },
                        onRebook = {
                            onNavigate(Screen.BookingFlow.createRoute(booking.providerUid, booking.serviceName, booking.totalAmount))
                        },
                        onPay = {
                            onNavigate(Screen.Payment.createRoute(booking._id))
                        },
                        onRate = {
                            onNavigate(Screen.RatingReview.createRoute(booking._id, booking.providerUid))
                        },
                        onChat = {
                            onChat(booking._id)
                        },
                        callViewModel = callViewModel
                    )
                }
            }
        }
    }


}

@Composable
fun BookingCardV2(
    booking: com.abhi.madadwala_1.data.remote.BookingResponse,
    onViewDetails: () -> Unit,
    onTrackPartner: () -> Unit,
    onRebook: () -> Unit,
    onPay: () -> Unit,
    onRate: () -> Unit,
    onChat: () -> Unit,
    callViewModel: CallViewModel
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isUnpaidDone = booking.status == "done" && booking.paymentStatus != "paid"

    val statusColor = when {
        isUnpaidDone -> Color(0xFFFFEBEE) // Light red for PAY NOW
        booking.status == "done" -> Color(0xFFE3F2FD) // Light blue background for DONE
        booking.status == "cancelled" -> Color(0xFFFFEBEE)
        else -> Color(0xFFE8F5E9)
    }

    val statusTextColor = when {
        isUnpaidDone -> Color(0xFFF44336) // Red for PAY NOW
        booking.status == "done" -> Color(0xFF2196F3)
        booking.status == "cancelled" -> Color(0xFFF44336)
        booking.status == "accepted" -> Color(0xFF4CAF50)
        booking.status == "arrived" -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { if (isUnpaidDone) onPay() else onViewDetails() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Provider Image
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (booking.providerImage?.isNotEmpty() == true) {
                        AsyncImage(
                            model = booking.providerImage,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = booking.providerName ?: "Partner",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = booking.serviceName.replace("(Custom)", "").trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    if (booking.status == "accepted" || booking.status == "on_the_way" || booking.status == "arrived") {
                        Surface(
                            color = Color(0xFFF5F5F5),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                "Urgent",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }

                        if (booking.otp != null) {
                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Service OTP: ${booking.otp}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = statusColor,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.then(
                            if (isUnpaidDone) Modifier.clickable { onPay() } else Modifier
                        )
                    ) {
                        Text(
                            text = if (isUnpaidDone) "PAY NOW" else booking.status.replace("_", " ").uppercase(),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusTextColor,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = try { booking.createdAt.take(10) } catch(e: Exception) { "14 May 2025" },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.Schedule, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = try {
                                if (booking.scheduledTime.contains("|")) {
                                    booking.scheduledTime.split("|").last().trim()
                                } else {
                                    booking.scheduledTime
                                }
                            } catch(e: Exception) { "10:30 AM" },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (booking.status == "done") {
                    if (booking.paymentStatus != "paid") {
                        BookingActionItem(Icons.Default.Payments, "Pay Now") { onPay() }
                        VerticalDivider(modifier = Modifier.height(20.dp), color = Color.LightGray.copy(alpha = 0.5f))
                    }
                    BookingActionItem(Icons.Default.History, "Rebook") { onRebook() }
                    if (booking.paymentStatus == "paid") {
                        VerticalDivider(modifier = Modifier.height(20.dp), color = Color.LightGray.copy(alpha = 0.5f))
                        BookingActionItem(Icons.Default.StarBorder, "Rate & Review") { onRate() }
                        VerticalDivider(modifier = Modifier.height(20.dp), color = Color.LightGray.copy(alpha = 0.5f))
                        BookingActionItem(Icons.Default.FileDownload, "Invoice") {
                            scope.launch {
                                InvoiceGenerator.generateInvoice(context, booking)
                            }
                        }
                    }
                } else {
                    BookingActionItem(Icons.Default.LocationOn, "Track Partner") { onTrackPartner() }
                    VerticalDivider(modifier = Modifier.height(20.dp), color = Color.LightGray.copy(alpha = 0.5f))
                    BookingActionItem(Icons.Default.Call, "Call") {
                        callViewModel.startCall(
                            bookingId = booking._id,
                            customerId = booking.customerUid,
                            partnerId = booking.providerUid,
                            partnerName = booking.providerName ?: "Partner"
                        )
                    }
                    VerticalDivider(modifier = Modifier.height(20.dp), color = Color.LightGray.copy(alpha = 0.5f))
                    BookingActionItem(Icons.AutoMirrored.Filled.Chat, "Chat") { onChat() }
                }
                VerticalDivider(modifier = Modifier.height(20.dp), color = Color.LightGray.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier.clickable { onViewDetails() }.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("View Details", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }


}

@Composable
fun BookingFilterChip(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF1B5E20) else Color.White,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(0.5f)),
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color.Gray,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Color.Gray
            )
        }
    }


}

@Composable
fun BookingActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable { onClick() }.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}


@Composable
fun WalletTabContent(user: com.abhi.madadwala_1.data.remote.UserResponse?, onNavigate: (String) -> Unit) {
    val viewModel: com.abhi.madadwala_1.ui.viewmodel.WalletViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    var isBalanceVisible by rememberSaveable { mutableStateOf(true) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9F9))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "My Wallet",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "Manage your balance and transactions",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "100% Secure",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            when (val state = uiState) {
                is com.abhi.madadwala_1.ui.viewmodel.WalletState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MadadwalaColors.Green)
                    }
                }
                is com.abhi.madadwala_1.ui.viewmodel.WalletState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = state.message, color = Color.Red)
                        Button(onClick = { viewModel.fetchWalletData() }, colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Green)) {
                            Text("Retry")
                        }
                    }
                }
                is com.abhi.madadwala_1.ui.viewmodel.WalletState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        item {
                            com.abhi.madadwala_1.ui.components.WalletBalanceCard(
                                balance = state.balance,
                                walletId = state.walletId,
                                isVisible = isBalanceVisible,
                                onToggleVisibility = {
                                    isBalanceVisible = !isBalanceVisible
                                    if (isBalanceVisible) {
                                        viewModel.fetchWalletData()
                                    }
                                },
                                onAddMoney = { onNavigate(Screen.AddMoney.route) },
                                onTransactions = { onNavigate(Screen.TransactionHistory.route) }
                            )
                        }

                        item {
                            com.abhi.madadwala_1.ui.components.WalletQuickActionsRow(
                                onAddMoney = { onNavigate(Screen.AddMoney.route) },
                                onSendMoney = { onNavigate(Screen.SendMoney.route) },
                                onSecureInfo = {
                                    Toast.makeText(context, "Your money is safe with our secure payment partners.", Toast.LENGTH_SHORT).show()
                                },
                                onHelpSupport = { onNavigate(Screen.HelpSupport.route) }
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Transactions",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                TextButton(onClick = { onNavigate(Screen.TransactionHistory.route) }) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("View All", color = Color(0xFF4CAF50))
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = Color(0xFF4CAF50)
                                        )
                                    }
                                }
                            }
                        }

                        items(state.transactions.take(5)) { transaction ->
                            TransactionItemV2(transaction, state.balance)
                        }

                        item {
                            com.abhi.madadwala_1.ui.components.InviteEarnBanner(
                                onInvite = { onNavigate(Screen.InviteEarn.route) }
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                }
                else -> {}
            }
        }
    }


}

@Composable
fun ProfileTabContent(
    name: String,
    phone: String,
    profileImage: String?,
    uid: String,
    userLat: Double,
    userLng: Double,
    totalBookings: Int,
    activeBookingId: String? = null,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit
) {
    var showEditProfile by rememberSaveable { mutableStateOf(false) }
    var showSavedAddresses by rememberSaveable { mutableStateOf(false) }
    var showSosConfirmation by rememberSaveable { mutableStateOf(false) }
    var isSendingSos by remember { mutableStateOf(false) }
    var legalContentType by rememberSaveable { mutableStateOf<String?>(null) }
    var currentName by remember { mutableStateOf(name) }
    var offersCount by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val authViewModel: AuthViewModel = viewModel()
    val callViewModel: CallViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )
    val authState by authViewModel.authState.collectAsState()
    val user = (authState as? AuthState.Authenticated)?.user
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    val unreadCount by preferenceManager.unreadNotificationsCount.collectAsState(initial = 0)

    LaunchedEffect(Unit) {
        try {
            val res = com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.getOffers()
            if (res.isSuccessful) {
                offersCount = res.body()?.size ?: 0
            }
        } catch (_: Exception) {}
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MadadwalaColors.Cream),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Header with Title and Icons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "My Profile",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MadadwalaColors.Green
                        )
                    )
                    Text(
                        text = "Manage your account and preferences",
                        style = MaterialTheme.typography.bodySmall,
                        color = MadadwalaColors.Gray
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        IconButton(onClick = { onNavigate(Screen.Notifications.route) }) {
                            if (unreadCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = MadadwalaColors.Red) {
                                            Text(unreadCount.toString(), color = Color.White)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications", tint = MadadwalaColors.Green)
                                }
                            } else {
                                Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications", tint = MadadwalaColors.Green)
                            }
                        }
                    }
                    var showHelpMenu by rememberSaveable { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showHelpMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Help", tint = MadadwalaColors.Green)
                        }
                        DropdownMenu(
                            expanded = showHelpMenu,
                            onDismissRequest = { showHelpMenu = false },
                            modifier = Modifier.width(200.dp).background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Help & Support", color = MadadwalaColors.Green, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    showHelpMenu = false
                                    onNavigate(Screen.HelpSupport.route)
                                },
                                leadingIcon = { Icon(Icons.Default.SupportAgent, contentDescription = null, tint = MadadwalaColors.Green) }
                            )
                        }
                    }
                }
            }
        }

        // Profile Header Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(CircleShape)
                                        .background(MadadwalaColors.Green.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!profileImage.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = profileImage,
                                            contentDescription = "Profile Photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = currentName.take(1).uppercase(),
                                            color = MadadwalaColors.Green,
                                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black)
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.weight(2f).padding(start = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentName,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MadadwalaColors.Green
                                    )
                                )
                                if (user?.isVerified == true) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = "+91 $phone",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MadadwalaColors.Gray
                            )
                            user?.email?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MadadwalaColors.Gray
                                )
                            }
                        }

                        // Customer Badge
                        Surface(
                            color = MadadwalaColors.Green.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Customer",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MadadwalaColors.Green
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    HorizontalDivider(color = MadadwalaColors.LightGray.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ProfileStatItem(
                            icon = Icons.Default.CalendarMonth,
                            value = "$totalBookings",
                            label = "Total Bookings"
                        )
                        Box(modifier = Modifier.width(1.dp).height(40.dp).background(MadadwalaColors.LightGray.copy(0.3f)))
                        ProfileStatItem(
                            icon = Icons.Default.AccountBalanceWallet,
                            value = "₹${String.format("%,.2f", user?.walletBalance ?: 0.0)}",
                            label = "Wallet Balance"
                        )
                        Box(modifier = Modifier.width(1.dp).height(40.dp).background(MadadwalaColors.LightGray.copy(0.3f)))
                        ProfileStatItem(
                            icon = Icons.Default.LocalOffer,
                            value = "${offersCount + (if ((user?.pendingReferralDiscount ?: 0.0) > 0) 1 else 0)}",
                            label = "Coupons"
                        )
                    }
                }
            }
        }

        // Edit Profile Item
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { showEditProfile = true },
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MadadwalaColors.LightGray.copy(0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MadadwalaColors.Green, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Edit Profile",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MadadwalaColors.Green
                            )
                        )
                        Text(
                            "Update your personal information",
                            style = MaterialTheme.typography.labelSmall,
                            color = MadadwalaColors.Gray
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MadadwalaColors.Gray)
                }
            }
        }

        // Emergency SOS Button
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { showSosConfirmation = true },
                shape = RoundedCornerShape(12.dp),
                color = MadadwalaColors.Red.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, MadadwalaColors.Red.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).background(MadadwalaColors.Red, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Emergency SOS",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = MadadwalaColors.Red
                            )
                        )
                        Text(
                            "Instant alert to Madadwala Support",
                            style = MaterialTheme.typography.labelSmall,
                            color = MadadwalaColors.Red.copy(0.7f)
                        )
                    }
                    if (isSendingSos) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MadadwalaColors.Red, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MadadwalaColors.Red.copy(0.5f))
                    }
                }
            }
        }

        // Sections
        item {
            ProfileSection("Account Settings") {
                ProfileOptionItem("Saved Addresses", "Manage your saved locations", Icons.Default.LocationOn) { showSavedAddresses = true }
                ProfileOptionItem("Favorites", "Your favorite services and partners", Icons.Default.Favorite) { onNavigate(Screen.Favorites.route) }
            }
        }

        item {
            ProfileSection("Promotions") {
                ProfileOptionItem("Offers & Coupons", "View exclusive offers and coupons", Icons.Default.LocalOffer) { onNavigate(Screen.Offers.route) }
                ProfileOptionItem("Invite & Earn", "Invite friends and earn rewards", Icons.Default.Group) { onNavigate(Screen.InviteEarn.route) }
            }
        }

        item {
            ProfileSection("Support & Info") {
                ProfileOptionItem("Help & Support", "Get help and contact support", Icons.Default.SupportAgent) { onNavigate(Screen.HelpSupport.route) }
                ProfileOptionItem("About Madadwala", "App info, terms and policies", Icons.Default.Info) { legalContentType = "About" }
                ProfileOptionItem("Terms & Conditions", "Usage terms and legal rules", Icons.Default.Description) { legalContentType = "Terms" }
                ProfileOptionItem("Privacy Policy", "How we handle your data", Icons.Default.PrivacyTip) { legalContentType = "Privacy" }
            }
        }

        // Logout
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { onLogout() },
                shape = RoundedCornerShape(12.dp),
                color = MadadwalaColors.Red.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, MadadwalaColors.Red.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MadadwalaColors.Red)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Logout", fontWeight = FontWeight.Bold, color = MadadwalaColors.Red)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }


    // Dialogs
    if (showEditProfile) {
        var newName by remember { mutableStateOf(currentName) }
        AlertDialog(
            onDismissRequest = { showEditProfile = false },
            containerColor = Color.White,
            title = { Text("Update Profile", fontWeight = FontWeight.Bold, color = MadadwalaColors.Green) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Your Name", color = MadadwalaColors.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = MadadwalaColors.Green, fontWeight = FontWeight.SemiBold),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MadadwalaColors.Green,
                        unfocusedBorderColor = MadadwalaColors.LightGray,
                        focusedLabelColor = MadadwalaColors.Green
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        user?.uid?.let { uid ->
                            scope.launch {
                                val res = com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.updateProfile(uid, mapOf("name" to newName))
                                if (res.isSuccessful) {
                                    currentName = newName
                                    authViewModel.refresh()
                                    showEditProfile = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Green),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfile = false }) {
                    Text("Cancel", color = MadadwalaColors.Gray, fontWeight = FontWeight.Medium)
                }
            }
        )
    }

    if (showSavedAddresses) {
        SavedAddressesDialog(uid = user?.uid ?: "", onDismiss = { showSavedAddresses = false })
    }

    if (showSosConfirmation) {
        AlertDialog(
            onDismissRequest = { showSosConfirmation = false },
            containerColor = Color.White,
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MadadwalaColors.Red, modifier = Modifier.size(48.dp)) },
            title = { Text("Emergency SOS", fontWeight = FontWeight.Black, color = MadadwalaColors.Red) },
            text = {
                Text(
                    "Are you in an emergency? This will send your current location and details to Madadwala Support team immediately.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSosConfirmation = false
                        isSendingSos = true
                        scope.launch {
                            try {
                                val sosData = mapOf(
                                    "uid" to uid,
                                    "name" to name,
                                    "phoneNumber" to phone,
                                    "location" to mapOf("lat" to userLat, "lng" to userLng),
                                    "bookingId" to (activeBookingId ?: "")
                                )
                                // Use trackingApiService (Render) because it supports real-time Sockets
                                val res = com.abhi.madadwala_1.data.remote.RetrofitClient.trackingApiService.sendSOS(sosData)
                                if (res.isSuccessful) {
                                    Toast.makeText(context, "Emergency alert sent successfully!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Failed to send alert. Please call support.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Connection error. Please call support.", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSendingSos = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Red),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("YES, SEND ALERT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSosConfirmation = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    if (legalContentType != null) {
        LegalContentDialog(
            type = legalContentType!!,
            onDismiss = { legalContentType = null }
        )
    }
}

@Composable
fun LegalContentDialog(type: String, onDismiss: () -> Unit) {
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
            1. Acceptance of Terms: By using Madadwala, you agree to comply with our service rules.
            
            2. User Responsibilities: Users must provide accurate location and contact information for service delivery.
            
            3. Payments: All payments must be made through our secure gateway or agreed cash-on-service methods.
            
            4. Cancellations: Cancellations are allowed up to 30 minutes before the scheduled time. Refunds are processed to the wallet.
            
            5. Prohibited Activities: Any harassment of service partners or misuse of the platform will result in an immediate permanent ban.
        """.trimIndent()
        "Privacy" -> """
            1. Data Collection: We collect your phone number, name, and location to provide accurate services.
            
            2. Data Usage: Your location is shared with the assigned partner ONLY during an active booking.
            
            3. Security: We use industry-standard encryption to protect your payment and personal data.
            
            4. Third Parties: We do not sell your personal information to third-party advertisers.
            
            5. Controls: You can request account deletion and data removal at any time through our support channel.
        """.trimIndent()
        else -> ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(title, fontWeight = FontWeight.Bold, color = MadadwalaColors.Green) },
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
                colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Green),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun ProfileSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.Black
            ),
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, MadadwalaColors.LightGray.copy(0.3f))
        ) {
            Column {
                content()
            }
        }
    }


}

@Composable
fun ProfileOptionItem(label: String, subtitle: String? = null, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(MadadwalaColors.Green.copy(0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MadadwalaColors.Green, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MadadwalaColors.Gray
                )
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Black.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
    }
}

@Composable
fun ProfileStatItem(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(MadadwalaColors.Green.copy(0.05f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MadadwalaColors.Green, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = Color.Black,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MadadwalaColors.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SavedAddressesDialog(uid: String, onDismiss: () -> Unit) {
    var addresses by remember { mutableStateOf<List<com.abhi.madadwala_1.data.remote.AddressResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showMapPicker by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val res = com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.getAddresses(uid)
            if (res.isSuccessful) addresses = res.body() ?: emptyList()
        } catch (_: Exception) {} finally { isLoading = false }
    }

    if (showMapPicker) {
        MapLocationPickerDialog(
            onDismiss = { showMapPicker = false },
            onLocationConfirmed = { lat, lng, addressText, label ->
                scope.launch {
                    val body = mapOf(
                        "label" to label,
                        "fullAddress" to addressText,
                        "lat" to lat,
                        "lng" to lng
                    )
                    val res = com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.addAddress(uid, body)
                    if (res.isSuccessful) {
                        showMapPicker = false
                        val refresh = com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.getAddresses(uid)
                        if (refresh.isSuccessful) addresses = refresh.body() ?: emptyList()
                    }
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Saved Addresses", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    if (addresses.isEmpty()) {
                        Text("No saved addresses", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(addresses) { addr ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Home, contentDescription = null, tint = MadadwalaColors.Green)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(addr.label, fontWeight = FontWeight.Bold)
                                    Text(addr.fullAddress, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        val res = com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.removeAddress(uid, addr._id)
                                        if (res.isSuccessful) addresses = addresses.filter { it._id != addr._id }
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(0.6f))
                                }
                            }
                            HorizontalDivider(color = MadadwalaColors.LightGray.copy(0.5f))
                        }
                    }
                }
                TextButton(
                    onClick = { showMapPicker = true },
                    modifier = Modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = MadadwalaColors.Green)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Address on Map", color = MadadwalaColors.Green, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = MadadwalaColors.Green, fontWeight = FontWeight.Bold) } }
    )
}

@Composable
fun MapLocationPickerDialog(
    onDismiss: () -> Unit,
    onLocationConfirmed: (Double, Double, String, String) -> Unit
) {
    val context = LocalContext.current
    val initialLocation = LatLng(21.6264, 73.0152) // Default to Ankleshwar
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLocation, 15f)
    }

    var addressText by remember { mutableStateOf("Locating...") }
    var selectedTag by remember { mutableStateOf("Home") }
    val tags = listOf("Home", "Work", "Other")

    val geocoder = remember { Geocoder(context, Locale.getDefault()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val center = cameraPositionState.position.target
            scope.launch {
                try {
                    val addresses = geocoder.getFromLocation(center.latitude, center.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        addressText = addresses[0].getAddressLine(0) ?: "Unknown Location"
                    }
                } catch (_: Exception) {
                    addressText = "Location pinpointed"
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = false)
                )

                // Static center marker
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MadadwalaColors.Red,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                        .offset(y = (-24).dp)
                )

                // Top Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            "Point to your house",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Bottom confirmation
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = Color.White,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = addressText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("SAVE AS", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tags.forEach { tag ->
                                FilterChip(
                                    selected = selectedTag == tag,
                                    onClick = { selectedTag = tag },
                                    label = { Text(tag) },
                                    leadingIcon = {
                                        if (selectedTag == tag) {
                                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MadadwalaColors.Green.copy(alpha = 0.1f),
                                        selectedLabelColor = MadadwalaColors.Green
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                val target = cameraPositionState.position.target
                                onLocationConfirmed(target.latitude, target.longitude, addressText, selectedTag)
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Green),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Confirm Location", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }


}

data class ServiceItem(val name: String, val icon: ImageVector, val color: Color)

@Composable
fun ServiceCard(service: ServiceItem, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().bounceClick { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MadadwalaColors.Lime.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = service.icon,
                contentDescription = null,
                tint = MadadwalaColors.Green,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = service.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MadadwalaColors.Green),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LocationPickerOverlay(
    uid: String,
    onDismiss: () -> Unit,
    onLocationSelected: (Double, Double, String) -> Unit,
    onUseLiveLocation: () -> Unit
) {
    var addresses by remember { mutableStateOf<List<com.abhi.madadwala_1.data.remote.AddressResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val res = com.abhi.madadwala_1.data.remote.RetrofitClient.apiService.getAddresses(uid)
            if (res.isSuccessful) addresses = res.body() ?: emptyList()
        } catch (_: Exception) {} finally { isLoading = false }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Select Location", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onUseLiveLocation,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MadadwalaColors.Green)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Use Live Location", fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MadadwalaColors.LightGray.copy(alpha = 0.5f))

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    Text("Saved Addresses", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (addresses.isEmpty()) {
                        Text("No saved addresses", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }

                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(addresses) { addr ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onLocationSelected(addr.lat, addr.lng, addr.fullAddress) }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Home, contentDescription = null, tint = MadadwalaColors.Green, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(addr.label, fontWeight = FontWeight.Bold)
                                    Text(addr.fullAddress, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Green)
                ) {
                    Text("Cancel")
                }
            }
        }
    }


}

data class AcceptanceData(
    val id: String,
    val category: String,
    val providerName: String
)

@Composable
fun RunningCharacter() {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(com.abhi.madadwala_1.R.raw.boy_error))
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun RocketCharacter() {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(com.abhi.madadwala_1.R.raw.businessman_rocket))
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun BidTimer(expiryTimeMillis: Long, onExpire: () -> Unit = {}) {
    var timeLeft by remember { mutableLongStateOf(0L) }
    val currentOnExpire by rememberUpdatedState(onExpire)

    LaunchedEffect(expiryTimeMillis) {
        while (true) {
            val now = System.currentTimeMillis()
            timeLeft = (expiryTimeMillis - now) / 1000
            if (timeLeft <= 0) {
                timeLeft = 0
                currentOnExpire()
                break
            }
            delay(1000)
        }
    }

    if (timeLeft > 0) {
        Surface(
            color = MadadwalaColors.Red.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Accept in ${timeLeft}s",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MadadwalaColors.Red,
                fontWeight = FontWeight.Bold
            )
        }
    }


}
