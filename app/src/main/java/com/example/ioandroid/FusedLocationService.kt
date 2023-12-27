package com.example.ioandroid

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

class FusedLocationService(private val activity: Activity) {
    private var fusedLocationClient : FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity);
    private var currentLocation: Location? = null;
    private var isLocationUpdating: Boolean = false;
    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            currentLocation = locationResult.lastLocation
        }
    }

    fun getLocation(): Location? {
        updateLocation();
        return currentLocation;
    }

    private fun updateLocation() {
        startLocationUpdates()
        stopLocationUpdates()
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
            if (!isLocationUpdating) {
                val locationRequest = createLocationRequest()
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
                isLocationUpdating = true
            }
        }
    }


    private fun stopLocationUpdates() {
        if(isLocationUpdating) {
            isLocationUpdating = false
        }
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.create().apply {
            interval = 10
            fastestInterval = 5
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }


}