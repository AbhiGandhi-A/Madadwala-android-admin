package com.abhi.madadwala_1.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhi.madadwala_1.data.remote.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.google.firebase.analytics.FirebaseAnalytics

sealed class ProviderDashboardState {
    object Loading : ProviderDashboardState()
    data class Success(
        val user: UserResponse,
        val bookings: List<BookingResponse>,
        val transactions: List<TransactionResponse>,
        val services: List<ServiceResponse>,
        val reviews: List<ReviewResponse> = emptyList(),
        val customRequests: List<CustomRequestResponse> = emptyList()
    ) : ProviderDashboardState()
    data class Error(val message: String) : ProviderDashboardState()
}

class ProviderViewModel : ViewModel() {
    private val apiService = RetrofitClient.apiService
    private val trackingApiService = RetrofitClient.trackingApiService
    private val auth = FirebaseAuth.getInstance()
    private var analytics: FirebaseAnalytics? = null
    val sessionStartTime = System.currentTimeMillis()
    
    fun initAnalytics(context: android.content.Context) {
        analytics = FirebaseAnalytics.getInstance(context)
    }

    // Store receipt times for local 30s expiry
    private val requestReceiptTimes = mutableMapOf<String, Long>()

    private val _dashboardState = MutableStateFlow<ProviderDashboardState>(ProviderDashboardState.Loading)
    val dashboardState: StateFlow<ProviderDashboardState> = _dashboardState

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline

    init {
        startDashboardPolling()
    }

    private fun startDashboardPolling() {
        viewModelScope.launch {
            var isFirstLoad = true
            while (true) {
                loadDashboardData(showLoading = isFirstLoad)
                isFirstLoad = false
                kotlinx.coroutines.delay(5000) // Poll every 5 seconds
            }
        }
    }

