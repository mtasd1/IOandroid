package com.example.ioandroid

data class GpsEntry(val gpsData: String, val label: String) {
    override fun toString(): String {
        return "Location: $gpsData\nLabel: $label"
    }
}
