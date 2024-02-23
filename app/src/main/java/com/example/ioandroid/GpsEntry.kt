package com.example.ioandroid

data class GpsEntry(val label: String, val cellStrength: String, val firstTime: String, val locationDataNetwork: String, val networkExtras: String, val secondTime: String, val locationDataGPS: String, val gpsExtras: String, val satellites: String, val bluetoothDevices: String, val wifiDevices: String, val nrWifiDevices: Int
) {
    override fun toString(): String {
        return "Label: $label\n cell Strength: $cellStrength \n $firstTime NETWORK_PROVIDER: $locationDataNetwork\n Network extras: $networkExtras \n \n $secondTime GPS_PROVIDER: $locationDataGPS\n GPS Extras: $gpsExtras\nSatellites: $satellites\n\nBluetooth Devices: $bluetoothDevices\n $nrWifiDevices WiFi Networks: $wifiDevices\n"
    }
}
