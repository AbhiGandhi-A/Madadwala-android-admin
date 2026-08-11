package com.abhi.madadwala_1.ui.screens

import android.location.Location
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.abhi.madadwala_1.data.remote.ProviderResponse
import com.abhi.madadwala_1.data.remote.RetrofitClient
import com.abhi.madadwala_1.ui.theme.MadadwalaColors
import com.abhi.madadwala_1.ui.viewmodel.AuthViewModel
import com.abhi.madadwala_1.ui.viewmodel.AuthState
import com.abhi.madadwala_1.ui.viewmodel.LocationViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListingScreen(
    category: String,
    onBack: () -> Unit,
    onProviderClick: (String) -> Unit,
    onBookClick: (String, String, Double) -> Unit
) {
    var providers by remember { mutableStateOf<List<ProviderResponse>>(emptyList()) }
    var favorites by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { searchQueryState }
    var filterOption by remember { mutableStateOf("Default") }
    var showFilterMenu by remember { mutableStateOf(false) }
    var showLocationPicker by remember { mutableStateOf(false) }

    BackHandler(enabled = isSearching || showFilterMenu || showLocationPicker) {
        if (isSearching) {
            isSearching = false
            searchQuery = ""
        } else if (showFilterMenu) {
            showFilterMenu = false
        } else if (showLocationPicker) {
            showLocationPicker = false
        }
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()
    val user = (authState as? AuthState.Authenticated)?.user

    val locationViewModel: LocationViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )
    val userLat by locationViewModel.latitude.collectAsState()
    val userLng by locationViewModel.longitude.collectAsState()
    val currentAddress by locationViewModel.address.collectAsState()

    LaunchedEffect(category, user?.uid, userLat, userLng) {
        scope.launch {
            try {
                val pResponse = RetrofitClient.apiService.getProviders(
                    category = category,
                    lat = if (userLat != 0.0) userLat else null,
                    lng = if (userLng != 0.0) userLng else null
                )
                if (pResponse.isSuccessful) {
                    providers = pResponse.body() ?: emptyList()
                }

                user?.uid?.let { uid ->
                    val fResponse = RetrofitClient.apiService.getFavorites(uid)
                    if (fResponse.isSuccessful) {
                        favorites = fResponse.body()?.map { it.uid }?.toSet() ?: emptySet()
                    }
                }
            } catch (e: Exception) {
            } finally {
                isLoading = false
            }
        }
    }

    fun toggleFavorite(providerUid: String) {
        val uid = user?.uid ?: return
        scope.launch {
            try {
                if (favorites.contains(providerUid)) {
                    val res = RetrofitClient.apiService.removeFromFavorites(uid, providerUid)
                    if (res.isSuccessful) favorites = favorites - providerUid
                } else {
                    val res = RetrofitClient.apiService.addToFavorites(uid, mapOf("providerUid" to providerUid))
                    if (res.isSuccessful) favorites = favorites + providerUid
                }
            } catch (e: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                TopAppBar(
                    title = {
                        if (isSearching) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search providers...", fontSize = 16.sp, color = MadadwalaColors.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = MadadwalaColors.Ink,
                                    unfocusedTextColor = MadadwalaColors.Ink,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                                singleLine = true
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = CircleShape,
                                    color = MadadwalaColors.Green.copy(alpha = 0.1f)
                                ) {
                                    Icon(
                                        imageVector = when(category.lowercase(Locale.ROOT)) {
                                            "electrical", "electrician" -> Icons.Rounded.FlashOn
                                            "plumbing", "plumber" -> Icons.Rounded.Build
                                            "cleaning" -> Icons.Rounded.CleaningServices
                                            else -> Icons.Rounded.Category
                                        },
                                        contentDescription = null,
                                        tint = MadadwalaColors.Green,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = MadadwalaColors.Ink
                                        )
                                    )
                                    Text(
                                        text = "Find verified ${category.lowercase(Locale.ROOT)}s near you",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MadadwalaColors.Gray
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (isSearching) {
                                isSearching = false
                                searchQuery = ""
                            } else {
                                onBack()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            isSearching = !isSearching
                            if (!isSearching) searchQuery = ""
                        }) {
                            Icon(
                                imageVector = if (isSearching) Icons.Default.Close else Icons.Rounded.Search,
                                contentDescription = "Search"
                            )
                        }

                        IconButton(onClick = { /* Filter action */ }) {
                            Icon(Icons.Rounded.FilterList, contentDescription = "Filter")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )

                // Info Badges Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InfoBadge(Icons.Rounded.Verified, "Verified\nProfessionals")
                    InfoBadge(Icons.Rounded.Timer, "Quick\nResponse")
                    InfoBadge(Icons.Rounded.CurrencyRupee, "Fair\nPricing")
                    InfoBadge(Icons.Rounded.StarRate, "4.8+ Avg\nRating")
                }
                
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)
            }
        },
        containerColor = Color(0xFFF9FBF9) // Very light green background
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Location Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = MadadwalaColors.Green,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentAddress,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MadadwalaColors.Ink,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.clickable { showLocationPicker = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelLarge,
                            color = MadadwalaColors.Green,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MadadwalaColors.Green,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (showLocationPicker) {
                CategoryLocationPickerOverlay(
                    uid = user?.uid ?: "",
                    onDismiss = { showLocationPicker = false },
                    onLocationSelected = { lat: Double, lng: Double, name: String ->
                        locationViewModel.setLocation(context, lat, lng, name)
                        showLocationPicker = false
                    },
                    onUseLiveLocation = {
                        locationViewModel.fetchLocation(context, force = true)
                        showLocationPicker = false
                    }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MadadwalaColors.Green)
                }
            } else {
                val filteredProviders = remember(providers, searchQuery, filterOption) {
                    val list = if (searchQuery.isBlank()) {
                        providers
                    } else {
                        providers.filter { it.name.contains(searchQuery, ignoreCase = true) }
                    }

                    when (filterOption) {
                        "Rating" -> list.sortedByDescending { it.rating }
                        "PriceLow" -> list.sortedBy { it.startingPrice }
                        "PriceHigh" -> list.sortedByDescending { it.startingPrice }
                        else -> list
                    }
                }

                if (filteredProviders.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MadadwalaColors.Gray.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isEmpty()) "No providers available" else "No providers found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MadadwalaColors.Gray
                            )
                        }
                    }
                } else {
                    Column {
                        // Providers Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Available Providers",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MadadwalaColors.Green.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${filteredProviders.size} found",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MadadwalaColors.Green,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Sort Dropdown
                            Box {
                                Surface(
                                    onClick = { showFilterMenu = true },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Sort by: ${
                                                when(filterOption) {
                                                    "Rating" -> "Top Rated"
                                                    "PriceLow" -> "Price: Low to High"
                                                    "PriceHigh" -> "Price: High to Low"
                                                    else -> "Default"
                                                }
                                            }",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MadadwalaColors.Ink
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MadadwalaColors.Ink)
                                    }
                                }

                                DropdownMenu(
                                    expanded = showFilterMenu,
                                    onDismissRequest = { showFilterMenu = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    listOf(
                                        "Default" to "Default",
                                        "Top Rated" to "Rating",
                                        "Price: Low to High" to "PriceLow",
                                        "Price: High to Low" to "PriceHigh"
                                    ).forEach { (label, value) ->
                                        DropdownMenuItem(
                                            text = { Text(label, color = MadadwalaColors.Ink) },
                                            onClick = {
                                                filterOption = value
                                                showFilterMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp), // Space for safety banner
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredProviders) { provider ->
                                val distance = remember(userLat, userLng, provider.lat, provider.lng) {
                                    if (userLat != 0.0 && provider.lat != null) {
                                        val results = FloatArray(1)
                                        Location.distanceBetween(userLat, userLng, provider.lat, provider.lng!!, results)
                                        val km = results[0] / 1000
                                        String.format("%.1f km away", km)
                                    } else {
                                        "Near you"
                                    }
                                }

                                ProviderCard(
                                    provider = provider,
                                    distance = distance,
                                    isFavorite = favorites.contains(provider.uid),
                                    onFavoriteToggle = { toggleFavorite(provider.uid) },
                                    onCardClick = { onProviderClick(provider.uid) },
                                    onBookClick = { onBookClick(provider.uid, provider.category, provider.startingPrice) }
                                )
                            }
                            
                            item {
                                // Safety Banner
                                SafetyBanner()
                            }
                        }
                    }
                }
            }
        }
    }
}

