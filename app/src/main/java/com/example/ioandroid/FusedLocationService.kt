package com.example.ioandroid

import android.app.Activity

class FusedLocationService(private val activity: Activity) /*: LocationService*/ {
    /*private var fusedLocationClient : FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)
    private var currentLocation: Location? = null
    private val trackButton: Button = activity.findViewById(R.id.btnTrack)
    private val spinnerLocation: Spinner = activity.findViewById(R.id.spinnerLocation)
    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            locationResult.lastLocation?.let {
                currentLocation = it
                enableTrackButton()
            }
        }
    }

    override fun getLocation(): Location? {
        updateLocation()
        return currentLocation
    }

    override fun getSatelliteInfo(): String {
        return ""
    }

    private fun updateLocation() {
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            startLocationUpdates()
        } else {
            fusedLocationClient.requestLocationUpdates(createLocationRequest(), locationCallback, Looper.myLooper())
        }
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.create().apply {
            interval = 10
            fastestInterval = 5
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun enableTrackButton() {
        if (!trackButton.isEnabled && currentLocation != null&& spinnerLocation.selectedItem != null) {
            trackButton.isEnabled = true
            trackButton.text = "Track"
        }
    }*/
}
