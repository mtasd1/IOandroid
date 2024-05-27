package com.example.ioandroid

import android.os.Bundle
import android.util.ArrayMap
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat

class TrackService(private val appCompatActivity: AppCompatActivity, private val isPredict: Boolean) {
    private lateinit var locationService: LocationService
    private lateinit var telephoneService: TelephoneService
    private lateinit var wifiService: WifiService
    private lateinit var bluetoothService: BluetoothService

    fun startService() {
        locationService = LocationManagerService(appCompatActivity, isPredict)
        locationService.getLocation()

        bluetoothService = BluetoothService(appCompatActivity)
        bluetoothService.startDiscovery()

        wifiService = WifiService(appCompatActivity)
        wifiService.enableWifi()

        telephoneService = TelephoneService(appCompatActivity)
    }

    fun getGpsEntry(
        selectedLocation: String,
        selectedDescription: String,
        selectedPeople: String
    ): GpsEntry {
        val gpsData = locationService.getLocation()

        val dateFormat = SimpleDateFormat("HH:mm:ss")

        val cellStrength = telephoneService.getSignalStrength()

        val timeStampNetwork = gpsData.first?.time?.let { dateFormat.format(it) } ?: "N/A"
        val latitudeNetwork = gpsData.first?.latitude ?: 0.0
        val longitudeNetwork = gpsData.first?.longitude ?: 0.0

        val timeStampGPS = gpsData.second?.time?.let { dateFormat.format(it) } ?: "N/A"
        val latitudeGPS = gpsData.second?.latitude ?: 0.0
        val longitudeGPS = gpsData.second?.longitude ?: 0.0

        val gpsExtras = bundleToMap(gpsData.second?.extras ?: Bundle())
        val satellites = locationService.getSatelliteInfoJSON()
        val nrSatellitesInFix = gpsExtras["satellites"] ?: satellites.length()
        val nrSatellitesInView = (locationService as LocationManagerService).getSatellitesInView()

        bluetoothService.startDiscovery()
        val blDevices = bluetoothService.getDevicesJSON()

        val wifiNetworks = wifiService.getWifiNetworksJSON()

        return GpsEntry(
            selectedLocation,
            selectedDescription,
            selectedPeople,
            cellStrength,
            timeStampNetwork,
            latitudeNetwork,
            longitudeNetwork,
            timeStampGPS,
            latitudeGPS,
            longitudeGPS,
            nrSatellitesInView,
            nrSatellitesInFix,
            satellites.toString(),
            blDevices.toString(),
            wifiNetworks.toString()
        )
    }

    fun stopService() {
        bluetoothService.stopDiscovery()
        locationService.stopLocationUpdates()
    }

    fun bundleToMap(bundle: Bundle): ArrayMap<String, Any> {
        val map = ArrayMap<String, Any>()
        for (key in bundle.keySet()) {
            val value = bundle.get(key)
            if (value is Bundle) {
                map[key] = bundleToMap(value)
            } else {
                map[key] = value!!
            }
        }
        return map
    }
}