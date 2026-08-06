package com.abhi.madadwala_1.ui.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

class LocationViewModel : ViewModel() {
    private val _address = MutableStateFlow("Fetching location...")
    val address: StateFlow<String> = _address

    private val _latitude = MutableStateFlow(0.0)
    val latitude: StateFlow<Double> = _latitude

    private val _longitude = MutableStateFlow(0.0)
    val longitude: StateFlow<Double> = _longitude

    @SuppressLint("MissingPermission")
    fun fetchLocation(context: Context) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                _latitude.value = it.latitude
                _longitude.value = it.longitude
                val geocoder = Geocoder(context, Locale.getDefault())
                try {
                    val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                    if (addresses?.isNotEmpty() == true) {
                        val addressLine = addresses[0].getAddressLine(0)
                        val city = addresses[0].locality ?: addresses[0].subAdminArea ?: ""
                        _address.value = if (city.isNotEmpty()) "$city, India" else addressLine
                        fun setLocation(lat: Double, lng: Double, addressName: String) {
        _latitude.value = lat
        _longitude.value = lng
        _address.value = addressName
    }
}
                } catch (e: Exception) {
                    _address.value = "Location found"
                    fun setLocation(lat: Double, lng: Double, addressName: String) {
        _latitude.value = lat
        _longitude.value = lng
        _address.value = addressName
    }
}
            } ?: run {
                _address.value = "Location not available"
                fun setLocation(lat: Double, lng: Double, addressName: String) {
        _latitude.value = lat
        _longitude.value = lng
        _address.value = addressName
    }
}
        }.addOnFailureListener {
            _address.value = "Permission denied"
            fun setLocation(lat: Double, lng: Double, addressName: String) {
        _latitude.value = lat
        _longitude.value = lng
        _address.value = addressName
    }
}
        fun setLocation(lat: Double, lng: Double, addressName: String) {
        _latitude.value = lat
        _longitude.value = lng
        _address.value = addressName
    }
}
    fun setLocation(lat: Double, lng: Double, addressName: String) {
        _latitude.value = lat
        _longitude.value = lng
        _address.value = addressName
    }
}
