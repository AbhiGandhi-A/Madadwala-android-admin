package com.abhi.madadwala_1.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.abhi.madadwala_1.data.remote.ProviderDetailResponse
import com.abhi.madadwala_1.data.remote.RetrofitClient
import com.abhi.madadwala_1.data.remote.ServiceResponse
import com.abhi.madadwala_1.ui.theme.MadadwalaColors
import com.abhi.madadwala_1.ui.viewmodel.AuthState
import com.abhi.madadwala_1.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProviderProfileScreen(
    providerUid: String,
    onBack: () -> Unit,
    onBookService: (String, Double) -> Unit
) {
    var detail by remember { mutableStateOf<ProviderDetailResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()
    val currentUser = (authState as? AuthState.Authenticated)?.user

    var showAllServices by remember { mutableStateOf(false) }
    var showAllReviews by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(providerUid) {
        scope.launch {
            try {
                val response = RetrofitClient.apiService.getProviderDetails(providerUid)
                if (response.isSuccessful) {
                    detail = response.body()
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoading = false
            }
        }
    }

    if (showReportDialog && currentUser != null) {
        ReportAbuseDialog(
            reporterUid = currentUser.uid,
            reportedUid = providerUid,
            onDismiss = { showReportDialog = false },
            onSubmitSuccess = {
                showReportDialog = false
                Toast.makeText(context, "Report submitted successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Provider Profile", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MadadwalaColors.Ink) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MadadwalaColors.Ink)
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Check out this professional on Madadwala: ${detail?.provider?.name}")
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Report Abuse", color = MadadwalaColors.Ink) },
                                onClick = {
                                    showMenu = false
                                    showReportDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Report, contentDescription = null, tint = Color.Red) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            if (detail != null) {
                BottomActionBar(
                    onChat = { /* Chat functionality - placeholder as no chat screen found */ },
                    onCall = { 
                        // Note: ProviderResponse doesn't have phone number in this model
                        // Use a dummy or handle if phone is available in another way
                    },
                    onDirections = {
                        val provider = detail!!.provider
                        if (provider.lat != null && provider.lng != null) {
                            val gmmIntentUri = Uri.parse("geo:${provider.lat},${provider.lng}?q=${provider.lat},${provider.lng}(${provider.name})")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            context.startActivity(mapIntent)
                        }
                    },
                    onBook = {
                        if (detail!!.services.isNotEmpty()) {
                            onBookService(detail!!.services.first().name, detail!!.services.first().price)
                        }
                    }
                )
            }
        },
        containerColor = Color.White
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MadadwalaColors.Green)
            }
        } else if (detail != null) {
            val provider = detail!!.provider
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Profile Header Section
                ProfileHeader(provider = provider)

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Section
                val joinedText = remember(provider.createdAt) {
                    if (provider.createdAt == null) return@remember "1 Month"
                    try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val createdDate = sdf.parse(provider.createdAt.split("T")[0])
                        val now = Calendar.getInstance()
                        val created = Calendar.getInstance().apply { time = createdDate!! }
                        
                        val years = now.get(Calendar.YEAR) - created.get(Calendar.YEAR)
                        val months = now.get(Calendar.MONTH) - created.get(Calendar.MONTH)
                        
                        val totalMonths = years * 12 + months
                        
                        if (totalMonths < 12) {
                            if (totalMonths <= 0) "1 Month" else "$totalMonths Month${if (totalMonths > 1) "s" else ""}"
                        } else {
                            val displayYears = totalMonths / 12
                            "$displayYears Year${if (displayYears > 1) "s" else ""}"
                        }
                    } catch (e: Exception) {
                        "1 Month"
                    }
                }

                StatsSection(
                    jobs = provider.totalJobs.toString(),
                    rating = String.format("%.1f", provider.rating),
                    responseTime = "10 mins",
                    joined = joinedText
                )

                Spacer(modifier = Modifier.height(24.dp))

                // About Section
                AboutSection(
                    providerName = provider.name,
                    bio = provider.bio ?: "Experienced and reliable professional providing safe and quality services."
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Services Offered Section
                ServicesSection(
                    services = detail!!.services,
                    showAll = showAllServices,
                    onToggleViewAll = { showAllServices = !showAllServices },
                    onBook = { service -> onBookService(service.name, service.price) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Reviews & Ratings Section
                ReviewsSection(
                    reviews = detail!!.reviews,
                    showAll = showAllReviews,
                    onToggleSeeAll = { showAllReviews = !showAllReviews }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ProfileHeader(provider: com.abhi.madadwala_1.data.remote.ProviderResponse) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Image with Verification Badge
        Box(contentAlignment = Alignment.BottomEnd) {
            AsyncImage(
                model = provider.profileImage ?: "https://via.placeholder.com/150",
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color(0xFFF0F0F0), CircleShape),
                contentScale = ContentScale.Crop
            )
            if (provider.isVerified) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(24.dp).padding(2.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(20.dp))

        // Provider Info
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                if (provider.isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = provider.category,
                style = MaterialTheme.typography.bodyMedium,
                color = MadadwalaColors.Gray
            )
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB400), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${provider.rating} (${provider.reviewCount} Review${if (provider.reviewCount != 1) "s" else ""})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            InfoRow(icon = Icons.Default.LocationOn, text = "${provider.distance ?: "0.0"} km away")
            InfoRow(icon = Icons.Default.FlashOn, text = "Starts from ₹${provider.startingPrice}")
            InfoRow(icon = Icons.Default.AccessTime, text = "Responds in 10 mins")
        }
    }
    
    // Online Badge
    Box(modifier = Modifier.padding(start = 36.dp)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF1F8E9),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Online",
                    color = Color(0xFF4CAF50),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = Color.Black)
    }
}

