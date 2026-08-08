package com.abhi.madadwala_1.ui.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.abhi.madadwala_1.data.remote.CallRequest
import com.abhi.madadwala_1.data.remote.RetrofitClient
import com.abhi.madadwala_1.services.SocketHandler
import com.abhi.madadwala_1.ui.screens.VoiceCallScreen
import com.abhi.madadwala_1.ui.theme.MadadwalaTheme
import com.abhi.madadwala_1.utils.NotificationHelper
import com.abhi.madadwala_1.utils.WebRTCManager
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed class CallState {
    object Idle : CallState()
    data class Outgoing(val callId: String, val partnerName: String, val partnerId: String) : CallState()
    data class Ringing(val callId: String, val partnerName: String, val partnerId: String) : CallState()
    data class Incoming(
        val callId: String,
        val customerName: String,
        val customerImage: String?,
        val bookingId: String,
        val callerId: String,
        val serviceName: String? = null,
        val location: String? = null,
        val bookingTime: String? = null
    ) : CallState()
    data class Connected(val callId: String, val otherPartyName: String, val duration: Long = 0) : CallState()
    object Ended : CallState()
}

class CallViewModel(application: Application) : AndroidViewModel(application) {
    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted

    private var webRTCManager: WebRTCManager? = null
    private var ringtone: android.media.Ringtone? = null
    private val notificationHelper = NotificationHelper(application)

    init {
        initializeSocket()
    }

    private fun initializeSocket() {
        // Ensure we always have the socket set with the correct URL tracked in SocketHandler
        SocketHandler.setSocket("https://madadwala-backend.onrender.com")
        
        val socket = SocketHandler.getSocket()
        SocketHandler.establishConnection() // Always try to connect
        
        // Use a safe way to add EVENT_CONNECT without clearing others
        socket?.on(Socket.EVENT_CONNECT) {
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            currentUser?.uid?.let { uid ->
                android.util.Log.d("CallViewModel", "Socket connected, joining room: $uid")
                socket.emit("join", uid)
                
                // If we have a pending incoming call from intent, notify ringing now
                val currentState = _callState.value
                if (currentState is CallState.Incoming) {
                    android.util.Log.d("CallViewModel", "Emitting ringing after connect for: ${currentState.callId}")
                    val ringingData = JSONObject().apply {
                        put("callId", currentState.callId)
                    }
                    socket.emit("ringing", ringingData)
                }
            }
        }

        // Also emit join if already connected
        if (socket?.connected() == true) {
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            currentUser?.uid?.let { uid ->
                android.util.Log.d("CallViewModel", "Socket already connected, joining room: $uid")
                socket.emit("join", uid)
            }
        }

        setupSocketListeners()
    }

    fun registerUser(uid: String) {
        val socket = SocketHandler.getSocket()
        if (socket == null) {
            initializeSocket()
            return
        }
        
        if (socket.connected()) {
            android.util.Log.d("CallViewModel", "Manually joining room: $uid")
            socket.emit("join", uid)
        } else {
            android.util.Log.d("CallViewModel", "Socket not connected, trying to connect for: $uid")
            SocketHandler.establishConnection()
        }
    }

    fun initWebRTC(context: android.content.Context) {
        if (webRTCManager == null) {
            webRTCManager = WebRTCManager(context.applicationContext) { event, data ->
                SocketHandler.getSocket()?.emit(event, data)
            }
        }
        
        // Handle ringtone based on state
        val state = _callState.value
        if (state is CallState.Incoming) {
            playRingtone(context)
        } else if (state is CallState.Connected || state is CallState.Ended || state is CallState.Idle) {
            stopRingtone()
        }
    }

    private fun playRingtone(context: android.content.Context) {
        if (ringtone == null) {
            val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
            ringtone = android.media.RingtoneManager.getRingtone(context.applicationContext, uri)
            ringtone?.play()
        }
    }

    private fun stopRingtone() {
        ringtone?.stop()
        ringtone = null
    }

    private var timerJob: kotlinx.coroutines.Job? = null

