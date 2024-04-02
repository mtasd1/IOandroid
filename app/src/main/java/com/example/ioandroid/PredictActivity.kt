package com.example.ioandroid

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.ArrayMap
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import java.text.SimpleDateFormat

class PredictActivity : AppCompatActivity() {
    private lateinit var switchPredict: SwitchCompat
    private lateinit var buttonPredict: Button

    private lateinit var currentLocation: GpsEntry

    private lateinit var locationService: LocationService
    private lateinit var telephoneService: TelephoneService
    private lateinit var wifiService: WifiService
    private lateinit var bluetoothService: BluetoothService

    private val handler = Handler(Looper.getMainLooper())
    private val delay = 2000L
    private val runnable = object : Runnable {
        override fun run() {
            trackLocation()
            handler.postDelayed(this, delay)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_predict) // You need to create this layout file


        switchPredict = findViewById(R.id.switchPredict)
        buttonPredict = findViewById(R.id.btnTrack)

        switchPredict.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                startActivity(Intent(this, MainActivity::class.java))
            }
        }

        buttonPredict.setOnClickListener {
            trackLocation()

            // Update the UI with the current location
            findViewById<TextView>(R.id.locationTextView).text = currentLocation.toString()
        }

        // get the location from the intent, there is already a function implemented in the MainActivity
        locationService = LocationManagerService(this)
        locationService.getLocation() // call the function when creating to initialize the Location

        bluetoothService = BluetoothService(this)
        bluetoothService.startDiscovery()

        wifiService = WifiService(this)
        wifiService.enableWifi()

        telephoneService = TelephoneService(this)


        // Load your machine learning model
        // val model = ...

        // Get the current location
        // val location = ...

        // Make a prediction based on the location
        // val prediction = model.predict(location)

        // Display the prediction
        // findViewById<TextView>(R.id.predictionTextView).text = prediction.toString()
    }

    fun trackLocation() {
        val gpsData = locationService.getLocation()

        val selectedLocation = ""
        val selectedDescription = ""
        val selectedPeople = ""
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

        currentLocation = GpsEntry(
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