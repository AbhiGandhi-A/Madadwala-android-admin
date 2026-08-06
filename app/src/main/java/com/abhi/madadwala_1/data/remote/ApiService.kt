package com.abhi.madadwala_1.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @GET("api/users/check")
    suspend fun checkUser(
        @Query("phoneNumber") phoneNumber: String,
        @Query("uid") uid: String? = null
    ): Response<UserCheckResponse>

    @Multipart
    @POST("api/users/register")
    suspend fun registerUser(
        @Part("uid") uid: RequestBody,
        @Part("phoneNumber") phoneNumber: RequestBody,
        @Part("role") role: RequestBody,
        @Part("name") name: RequestBody,
        @Part("email") email: RequestBody?,
        @Part("category") category: RequestBody? = null,
        @Part("profession") profession: RequestBody? = null,
        @Part("aadhaarNumber") aadhaarNumber: RequestBody? = null,
        @Part profileImage: MultipartBody.Part?,
        @Part aadhaarImage: MultipartBody.Part?
    ): Response<UserResponse>

    @GET("api/categories")
    suspend fun getCategories(): Response<List<CategoryResponse>>

    @GET("api/providers")
    suspend fun getProviders(@Query("category") category: String? = null): Response<List<ProviderResponse>>

    @GET("api/providers/{uid}")
    suspend fun getProviderDetails(@Path("uid") uid: String): Response<ProviderDetailResponse>

    @POST("api/bookings")
    suspend fun createBooking(@Body booking: BookingRequest): Response<BookingResponse>

    @GET("api/bookings/customer/{uid}")
    suspend fun getCustomerBookings(@Path("uid") uid: String): Response<List<BookingResponse>>

    @GET("api/bookings/provider/{uid}")
    suspend fun getProviderBookings(@Path("uid") uid: String): Response<List<BookingResponse>>

    @GET("api/bookings/{id}")
    suspend fun getBookingDetails(@Path("id") id: String): Response<BookingResponse>

    @PATCH("api/bookings/{id}")
    suspend fun updateBookingStatus(
        @Path("id") id: String,
        @Body status: Map<String, String>
    ): Response<MessageResponse>

    @POST("api/bookings/{id}/verify-otp")
    suspend fun verifyBookingOtp(
        @Path("id") id: String,
        @Body request: Map<String, String>
    ): Response<MessageResponse>

    @POST("api/bookings/{id}/complete-payment")
    suspend fun completePayment(
        @Path("id") id: String
    ): Response<MessageResponse>

    @POST("api/call/start")
    suspend fun startCall(
        @Body request: CallRequest
    ): Response<CallResponse>

    @POST("api/payments/create-order")
    suspend fun createRazorpayOrder(
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Response<RazorpayOrderResponse>

    @PATCH("api/bookings/{id}/location")
    suspend fun updateBookingLocation(
        @Path("id") id: String,
        @Body location: Map<String, @JvmSuppressWildcards Any>
    ): Response<MessageResponse>

    @GET("api/wallet/transactions/{uid}")
    suspend fun getWalletTransactions(@Path("uid") uid: String): Response<List<TransactionResponse>>

    @PATCH("api/providers/{uid}/availability")
    suspend fun updateProviderAvailability(
        @Path("uid") uid: String,
        @Body request: Map<String, Boolean>
    ): Response<MessageResponse>

    @PATCH("api/providers/{uid}/location")
    suspend fun updateProviderLocation(
        @Path("uid") uid: String,
        @Body location: Map<String, Double>
    ): Response<MessageResponse>

    @PUT("api/providers/{uid}/services/{serviceId}")
    suspend fun updateServiceCharge(
        @Path("uid") uid: String,
        @Path("serviceId") serviceId: String,
        @Body request: Map<String, Double>
    ): Response<MessageResponse>

    @POST("api/providers/{uid}/services")
    suspend fun addService(
        @Path("uid") uid: String,
        @Body service: Map<String, @JvmSuppressWildcards Any>
    ): Response<ServiceResponse>

    @POST("api/users/{uid}/addresses")
    suspend fun addAddress(@Path("uid") uid: String, @Body address: Map<String, @JvmSuppressWildcards Any>): Response<MessageResponse>

    @GET("api/users/{uid}/addresses")
    suspend fun getAddresses(@Path("uid") uid: String): Response<List<AddressResponse>>

    @DELETE("api/users/{uid}/addresses/{addressId}")
    suspend fun removeAddress(@Path("uid") uid: String, @Path("addressId") addressId: String): Response<MessageResponse>

    @POST("api/users/bank-details/{uid}")
    suspend fun updateBankDetails(@Path("uid") uid: String, @Body details: Map<String, String>): Response<MessageResponse>

    @GET("api/provider/performance/{uid}")
    suspend fun getPerformanceReport(@Path("uid") uid: String): Response<PerformanceReportResponse>

    // Reviews
    @POST("api/reviews")
    suspend fun submitReview(@Body review: ReviewRequest): Response<ReviewResponse>

    // Profile & Favorites
    @PATCH("api/users/{uid}")
    suspend fun updateProfile(@Path("uid") uid: String, @Body profile: Map<String, String?>): Response<MessageResponse>

    @Multipart
    @PATCH("api/users/{uid}/profile-image")
    suspend fun updateProfileImage(
        @Path("uid") uid: String,
        @Part profileImage: MultipartBody.Part
    ): Response<MessageResponse>

    @GET("api/users/{uid}/favorites")
    suspend fun getFavorites(@Path("uid") uid: String): Response<List<ProviderResponse>>

    @POST("api/users/{uid}/favorites")
    suspend fun addToFavorites(@Path("uid") uid: String, @Body request: Map<String, String>): Response<MessageResponse>

    @DELETE("api/users/{uid}/favorites/{providerUid}")
    suspend fun removeFromFavorites(@Path("uid") uid: String, @Path("providerUid") providerUid: String): Response<MessageResponse>

    // Offers
    @GET("api/offers")
    suspend fun getOffers(): Response<List<OfferResponse>>

    @POST("api/admin/offers")
    suspend fun createOffer(@Body offer: OfferRequest): Response<OfferResponse>

    @PUT("api/admin/offers/{id}")
    suspend fun updateOffer(@Path("id") id: String, @Body offer: OfferRequest): Response<MessageResponse>

    @DELETE("api/admin/offers/{id}")
    suspend fun deleteOffer(@Path("id") id: String): Response<MessageResponse>

    @GET("api/banners")
    suspend fun getBanners(): Response<List<BannerResponse>>

    @Multipart
    @POST("api/admin/banners")
    suspend fun createBanner(
        @Part("title") title: RequestBody,
        @Part("subtitle") subtitle: RequestBody,
        @Part image: MultipartBody.Part
    ): Response<BannerResponse>

    @Multipart
    @PUT("api/admin/banners/{id}")
    suspend fun updateBanner(
        @Path("id") id: String,
        @Part("title") title: RequestBody,
        @Part("subtitle") subtitle: RequestBody,
        @Part image: MultipartBody.Part?
    ): Response<MessageResponse>

    @DELETE("api/admin/banners/{id}")
    suspend fun deleteBanner(@Path("id") id: String): Response<MessageResponse>

    // Admin
    @GET("api/admin/pending-providers")
    suspend fun getPendingProviders(): Response<List<UserResponse>>

    @POST("api/admin/approve-provider")
    suspend fun approveProvider(@Body request: ApproveProviderRequest): Response<MessageResponse>

    @POST("api/admin/categories")
    suspend fun createCategory(@Body request: Map<String, String>): Response<CategoryResponse>

    @DELETE("api/admin/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: String): Response<MessageResponse>

    @GET("api/admin/active-jobs")
    suspend fun getActiveJobs(): Response<List<BookingResponse>>

    @GET("api/admin/settings")
    suspend fun getAdminSettings(): Response<Map<String, Any>>

    @POST("api/admin/settings")
    suspend fun updateAdminSetting(@Body request: Map<String, @JvmSuppressWildcards Any>): Response<MessageResponse>

    @GET("api/admin/withdrawals/pending")
    suspend fun getPendingWithdrawals(): Response<List<WithdrawalResponse>>

    @PATCH("api/admin/withdrawals/{id}")
    suspend fun updateWithdrawalStatus(@Path("id") id: String, @Body status: Map<String, String>): Response<MessageResponse>

    @POST("api/withdrawals/request")
    suspend fun requestWithdrawal(@Body request: Map<String, @JvmSuppressWildcards Any>): Response<MessageResponse>

    @GET("api/admin/analytics")
    suspend fun getAdminAnalytics(): Response<AdminAnalyticsResponse>

    // Support Chat
    @GET("api/support/messages/{userId}")
    suspend fun getSupportMessages(@Path("userId") userId: String): Response<List<SupportMessage>>

    @POST("api/support/messages")
    suspend fun sendSupportMessage(@Body message: SupportMessage): Response<SupportMessage>

    @GET("api/admin/support/chats")
    suspend fun getSupportChats(): Response<List<SupportChat>>

    @PATCH("api/support/status/{userId}")
    suspend fun updateSupportStatus(
        @Path("userId") userId: String,
        @Body status: Map<String, String>
    ): Response<MessageResponse>

    // Custom Requests
    @POST("api/users/fcm-token")
    suspend fun updateFcmToken(
        @Body request: Map<String, String>
    ): Response<MessageResponse>

    @POST("api/custom-requests")
    suspend fun createCustomRequest(@Body request: CustomRequest): Response<MessageResponse>

    @GET("api/custom-requests")
    suspend fun getCustomRequests(): Response<List<CustomRequestResponse>>

    @GET("api/custom-requests/customer/{uid}")
    suspend fun getCustomerCustomRequests(@Path("uid") uid: String): Response<List<CustomRequestResponse>>

    @PATCH("api/custom-requests/{id}/status")
    suspend fun updateCustomRequestStatus(
        @Path("id") id: String,
        @Body request: Map<String, String>
    ): Response<MessageResponse>

    @POST("api/custom-requests/{id}/bid")
    suspend fun submitBid(
        @Path("id") id: String,
        @Body bid: BidRequest
    ): Response<MessageResponse>

    @POST("api/custom-requests/{id}/accept-bid")
    suspend fun acceptBid(
        @Path("id") id: String,
        @Body request: Map<String, String>
    ): Response<BookingResponse>

    @POST("api/custom-requests/{id}/direct-accept")
    suspend fun directAcceptRequest(
        @Path("id") id: String,
        @Body request: Map<String, String>
    ): Response<BookingResponse>

    @Multipart
    @POST("api/reports")
    suspend fun submitReport(
        @Part("reporterUid") reporterUid: RequestBody,
        @Part("reportedUid") reportedUid: RequestBody,
        @Part("reason") reason: RequestBody,
        @Part("description") description: RequestBody,
        @Part evidence: List<MultipartBody.Part>?
    ): Response<MessageResponse>
}

data class CallRequest(
    val bookingId: String,
    val customerId: String,
    val partnerId: String,
    val callerId: String? = null
)

data class CallResponse(
    val success: Boolean,
    val callId: String
)

data class RazorpayOrderResponse(
    val id: String,
    val currency: String,
    val amount: Long,
    val keyId: String
)

data class BidRequest(
    val providerUid: String,
    val providerName: String,
    val price: Double
)

data class BidResponse(
    val providerUid: String,
    val providerName: String,
    val price: Double,
    val createdAt: String
)

data class CustomRequest(
    val customerUid: String,
    val customerName: String,
    val category: String,
    val problem: String,
    val minPrice: Double?,
    val maxPrice: Double?,
    val isAutoPrice: Boolean,
    val lat: Double? = null,
    val lng: Double? = null
)

data class CustomRequestResponse(
    val _id: String,
    val customerUid: String,
    val customerName: String,
    val category: String,
    val problem: String,
    val minPrice: Double?,
    val maxPrice: Double?,
    val isAutoPrice: Boolean,
    val status: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val bids: List<BidResponse> = emptyList(),
    val acceptedProviderUid: String? = null,
    val acceptedProviderName: String? = null,
    val rejectedBy: List<String> = emptyList(),
    val createdAt: String,
    val localReceivedTime: Long = 0L // Helper field for local UI timer
)

data class AdminAnalyticsResponse(
    val totalUsers: Int,
    val totalProviders: Int,
    val totalBookings: Int,
    val totalRevenue: Double,
    val categories: List<CategoryStat>
)

data class CategoryStat(
    val name: String,
    val ratio: Float
)

data class PerformanceReportResponse(
    val totalEarned: Double,
    val totalWork: Int,
    val weekly: List<Double>,
    val monthly: List<Double>,
    val joinDate: String,
    val dailyActivity: List<ActivityLog>,
    val averageRating: Float? = 0f,
    val completionRate: Int? = 100,
    val onTimeArrival: Int? = 95
)

data class WithdrawalResponse(
    val _id: String,
    val providerUid: String,
    val providerName: String,
    val amount: Double,
    val accountNumber: String,
    val ifscCode: String,
    val holderName: String,
    val status: String,
    val rejectionReason: String? = null,
    val payoutId: String? = null,
    val errorMessage: String? = null,
    val createdAt: String
)

data class ActivityLog(
    val date: String,
    val isOnline: Boolean
)

data class ApproveProviderRequest(val uid: String)
data class MessageResponse(val message: String)

data class CategoryResponse(
    val _id: String,
    val name: String,
    val icon: String?,
    val image: String?
)

data class ProviderResponse(
    val uid: String,
    val name: String,
    val rating: Float,
    val reviewCount: Int,
    val distance: String?,
    val startingPrice: Double,
    val category: String,
    val isVerified: Boolean,
    val profileImage: String? = null,
    val isAvailable: Boolean = true,
    val totalJobs: Int = 0,
    val totalEarnings: Double = 0.0,
    val lat: Double? = null,
    val lng: Double? = null,
    val bio: String? = null,
    val createdAt: String? = null
)

data class ProviderDetailResponse(
    val provider: ProviderResponse,
    val services: List<ServiceResponse>,
    val reviews: List<ReviewResponse>
)

data class ServiceResponse(
    val _id: String,
    val name: String,
    val price: Double,
    val description: String?
)

data class BookingRequest(
    val customerUid: String,
    val customerName: String,
    val providerUid: String,
    val serviceName: String,
    val address: String,
    val scheduledTime: String,
    val totalAmount: Double,
    val customerLat: Double? = null,
    val customerLng: Double? = null
)

data class BookingResponse(
    val _id: String,
    val customerUid: String,
    val providerUid: String,
    val providerName: String? = null,
    val customerName: String? = null,
    val serviceName: String,
    val status: String,
    val address: String,
    val scheduledTime: String,
    val totalAmount: Double,
    val paymentStatus: String? = null,
    val otp: String? = null,
    val customerLat: Double? = null,
    val customerLng: Double? = null,
    val providerLat: Double? = null,
    val providerLng: Double? = null,
    val partnerComment: String? = null,
    val customerPhone: String? = null,
    val providerImage: String? = null,
    val createdAt: String
)

data class ReviewRequest(
    val bookingId: String,
    val providerUid: String,
    val customerUid: String,
    val customerName: String,
    val rating: Int,
    val comment: String
)

data class ReviewResponse(
    val _id: String,
    val rating: Int,
    val comment: String,
    val customerName: String,
    val createdAt: String
)

data class UserCheckResponse(
    val exists: Boolean,
    val user: UserResponse?
)

data class BankDetailsResponse(
    val accountNumber: String?,
    val ifscCode: String?,
    val accountHolderName: String?
)

data class UserResponse(
    val uid: String,
    val phoneNumber: String,
    val role: String,
    val name: String?,
    val email: String?,
    val profileImage: String?,
    val aadhaarImage: String?,
    val category: String?,
    val walletBalance: Double,
    val isVerified: Boolean,
    val rating: Float? = null,
    val reviewCount: Int? = null,
    val totalJobs: Int? = null,
    val totalEarnings: Double? = null,
    val aadhaarNumber: String? = null,
    val verificationDate: String? = null,
    val profession: String? = null,
    val bankDetails: BankDetailsResponse? = null
)

data class TransactionResponse(
    val _id: String,
    val userUid: String,
    val type: String, // 'credit' or 'debit'
    val amount: Double,
    val title: String,
    val description: String?,
    val createdAt: String
)

data class AddressResponse(
    val _id: String,
    val label: String,
    val fullAddress: String,
    val lat: Double,
    val lng: Double
)

data class OfferRequest(
    val title: String,
    val description: String,
    val code: String,
    val discount: Int,
    val expiryDate: String
)

data class OfferResponse(
    val _id: String,
    val title: String,
    val description: String,
    val code: String,
    val discount: Int,
    val expiryDate: String,
    val createdAt: String
)

data class BannerResponse(
    val _id: String,
    val image: String,
    val title: String,
    val subtitle: String,
    val isActive: Boolean,
    val createdAt: String
)

data class SupportMessage(
    val _id: String? = null,
    val senderUid: String,
    val receiverUid: String,
    val message: String,
    val timestamp: String? = null,
    val isAdmin: Boolean = false
)

data class SupportChat(
    val userUid: String,
    val userName: String,
    val lastMessage: String,
    val lastTimestamp: String,
    val unreadCount: Int,
    val status: String // "pending", "active", "closed"
)
