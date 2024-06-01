package com.example.ioandroid.services.sensorServices

import android.location.Location
import org.json.JSONArray

interface LocationService {
    fun getLocation(): Pair<Location?, Location?>

    fun stopLocationUpdates()

    fun getSatelliteInfo(): List<Triple<String, Int, Float>>

    fun getSatelliteInfoJSON(): JSONArray
}