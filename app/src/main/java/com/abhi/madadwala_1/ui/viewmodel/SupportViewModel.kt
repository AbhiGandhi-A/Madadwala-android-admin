package com.abhi.madadwala_1.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhi.madadwala_1.data.remote.RetrofitClient
import com.abhi.madadwala_1.data.remote.SupportMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SupportState {
    object Bot : SupportState()
    object Waiting : SupportState()
    object Active : SupportState()
}

class SupportViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<SupportMessage>>(emptyList())
    val messages: StateFlow<List<SupportMessage>> = _messages

    private val _isExecutiveJoined = MutableStateFlow(false)
    val isExecutiveJoined: StateFlow<Boolean> = _isExecutiveJoined

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _supportState = MutableStateFlow<SupportState>(SupportState.Bot)
    val supportState: StateFlow<SupportState> = _supportState

    fun loadMessages(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.apiService.getSupportMessages(userId)
                if (response.isSuccessful) {
                    val msgs = response.body() ?: emptyList()
                    _messages.value = msgs
                    if (msgs.any { it.isAdmin }) {
                        _isExecutiveJoined.value = true
                        _supportState.value = SupportState.Active
                    }
                }
            } catch (e: Exception) {} finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(userId: String, userName: String, text: String, isAdmin: Boolean = false) {
        val newMessage = SupportMessage(
            senderUid = userId,
            receiverUid = if (isAdmin) userId else "admin",
            message = text,
            isAdmin = isAdmin
        )
        
        _messages.value = _messages.value + newMessage
        
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.sendSupportMessage(newMessage)
                if (!isAdmin && _supportState.value == SupportState.Waiting) {
                    // Just wait for admin response
                }
            } catch (e: Exception) {}
        }
    }

    fun requestAgent(userId: String) {
        _supportState.value = SupportState.Waiting
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.updateSupportStatus(userId, mapOf("status" to "waiting"))
                val waitingMsg = SupportMessage(
                    senderUid = "system",
                    receiverUid = userId,
                    message = "Waiting for agent to join...",
                    isAdmin = true
                )
                _messages.value = _messages.value + waitingMsg
            } catch (e: Exception) {}
        }
    }

    fun startPolling(userId: String) {
        viewModelScope.launch {
            while (true) {
                delay(4000)
                val response = RetrofitClient.apiService.getSupportMessages(userId)
                if (response.isSuccessful) {
                    val newMsgs = response.body() ?: emptyList()
                    if (newMsgs.size > _messages.value.size) {
                        _messages.value = newMsgs
                        if (newMsgs.any { it.isAdmin && it.senderUid == "admin" }) {
                            _isExecutiveJoined.value = true
                            _supportState.value = SupportState.Active
                            RetrofitClient.apiService.updateSupportStatus(userId, mapOf("status" to "active"))
                        }
                    }
                }
            }
        }
    }
}
