package com.example.ioandroid

data class GpsEntry(
    val label: String, val cellStrength: Int, val cellType: String, val timeStampNetwork: String, val hAccuracyNetwork: Float, val vAccuracyNetwork: Float, val bAccuracyNetwork: Float, val speedAccuracyNetwork: Float, val networkLocationType: Any, val timeStampGPS: String, val hAccuracyGPS: Float, val vAccuracyGPS: Float, val bAccuracyGPS: Float, val speedAccuracyGPS: Float, val nrSatellitesInView: Int, val nrSatellitesInFix: Any, val minCn0GPS: Any, val meanCn0GPS: Any, val maxCn0GPS: Any, val satellites: String, val nrBlDevices: Int, val minCn0Bl: Int, val meanCn0Bl: Float, val maxCn0Bl: Float, val bluetoothDevices: String, val nrWifiDevices: Int, val minCn0Wifi: Int, val meanCn0Wifi: Float, val maxCn0Wifi: Int, val wifiDevices: String
) {
    override fun toString(): String {
        return "GpsEntry(label='$label', cellStrength=$cellStrength, cellType='$cellType', timeStampNetwork='$timeStampNetwork', hAccuracyNetwork=$hAccuracyNetwork, vAccuracyNetwork=$vAccuracyNetwork, bAccurracyNetwork=$bAccuracyNetwork, speedAccuracyNetwork=$speedAccuracyNetwork, networkLocationType='$networkLocationType', \n timeStampGPS='$timeStampGPS', hAccuracyGPS=$hAccuracyGPS, vAccuracyGPS=$vAccuracyGPS, bAccurracyGPS=$bAccuracyGPS, speedAccuracyGPS=$speedAccuracyGPS, nrSatellitesInView=$nrSatellitesInView, nrSatellitesInFix=$nrSatellitesInFix,  minCn0GPS=$minCn0GPS, meanCn0GPS=$meanCn0GPS, maxCn0GPS=$maxCn0GPS,\n satellites=$satellites,\n nrBlDevices=$nrBlDevices, minCn0Bl=$minCn0Bl, meanCn0Bl=$meanCn0Bl, maxCn0Bl=$maxCn0Bl,\n bluetoothDevices=$bluetoothDevices, nrWifiDevices=$nrWifiDevices, minCn0Wifi=$minCn0Wifi, meanCn0Wifi=$meanCn0Wifi, maxCn0Wifi=$maxCn0Wifi,\n wifiDevices='$wifiDevices')"
    }

    fun toCSV(): String {
        return "$label;$cellStrength;$cellType;$timeStampNetwork;$hAccuracyNetwork;$vAccuracyNetwork;$bAccuracyNetwork;$speedAccuracyNetwork;$networkLocationType;$timeStampGPS;$hAccuracyGPS;$vAccuracyGPS;$bAccuracyGPS;$speedAccuracyGPS;$nrSatellitesInView; $nrSatellitesInFix;$minCn0GPS;$meanCn0GPS;$maxCn0GPS;$satellites;$nrBlDevices;$minCn0Bl;$meanCn0Bl;$maxCn0Bl;$bluetoothDevices;$nrWifiDevices;$minCn0Wifi;$meanCn0Wifi;$maxCn0Wifi;$wifiDevices"
    }
    fun toCSVHeader(): String {
        return "label;cellStrength;cellType;timeStampNetwork;hAccuracyNetwork;vAccuracyNetwork;bAccuracyNetwork;speedAccuracyNetwork;networkLocationType;timeStampGPS;hAccuracyGPS;vAccuracyGPS;bAccuracyGPS;speedAccuracyGPS;nrSatellitesInView;nrSatellitesInFix;minCn0GPS;meanCn0GPS;maxCn0GPS;satellites;nrBlDevices;minCn0Bl;meanCn0Bl;maxCn0Bl;bluetoothDevices;nrWifiDevices;minCn0Wifi;meanCn0Wifi;maxCn0Wifi;wifiDevices"
    }
}