@Composable
fun StatsSection(jobs: String, rating: String, responseTime: String, joined: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(modifier = Modifier.weight(1f), icon = Icons.Default.Shield, value = jobs, label = "Jobs Completed")
        StatCard(modifier = Modifier.weight(1f), icon = Icons.Default.StarBorder, value = rating, label = "Rating")
        StatCard(modifier = Modifier.weight(1f), icon = Icons.Default.AccessTime, value = responseTime, label = "Response Time")
        StatCard(modifier = Modifier.weight(1f), icon = Icons.Default.CalendarToday, value = joined, label = "Joined")
    }
}

@Composable
fun StatCard(modifier: Modifier, icon: ImageVector, value: String, label: String) {
    Surface(
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFF5F5F5)),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = MadadwalaColors.Green, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = label, fontSize = 10.sp, color = MadadwalaColors.Gray, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun AboutSection(providerName: String, bio: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0)),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "About $providerName",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = bio,
                    fontSize = 14.sp,
                    color = MadadwalaColors.Gray,
                    lineHeight = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = MadadwalaColors.Green,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "Verified\nProfessional",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MadadwalaColors.Green,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServicesSection(
    services: List<ServiceResponse>,
    showAll: Boolean,
    onToggleViewAll: () -> Unit,
    onBook: (ServiceResponse) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Services Offered", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            TextButton(onClick = onToggleViewAll) {
                Text(text = if (showAll) "View Less" else "View All", color = MadadwalaColors.Green, fontWeight = FontWeight.Bold)
                Icon(
                    if (showAll) Icons.Default.KeyboardArrowUp else Icons.AutoMirrored.Filled.KeyboardArrowRight, 
                    contentDescription = null, 
                    tint = MadadwalaColors.Green
                )
            }
        }

        if (services.isEmpty()) {
            Text("No services available", color = Color.Gray)
        } else {
            if (showAll) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    services.forEach { service ->
                        ServiceCard(service = service, onBook = { onBook(service) })
                    }
                }
            } else {
                val pagerState = rememberPagerState(pageCount = { services.size })
                
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val service = services[page]
                    ServiceCard(service = service, onBook = { onBook(service) })
                }

                if (services.size > 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    // Pager Indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(services.size) { iteration ->
                            val color = if (pagerState.currentPage == iteration) MadadwalaColors.Green else Color.LightGray
                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(if (pagerState.currentPage == iteration) 10.dp else 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceCard(service: ServiceResponse, onBook: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0)),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Service Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MadadwalaColors.Green, modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = service.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = service.description ?: "Professional service installation",
                    fontSize = 12.sp,
                    color = MadadwalaColors.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Starts from", fontSize = 10.sp, color = MadadwalaColors.Gray)
                Text(text = "₹${service.price}", fontWeight = FontWeight.Black, color = MadadwalaColors.Green, fontSize = 16.sp)
            }

            Column(horizontalAlignment = Alignment.End) {
                Button(
                    onClick = onBook,
                    colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Green),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Book Now", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Duration: 30-60 mins", fontSize = 9.sp, color = MadadwalaColors.Gray)
            }
        }
    }
}

