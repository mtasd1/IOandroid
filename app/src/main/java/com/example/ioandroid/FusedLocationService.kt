package com.example.ioandroid

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback

import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

class FusedLocationService {
    private lateinit var fusedLocationClient : FusedLocationProviderClient;
    private lateinit var context: Activity;
    private var currentLocation: Location? = null;
    private lateinit var locationCallback: LocationCallback;

    //create a constructor
    constructor(activity: Activity) {
        context = activity;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                p0.lastLocation?.let {
                    currentLocation = it
                }
            }
        }
    }

    //create function to get the location
    private fun updateLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(context, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
        else {
         // permissions are granted now get the location and return it as a string
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        locationCallback.onLocationResult(LocationResult.create(listOf(location)))

                        currentLocation = location
                        Toast.makeText(context, "GPS with accuracy of " + currentLocation!!.accuracy, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Location is null", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    fun getLocation(): Location? {
        updateLocation();
        return currentLocation;
    }
}