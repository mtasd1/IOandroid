package com.example.ioandroid

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat

class LocationManagerService(private var activity: Activity): LocationService {
    private val locationManager = activity.getSystemService(Activity.LOCATION_SERVICE) as LocationManager
    private var currentLocationGPS: Location? = null
    private var currentLocationNetwork: Location? = null
    private val satellites = mutableListOf<Pair<Int,Float>>()
    private var nrSatellitesInFix = 0
    private var nrSatellitesInView = 0
    private var satellitesText = ""
    private val trackButton: Button = activity.findViewById(R.id.btnTrack)
    private val spinnerLocation: Spinner = activity.findViewById(R.id.spinnerLocation)

    private val locationListenerGPS: LocationListener =
        LocationListener { location ->
            currentLocationGPS = location
            enableTrackButton()
        }

    private val locationListenerNetwork: LocationListener =
        LocationListener { location ->
            currentLocationNetwork = location
            enableTrackButton()
        }

    val gnssStatusCallback = object: GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            super.onSatelliteStatusChanged(status)
            satellites.clear()
            nrSatellitesInFix = 0
            nrSatellitesInView = status.satelliteCount
            for (i in 0 until nrSatellitesInView) {
                if(status.usedInFix(i)){
                    satellites.add(Pair(status.getSvid(i), status.getCn0DbHz(i)))
                    nrSatellitesInFix++
                }
            }
            //sort the satellites by their signal strength
            satellites.sortByDescending { it.second }
            satellitesText = satellites.toString()
            Toast.makeText(activity, "in View: $nrSatellitesInView in Fix: $nrSatellitesInFix \n $satellitesText", Toast.LENGTH_SHORT).show()
        }
    }

    private fun enableTrackButton() {
        if (!trackButton.isEnabled && (currentLocationGPS != null || currentLocationNetwork != null) && spinnerLocation.selectedItem != null) {
            trackButton.isEnabled = true
            trackButton.text = "Track"
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getLocation(): Pair<Location?, Location?> {
        updateLocation()
        getIDsOfSatellites()
        return Pair(currentLocationNetwork, currentLocationGPS)
    }

    override fun getSatelliteInfo(): String {
        return satellitesText
    }

    @SuppressLint("MissingPermission")
    private fun getIDsOfSatellites(): List<Pair<Int,Float>> {
        checkPermissions()
        locationManager.registerGnssStatusCallback(gnssStatusCallback)
        return satellites
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun updateLocation() {
        startLocationUpdates(LocationManager.GPS_PROVIDER, locationListenerGPS)
        startLocationUpdates(LocationManager.NETWORK_PROVIDER, locationListenerNetwork)
    }

    //This method is called only once to start the location updates, after that the locationListener will be called
    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(provider: String, locationListener: LocationListener) {
        unregisterGNSSStatusCallback()
        checkPermissions()
        locationManager.requestLocationUpdates(provider, 1, 1f, locationListener)
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
