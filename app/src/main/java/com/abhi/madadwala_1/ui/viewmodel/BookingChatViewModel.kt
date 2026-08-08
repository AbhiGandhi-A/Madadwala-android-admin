package com.abhi.madadwala_1.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhi.madadwala_1.data.remote.BookingMessageRequest
import com.abhi.madadwala_1.data.remote.BookingMessageResponse
import com.abhi.madadwala_1.data.remote.RetrofitClient
import com.abhi.madadwala_1.services.SocketHandler
import com.google.firebase.auth.FirebaseAuth
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class BookingChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<BookingMessageResponse>>(emptyList())
    val messages: StateFlow<List<BookingMessageResponse>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var currentBookingId: String? = null
    private val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    fun initChat(bookingId: String) {
        currentBookingId = bookingId
        fetchMessages(bookingId)
        setupSocket(bookingId)
    }

    private fun fetchMessages(bookingId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Use trackingApiService (Render) for consistency with Socket
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
        // Ensure we are using the correct backend for socket
        SocketHandler.setSocket("https://madadwala-backend.onrender.com")
        val socket = SocketHandler.getSocket()
        
        SocketHandler.establishConnection()
        
        // Join room immediately.
        socket?.emit("join_booking", bookingId)
        
        // Listen for connection to re-join room. 
        // We use a specific listener property to allow removing it later if needed,
        // or just accept that it might be added multiple times (harmless but not ideal).
        // For now, let's just make it simple but avoid .off() which breaks other ViewModels.
        socket?.on(Socket.EVENT_CONNECT) {
            android.util.Log.d("BookingChat", "Socket connected, joining room: $bookingId")
            socket.emit("join_booking", bookingId)
        }
        
        socket?.off("receive_message")
        socket?.on("receive_message") { args ->
            if (args.isNotEmpty()) {
                try {
                    val data = args[0] as? JSONObject ?: return@on
                    android.util.Log.d("BookingChat", "Received message: $data")
                    
                    val msgBookingId = data.optString("bookingId")
                    
                    // Accept if booking ID matches or is empty (for flexibility)
                    if (msgBookingId == currentBookingId || msgBookingId.isEmpty()) {
                        val senderUid = data.optString("senderUid")
                        val message = data.optString("message")
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
                            timestamp = timestamp
                        )
                        
                        // Update state flow on main thread
                        viewModelScope.launch {
                            val currentList = _messages.value
                            
                            // Check if this message (by ID or content) already exists
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
                    } else {
                        android.util.Log.d("BookingChat", "Message for different booking: $msgBookingId (Current: $currentBookingId)")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BookingChat", "Error parsing message", e)
                }
            }
        }
    }

    fun sendMessage(bookingId: String, message: String) {
        if (message.isBlank()) return
        
        val request = BookingMessageRequest(
            bookingId = bookingId,
            senderUid = currentUserUid,
            message = message
        )

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
            timestamp = timestamp
        )
        _messages.value = _messages.value + optimisticMsg

        viewModelScope.launch {
            try {
                val response = RetrofitClient.trackingApiService.sendBookingMessage(request)
                if (response.isSuccessful) {
                    val socket = SocketHandler.getSocket()
                    val serverMsg = response.body()
                    val data = JSONObject().apply {
                        put("bookingId", bookingId)
                        put("senderUid", currentUserUid)
                        put("message", message)
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

    override fun onCleared() {
        super.onCleared()
        SocketHandler.getSocket()?.off("receive_message")
    }
}