// Global search state to avoid losing focus/resetting
private val searchQueryState = mutableStateOf("")

@Composable
fun InfoBadge(icon: ImageVector, text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MadadwalaColors.Green,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                lineHeight = 12.sp,
                textAlign = TextAlign.Center
            ),
            color = MadadwalaColors.Ink,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SafetyBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        color = MadadwalaColors.Green.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, MadadwalaColors.Green.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = MadadwalaColors.Green.copy(alpha = 0.1f)
            ) {
                Icon(
                    Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = MadadwalaColors.Green,
                    modifier = Modifier.padding(6.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your Safety, Our Priority",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MadadwalaColors.Ink
                )
                Text(
                    text = "All our service providers are verified and background checked",
                    style = MaterialTheme.typography.bodySmall,
                    color = MadadwalaColors.Gray
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MadadwalaColors.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ProviderCard(
    provider: ProviderResponse,
    distance: String,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onCardClick: () -> Unit,
    onBookClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Provider Image
                Box {
                    if (!provider.profileImage.isNullOrEmpty()) {
                        AsyncImage(
                            model = provider.profileImage,
                            contentDescription = null,
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MadadwalaColors.Cream),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MadadwalaColors.Green, modifier = Modifier.size(40.dp))
                        }
                    }
                    
                    // Availability Dot
                    if (provider.isAvailable) {
                        Surface(
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.BottomEnd)
                                .offset(x = 4.dp, y = 4.dp),
                            shape = CircleShape,
                            color = Color(0xFF4CAF50),
                            border = BorderStroke(2.dp, Color.White)
                        ) {}
                    }
                    
                    // Top Rated Badge
                    if (provider.rating >= 4.0) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = (-4).dp, y = (-8).dp),
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFB400)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Top Rated", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = provider.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MadadwalaColors.Ink
                            )
                            if (provider.isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Rounded.Verified,
                                    contentDescription = "Verified",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        IconButton(
                            onClick = onFavoriteToggle,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color.Red else MadadwalaColors.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Text(
                        text = provider.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MadadwalaColors.Gray
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFFFFB400), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", provider.rating),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MadadwalaColors.Ink
                        )
                        Text(
                            text = " (${provider.reviewCount} reviews)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MadadwalaColors.Gray
                        )
                        
                        // Reply Status (Using real data if available, or logic)
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MadadwalaColors.Green.copy(alpha = 0.05f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (provider.rating >= 4.0) Icons.Rounded.FlashOn else Icons.Rounded.QueryBuilder,
                                    contentDescription = null,
                                    tint = MadadwalaColors.Green,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (provider.rating >= 4.0) "Quick Response" else "Usually replies fast",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = MadadwalaColors.Green,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = MadadwalaColors.Gray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = distance,
                            style = MaterialTheme.typography.bodySmall,
                            color = MadadwalaColors.Gray
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CurrencyRupee, contentDescription = null, tint = MadadwalaColors.Gray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Starts from ₹${provider.startingPrice}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MadadwalaColors.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            // Skills/Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Since we don't have real tags in ProviderResponse, we use category-based tags
                val tags = when(provider.category.lowercase(Locale.ROOT)) {
                    "electrical", "electrician" -> listOf("Light Fitting", "Wiring", "Switch Repair")
                    "plumbing", "plumber" -> listOf("Pipe Leak", "Tap Repair", "Drainage")
                    "cleaning" -> listOf("Deep Cleaning", "Bathroom", "Kitchen")
                    else -> listOf(provider.category, "Verified", "Expert")
                }
                
                tags.take(3).forEach { tag ->
                    ServiceTag(tag)
                }
                if (tags.size > 3) {
                    Text(
                        text = "+${tags.size - 3} more",
                        style = MaterialTheme.typography.labelSmall,
                        color = MadadwalaColors.Gray,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCardClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MadadwalaColors.Green),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = MadadwalaColors.Green)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Profile", color = MadadwalaColors.Green, fontWeight = FontWeight.Bold)
                    }
                }
                
                Button(
                    onClick = onBookClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Green),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Book Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceTag(tag: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MadadwalaColors.Green.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, MadadwalaColors.Green.copy(alpha = 0.1f))
    ) {
        Text(
            text = tag,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MadadwalaColors.Green
        )
    }
}

