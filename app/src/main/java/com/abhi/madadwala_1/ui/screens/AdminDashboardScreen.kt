package com.abhi.madadwala_1.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.abhi.madadwala_1.data.remote.*
import com.abhi.madadwala_1.ui.theme.MadadwalaColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(onLogout: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(1) }

    BackHandler(enabled = selectedTab != 1) {
        selectedTab = 1
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Madadwala Admin", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))
                        Text("System Administrator", style = MaterialTheme.typography.bodySmall.copy(color = MadadwalaColors.Teal))
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = MadadwalaColors.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MadadwalaColors.Cream)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val items = listOf(
                    Triple(0, Icons.Default.Analytics, "Analytics"),
                    Triple(1, Icons.Default.GroupAdd, "Requests"),
                    Triple(2, Icons.Default.Payments, "Withdrawals"),
                    Triple(3, Icons.Default.SupportAgent, "Support"),
                    Triple(4, Icons.Default.ManageAccounts, "Manage")
                )
                items.forEach { (index, icon, label) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MadadwalaColors.Teal,
                            selectedTextColor = MadadwalaColors.Teal,
                            unselectedIconColor = MadadwalaColors.Gray,
                            indicatorColor = MadadwalaColors.Teal.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        },
        containerColor = MadadwalaColors.Cream
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "AdminTabTransition"
            ) { tab ->
                when (tab) {
                    0 -> AnalyticsTab()
                    1 -> ProviderRequestsTab()
                    2 -> WithdrawalsTab()
                    3 -> SupportChatTab()
                    4 -> ManagementTab()
                }
            }
        }
    }
}

