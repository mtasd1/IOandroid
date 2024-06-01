package com.example.ioandroid.services.sensorServices

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.core.app.ActivityCompat
import com.example.ioandroid.UI.CollectActivity
import org.json.JSONArray
import org.json.JSONObject

class WifiService(private val context: Context) {
    private val wifiManager: WifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun getWifiNetworks(): List<ScanResult> {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context as CollectActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_WIFI_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context as CollectActivity,
                arrayOf(Manifest.permission.ACCESS_WIFI_STATE),
                1
            )
        }

        return wifiManager.scanResults
    }

    fun getWifiNetworksJSON(): JSONArray {
        val wifiNetworksJSON = JSONArray()
        val wifiNetworks = getWifiNetworks()
        for (i in 0 until wifiNetworks.size) {
            val wifiNetwork = JSONObject()
            wifiNetwork.put("SSID", wifiNetworks[i].SSID)
            wifiNetwork.put("level", wifiNetworks[i].level)
            wifiNetworksJSON.put(wifiNetwork)
        }
        return wifiNetworksJSON
    }

    fun enableWifi() {
        if (!wifiManager.isWifiEnabled) {
            wifiManager.isWifiEnabled = true
        }
    }
}