@Composable
fun ReviewsSection(
    reviews: List<com.abhi.madadwala_1.data.remote.ReviewResponse>,
    showAll: Boolean,
    onToggleSeeAll: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Reviews & Ratings", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MadadwalaColors.Ink)
            if (reviews.size > 2) {
                TextButton(onClick = onToggleSeeAll) {
                    Text(text = if (showAll) "See Less" else "See All (${reviews.size})", color = MadadwalaColors.Green, fontWeight = FontWeight.Bold)
                    Icon(
                        if (showAll) Icons.Default.KeyboardArrowUp else Icons.AutoMirrored.Filled.KeyboardArrowRight, 
                        contentDescription = null, 
                        tint = MadadwalaColors.Green
                    )
                }
            }
        }

        if (reviews.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MadadwalaColors.Cream.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Comment, contentDescription = null, tint = MadadwalaColors.Gray.copy(0.3f), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No reviews yet", color = MadadwalaColors.Gray, style = MaterialTheme.typography.bodyMedium)
                    Text("Be the first to rate this professional", color = MadadwalaColors.Gray.copy(0.7f), style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            RatingSummary(reviews = reviews)
            
            Spacer(modifier = Modifier.height(16.dp))

            val displayReviews = if (showAll) reviews else reviews.take(2)
            displayReviews.forEach { review ->
                ReviewCard(review)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun RatingSummary(reviews: List<com.abhi.madadwala_1.data.remote.ReviewResponse>) {
    val totalReviews = reviews.size
    val averageRating = if (totalReviews > 0) reviews.map { it.rating }.average() else 0.0
    val ratingCounts = reviews.groupingBy { it.rating }.eachCount()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MadadwalaColors.Cream.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(end = 24.dp)
            ) {
                Text(
                    text = String.format("%.1f", averageRating),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
                Row {
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (index < averageRating.toInt()) Color(0xFFFFB400) else Color.LightGray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Out of 5",
                    style = MaterialTheme.typography.labelSmall,
                    color = MadadwalaColors.Gray
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                for (i in 5 downTo 1) {
                    val count = ratingCounts[i] ?: 0
                    val progress = if (totalReviews > 0) count.toFloat() / totalReviews else 0f
                    RatingBar(label = "$i", progress = progress)
                }
            }
        }
    }
}

@Composable
fun RatingBar(label: String, progress: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 1.dp)
    ) {
        Text(
            text = label, 
            fontSize = 11.sp, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(10.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(CircleShape),
            color = MadadwalaColors.Green,
            trackColor = Color.LightGray.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun ReviewCard(review: com.abhi.madadwala_1.data.remote.ReviewResponse) {
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
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0)),
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                val avatarColor = remember(review.customerName) {
                    val colors = listOf(
                        Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFF9C27B0),
                        Color(0xFFFF9800), Color(0xFFE91E63), Color(0xFF00BCD4)
                    )
                    colors[review.customerName.length % colors.size]
                }
                
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(avatarColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = review.customerName.take(1).uppercase(),
                        color = avatarColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = review.customerName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (index < review.rating) Color(0xFFFFB400) else Color.LightGray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formattedDate,
                            fontSize = 11.sp,
                            color = MadadwalaColors.Gray
                        )
                    }
                }
                
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Verified",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        fontSize = 9.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = review.comment,
                fontSize = 14.sp,
                color = Color(0xFF424242),
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun BottomActionBar(
    onChat: () -> Unit,
    onCall: () -> Unit,
    onDirections: () -> Unit,
    onBook: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 16.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ActionIconButton(icon = Icons.Default.Chat, label = "Chat", onClick = onChat)
            VerticalDivider(modifier = Modifier.height(32.dp).padding(horizontal = 4.dp), color = Color(0xFFF0F0F0))
            ActionIconButton(icon = Icons.Default.Call, label = "Call", onClick = onCall)
            VerticalDivider(modifier = Modifier.height(32.dp).padding(horizontal = 4.dp), color = Color(0xFFF0F0F0))
            ActionIconButton(icon = Icons.Default.Directions, label = "Directions", onClick = onDirections)
            
            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onBook,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Green),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Book Now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun ActionIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = Color(0xFF424242), modifier = Modifier.size(24.dp))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF424242))
    }
}

