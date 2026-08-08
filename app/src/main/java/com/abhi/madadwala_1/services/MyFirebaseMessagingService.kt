package com.abhi.madadwala_1.services

import android.util.Log
import com.abhi.madadwala_1.data.remote.RetrofitClient
import com.abhi.madadwala_1.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.abhi.madadwala_1.data.PreferenceManager
import com.abhi.madadwala_1.data.NotificationData
import kotlinx.coroutines.*

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager(applicationContext)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d("FCM", "From: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Madadwala Update"

        val message = remoteMessage.notification?.body
            ?: remoteMessage.data["message"]
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["content"]
            ?: "You have a new update"

        val bookingId = remoteMessage.data["bookingId"]

        scope.launch {
            try {
                preferenceManager.incrementUnreadNotificationsCount()
                preferenceManager.saveNotification(NotificationData(title, message, bookingId = bookingId))
                Log.d("FCM", "Notification saved to local storage")
            } catch (e: Exception) {
                Log.e("FCM", "Error saving notification", e)
            }
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d("FCM", "Message Notification Body: ${it.body}")
            NotificationHelper(applicationContext).showNotification(
                it.title,
                it.body,
                remoteMessage.data
            )
        }

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FCM", "Message data payload: ${remoteMessage.data}")

            val isCall = remoteMessage.data["type"] == "call" || remoteMessage.data.containsKey("callId")

            if (isCall) {
                // For calls, we always trigger the notification helper
                NotificationHelper(applicationContext).showNotification(
                    title,
                    message,
                    remoteMessage.data
                )
            } else if (remoteMessage.notification == null) {
                NotificationHelper(applicationContext).showNotification(
                    title,
                    message,
                    remoteMessage.data
                )
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            scope.launch {
                try {
                    val response = RetrofitClient.apiService.updateFcmToken(
                        mapOf("uid" to uid, "fcmToken" to token)
                    )
                    if (response.isSuccessful) {
                        Log.d("FCM", "Token updated successfully on server")
                    } else {
                        Log.e("FCM", "Failed to update token on server: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e("FCM", "Error updating token on server", e)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
