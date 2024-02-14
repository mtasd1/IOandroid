package com.example.ioandroid

data class GpsEntry(val label: String, val firstTime: String, val locationDataNetwork: String, val secondTime: String, val locationDataGPS: String, val satellites: String
) {
    override fun toString(): String {
        return "Label: $label\n $firstTime NETWORK_PROVIDER: $locationDataNetwork\n $secondTime GPS_PROVIDER: $locationDataGPS\nSatellites: $satellites"
    }
}
