package com.example.ioandroid

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.core.app.ActivityCompat
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
                context as MainActivity,
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
                context as MainActivity,
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

    fun isWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }

    fun enableWifi() {
        if (!wifiManager.isWifiEnabled) {
            wifiManager.isWifiEnabled = true
        }
    }

    fun disableWifi() {
        if (wifiManager.isWifiEnabled) {
            wifiManager.isWifiEnabled = false
        }
    }

    fun getMinCn0(JSON: JSONArray): Int {
        var minCn0 = 0
        for (i in 0 until JSON.length()) {
            val wifiNetwork = JSON.getJSONObject(i)
            if (wifiNetwork.getInt("level") < minCn0) {
                minCn0 = wifiNetwork.getInt("level")
            }
        }
        return minCn0
    }

    fun getMeanCn0(JSON: JSONArray): Float {
        var meanCn0 = 0.0f
        for (i in 0 until JSON.length()) {
            val wifiNetwork = JSON.getJSONObject(i)
            meanCn0 += wifiNetwork.getInt("level")
        }
        meanCn0 /= JSON.length()
        return meanCn0
    }

    fun getMaxCn0(JSON: JSONArray): Int {
        var maxCn0 = -100
        for (i in 0 until JSON.length()) {
            val wifiNetwork = JSON.getJSONObject(i)
            if (wifiNetwork.getInt("level") > maxCn0) {
                maxCn0 = wifiNetwork.getInt("level")
            }
        }
        return maxCn0
    }
}