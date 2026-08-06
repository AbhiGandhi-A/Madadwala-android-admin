package com.abhi.madadwala_1.services

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import io.socket.client.Socket
import org.json.JSONObject

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var socket: Socket? = null
    private var bookingId: String? = null
    private var lastLat: Double? = null
    private var lastLng: Double? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        socket = SocketHandler.getSocket()
        
        createNotificationChannel()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val currentLat = location.latitude
                    val currentLng = location.longitude

                    // 2. Don't send duplicate locations (Check if moved > 5 meters)
                    val shouldUpdate = if (lastLat == null || lastLng == null) {
                        true
                    } else {
                        val results = FloatArray(1)
                        android.location.Location.distanceBetween(
                            lastLat!!, lastLng!!,
                            currentLat, currentLng,
                            results
                        )
                        results[0] >= 5 // Update only if moved more than 5 meters
                    }

                    if (shouldUpdate) {
                        // 3. Handle socket reconnection
                        if (socket?.connected() != true) {
                            socket?.connect()
                        }

                        val data = JSONObject()
                        data.put("bookingId", bookingId)
                        data.put("lat", currentLat)
                        data.put("lng", currentLng)
                        data.put("role", "provider")
                        
                        socket?.emit("update_location", data)
                        
                        lastLat = currentLat
                        lastLng = currentLng
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        bookingId = intent?.getStringExtra("bookingId")
        bookingId?.let {
            if (socket?.connected() != true) {
                socket?.connect()
            }
            socket?.emit("join_booking", it)
        }
        
        val notification = NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("Madadwala Tracking")
            .setContentText("Your location is being shared with the customer.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
        startLocationUpdates()
        
        return START_STICKY
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setMinUpdateIntervalMillis(2000)
            .setMinUpdateDistanceMeters(5f)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (unlikely: SecurityException) {
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "location_channel",
                "Location Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        
        // 1. Disconnect/Leave booking when service stops
        bookingId?.let {
            socket?.emit("leave_booking", it)
        }
        // Not calling socket?.disconnect() as it's a shared SocketHandler instance
    }
}
