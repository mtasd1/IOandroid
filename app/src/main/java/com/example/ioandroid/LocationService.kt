package com.example.ioandroid

import android.location.Location
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date

interface LocationService {
    fun getLocation(): Pair<Location?, Location?>

    fun stopLocationUpdates()

    fun getFormattedTime(currentTime: Long, format: SimpleDateFormat): String {
        return format.format(Date(currentTime))
    }

    fun getSatelliteInfo(): List<Triple<String, Int, Float>>

    fun getSatelliteInfoJSON(): JSONArray

    fun getFormattedLocation(location: Location) {
        //TODO: implement this method
    }
}