@Composable
fun WithdrawalsTab() {
    var withdrawals by remember { mutableStateOf<List<WithdrawalResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showRejectDialog by remember { mutableStateOf<WithdrawalResponse?>(null) }
    var rejectionReason by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val fetch = {
        scope.launch {
            isLoading = true
            try {
                val res = RetrofitClient.apiService.getPendingWithdrawals()
                if (res.isSuccessful) withdrawals = res.body() ?: emptyList()
            } catch (e: Exception) {} finally { isLoading = false }
        }
    }

    LaunchedEffect(Unit) { fetch() }

    BackHandler(enabled = showRejectDialog != null) {
        showRejectDialog = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MadadwalaColors.Teal)
        } else if (withdrawals.isEmpty()) {
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(64.dp), tint = MadadwalaColors.LightGray)
                Text("No pending withdrawal requests", color = MadadwalaColors.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("Withdrawal Requests", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                }
                items(withdrawals) { req ->
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(req.providerName, fontWeight = FontWeight.Bold)
                                    Text("Requested: ₹${req.amount}", color = MadadwalaColors.GreenDark, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                }
                                Surface(color = when(req.status.lowercase()) {
                                    "pending" -> MadadwalaColors.Teal.copy(alpha = 0.1f)
                                    "failed" -> MadadwalaColors.Red.copy(alpha = 0.1f)
                                    else -> MadadwalaColors.Gray.copy(alpha = 0.1f)
                                }, shape = RoundedCornerShape(8.dp)) {
                                    Text(req.status.uppercase(), modifier = Modifier.padding(6.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = when(req.status.lowercase()) {
                                        "pending" -> MadadwalaColors.Teal
                                        "failed" -> MadadwalaColors.Red
                                        else -> MadadwalaColors.Gray
                                    })
                                }
                            }
                            
                            if (req.errorMessage != null) {
                                Text("Error: ${req.errorMessage}", color = MadadwalaColors.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                            
                            HorizontalDivider(Modifier.padding(vertical = 12.dp))
                            
                            Text("Bank Details", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("A/C: ${req.accountNumber}", fontWeight = FontWeight.Medium)
                            Text("IFSC: ${req.ifscCode}", fontWeight = FontWeight.Medium)
                            Text("Holder: ${req.holderName}", fontWeight = FontWeight.Medium)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { showRejectDialog = req },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Red.copy(0.1f), contentColor = MadadwalaColors.Red)
                                ) { Text("Reject") }
                                
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val res = RetrofitClient.apiService.updateWithdrawalStatus(req._id, mapOf("status" to "approved"))
                                            if (res.isSuccessful) fetch()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Teal)
                                ) { Text("Approve & Pay") }
                            }
                        }
                    }
                }
            }
        }

        if (showRejectDialog != null) {
            AlertDialog(
                onDismissRequest = { showRejectDialog = null },
                title = { Text("Reject Withdrawal") },
                text = {
                    Column {
                        Text("Reason for rejection:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = rejectionReason,
                            onValueChange = { rejectionReason = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. Invalid bank details") }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                val res = RetrofitClient.apiService.updateWithdrawalStatus(
                                    showRejectDialog!!._id, 
                                    mapOf("status" to "rejected", "rejectionReason" to rejectionReason)
                                )
                                if (res.isSuccessful) {
                                    showRejectDialog = null
                                    rejectionReason = ""
                                    fetch()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Red)
                    ) { Text("Reject Request") }
                },
                dismissButton = {
                    TextButton(onClick = { showRejectDialog = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun AnalyticsTab() {
    var analytics by remember { mutableStateOf<AdminAnalyticsResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val response = RetrofitClient.apiService.getAdminAnalytics()
                if (response.isSuccessful) {
                    analytics = response.body()
                }
            } catch (e: Exception) {}
            finally { isLoading = false }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MadadwalaColors.Teal)
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Business Overview", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminStatCard("Revenue", "₹${analytics?.totalRevenue ?: 0}", Icons.Default.TrendingUp, MadadwalaColors.Green, Modifier.weight(1f))
                AdminStatCard("Total Jobs", "${analytics?.totalBookings ?: 0}", Icons.Default.WorkHistory, MadadwalaColors.Teal, Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminStatCard("Users", "${analytics?.totalUsers ?: 0}", Icons.Default.Group, Color.Blue, Modifier.weight(1f))
                AdminStatCard("Providers", "${analytics?.totalProviders ?: 0}", Icons.Default.Engineering, MadadwalaColors.Amber, Modifier.weight(1f))
            }
        }
        
        analytics?.categories?.let { categories ->
            if (categories.isNotEmpty()) {
                item {
                    Text("Top Categories", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
                items(categories) { stat ->
                    Column(modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)).padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stat.name, fontWeight = FontWeight.Bold)
                            Text("${(stat.ratio * 100).toInt()}%")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { stat.ratio },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = MadadwalaColors.Teal,
                            trackColor = MadadwalaColors.LightGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderRequestsTab() {
    var requests by remember { mutableStateOf<List<UserResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedRequest by remember { mutableStateOf<UserResponse?>(null) }
    var showRejectDialog by remember { mutableStateOf<UserResponse?>(null) }
    var rejectionReason by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val fetchRequests: suspend () -> Unit = {
        isLoading = true
        try {
            val response = RetrofitClient.apiService.getPendingProviders()
            if (response.isSuccessful) {
                requests = response.body() ?: emptyList()
            }
        } catch (e: Exception) {}
        finally { isLoading = false }
    }

    LaunchedEffect(Unit) {
        fetchRequests()
    }

    BackHandler(enabled = selectedRequest != null) {
        selectedRequest = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MadadwalaColors.Teal)
        } else if (requests.isEmpty()) {
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(64.dp), tint = MadadwalaColors.LightGray)
                Text("No pending registrations", color = MadadwalaColors.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("Pending Registrations", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                }
                items(requests) { request ->
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Color.White) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MadadwalaColors.Cream), contentAlignment = Alignment.Center) {
                                    if (request.profileImage != null) {
                                        AsyncImage(model = request.profileImage, contentDescription = null, modifier = Modifier.fillMaxSize())
                                    } else {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = MadadwalaColors.Teal)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(request.name ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text(request.phoneNumber, color = MadadwalaColors.Gray, fontSize = 14.sp)
                                }
                                IconButton(onClick = { selectedRequest = request }) {
                                    Icon(Icons.Default.Visibility, contentDescription = "View Details", tint = MadadwalaColors.Teal)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { showRejectDialog = request },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Red.copy(alpha = 0.1f), contentColor = MadadwalaColors.Red),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Reject")
                                }
                                Button(
                                    onClick = { 
                                        scope.launch {
                                            try {
                                                val res = RetrofitClient.apiService.approveProvider(ApproveProviderRequest(request.uid))
                                                if (res.isSuccessful) fetchRequests()
                                            } catch (e: Exception) {}
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Teal),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Approve", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showRejectDialog != null) {
            AlertDialog(
                onDismissRequest = { showRejectDialog = null },
                title = { Text("Reject Application") },
                text = {
                    Column {
                        Text("Reason for rejection:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = rejectionReason,
                            onValueChange = { rejectionReason = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. Blurry Aadhaar image") },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val res = RetrofitClient.apiService.rejectProvider(mapOf(
                                        "uid" to showRejectDialog!!.uid,
                                        "reason" to rejectionReason
                                    ))
                                    if (res.isSuccessful) {
                                        showRejectDialog = null
                                        rejectionReason = ""
                                        fetchRequests()
                                    }
                                } catch (e: Exception) {}
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Red),
                        enabled = rejectionReason.isNotBlank()
                    ) { Text("Confirm Reject") }
                },
                dismissButton = {
                    TextButton(onClick = { showRejectDialog = null }) { Text("Cancel") }
                }
            )
        }

        // Details Overlay
        AnimatedVisibility(
            visible = selectedRequest != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            selectedRequest?.let { request ->
                RequestDetailDialog(
                    request = request,
                    onDismiss = { selectedRequest = null },
                    onReject = { 
                        showRejectDialog = request
                        selectedRequest = null 
                    },
                    onApprove = {
                        scope.launch {
                            try {
                                val res = RetrofitClient.apiService.approveProvider(ApproveProviderRequest(request.uid))
                                if (res.isSuccessful) {
                                    fetchRequests()
                                    selectedRequest = null
                                }
                            } catch (e: Exception) {}
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailDialog(request: UserResponse, onDismiss: () -> Unit, onReject: () -> Unit, onApprove: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MadadwalaColors.Cream
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Application Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
            
            LazyColumn(
                modifier = Modifier.weight(1f).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Text("Identity Documents", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
                
                item {
                    Column {
                        Text("Aadhaar Card", style = MaterialTheme.typography.labelSmall.copy(color = MadadwalaColors.Gray))
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)).background(MadadwalaColors.LightGray.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (request.aadhaarImage != null) {
                                AsyncImage(model = request.aadhaarImage, contentDescription = "Aadhaar", modifier = Modifier.fillMaxSize())
                            } else {
                                Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(48.dp), tint = MadadwalaColors.Gray)
                            }
                        }
                    }
                }

                item {
                    Column {
                        Text("Profile / Selfie", style = MaterialTheme.typography.labelSmall.copy(color = MadadwalaColors.Gray))
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier.size(150.dp).clip(CircleShape).background(MadadwalaColors.LightGray.copy(alpha = 0.5f)).align(Alignment.CenterHorizontally),
                            contentAlignment = Alignment.Center
                        ) {
                            if (request.profileImage != null) {
                                AsyncImage(model = request.profileImage, contentDescription = "Selfie", modifier = Modifier.fillMaxSize())
                            } else {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(48.dp), tint = MadadwalaColors.Gray)
                            }
                        }
                    }
                }

                item {
                    Text("Personal Details", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
                
                item {
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            DetailRow("Full Name", request.name ?: "N/A")
                            DetailRow("Phone", request.phoneNumber)
                            DetailRow("Role", request.role)
                            DetailRow("Status", if(request.isVerified) "Verified" else "Pending")
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp,
                color = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onReject,
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Red.copy(alpha = 0.1f), contentColor = MadadwalaColors.Red),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reject", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Teal),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Approve", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MadadwalaColors.Gray, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MadadwalaColors.Ink)
    }
}

@Composable
fun LiveMonitoringTab() {
    var activeJobs by remember { mutableStateOf<List<BookingResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while(true) {
            try {
                val response = RetrofitClient.apiService.getActiveJobs()
                if (response.isSuccessful) {
                    activeJobs = response.body() ?: emptyList()
                }
            } catch (e: Exception) {}
            finally { isLoading = false }
            delay(10000) // Refresh every 10 seconds
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading && activeJobs.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MadadwalaColors.Teal)
        } else if (activeJobs.isEmpty()) {
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(64.dp), tint = MadadwalaColors.LightGray)
                Text("No active jobs currently", color = MadadwalaColors.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("Live Job Monitoring", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                }
                items(activeJobs) { job ->
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(
                                when(job.status.lowercase()) {
                                    "in_progress" -> MadadwalaColors.Green
                                    "on_the_way" -> MadadwalaColors.Amber
                                    else -> MadadwalaColors.Teal
                                }
                            ))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${job.providerName ?: "Partner"} is ${job.status.replace("_", " ")}", fontWeight = FontWeight.Bold)
                                Text("Customer: ${job.customerName ?: "User"}", color = MadadwalaColors.Gray, fontSize = 12.sp)
                                Text("Address: ${job.address}", color = MadadwalaColors.Gray, fontSize = 10.sp, maxLines = 1)
                            }
                            Surface(color = MadadwalaColors.Teal.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                                Text(job.serviceName, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = MadadwalaColors.Teal, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManagementTab() {
    var showOfferList by remember { mutableStateOf(false) }
    var showBannerList by remember { mutableStateOf(false) }
    var showCategoryList by remember { mutableStateOf(false) }
    var showCommissionDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = showOfferList || showBannerList || showCategoryList || showCommissionDialog) {
        showOfferList = false
        showBannerList = false
        showCategoryList = false
        showCommissionDialog = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("System Management", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))
        Spacer(modifier = Modifier.height(24.dp))
        
        ManagementOption("Commission Settings", Icons.Default.Percent) {
            showCommissionDialog = true
        }
        ManagementOption("Manage Categories", Icons.Default.Category) {
            showCategoryList = true
        }
        ManagementOption("Manage Offers", Icons.Default.LocalOffer) {
            showOfferList = true
        }
        ManagementOption("Manage Home Banners", Icons.Default.ViewCarousel) {
            showBannerList = true
        }
        ManagementOption("System Settings", Icons.Default.Settings) {}
        ManagementOption("Help & Support Center", Icons.Default.SupportAgent) {}
    }

    if (showCommissionDialog) {
        CommissionSettingsDialog(onDismiss = { showCommissionDialog = false })
    }
    if (showCategoryList) {
        CategoryListDialog(onDismiss = { showCategoryList = false })
    }
    if (showOfferList) {
        OfferListDialog(onDismiss = { showOfferList = false })
    }
    if (showBannerList) {
        BannerListDialog(onDismiss = { showBannerList = false })
    }
}

@Composable
fun CommissionSettingsDialog(onDismiss: () -> Unit) {
    var commission by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            val res = RetrofitClient.apiService.getAdminSettings()
            if (res.isSuccessful) {
                val settings = res.body()
                commission = settings?.get("commission_percentage")?.toString()?.split(".")?.get(0) ?: "15"
            }
        } catch (e: Exception) {} finally { isLoading = false }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Earnings Commission", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Set the percentage the company takes from each job. Partners will receive the remaining amount.", fontSize = 14.sp, color = Color.Gray)
                if (isLoading) {
                    Box(Modifier.fillMaxWidth(), Alignment.Center) { CircularProgressIndicator(color = MadadwalaColors.Teal) }
                } else {
                    OutlinedTextField(
                        value = commission,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) commission = it },
                        label = { Text("Commission Percentage (%)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text("%") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        try {
                            val value = commission.toIntOrNull() ?: 15
                            val res = RetrofitClient.apiService.updateAdminSetting(mapOf("key" to "commission_percentage", "value" to value))
                            if (res.isSuccessful) {
                                android.widget.Toast.makeText(context, "Commission updated successfully", android.widget.Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        } catch (e: Exception) {} finally { isSaving = false }
                    }
                },
                enabled = !isLoading && !isSaving && commission.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Teal)
            ) {
                if (isSaving) CircularProgressIndicator(Modifier.size(20.dp), Color.White)
                else Text("Update Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListDialog(onDismiss: () -> Unit) {
    var categories by remember { mutableStateOf<List<CategoryResponse>>(emptyList()) }
    var showCreate by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val fetch = {
        scope.launch {
            isLoading = true
            try {
                val res = RetrofitClient.apiService.getCategories()
                if (res.isSuccessful) categories = res.body() ?: emptyList()
            } catch (e: Exception) {} finally { isLoading = false }
        }
    }

    LaunchedEffect(Unit) { fetch() }

    Surface(modifier = Modifier.fillMaxSize(), color = MadadwalaColors.Cream) {
        Column {
            TopAppBar(
                title = { Text("Manage Categories") },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = { IconButton(onClick = { showCreate = true }) { Icon(Icons.Default.Add, null, tint = MadadwalaColors.Teal) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
            
            if (isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = MadadwalaColors.Teal) }
            } else {
                LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(categories) { cat ->
                        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), Color.White) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(getIconForName(cat.icon ?: "Category"), contentDescription = null, tint = MadadwalaColors.Teal)
                                Spacer(Modifier.width(12.dp))
                                Text(cat.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                IconButton(onClick = { /* Edit logic would go here, adding state for consistency */ }) { Icon(Icons.Default.Edit, null, tint = MadadwalaColors.Teal.copy(0.6f)) }
                                IconButton(onClick = {
                                    scope.launch {
                                        RetrofitClient.apiService.deleteCategory(cat._id)
                                        fetch()
                                    }
                                }) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.6f)) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CategoryManagementDialog(onDismiss = { showCreate = false; fetch() })
    }
}

@Composable
fun CategoryManagementDialog(onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("Category") }
    var showIconPicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val icons = listOf(
        "Cleaning" to Icons.Default.CleaningServices,
        "Plumbing" to Icons.Default.Handyman,
        "Electrical" to Icons.Default.ElectricBolt,
        "Painting" to Icons.Default.Brush,
        "AC" to Icons.Default.Air,
        "Home" to Icons.Default.Home,
        "Work" to Icons.Default.Work,
        "Car" to Icons.Default.DirectionsCar,
        "Health" to Icons.Default.MedicalServices,
        "Pets" to Icons.Default.Pets
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Category", fontWeight = FontWeight.Bold, color = MadadwalaColors.Teal) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Category Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                
                Surface(
                    onClick = { showIconPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(getIconForName(selectedIcon), null, tint = MadadwalaColors.Teal)
                        Spacer(Modifier.width(12.dp))
                        Text("Selected Icon: $selectedIcon", modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val res = RetrofitClient.apiService.createCategory(mapOf("name" to name, "icon" to selectedIcon))
                            if (res.isSuccessful) onDismiss()
                        } catch (e: Exception) {}
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Teal),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) } }
    )

    if (showIconPicker) {
        AlertDialog(
            onDismissRequest = { showIconPicker = false },
            title = { Text("Choose Icon") },
            text = {
                LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.height(300.dp)) {
                    items(icons) { (iconName, icon) ->
                        IconButton(onClick = { selectedIcon = iconName; showIconPicker = false }) {
                            Icon(icon, null, tint = if (selectedIcon == iconName) MadadwalaColors.Teal else Color.Gray)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showIconPicker = false }) { Text("Close") } }
        )
    }
}

fun getIconForName(name: String?): ImageVector {
    return when(name) {
        "Cleaning" -> Icons.Default.CleaningServices
        "Plumbing" -> Icons.Default.Handyman
        "Electrical" -> Icons.Default.ElectricBolt
        "Painting" -> Icons.Default.Brush
        "AC" -> Icons.Default.Air
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
fun OfferListDialog(onDismiss: () -> Unit) {
    var offers by remember { mutableStateOf<List<com.abhi.madadwala_1.data.remote.OfferResponse>>(emptyList()) }
    var showCreate by remember { mutableStateOf(false) }
    var editingOffer by remember { mutableStateOf<com.abhi.madadwala_1.data.remote.OfferResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val fetch = {
        scope.launch {
            isLoading = true
            try {
                val res = RetrofitClient.apiService.getOffers()
                if (res.isSuccessful) offers = res.body() ?: emptyList()
            } catch (e: Exception) {} finally { isLoading = false }
        }
    }

    LaunchedEffect(Unit) { fetch() }

    Surface(modifier = Modifier.fillMaxSize(), color = MadadwalaColors.Cream) {
        Column {
            TopAppBar(
                title = { Text("Manage Offers") },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = { IconButton(onClick = { showCreate = true }) { Icon(Icons.Default.Add, null, tint = MadadwalaColors.Teal) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
            
            if (isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = MadadwalaColors.Teal) }
            } else {
                LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(offers) { offer ->
                        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), Color.White) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(offer.title, fontWeight = FontWeight.Bold)
                                    Text(offer.code, color = MadadwalaColors.Teal, style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(onClick = { editingOffer = offer }) { Icon(Icons.Default.Edit, null, tint = MadadwalaColors.Teal.copy(0.6f)) }
                                IconButton(onClick = {
                                    scope.launch {
                                        RetrofitClient.apiService.deleteOffer(offer._id)
                                        fetch()
                                    }
                                }) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.6f)) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate || editingOffer != null) {
        OfferManagementDialog(
            offer = editingOffer,
            onDismiss = { 
                showCreate = false
                editingOffer = null
                fetch() 
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BannerListDialog(onDismiss: () -> Unit) {
    var banners by remember { mutableStateOf<List<com.abhi.madadwala_1.data.remote.BannerResponse>>(emptyList()) }
    var showCreate by remember { mutableStateOf(false) }
    var editingBanner by remember { mutableStateOf<com.abhi.madadwala_1.data.remote.BannerResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val fetch = {
        scope.launch {
            isLoading = true
            try {
                val res = RetrofitClient.apiService.getBanners()
                if (res.isSuccessful) banners = res.body() ?: emptyList()
            } catch (e: Exception) {} finally { isLoading = false }
        }
    }

    LaunchedEffect(Unit) { fetch() }

    Surface(modifier = Modifier.fillMaxSize(), color = MadadwalaColors.Cream) {
        Column {
            TopAppBar(
                title = { Text("Manage Banners") },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = { IconButton(onClick = { showCreate = true }) { Icon(Icons.Default.Add, null, tint = MadadwalaColors.Teal) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
            
            if (isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = MadadwalaColors.Teal) }
            } else {
                LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(banners) { banner ->
                        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), Color.White) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(banner.image, null, Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(banner.title, fontWeight = FontWeight.Bold)
                                    Text(banner.subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                IconButton(onClick = { editingBanner = banner }) { Icon(Icons.Default.Edit, null, tint = MadadwalaColors.Teal.copy(0.6f)) }
                                IconButton(onClick = {
                                    scope.launch {
                                        RetrofitClient.apiService.deleteBanner(banner._id)
                                        fetch()
                                    }
                                }) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.6f)) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate || editingBanner != null) {
        BannerManagementDialog(
            banner = editingBanner,
            onDismiss = { 
                showCreate = false
                editingBanner = null
                fetch() 
            }
        )
    }
}

@Composable
fun OfferManagementDialog(offer: com.abhi.madadwala_1.data.remote.OfferResponse? = null, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(offer?.title ?: "") }
    var desc by remember { mutableStateOf(offer?.description ?: "") }
    var code by remember { mutableStateOf(offer?.code ?: "") }
    var discount by remember { mutableStateOf(offer?.discount?.toString() ?: "") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (offer == null) "Create New Offer" else "Edit Offer", fontWeight = FontWeight.Bold, color = MadadwalaColors.Teal) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Offer Title") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Promo Code") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = discount, onValueChange = { discount = it }, label = { Text("Discount %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val request = com.abhi.madadwala_1.data.remote.OfferRequest(title, desc, code, discount.toIntOrNull() ?: 0, "2026-12-31")
                            val res = if (offer == null) {
                                RetrofitClient.apiService.createOffer(request)
                            } else {
                                RetrofitClient.apiService.updateOffer(offer._id, request)
                            }
                            if (res.isSuccessful) onDismiss()
                        } catch (e: Exception) {}
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Teal),
                shape = RoundedCornerShape(12.dp)
            ) { Text(if (offer == null) "Create Offer" else "Update Offer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) } }
    )
}

@Composable
fun BannerManagementDialog(banner: com.abhi.madadwala_1.data.remote.BannerResponse? = null, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(banner?.title ?: "") }
    var subtitle by remember { mutableStateOf(banner?.subtitle ?: "") }
    var imageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { imageUri = it }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (banner == null) "Add Home Banner" else "Edit Banner", fontWeight = FontWeight.Bold, color = MadadwalaColors.Teal) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Gray.copy(alpha = 0.1f))
                        .border(2.dp, MadadwalaColors.Green, RoundedCornerShape(20.dp))
                        .clickable { photoPicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    // Decide what to show: new selected image OR existing server image
                    val model = if (imageUri != null) imageUri else if (banner != null && banner.image.isNotEmpty()) banner.image else null
                    
                    if (model != null) {
                        AsyncImage(
                            model = model,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onError = { 
                                // This will trigger if the URL is private or invalid
                            }
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MadadwalaColors.Green, modifier = Modifier.size(48.dp))
                            Text("Upload Banner Image", style = MaterialTheme.typography.labelMedium, color = MadadwalaColors.Green)
                        }
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Banner Title") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text("Subtitle") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val tPart = title.toRequestBody("text/plain".toMediaTypeOrNull())
                            val sPart = subtitle.toRequestBody("text/plain".toMediaTypeOrNull())
                            
                            var iPart: MultipartBody.Part? = null
                            imageUri?.let { uri ->
                                val inputStream = context.contentResolver.openInputStream(uri)
                                val file = java.io.File(context.cacheDir, "temp_banner.jpg")
                                inputStream?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                                iPart = MultipartBody.Part.createFormData("image", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
                            }

                            val res = if (banner == null) {
                                iPart?.let { RetrofitClient.apiService.createBanner(tPart, sPart, it) }
                            } else {
                                RetrofitClient.apiService.updateBanner(banner._id, tPart, sPart, iPart)
                            }
                            if (res?.isSuccessful == true) onDismiss()
                        } catch (e: Exception) {}
                    }
                },
                enabled = (imageUri != null || banner != null) && title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MadadwalaColors.Teal),
                shape = RoundedCornerShape(12.dp)
            ) { Text(if (banner == null) "Add Banner" else "Update Banner") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) } }
    )
}

@Composable
fun ManagementOption(label: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = Color.White
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MadadwalaColors.Teal)
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

@Composable
fun SupportChatTab() {
    var chats by remember { mutableStateOf<List<SupportChat>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedChat by remember { mutableStateOf<SupportChat?>(null) }
    val scope = rememberCoroutineScope()

    val fetch = {
        scope.launch {
            isLoading = true
            try {
                val res = RetrofitClient.apiService.getSupportChats()
                if (res.isSuccessful) chats = res.body() ?: emptyList()
            } catch (e: Exception) {} finally { isLoading = false }
        }
    }

    LaunchedEffect(Unit) { fetch() }

    BackHandler(enabled = selectedChat != null) {
        selectedChat = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading && chats.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MadadwalaColors.Teal)
        } else if (chats.isEmpty()) {
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.SupportAgent, null, modifier = Modifier.size(64.dp), tint = MadadwalaColors.LightGray)
                Text("No support requests", color = MadadwalaColors.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("Customer Support Chats", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                }
                items(chats) { chat ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { selectedChat = chat },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MadadwalaColors.Teal.copy(0.1f)), contentAlignment = Alignment.Center) {
                                Text(chat.userName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MadadwalaColors.Teal)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(chat.userName, fontWeight = FontWeight.Bold)
                                    if (chat.unreadCount > 0) {
                                        Spacer(Modifier.width(8.dp))
                                        Surface(color = MadadwalaColors.Red, shape = CircleShape) {
                                            Text("${chat.unreadCount}", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (chat.status == "waiting") {
                                        Spacer(Modifier.width(8.dp))
                                        Surface(color = MadadwalaColors.Amber, shape = RoundedCornerShape(4.dp)) {
                                            Text("WAITING", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                                Text(chat.lastMessage, color = if (chat.status == "waiting") MadadwalaColors.Green else MadadwalaColors.Gray, fontSize = 14.sp, maxLines = 1, fontWeight = if (chat.status == "waiting") FontWeight.Bold else FontWeight.Normal)
                            }
                            val displayTime = try {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                                val date = sdf.parse(chat.lastTimestamp)
                                java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(date)
                            } catch (e: Exception) { "" }
                            
                            Text(
                                displayTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        if (selectedChat != null) {
            AdminChatDetailDialog(chat = selectedChat!!, onDismiss = { selectedChat = null; fetch() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminChatDetailDialog(chat: SupportChat, onDismiss: () -> Unit) {
    var messages by remember { mutableStateOf<List<SupportMessage>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val fetchMessages = {
        scope.launch {
            try {
                val res = RetrofitClient.apiService.getSupportMessages(chat.userUid)
                if (res.isSuccessful) messages = res.body() ?: emptyList()
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(Unit) {
        fetchMessages()
        while(true) {
            delay(3000)
            fetchMessages()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MadadwalaColors.Cream) {
        Column {
            TopAppBar(
                title = { Column {
                    Text(chat.userName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Customer Support", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }},
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { msg ->
                    val isAdmin = msg.isAdmin
                    val displayTime = try {
                        if (msg.timestamp != null) {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                            val date = sdf.parse(msg.timestamp)
                            java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(date)
                        } else ""
                    } catch (e: Exception) { "" }
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isAdmin) Alignment.End else Alignment.Start
                    ) {
                        Surface(
                            color = if (isAdmin) MadadwalaColors.Teal else Color.White,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(msg.message, modifier = Modifier.padding(12.dp), color = if (isAdmin) Color.White else Color.Black)
                        }
                        if (displayTime.isNotEmpty()) {
                            Text(displayTime, style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(4.dp))
                        }
                    }
                }
            }

            Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 8.dp, color = Color.White) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Reply to customer...") },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                    )
                    IconButton(onClick = {
                        if (messageText.isNotBlank()) {
                            scope.launch {
                                val msg = SupportMessage(senderUid = "admin", receiverUid = chat.userUid, message = messageText, isAdmin = true)
                                val res = RetrofitClient.apiService.sendSupportMessage(msg)
                                if (res.isSuccessful) {
                                    messageText = ""
                                    fetchMessages()
                                }
                            }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = MadadwalaColors.Teal)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminStatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black))
            Text(label, style = MaterialTheme.typography.labelSmall.copy(color = MadadwalaColors.Gray))
        }
    }
}