@Composable
fun CategoryLocationPickerOverlay(
    uid: String,
    onDismiss: () -> Unit,
    onLocationSelected: (Double, Double, String) -> Unit,
    onUseLiveLocation: () -> Unit
) {
    var addresses by remember { mutableStateOf<List<com.abhi.madadwala_1.data.remote.AddressResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(uid) {
        try {
            val res = RetrofitClient.apiService.getAddresses(uid)
            if (res.isSuccessful) addresses = res.body() ?: emptyList()
        } catch (e: Exception) {} finally { isLoading = false }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Select Location",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MadadwalaColors.Ink
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onUseLiveLocation,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MadadwalaColors.Green)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Use Live Location", fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MadadwalaColors.LightGray.copy(alpha = 0.5f)
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MadadwalaColors.Green)
                    }
                } else {
                    Text(
                        "Saved Addresses",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (addresses.isEmpty()) {
                        Text(
                            "No saved addresses",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(addresses) { addr ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onLocationSelected(addr.lat, addr.lng, addr.label) }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Home,
                                    contentDescription = null,
                                    tint = MadadwalaColors.Green,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(addr.label, fontWeight = FontWeight.Bold, color = MadadwalaColors.Ink)
                                    Text(
                                        addr.fullAddress,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Green)
            ) {
                Text("Cancel")
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}
