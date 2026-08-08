package com.abhi.madadwala_1.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhi.madadwala_1.data.remote.BookingRequest
import com.abhi.madadwala_1.data.remote.BookingResponse
import com.abhi.madadwala_1.data.remote.RetrofitClient
import com.abhi.madadwala_1.ui.components.FormTextField
import com.abhi.madadwala_1.ui.components.PrimaryButton
import com.abhi.madadwala_1.ui.components.StepProgressBar
import com.abhi.madadwala_1.ui.theme.MadadwalaColors
import com.abhi.madadwala_1.ui.viewmodel.AuthViewModel
import com.abhi.madadwala_1.ui.viewmodel.AuthState
import kotlinx.coroutines.launch

import android.widget.Toast
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalContext
import com.abhi.madadwala_1.ui.viewmodel.LocationViewModel
import com.abhi.madadwala_1.utils.AnalyticsHelper
import com.abhi.madadwala_1.utils.NotificationHelper
import java.text.SimpleDateFormat
import java.util.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingFlowScreen(
    providerUid: String,
    serviceName: String,
    price: Double,
    onBack: () -> Unit,
    onBookingConfirmed: (String) -> Unit
) {
    val context = LocalContext.current
    val analyticsHelper = remember { AnalyticsHelper(context) }
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()
    
    val locationViewModel: LocationViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )
    val userLat by locationViewModel.latitude.collectAsState()
    val userLng by locationViewModel.longitude.collectAsState()
    val detectedAddress by locationViewModel.address.collectAsState()
    var isUsingCurrentLocation by remember { mutableStateOf(false) }

    val customerUid = (authState as? AuthState.Authenticated)?.firebaseUser?.uid ?: ""
    val customerName = (authState as? AuthState.Authenticated)?.user?.name ?: "Customer"

    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 4
    val scope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }

    // State for booking
    var issueDescription by remember { mutableStateOf("") }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    
    // Detailed Address State
    var houseNo by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var selectedState by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    
    val fullAddress = remember(houseNo, area, city, selectedState, pincode, detectedAddress, isUsingCurrentLocation) {
        if (isUsingCurrentLocation) detectedAddress
        else listOf(houseNo, area, city, selectedState, pincode).filter { it.isNotEmpty() }.joinToString(", ")
    }
    
    var selectedTime by remember { mutableStateOf("ASAP") }
    
    // Provider's existing bookings to disable slots
    var providerBookings by remember { mutableStateOf<List<BookingResponse>>(emptyList()) }
    
    LaunchedEffect(providerUid) {
        try {
            val response = RetrofitClient.apiService.getProviderBookings(providerUid)
            if (response.isSuccessful) {
                providerBookings = response.body() ?: emptyList()
            }
        } catch (e: Exception) {
            // Ignore fetch error for slots
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Book $serviceName", color = MadadwalaColors.Ink, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { if (currentStep > 1) currentStep-- else onBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MadadwalaColors.Ink)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                StepProgressBar(currentStep = currentStep, totalSteps = totalSteps)
            }
        },
        containerColor = Color.White
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                },
                label = "StepTransition"
            ) { step ->
                when (step) {
                    1 -> Step1(
                        value = issueDescription, 
                        onValueChange = { issueDescription = it }, 
                        selectedImages = selectedImages,
                        onImagesSelected = { selectedImages = it },
                        onNext = { currentStep = 2 }
                    )
                    2 -> Step2(
                        houseNo = houseNo, onHouseNoChange = { houseNo = it; isUsingCurrentLocation = false },
                        area = area, onAreaChange = { area = it; isUsingCurrentLocation = false },
                        city = city, onCityChange = { city = it; isUsingCurrentLocation = false },
                        state = selectedState, onStateChange = { selectedState = it; isUsingCurrentLocation = false },
                        pincode = pincode, onPincodeChange = { pincode = it; isUsingCurrentLocation = false },
                        detectedAddress = detectedAddress,
                        isUsingCurrentLocation = isUsingCurrentLocation,
                        onUseCurrentLocation = { 
                            isUsingCurrentLocation = true
                            locationViewModel.fetchLocation(context)
                        },
                        onNext = { currentStep = 3 }
                    )
                    3 -> Step3(
                        selected = selectedTime, 
                        onValueChange = { selectedTime = it }, 
                        providerBookings = providerBookings,
                        onNext = { currentStep = 4 }
                    )
                    4 -> Step4(
                        service = serviceName, 
                        price = price, 
                        address = fullAddress, 
                        time = selectedTime, 
                        issue = issueDescription,
                        isLoading = isSubmitting, 
                        onEditStep = { currentStep = it },
                        onConfirm = {
                            if (customerUid.isEmpty()) {
                                Toast.makeText(context, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
                                return@Step4
                            }
                            scope.launch {
                                isSubmitting = true
                                try {
                                    val customerUidBody = customerUid.toRequestBody("text/plain".toMediaTypeOrNull())
                                    val customerNameBody = customerName.toRequestBody("text/plain".toMediaTypeOrNull())
                                    val providerUidBody = providerUid.toRequestBody("text/plain".toMediaTypeOrNull())
                                    val serviceNameBody = serviceName.toRequestBody("text/plain".toMediaTypeOrNull())
                                    val addressBody = fullAddress.toRequestBody("text/plain".toMediaTypeOrNull())
                                    val scheduledTimeBody = selectedTime.toRequestBody("text/plain".toMediaTypeOrNull())
                                    val totalAmountBody = price.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                                    val latBody = userLat.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                                    val lngBody = userLng.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                                    
                                    val imageParts = selectedImages.mapNotNull { uri ->
                                        val file = uriToFile(context, uri)
                                        if (file != null) {
                                            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                                            MultipartBody.Part.createFormData("issueImages", file.name, requestFile)
                                        } else null
                                    }

                                    val response = RetrofitClient.apiService.createBooking(
                                        customerUidBody, customerNameBody, providerUidBody, null,
                                        serviceNameBody, addressBody, scheduledTimeBody, totalAmountBody,
                                        latBody, lngBody, imageParts
                                    )
                                    if (response.isSuccessful) {
                                        val b = response.body()
                                        val bookingId = b?._id
                                        
                                        bookingId?.let { 
                                            analyticsHelper.logBookingEvent(it, serviceName, price)
                                            onBookingConfirmed(it) 
                                        }
                                    } else {
                                        Toast.makeText(context, "Booking failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun uriToFile(context: android.content.Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.cacheDir, "issue_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file
    } catch (e: Exception) {
        null
    }
}

@Composable
fun Step1(
    value: String, 
    onValueChange: (String) -> Unit, 
    selectedImages: List<Uri>,
    onImagesSelected: (List<Uri>) -> Unit,
    onNext: () -> Unit
) {
    val scrollState = rememberScrollState()
    val maxChars = 300
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val newList = (selectedImages + uris).take(5)
        onImagesSelected(newList)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Tell us what’s wrong",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MadadwalaColors.TealDarker
        )
        Text(
            "Provide a few details about the issue so we can connect you with the right expert.",
            style = MaterialTheme.typography.bodyMedium,
            color = MadadwalaColors.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Describe Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MadadwalaColors.LightGray.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MadadwalaColors.Teal.copy(alpha = 0.1f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = MadadwalaColors.Teal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row {
                            Text(
                                "Describe your issue",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MadadwalaColors.TealDarker
                            )
                            Text(
                                " *",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                        }
                        Text(
                            "What seems to be the problem?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MadadwalaColors.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = value,
                    onValueChange = { if (it.length <= maxChars) onValueChange(it) },
                    placeholder = {
                        Text(
                            "e.g., Switchboard not working, frequent power trip, light not working, etc.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MadadwalaColors.Gray.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MadadwalaColors.Teal.copy(alpha = 0.3f),
                        unfocusedBorderColor = MadadwalaColors.LightGray,
                        cursorColor = MadadwalaColors.Teal
                    )
                )

                Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(
                        text = "${value.length}/$maxChars",
                        style = MaterialTheme.typography.labelSmall,
                        color = MadadwalaColors.Gray,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Add photos (optional)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MadadwalaColors.TealDarker
        )
        Text(
            "Add photos to help the expert understand better.",
            style = MaterialTheme.typography.bodySmall,
            color = MadadwalaColors.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Upload Photos Box
        Surface(
            onClick = { if (selectedImages.size < 5) launcher.launch("image/*") },
            shape = RoundedCornerShape(16.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, MadadwalaColors.Teal.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MadadwalaColors.Teal.copy(alpha = 0.1f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = MadadwalaColors.Teal,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Upload photos",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MadadwalaColors.Teal,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        )
                        Text(
                            "Max 5 photos, up to 5 MB each",
                            style = MaterialTheme.typography.bodySmall,
                            color = MadadwalaColors.Gray
                        )
                    }
                }
                
                if (selectedImages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.heightIn(max = 200.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedImages) { uri ->
                            Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp))) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { onImagesSelected(selectedImages.filter { it != uri }) },
                                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(0.5f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            text = "Next",
            onClick = onNext,
            enabled = value.isNotBlank(),
            trailingIcon = {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun Step2(
    houseNo: String, onHouseNoChange: (String) -> Unit,
    area: String, onAreaChange: (String) -> Unit,
    city: String, onCityChange: (String) -> Unit,
    state: String, onStateChange: (String) -> Unit,
    pincode: String, onPincodeChange: (String) -> Unit,
    detectedAddress: String,
    isUsingCurrentLocation: Boolean,
    onUseCurrentLocation: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Where do you need the service?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MadadwalaColors.TealDarker
        )
        Text(
            "Please enter your service address",
            style = MaterialTheme.typography.bodyMedium,
            color = MadadwalaColors.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Address Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MadadwalaColors.LightGray.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MadadwalaColors.Teal,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Enter full address",
                        style = MaterialTheme.typography.titleMedium,
                        color = MadadwalaColors.Teal,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                FormTextField(
                    value = houseNo,
                    onValueChange = onHouseNoChange,
                    label = "House / Flat No., Building, Street",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormTextField(
                        value = area,
                        onValueChange = onAreaChange,
                        label = "Area / Locality",
                        modifier = Modifier.weight(1f)
                    )
                    FormTextField(
                        value = city,
                        onValueChange = onCityChange,
                        label = "City",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormTextField(
                        value = state,
                        onValueChange = onStateChange,
                        label = "State",
                        modifier = Modifier.weight(1f),
                        trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) }
                    )
                    FormTextField(
                        value = pincode,
                        onValueChange = onPincodeChange,
                        label = "Pincode",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isUsingCurrentLocation) {
                    Text(
                        text = "Current Location: $detectedAddress",
                        style = MaterialTheme.typography.bodySmall,
                        color = MadadwalaColors.Teal,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Current Location Button
                Surface(
                    onClick = onUseCurrentLocation,
                    shape = RoundedCornerShape(16.dp),
                    color = if (isUsingCurrentLocation) MadadwalaColors.Teal.copy(alpha = 0.15f) else MadadwalaColors.Teal.copy(alpha = 0.05f),
                    modifier = Modifier.fillMaxWidth(),
                    border = if (isUsingCurrentLocation) BorderStroke(1.dp, MadadwalaColors.Teal) else null
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = MadadwalaColors.Teal
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Use my current location",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MadadwalaColors.TealDarker
                            )
                            Text(
                                "Detect address automatically",
                                style = MaterialTheme.typography.bodySmall,
                                color = MadadwalaColors.Gray
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MadadwalaColors.Teal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tip Section
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MadadwalaColors.Cream.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null,
                            tint = MadadwalaColors.Teal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Tip",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MadadwalaColors.TealDarker
                    )
                    Text(
                        "Providing accurate address helps us find the right professional near you.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MadadwalaColors.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            text = "Next",
            onClick = onNext,
            enabled = isUsingCurrentLocation || (houseNo.isNotEmpty() && city.isNotEmpty() && pincode.isNotEmpty()),
            trailingIcon = {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun Step3(
    selected: String, 
    onValueChange: (String) -> Unit, 
    providerBookings: List<BookingResponse>,
    onNext: () -> Unit
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

    var selectedDate by remember { 
        mutableStateOf(
            try {
                if (selected.contains("|")) {
                    val datePart = selected.split("|")[0].trim()
                    dateFormat.parse(datePart) ?: todayDate
                } else todayDate
            } catch (e: Exception) { todayDate }
        )
    }

    val timeSlots = listOf(
        "09:00 AM", "10:00 AM", "11:00 AM",
        "12:00 PM", "01:00 PM", "02:00 PM",
        "03:00 PM", "04:00 PM", "05:00 PM",
        "06:00 PM", "07:00 PM", "08:00 PM"
    )

    // Extract booked slots for the selected date from real data
    val bookedSlotsForSelectedDate = providerBookings.filter { booking ->
        booking.scheduledTime.startsWith(dateFormat.format(selectedDate))
    }.map { it.scheduledTime.split("|").last().trim() }

    // Check if selectedDate is today to disable past times if needed (Optional but good)
    val isSelectedToday = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).format(selectedDate) == 
                         SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).format(todayDate)

    val disabledSlots = bookedSlotsForSelectedDate

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "When should the provider arrive?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MadadwalaColors.TealDarker
        )
        Text(
            "Select a date and choose a time that works for you",
            style = MaterialTheme.typography.bodyMedium,
            color = MadadwalaColors.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MadadwalaColors.LightGray.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(dates) { date ->
                        val isSelected = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).format(date) == 
                                       SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).format(selectedDate)
                        val isToday = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).format(date) == 
                                    SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).format(todayDate)
                        
                        val dayName = dayNameFormat.format(date)
                        val dayNum = dayNumFormat.format(date)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isToday) "Today" else dayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MadadwalaColors.Teal else MadadwalaColors.Gray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            
                            Surface(
                                onClick = { selectedDate = date },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MadadwalaColors.Teal else Color.White,
                                border = if (isSelected) null else BorderStroke(1.dp, MadadwalaColors.LightGray.copy(alpha = 0.5f)),
                                modifier = Modifier.size(width = 60.dp, height = 72.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = dayNum,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MadadwalaColors.TealDarker
                                    )
                                    if (isSelected) {
                                        Text(
                                            text = dayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.AccessTime,
                contentDescription = null,
                tint = MadadwalaColors.Teal,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Available time slots for ${dateFormat.format(selectedDate)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MadadwalaColors.TealDarker
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MadadwalaColors.LightGray.copy(alpha = 0.5f))

        val columns = 3
        timeSlots.chunked(columns).forEach { rowSlots ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowSlots.forEach { slot ->
                    val fullSlotString = "${dateFormat.format(selectedDate)} | $slot"
                    val isSelected = selected == fullSlotString
                    val isDisabled = disabledSlots.contains(slot)
                    
                    Surface(
                        onClick = { if (!isDisabled) onValueChange(fullSlotString) },
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isSelected -> MadadwalaColors.Teal
                            isDisabled -> MadadwalaColors.LightGray.copy(alpha = 0.3f)
                            else -> Color.White
                        },
                        border = BorderStroke(
                            1.dp, 
                            if (isSelected) MadadwalaColors.Teal 
                            else if (isDisabled) Color.Transparent 
                            else MadadwalaColors.Teal.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.weight(1f).height(44.dp),
                        enabled = !isDisabled
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = slot,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = when {
                                    isSelected -> Color.White
                                    isDisabled -> MadadwalaColors.Gray.copy(alpha = 0.5f)
                                    else -> MadadwalaColors.Teal
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MadadwalaColors.Teal.copy(alpha = 0.05f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MadadwalaColors.Teal,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "You can reschedule later",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MadadwalaColors.TealDarker
                    )
                    Text(
                        "We'll notify the provider about your preferred time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MadadwalaColors.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(32.dp))

        PrimaryButton(
            text = "Next",
            onClick = onNext,
            enabled = selected.contains("|"),
            trailingIcon = {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun Step4(
    service: String,
    price: Double,
    address: String,
    time: String,
    issue: String,
    isLoading: Boolean,
    onEditStep: (Int) -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Review & Confirm",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MadadwalaColors.TealDarker
        )
        Text(
            "Please review your booking details before confirming",
            style = MaterialTheme.typography.bodyMedium,
            color = MadadwalaColors.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MadadwalaColors.LightGray.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SummaryRow(
                    icon = Icons.Default.Bolt,
                    label = "Service",
                    value = "$service (₹$price)",
                    onEdit = { onEditStep(1) }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MadadwalaColors.LightGray.copy(alpha = 0.3f))
                
                SummaryRow(
                    icon = Icons.Default.LocationOn,
                    label = "Location",
                    value = address,
                    onEdit = { onEditStep(2) }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MadadwalaColors.LightGray.copy(alpha = 0.3f))

                val timeParts = time.split("|")
                val displayDate = timeParts.getOrNull(0)?.trim() ?: "ASAP"
                val displayTime = timeParts.getOrNull(1)?.trim() ?: ""

                SummaryRow(
                    icon = Icons.Default.CalendarToday,
                    label = "Scheduled Date",
                    value = displayDate,
                    onEdit = { onEditStep(3) }
                )
                
                if (displayTime.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MadadwalaColors.LightGray.copy(alpha = 0.3f))
                    SummaryRow(
                        icon = Icons.Default.AccessTime,
                        label = "Scheduled Time",
                        value = displayTime,
                        onEdit = { onEditStep(3) }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MadadwalaColors.LightGray.copy(alpha = 0.3f))

                SummaryRow(
                    icon = Icons.Default.ChatBubbleOutline,
                    label = "Issue (Optional)",
                    value = if (issue.isEmpty()) "Not specified" else issue,
                    onEdit = { onEditStep(1) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Trust Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MadadwalaColors.Teal.copy(alpha = 0.05f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = MadadwalaColors.Teal,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "Your booking is safe with us",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MadadwalaColors.TealDarker
                    )
                    Text(
                        "We'll share your details only with the service provider after you confirm.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MadadwalaColors.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        PrimaryButton(
            text = "Confirm Booking",
            onClick = onConfirm,
            isLoading = isLoading,
            leadingIcon = {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MadadwalaColors.Gray,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "You can reschedule or cancel anytime before the provider arrives.",
                style = MaterialTheme.typography.labelSmall,
                color = MadadwalaColors.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SummaryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MadadwalaColors.Teal.copy(alpha = 0.05f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MadadwalaColors.Teal,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MadadwalaColors.TealDarker
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MadadwalaColors.Gray
            )
        }

        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier.height(36.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MadadwalaColors.LightGray.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MadadwalaColors.Teal)
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Edit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}
