package com.abhi.madadwala_1.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhi.madadwala_1.data.remote.RetrofitClient
import com.abhi.madadwala_1.data.remote.UserResponse
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import android.util.Log

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class OtpSent(val verificationId: String) : AuthState()
    data class Authenticated(val user: UserResponse?, val firebaseUser: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
    object NewUser : AuthState()
}

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val apiService = RetrofitClient.apiService
    private lateinit var analytics: FirebaseAnalytics

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun initAnalytics(context: android.content.Context) {
        analytics = FirebaseAnalytics.getInstance(context)
    }

    init {
        checkSession()
    }

    private fun checkSession() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _authState.value = AuthState.Loading
            viewModelScope.launch {
                checkUserRegistration(currentUser)
            }
        }
    }

    private var verificationId: String? = null

    fun sendOtp(phoneNumber: String, activity: Activity) {
        _authState.value = AuthState.Loading
        
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+91$phoneNumber")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    _authState.value = AuthState.Error(e.localizedMessage ?: "Verification failed")
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id
                    _authState.value = AuthState.OtpSent(id)
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(otp: String) {
        val id = verificationId ?: return
        val credential = PhoneAuthProvider.getCredential(id, otp)
        signInWithCredential(credential)
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = auth.signInWithCredential(credential).await()
                val firebaseUser = result.user
                if (firebaseUser != null) {
                    checkUserRegistration(firebaseUser)
                } else {
                    _authState.value = AuthState.Error("Sign in failed")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Authentication error")
            }
        }
    }

    private suspend fun checkUserRegistration(firebaseUser: FirebaseUser) {
        try {
            val response = apiService.checkUser(
                phoneNumber = firebaseUser.phoneNumber ?: "",
                uid = firebaseUser.uid
            )
            if (response.isSuccessful) {
                val checkResult = response.body()
                if (checkResult?.exists == true) {
                    val user = checkResult.user
                    if (user?.role == "provider" && !user.isVerified) {
                        _authState.value = AuthState.Error("Your application is currently under review. Please try again later.")
                    } else {
                        if (::analytics.isInitialized) {
                            user?.role?.let { analytics.setUserProperty("user_role", it) }
                            analytics.setUserId(firebaseUser.uid)
                        }
                        updateFcmTokenOnServer(firebaseUser.uid)
                        _authState.value = AuthState.Authenticated(user, firebaseUser)
                    }
                } else {
                    _authState.value = AuthState.NewUser
                }
            } else {
                _authState.value = AuthState.Error("Failed to check user registration")
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.localizedMessage ?: "Network error")
        }
    }

    private fun updateFcmTokenOnServer(uid: String) {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                apiService.updateFcmToken(mapOf("uid" to uid, "fcmToken" to token))
                Log.d("AuthViewModel", "FCM Token updated successfully")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to update FCM Token", e)
            }
        }
    }

    fun refresh() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                checkUserRegistration(currentUser)
            }
        }
    }

    fun logout() {
        auth.signOut()
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
