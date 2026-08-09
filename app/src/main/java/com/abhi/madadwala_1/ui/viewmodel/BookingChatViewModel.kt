package com.abhi.madadwala_1.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhi.madadwala_1.data.remote.BookingMessageResponse
import com.abhi.madadwala_1.data.remote.BookingResponse
import com.abhi.madadwala_1.data.remote.ProviderResponse
import com.abhi.madadwala_1.data.remote.RetrofitClient
import com.abhi.madadwala_1.services.SocketHandler
import com.google.firebase.auth.FirebaseAuth
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class BookingChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<BookingMessageResponse>>(emptyList())
    val messages: StateFlow<List<BookingMessageResponse>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _bookingDetails = MutableStateFlow<BookingResponse?>(null)
    val bookingDetails: StateFlow<BookingResponse?> = _bookingDetails

    private val _partnerProfile = MutableStateFlow<ProviderResponse?>(null)
    val partnerProfile: StateFlow<ProviderResponse?> = _partnerProfile

    private var currentBookingId: String? = null
    private val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    fun initChat(bookingId: String) {
        currentBookingId = bookingId
        fetchMessages(bookingId)
        fetchBookingDetails(bookingId)
        setupSocket(bookingId)
    }

    private fun fetchBookingDetails(bookingId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getBookingDetails(bookingId)
                if (response.isSuccessful) {
                    val booking = response.body()
                    _bookingDetails.value = booking
                    
                    // Fetch partner profile for online status
                    booking?.providerUid?.let { uid ->
                        fetchPartnerProfile(uid)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun fetchPartnerProfile(uid: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getProviderDetails(uid)
                if (response.isSuccessful) {
                    _partnerProfile.value = response.body()?.provider
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun fetchMessages(bookingId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.trackingApiService.getBookingMessages(bookingId)
                if (response.isSuccessful) {
                    _messages.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun setupSocket(bookingId: String) {
        SocketHandler.setSocket("https://madadwala-backend.onrender.com")
        val socket = SocketHandler.getSocket()
        SocketHandler.establishConnection()
        
        socket?.emit("join_booking", bookingId)
        
        socket?.on(Socket.EVENT_CONNECT) {
            socket.emit("join_booking", bookingId)
        }
        
        socket?.off("receive_message")
        socket?.on("receive_message") { args ->
            if (args.isNotEmpty()) {
                try {
                    val data = args[0] as? JSONObject ?: return@on
                    val msgBookingId = data.optString("bookingId")
                    
                    if (msgBookingId == currentBookingId || msgBookingId.isEmpty()) {
                        val senderUid = data.optString("senderUid")
                        val message = data.optString("message")
                        val imageUrl = data.optString("imageUrl").takeIf { it.isNotEmpty() }
                        val timestamp = if (data.has("timestamp")) data.getString("timestamp") else {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                            sdf.format(java.util.Date())
                        }
                        val msgId = data.optString("_id", "temp_${System.currentTimeMillis()}")
                        
                        val newMessage = BookingMessageResponse(
                            _id = msgId,
                            bookingId = msgBookingId.ifEmpty { currentBookingId ?: "" },
                            senderUid = senderUid,
                            message = message,
                            imageUrl = imageUrl,
                            timestamp = timestamp
                        )
                        
                        viewModelScope.launch {
                            val currentList = _messages.value
                            val existingIndex = currentList.indexOfFirst { 
                                it._id == msgId || (it._id.startsWith("temp_") && it.message == message && it.senderUid == senderUid)
                            }
                            
                            if (existingIndex != -1) {
                                val newList = currentList.toMutableList()
                                newList[existingIndex] = newMessage
                                _messages.value = newList
                            } else {
                                _messages.value = currentList + newMessage
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun sendMessage(bookingId: String, message: String, imageUri: Uri? = null, context: Context? = null) {
        if (message.isBlank() && imageUri == null) return
        
        // Optimistic UI update
        val tempId = "temp_${System.currentTimeMillis()}"
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val timestamp = sdf.format(java.util.Date())
        
        val optimisticMsg = BookingMessageResponse(
            _id = tempId,
            bookingId = bookingId,
            senderUid = currentUserUid,
            message = message,
            imageUrl = imageUri?.toString(), // Use local URI temporarily
            timestamp = timestamp
        )
        _messages.value = _messages.value + optimisticMsg

        viewModelScope.launch {
            try {
                val bookingIdPart = bookingId.toRequestBody("text/plain".toMediaTypeOrNull())
                val senderUidPart = currentUserUid.toRequestBody("text/plain".toMediaTypeOrNull())
                val messagePart = message.toRequestBody("text/plain".toMediaTypeOrNull())
                
                var imagePart: MultipartBody.Part? = null
                if (imageUri != null && context != null) {
                    val file = getFileFromUri(context, imageUri)
                    if (file != null) {
                        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                        imagePart = MultipartBody.Part.createFormData("chatImage", file.name, requestFile)
                    }
                }

                val response = RetrofitClient.trackingApiService.sendBookingMessage(
                    bookingIdPart, senderUidPart, messagePart, imagePart
                )

                if (response.isSuccessful) {
                    val serverMsg = response.body()
                    val socket = SocketHandler.getSocket()
                    val data = JSONObject().apply {
                        put("bookingId", bookingId)
                        put("senderUid", currentUserUid)
                        put("message", message)
                        put("imageUrl", serverMsg?.imageUrl)
                        put("_id", serverMsg?._id ?: tempId)
                        put("timestamp", serverMsg?.timestamp ?: timestamp)
                    }
                    socket?.emit("send_message", data)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _messages.value = _messages.value.filter { it._id != tempId }
            }
        }
    }

    private fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, "chat_image_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        SocketHandler.getSocket()?.off("receive_message")
    }
}