    private fun setupSocketListeners() {
        val socket = SocketHandler.getSocket()
        
        socket?.off("incoming_call")
        socket?.on("incoming_call") { args ->
            try {
                val data = args[0] as JSONObject
                val callId = data.optString("callId", "")
                
                // Try to get name from various possible fields
                val name = if (data.has("callerName")) {
                    data.getString("callerName")
                } else if (data.has("customerName")) {
                    data.getString("customerName")
                } else if (data.has("providerName")) {
                    data.getString("providerName")
                } else {
                    "Someone"
                }

                val image = if (data.has("callerImage")) {
                    data.getString("callerImage")
                } else if (data.has("customerImage")) {
                    data.getString("customerImage")
                } else if (data.has("providerImage")) {
                    data.getString("providerImage")
                } else {
                    null
                }

                val bookingId = data.optString("bookingId", "")
                val callerId = data.optString("callerId", "")
                
                android.util.Log.d("CallViewModel", "Incoming call: $callId from $name ($callerId)")
                
                _callState.value = CallState.Incoming(callId, name, image, bookingId, callerId)

                // Fetch extra details for the UI
                if (bookingId.isNotEmpty()) {
                    viewModelScope.launch {
                        try {
                            val response = RetrofitClient.apiService.getBookingDetails(bookingId)
                            if (response.isSuccessful) {
                                val booking = response.body()
                                val currentState = _callState.value
                                if (currentState is CallState.Incoming && currentState.callId == callId) {
                                    _callState.value = currentState.copy(
                                        serviceName = booking?.serviceName,
                                        location = booking?.address,
                                        bookingTime = booking?.scheduledTime
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("CallViewModel", "Error fetching booking details for call", e)
                        }
                    }
                }

                // Notify server that it's ringing on our side
                val ringingData = JSONObject().apply {
                    put("callId", callId)
                }
                socket.emit("ringing", ringingData)
            } catch (e: Exception) {
                android.util.Log.e("CallViewModel", "Error handling incoming_call", e)
            }
        }

        socket?.off("ringing")
        socket?.on("ringing") {
            val currentState = _callState.value
            if (currentState is CallState.Outgoing) {
                _callState.value = CallState.Ringing(currentState.callId, currentState.partnerName, currentState.partnerId)
            }
        }

        socket?.off("call_accepted")
        socket?.on("call_accepted") {
            val currentState = _callState.value
            if (currentState is CallState.Outgoing || currentState is CallState.Ringing) {
                val callId = if (currentState is CallState.Outgoing) currentState.callId else (currentState as CallState.Ringing).callId
                val partnerName = if (currentState is CallState.Outgoing) currentState.partnerName else (currentState as CallState.Ringing).partnerName
                val partnerId = if (currentState is CallState.Outgoing) currentState.partnerId else (currentState as CallState.Ringing).partnerId

                android.util.Log.d("CallViewModel", "Call accepted by $partnerName ($partnerId). Starting WebRTC.")
                
                _callState.value = CallState.Connected(callId, partnerName)
                startTimer()
                
                // IMPORTANT: Start WebRTC Offer from Caller side
                webRTCManager?.startCall(partnerId)
            }
        }

        socket?.off("call_ended")
        socket?.on("call_ended") {
            stopRingtone()
            notificationHelper.cancelCallNotification()
            stopTimer()
            _callState.value = CallState.Ended
            webRTCManager?.stopCall()

            viewModelScope.launch {
                kotlinx.coroutines.delay(2000)
                _callState.value = CallState.Idle
            }
        }

        // WebRTC Signaling
        socket?.off("offer")
        socket?.on("offer") { args ->
            val data = args[0] as JSONObject
            webRTCManager?.handleOffer(data)
        }

        socket?.off("answer")
        socket?.on("answer") { args ->
            val data = args[0] as JSONObject
            webRTCManager?.handleAnswer(data)
        }

        socket?.off("ice_candidate")
        socket?.on("ice_candidate") { args ->
            val data = args[0] as JSONObject
            webRTCManager?.handleIceCandidate(data)
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                val currentState = _callState.value
                if (currentState is CallState.Connected) {
                    _callState.value = currentState.copy(duration = currentState.duration + 1)
                } else {
                    break
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun handleIncomingCallFromIntent(
        callId: String,
        callerName: String,
        callerImage: String?,
        bookingId: String,
        callerId: String
    ) {
        if (_callState.value is CallState.Idle || _callState.value is CallState.Ended) {
            android.util.Log.d("CallViewModel", "Handling call from intent: $callId")
            _callState.value = CallState.Incoming(callId, callerName, callerImage, bookingId, callerId)
            
            // Notify server that it's ringing on our side
            val socket = SocketHandler.getSocket()
            if (socket?.connected() == true) {
                val ringingData = JSONObject().apply {
                    put("callId", callId)
                }
                socket.emit("ringing", ringingData)
            } else {
                initializeSocket()
            }
            
            // Fetch extra details
            if (bookingId.isNotEmpty()) {
                viewModelScope.launch {
                    try {
                        val response = RetrofitClient.apiService.getBookingDetails(bookingId)
                        if (response.isSuccessful) {
                            val booking = response.body()
                            val currentState = _callState.value
                            if (currentState is CallState.Incoming && currentState.callId == callId) {
                                _callState.value = currentState.copy(
                                    serviceName = booking?.serviceName,
                                    location = booking?.address,
                                    bookingTime = booking?.scheduledTime
                                )
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CallViewModel", "Error fetching booking details for call", e)
                    }
                }
            }
        }
    }

    fun startCall(bookingId: String, customerId: String, partnerId: String, partnerName: String) {
        viewModelScope.launch {
            try {
                // Ensure socket is connected before starting call
                val socket = SocketHandler.getSocket()
                if (socket?.connected() != true) {
                    SocketHandler.establishConnection()
                }

                // IMPORTANT: Use trackingApiService (Render) because Vercel doesn't support Sockets
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val currentUid = currentUser?.uid ?: ""
                
                val response = RetrofitClient.trackingApiService.startCall(
                    CallRequest(bookingId, customerId, partnerId, currentUid)
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    val callId = response.body()?.callId ?: ""
                    
                    // Determine the target ID (the person we are calling)
                    val targetId = if (currentUid == customerId) partnerId else customerId
                    
                    _callState.value = CallState.Outgoing(callId, partnerName, targetId)
                } else {
                    android.util.Log.e("CallViewModel", "Start call failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("CallViewModel", "Start call error", e)
            }
        }
    }

    fun acceptCall(callId: String) {
        stopRingtone()
        notificationHelper.cancelCallNotification()
        val socket = SocketHandler.getSocket()
        val data = JSONObject().apply {
            put("callId", callId)
        }
        socket?.emit("call_accepted", data)
        
        val currentState = _callState.value
        if (currentState is CallState.Incoming) {
            _callState.value = CallState.Connected(callId, currentState.customerName)
            startTimer()

            // Re-initialize WebRTC on accept to be ready for the offer
            // The Caller will receive 'call_accepted' and send the Offer.
        }
    }

    fun toggleSpeaker() {
        val newState = !_isSpeakerOn.value
        _isSpeakerOn.value = newState
        webRTCManager?.toggleSpeaker(newState)
    }

    fun toggleMic() {
        val newState = !_isMuted.value
        _isMuted.value = newState
        webRTCManager?.toggleMic(newState)
    }

    fun endCall(callId: String) {
        stopRingtone()
        notificationHelper.cancelCallNotification()
        val socket = SocketHandler.getSocket()
        val data = JSONObject().apply {
            put("callId", callId)
        }
        socket?.emit("end_call", data)
        stopTimer()
        _callState.value = CallState.Ended
        webRTCManager?.stopCall()
        // Reset local states
        _isSpeakerOn.value = false
        _isMuted.value = false
    }
}

class IncomingCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Window flags for lock screen and waking up
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        
        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        val callId = intent.getStringExtra("callId")
        if (callId == null) {
            finish()
            return
        }
        
        val callerName = intent.getStringExtra("callerName") ?: "Partner"
        val callerImage = intent.getStringExtra("callerImage")
        val bookingId = intent.getStringExtra("bookingId") ?: ""
        val callerId = intent.getStringExtra("callerId") ?: ""

        val viewModel = ViewModelProvider(this)[CallViewModel::class.java]
        viewModel.handleIncomingCallFromIntent(
            callId, callerName, callerImage, bookingId, callerId
        )

        setContent {
            MadadwalaTheme {
                VoiceCallScreen(
                    viewModel = viewModel,
                    onDismiss = { finish() }
                )
            }
        }
    }
}