    fun loadDashboardData(showLoading: Boolean = true) {
        val uid = auth.currentUser?.uid ?: return
        if (showLoading) {
            _dashboardState.value = ProviderDashboardState.Loading
        }
        viewModelScope.launch {
            try {
                val userResponse = apiService.checkUser(auth.currentUser?.phoneNumber ?: "", uid)
                val bookingsResponse = apiService.getProviderBookings(uid)
                val transactionsResponse = apiService.getWalletTransactions(uid)
                val detailsResponse = apiService.getProviderDetails(uid)
                val customRequestsResponse = apiService.getCustomRequests()

                if (userResponse.isSuccessful && detailsResponse.isSuccessful) {
                    val rawUserUnwrapped = userResponse.body()?.user!!
                    val providerInfo = detailsResponse.body()?.provider
                    val bookings = if (bookingsResponse.isSuccessful) bookingsResponse.body() ?: emptyList() else emptyList()
                    val transactions = if (transactionsResponse.isSuccessful) transactionsResponse.body() ?: emptyList() else emptyList()
                    
                    val providerCategory = providerInfo?.category ?: rawUserUnwrapped.category
                    val pLat = providerInfo?.lat
                    val pLng = providerInfo?.lng

                    // Filter custom requests by category, status, rejection, and location (50km radius)
                    val allCustomRequests = if (customRequestsResponse.isSuccessful) customRequestsResponse.body() ?: emptyList() else emptyList()
                    val now = System.currentTimeMillis()
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply { 
                        timeZone = java.util.TimeZone.getTimeZone("UTC") 
                    }

                    // Clear map of old request times that are no longer in the active list
                    val activeIds = allCustomRequests.map { it._id }.toSet()
                    requestReceiptTimes.keys.retainAll(activeIds)

                    val customRequests = allCustomRequests.filter { req ->
                        // If provider is offline, don't show any custom requests
                        if (!_isOnline.value) return@filter false

                        val categoryMatch = req.category.lowercase() == providerCategory?.lowercase()
                        val statusMatch = (req.status.lowercase() == "pending" || req.status.lowercase() == "requested")
                        val notRejected = !req.rejectedBy.contains(uid)
                        val notAlreadyBid = req.bids.none { it.providerUid == uid }
                        
                        // Strict Real-Time Check: Only show requests created AFTER the app session started
                        var isRealTime = false
                        try {
                            val createdTime = sdf.parse(req.createdAt)?.time ?: 0L
                            // Allow a 10-second buffer for clock sync issues, but otherwise strictly new
                            isRealTime = createdTime >= (sessionStartTime - 10000L)
                        } catch (e: Exception) {}

                        // Distance check: wide enough for Ankleshwar area (50km)
                        var locationMatch = true
                        if (pLat != null && pLng != null && req.lat != null && req.lng != null) {
                            val results = FloatArray(1)
                            android.location.Location.distanceBetween(pLat, pLng, req.lat, req.lng, results)
                            locationMatch = results[0] <= 50000 // 50km
                        }
                        
                        if (categoryMatch && statusMatch && notRejected && notAlreadyBid && locationMatch && isRealTime) {
                            if (!requestReceiptTimes.containsKey(req._id)) {
                                requestReceiptTimes[req._id] = now
                            }
                            true
                        } else false
                    }.map { req ->
                        req.copy(localReceivedTime = requestReceiptTimes[req._id] ?: now)
                    }
                    
                    // Merge provider info into user object
                    val mergedUser = rawUserUnwrapped.copy(
                        profileImage = if (providerInfo?.profileImage?.isNotEmpty() == true) providerInfo.profileImage else rawUserUnwrapped.profileImage,
                        rating = providerInfo?.rating,
                        reviewCount = providerInfo?.reviewCount,
                        totalJobs = bookings.count { it.status == "done" },
                        totalEarnings = bookings.filter { it.paymentStatus?.lowercase() == "paid" }.sumOf { it.totalAmount },
                        aadhaarNumber = providerInfo?.aadhaarNumber ?: rawUserUnwrapped.aadhaarNumber,
                        verificationDate = providerInfo?.verificationDate ?: rawUserUnwrapped.verificationDate,
                        profession = providerInfo?.profession ?: rawUserUnwrapped.profession
                    )

                    _isOnline.value = providerInfo?.isAvailable ?: true

                    _dashboardState.value = ProviderDashboardState.Success(
                        user = mergedUser,
                        bookings = bookings,
                        transactions = transactions,
                        services = detailsResponse.body()?.services ?: emptyList(),
                        reviews = detailsResponse.body()?.reviews ?: emptyList(),
                        customRequests = customRequests
                    )
                } else if (showLoading) {
                    val errorMsg = listOf(userResponse, detailsResponse)
                        .filter { !it.isSuccessful }
                        .joinToString { "${it.code()}: ${it.message()}" }
                    _dashboardState.value = ProviderDashboardState.Error("Essential data failed to load: $errorMsg")
                }
            } catch (e: Exception) {
                if (showLoading) {
                    _dashboardState.value = ProviderDashboardState.Error("Network error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun toggleOnlineStatus() {
        val uid = auth.currentUser?.uid ?: return
        val newStatus = !_isOnline.value
        setOnlineStatus(newStatus)
    }

    fun setOnlineStatus(online: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val response = apiService.updateProviderAvailability(uid, mapOf("isAvailable" to online))
                if (response.isSuccessful) {
                    _isOnline.value = online
                    analytics?.setUserProperty("online_status", online.toString())
                }
            } catch (e: Exception) {}
        }
    }

    fun updateLocation(lat: Double, lng: Double) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                trackingApiService.updateProviderLocation(uid, mapOf("lat" to lat, "lng" to lng))
            } catch (e: Exception) {}
        }
    }

    fun updateServicePrice(serviceId: String, newPrice: Double) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val response = apiService.updateServiceCharge(uid, serviceId, mapOf("price" to newPrice))
                if (response.isSuccessful) {
                    loadDashboardData() // Reload to get updated prices
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun addService(name: String, price: Double) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val response = apiService.addService(uid, mapOf("name" to name, "price" to price))
                if (response.isSuccessful) {
                    loadDashboardData()
                }
            } catch (e: Exception) {}
        }
    }

    fun updateCustomRequestStatus(requestId: String, status: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val response = apiService.updateCustomRequestStatus(
                    requestId, 
                    mapOf("status" to status, "providerUid" to uid)
                )
                if (response.isSuccessful) {
                    loadDashboardData()
                }
            } catch (e: Exception) {}
        }
    }

    fun submitBid(requestId: String, price: Double) {
        val user = (dashboardState.value as? ProviderDashboardState.Success)?.user ?: return
        viewModelScope.launch {
            try {
                val commissionFromPartner = price * 0.15
                val platformFeeFromCustomer = price * 0.05
                val bid = BidRequest(
                    providerUid = user.uid,
                    providerName = user.name ?: "Partner",
                    price = price, // Job Value (e.g. 1000)
                    commission = commissionFromPartner, // Partner deduction (150)
                    totalPrice = price + platformFeeFromCustomer // Customer pay (1050)
                )
                val response = apiService.submitBid(requestId, bid)
                if (response.isSuccessful) {
                    loadDashboardData()
                }
            } catch (e: Exception) {}
        }
    }

    fun updateBookingStatus(bookingId: String, status: String, scheduledTime: String? = null, comment: String? = null) {
        val user = (dashboardState.value as? ProviderDashboardState.Success)?.user
        viewModelScope.launch {
            try {
                val body = mutableMapOf<String, String>("status" to status)
                scheduledTime?.let { body["scheduledTime"] = it }
                comment?.let { body["partnerComment"] = it }
                val pName = user?.name
                body["providerName"] = if (pName.isNullOrEmpty() || pName == "undefined") "Partner" else pName
                
                val response = apiService.updateBookingStatus(bookingId, body)
                if (response.isSuccessful) {
                    loadDashboardData()
                }
            } catch (e: Exception) {}
        }
    }

    fun directAccept(requestId: String) {
        val user = (dashboardState.value as? ProviderDashboardState.Success)?.user ?: return
        viewModelScope.launch {
            try {
                val response = apiService.directAcceptRequest(
                    requestId,
                    mapOf(
                        "providerUid" to user.uid,
                        "providerName" to (user.name ?: "Partner")
                    )
                )
                if (response.isSuccessful) {
                    loadDashboardData()
                }
            } catch (e: Exception) {}
        }
    }

    fun cancelBooking(bookingId: String, reason: String) {
        viewModelScope.launch {
            try {
                val response = apiService.cancelBooking(
                    bookingId,
                    mapOf(
                        "reason" to reason,
                        "cancelledBy" to "provider"
                    )
                )
                if (response.isSuccessful) {
                    loadDashboardData()
                }
            } catch (e: Exception) {}
        }
    }

    fun uploadProfileImage(uri: android.net.Uri, context: android.content.Context) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: return@launch
                
                val mediaType = "image/jpeg".toMediaTypeOrNull()
                val requestFile = bytes.toRequestBody(mediaType)
                val body = MultipartBody.Part.createFormData("profileImage", "profile.jpg", requestFile)
                
                val response = apiService.updateProfileImage(uid, body)
                if (response.isSuccessful) {
                    loadDashboardData()
                }
            } catch (e: Exception) {}
        }
    }
}