@Composable
fun ReportAbuseDialog(
    reporterUid: String,
    reportedUid: String,
    onDismiss: () -> Unit,
    onSubmitSuccess: () -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isSubmitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedUris = uris
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Report Abuse", fontWeight = FontWeight.Bold, color = MadadwalaColors.Ink) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Select Reason", style = MaterialTheme.typography.bodySmall, color = MadadwalaColors.Ink)
                val reasons = listOf("Harassment", "Fake Profile", "Spam", "Unprofessional Behavior", "Other")
                var expanded by remember { mutableStateOf(false) }
                
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MadadwalaColors.LightGray),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MadadwalaColors.Ink)
                    ) {
                        Text(if (reason.isEmpty()) "Choose a reason" else reason, color = MadadwalaColors.Ink)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MadadwalaColors.Ink)
                    }
                    DropdownMenu(
                        expanded = expanded, 
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        reasons.forEach { r ->
                            DropdownMenuItem(text = { Text(r, color = MadadwalaColors.Ink) }, onClick = { reason = r; expanded = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Describe the issue", color = MadadwalaColors.Gray) },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MadadwalaColors.Ink,
                        unfocusedTextColor = MadadwalaColors.Ink,
                        focusedBorderColor = MadadwalaColors.Green,
                        unfocusedBorderColor = MadadwalaColors.LightGray
                    )
                )

                Text("Attach Evidence (Images/Video)", style = MaterialTheme.typography.bodySmall, color = MadadwalaColors.Ink)
                OutlinedButton(
                    onClick = { launcher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MadadwalaColors.LightGray),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MadadwalaColors.Ink)
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null, tint = MadadwalaColors.Ink)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (selectedUris.isEmpty()) "Attach Files" else "${selectedUris.size} files attached",
                        color = MadadwalaColors.Ink
                    )
                }
                
                if (selectedUris.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(selectedUris) { uri ->
                            Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray)) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.isEmpty() || description.isEmpty()) {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSubmitting = true
                    scope.launch {
                        try {
                            val reporterBody = reporterUid.toRequestBody("text/plain".toMediaTypeOrNull())
                            val reportedBody = reportedUid.toRequestBody("text/plain".toMediaTypeOrNull())
                            val reasonBody = reason.toRequestBody("text/plain".toMediaTypeOrNull())
                            val descBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
                            
                            val evidenceParts = selectedUris.mapNotNull { uri ->
                                val file = getFileFromUri(context, uri)
                                if (file != null) {
                                    val requestFile = file.asRequestBody(context.contentResolver.getType(uri)?.toMediaTypeOrNull())
                                    MultipartBody.Part.createFormData("evidence", file.name, requestFile)
                                } else null
                            }

                            val response = RetrofitClient.apiService.submitReport(
                                reporterBody, reportedBody, reasonBody, descBody, evidenceParts
                            )

                            if (response.isSuccessful) {
                                onSubmitSuccess()
                            } else {
                                Toast.makeText(context, "Failed to submit report", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isSubmitting = false
                        }
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                else Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel", color = MadadwalaColors.Gray)
            }
        }
    )
}

private fun getFileFromUri(context: android.content.Context, uri: Uri): File? {
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.cacheDir, "report_${System.currentTimeMillis()}_${uri.lastPathSegment}")
        val outputStream = FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        return file
    } catch (e: Exception) {
        return null
    }
}


