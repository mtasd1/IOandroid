package com.example.ioandroid.services

import android.os.Build
import android.os.Bundle
import android.util.ArrayMap
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.ioandroid.models.DataEntry
import com.example.ioandroid.services.sensorServices.BluetoothService
import com.example.ioandroid.services.sensorServices.LocationManagerService
import com.example.ioandroid.services.sensorServices.LocationService
import com.example.ioandroid.services.sensorServices.TelephoneService
import com.example.ioandroid.services.sensorServices.WifiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat

class TrackService(private val appCompatActivity: AppCompatActivity, private val isPredict: Boolean) {
    private lateinit var locationService: LocationService
    private lateinit var telephoneService: TelephoneService
    private lateinit var wifiService: WifiService
    private lateinit var bluetoothService: BluetoothService

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    @RequiresApi(Build.VERSION_CODES.S)
    fun startService(nWarmUp: Int) {
        locationService = LocationManagerService(appCompatActivity, isPredict)
        locationService.getLocation()

        bluetoothService = BluetoothService(appCompatActivity)
        bluetoothService.startDiscovery()

        wifiService = WifiService(appCompatActivity)
        wifiService.enableWifi()

        telephoneService = TelephoneService(appCompatActivity)

        // Warm up the services by calling them nWarmUp times
        for (i in 0 until nWarmUp) {
            getDataEntry("not", "relevant", "yet")
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun getDataEntry(
        selectedLocation: String,
        selectedDescription: String,
        selectedPeople: String
    ): DataEntry {
        val gpsData = locationService.getLocation()
        val blDevices = bluetoothService.getDevicesJSON()
        val wifiNetworks = wifiService.getWifiNetworksJSON()
        val cellStrength = telephoneService.getSignalStrength()

        val dateFormat = SimpleDateFormat("HH:mm:ss")


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

        return DataEntry(
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