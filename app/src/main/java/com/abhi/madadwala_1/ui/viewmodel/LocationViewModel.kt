package com.abhi.madadwala_1.ui.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhi.madadwala_1.data.PreferenceManager
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class LocationViewModel : ViewModel() {
    private val _address = MutableStateFlow("Fetching location...")
    val address: StateFlow<String> = _address

    private val _latitude = MutableStateFlow(0.0)
    val latitude: StateFlow<Double> = _latitude

    private val _longitude = MutableStateFlow(0.0)
    val longitude: StateFlow<Double> = _longitude

    private val _isManual = MutableStateFlow(false)
    val isManual: StateFlow<Boolean> = _isManual

    private var isInitialized = false
    private var isInitializing = false

    fun init(context: Context, onComplete: () -> Unit = {}) {
        if (isInitialized) {
            onComplete()
            return
        }
        if (isInitializing) return
        isInitializing = true

        val preferenceManager = PreferenceManager(context)
        viewModelScope.launch {
            try {
                val lat = preferenceManager.lastLatitude.first()
                val lng = preferenceManager.lastLongitude.first()
                val addr = preferenceManager.lastAddress.first()
                val manual = preferenceManager.isManualLocation.first()

                if (lat != null && lng != null && addr != null) {
                    _latitude.value = lat
                    _longitude.value = lng
                    _address.value = addr
                    _isManual.value = manual
                }

                // Fetch current location on open if not manual
                if (!manual) {
                    performFetch(context, false)
                }
            } catch (e: Exception) {
                // Ignore
            } finally {
                isInitialized = true
                isInitializing = false
                onComplete()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchLocation(context: Context, force: Boolean = false) {
        viewModelScope.launch {
            // Ensure we are initialized before deciding to fetch
            if (!isInitialized) {
                init(context) {
                    // If init was called, it already attempted a fetch if not manual.
                    // If force is true, we should fetch anyway.
                    if (force) {
                        performFetch(context, true)
                    }
                }
            } else {
                performFetch(context, force)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun performFetch(context: Context, force: Boolean) {
        // If it's a manual location and we're not forcing, don't override with live location
        if (!force && _isManual.value && _latitude.value != 0.0) return

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val preferenceManager = PreferenceManager(context)

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
                        val finalAddress = if (city.isNotEmpty()) "$city, India" else addressLine
                        _address.value = finalAddress

                        viewModelScope.launch {
                            preferenceManager.saveLocation(it.latitude, it.longitude, finalAddress, false)
                            _isManual.value = false
                        }
                    }
                } catch (e: Exception) {
                    _address.value = "Location found"
                }
            } ?: run {
                _address.value = "Location not available"
            }
        }.addOnFailureListener {
            _address.value = "Permission denied"
        }
    }

    fun setLocation(context: Context, lat: Double, lng: Double, addressName: String) {
        _latitude.value = lat
        _longitude.value = lng
        _address.value = addressName
        _isManual.value = true

        val preferenceManager = PreferenceManager(context)
        viewModelScope.launch {
            preferenceManager.saveLocation(lat, lng, addressName, true)
        }
    }
}
