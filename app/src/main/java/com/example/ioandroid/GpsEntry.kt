package com.example.ioandroid

import android.location.Location

data class GpsEntry(val gpsData: Location, val label: String) {
    override fun toString(): String {
        return "Location: $gpsData\nLabel: $label"
    }
}
