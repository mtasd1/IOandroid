package com.example.ioandroid

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.core.app.ActivityCompat

class LocationManagerService(private var activity: Activity): LocationService {
    private val locationManager = activity.getSystemService(Activity.LOCATION_SERVICE) as LocationManager
    private val provider = locationManager.getProvider(LocationManager.GPS_PROVIDER)
    private var currentLocation: Location? = null
    val satellites = mutableListOf<Pair<Int,Float>>()
    private val trackButton: Button = activity.findViewById(R.id.btnTrack)
    private val spinnerLocation: Spinner = activity.findViewById(R.id.spinnerLocation)

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            currentLocation = location
            enableTrackButton()
        }
    }
    val gnssStatusCallback = object: GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            super.onSatelliteStatusChanged(status)
            satellites.clear()
            for (i in 0 until status.satelliteCount) {
                if(status.usedInFix(i)){
                    satellites.add(Pair(status.getSvid(i), status.getCn0DbHz(i)))
                }
            }
            //sort the satellites by their signal strength
            satellites.sortByDescending { it.second }
            Toast.makeText(activity, "Satellites: ${satellites}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun enableTrackButton() {
        if (!trackButton.isEnabled && currentLocation != null && spinnerLocation.selectedItem != null) {
            trackButton.isEnabled = true
            trackButton.text = "Track"
        }
    }

    override fun getLocation(): Location? {
        updateLocation()
        getIDsOfSatellites()
        return currentLocation
    }

    @SuppressLint("MissingPermission")
    private fun getIDsOfSatellites(): List<Pair<Int,Float>> {
        checkPermissions()
        locationManager.registerGnssStatusCallback(gnssStatusCallback)
        return satellites
    }

    private fun updateLocation() {
        startLocationUpdates()
    }

    //This method is called only once to start the location updates, after that the locationListener will be called
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        unregisterGNSSStatusCallback()
        checkPermissions()
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1f, locationListener)
    }

    private fun unregisterGNSSStatusCallback() {
        locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
    }

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1) }
    }
}
