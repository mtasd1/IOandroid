package com.example.ioandroid

import android.location.Location
import java.text.SimpleDateFormat
import java.util.Date

interface LocationService {
    fun getLocation(): Pair<Location?, Location?>

    fun getFormattedTime(currentTime: Long, format: SimpleDateFormat): String {
        return format.format(Date(currentTime))
    }

    fun getSatelliteInfo(): String

    fun getFormattedLocation(location: Location) {
        //TODO: implement this method
    }
}