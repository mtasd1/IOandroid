package com.example.ioandroid

data class GpsEntry(
    val label: String, val locationDescription: String, val people: String, val cellStrength: Int, val timeStampNetwork: String, val latitudeNetwork: Double, val longitudeNetwork: Double, val timeStampGPS: String, val latitudeGPS: Double, val longitudeGPS: Double, val nrSatellitesInView: Int, val nrSatellitesInFix: Any, val satellites: String, val bluetoothDevices: String, val wifiDevices: String
) {
    override fun toString(): String {
        return "GpsEntry(label='$label', locationDescription='$locationDescription', people='$people', cellStrength=$cellStrength, timeStampNetwork='$timeStampNetwork', latitudeNetwork='$latitudeNetwork', longitudeNetwork='$longitudeNetwork', \n timeStampGPS='$timeStampGPS', latitudeGPS=$latitudeGPS, longitudeGPS=$longitudeGPS, nrSatellitesInView=$nrSatellitesInView, nrSatellitesInFix=$nrSatellitesInFix, \n satellites=$satellites,\n bluetoothDevices=$bluetoothDevices,\n wifiDevices='$wifiDevices')"
    }

    fun toCSV(): String {
        return "$label;$locationDescription;$people;$cellStrength;$timeStampNetwork;$latitudeNetwork;$longitudeNetwork;$timeStampGPS;$latitudeGPS;$longitudeGPS;$nrSatellitesInView; $nrSatellitesInFix;$satellites;$bluetoothDevices;$wifiDevices"
    }
    fun toCSVHeader(): String {
        return "label;locationDescription;people;cellStrength;timeStampNetwork;latitudeNetwork;longitudeNetwork;timeStampGPS;latitudeGPS;longitudeGPS;nrSatellitesInView;nrSatellitesInFix;satellites;bluetoothDevices;wifiDevices"
    }
}
