package com.example.ioandroid

data class GpsEntry(val dateAndTime: String,val gpsData: String, val label: String) {
    override fun toString(): String {
        return "Date: $dateAndTime\nLocation: $gpsData\nLabel: $label"
    }
}
