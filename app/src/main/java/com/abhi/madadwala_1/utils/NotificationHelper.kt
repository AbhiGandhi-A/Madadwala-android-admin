package com.abhi.madadwala_1.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.abhi.madadwala_1.MainActivity
import com.abhi.madadwala_1.R
import com.abhi.madadwala_1.ui.viewmodel.IncomingCallActivity

class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "madadwala_notifications"
        private const val CHANNEL_NAME = "Madadwala Notifications"
        private const val CHANNEL_DESC = "Notifications for bookings and updates"
        
        private const val CALL_CHANNEL_ID = "madadwala_calls"
        private const val CALL_CHANNEL_NAME = "Madadwala Calls"
        private const val CALL_CHANNEL_DESC = "Notifications for incoming calls"

        const val CALL_NOTIFICATION_ID = 1001
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Standard Channel
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)

            // Call Channel
            val callChannel = NotificationChannel(
                CALL_CHANNEL_ID,
                CALL_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CALL_CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build()
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE), audioAttributes)
            }
            notificationManager.createNotificationChannel(callChannel)
        }
    }

    fun showNotification(title: String?, message: String?, data: Map<String, String>? = null) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val isCall = data?.get("type") == "call" || data?.containsKey("callId") == true

        // Ensure channels are created
        createNotificationChannels()

        val intent = if (isCall) {
            Intent(context, IncomingCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                data?.forEach { (key, value) ->
                    putExtra(key, value)
                }
            }
        } else {
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                data?.forEach { (key, value) ->
                    putExtra(key, value)
                }
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            if (isCall) 1001 else 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = if (isCall) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        // Determine Large Icon based on content (Simulating the UI in the image)
        val largeIconRes = when {
            title?.contains("Job", ignoreCase = true) == true -> R.drawable.app_logo
            title?.contains("Payment", ignoreCase = true) == true -> R.drawable.app_logo
            title?.contains("Rating", ignoreCase = true) == true || title?.contains("Great Job", ignoreCase = true) == true -> R.drawable.app_logo
            title?.contains("Booking", ignoreCase = true) == true -> R.drawable.app_logo
            else -> R.drawable.app_logo
        }

        val notificationBuilder = NotificationCompat.Builder(context, if (isCall) CALL_CHANNEL_ID else CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setSubText("🔔") // Adds bell emoji next to app name "Madadwala"
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setLargeIcon(createStyledLargeIcon(largeIconRes))
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(if (isCall) NotificationCompat.CATEGORY_CALL else NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(ContextCompat.getColor(context, R.color.black)) // Standard dark background for notification

        if (isCall) {
            notificationBuilder.setFullScreenIntent(pendingIntent, true)
            notificationBuilder.setOngoing(true)
            notificationBuilder.setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
        }

        notificationManager.notify(if (isCall) CALL_NOTIFICATION_ID else System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun createStyledLargeIcon(resourceId: Int): Bitmap {
        val size = 128
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        // Draw white rounded background
        paint.color = Color.WHITE
        val rectF = RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawRoundRect(rectF, 30f, 30f, paint)
        
        // Draw the icon centered
        val icon = BitmapFactory.decodeResource(context.resources, resourceId)
        if (icon != null) {
            val padding = 25
            val destRect = Rect(padding, padding, size - padding, size - padding)
            canvas.drawBitmap(icon, null, destRect, null)
        }
        
        return output
    }

    fun cancelCallNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(CALL_NOTIFICATION_ID)
    }

}
