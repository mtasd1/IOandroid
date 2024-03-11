package com.example.ioandroid

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.ArrayMap
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ExpandableListView
import android.widget.ListView
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity() {

    private val gpsEntries = mutableListOf<GpsEntry>()
    //private lateinit var adapter: ArrayAdapter<GpsEntry>
    private lateinit var adapter: ExpandableListAdapter;

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
    private var isTracking = false

    private val PREFS_NAME = "MyPrefsFile"


    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val spinnerLocation: Spinner = findViewById(R.id.spinnerLocation)
        val btnTrack: Button = findViewById(R.id.btnTrack)
        val btnStopTrack: Button = findViewById(R.id.btnStopTrack)
        val listView: ExpandableListView = findViewById(R.id.expandableListView)
        val btnDelete: Button = findViewById(R.id.btnDelete)
        val btnDeleteAll: Button = findViewById(R.id.btnDeleteAll)
        val btnExport: Button = findViewById(R.id.btnExport)
        var selectedGroupPosition = AdapterView.INVALID_POSITION



        locationService = LocationManagerService(this)
        locationService.getLocation() // call the function when creating to initialize the Location

        bluetoothService = BluetoothService(this)
        bluetoothService.startDiscovery()

        wifiService = WifiService(this)
        wifiService.enableWifi()

        telephoneService = TelephoneService(this)

        btnTrack.isEnabled = false
        btnStopTrack.isEnabled = false
        btnDelete.isEnabled = false


        // Adapter for the Spinner
        val labelAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.location_options,
            android.R.layout.simple_spinner_item
        )
        labelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLocation.adapter = labelAdapter

        // Adapter for the ListView
        adapter = ExpandableListAdapter(this, gpsEntries)
        listView.setAdapter(adapter)
        listView.choiceMode = ListView.CHOICE_MODE_SINGLE

        // Load saved entries from SharedPreferences
        loadEntriesFromSharedPreferences()

        // Set a listener to handle the item selection in the Spinner
        spinnerLocation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long) {
                btnDelete.isEnabled = true
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                btnDelete.isEnabled = false
            }
        }

        listView.setOnGroupClickListener { _, _, groupPosition, _ ->
            selectedGroupPosition = groupPosition
            btnDelete.isEnabled = true
            false
        }

        listView.setOnItemLongClickListener { parent, _, position, _ ->
            val selectedItem = parent.getItemAtPosition(position) as GpsEntry
            copyToClipboard(selectedItem.toString())
            true
        }

        // Set a listener for the Track button
        btnTrack.setOnClickListener {
            // Label, cellStrength, cellType, timeStampNetwork, hAccuracyNetwork, vAccuracyNetwork, networkLocationType, timeStampGPS, hAccuracyGPS, vAccuracyGPS, nrSatellites, top7 Satellites, bluetoothDevices, wifiNetworks, nrWifiNetworks
            handler.post(runnable) // start the tracking
            isTracking = true
            btnStopTrack.isEnabled = true
        }

        btnStopTrack.setOnClickListener {
            handler.removeCallbacks(runnable) // stop the tracking
            isTracking = false
            btnStopTrack.isEnabled = false
        }

        // Set a listener for the Delete button
        btnDelete.setOnClickListener {
            Toast.makeText(this, "Pos: $selectedGroupPosition", Toast.LENGTH_SHORT).show()
            if (selectedGroupPosition != AdapterView.INVALID_POSITION) {
                adapter.removeGroup(selectedGroupPosition)
                if (gpsEntries.isEmpty()) btnDelete.isEnabled = false
                saveEntriesToSharedPreferences()
            } else {
                Toast.makeText(this, "No entry selected", Toast.LENGTH_SHORT).show()
            }
        }

        // Set a listener for the Delete All button
        btnDeleteAll.setOnClickListener {
            gpsEntries.clear()
            adapter.notifyDataSetChanged()
            saveEntriesToSharedPreferences()
        }

        // Set a listener for the Export button
        btnExport.setOnClickListener {
            gpsEntries.clear()
            val entries = loadEntriesFromSharedPreferences()
            exportData(entries)
            Toast.makeText(this, "Exported to CSV", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        locationService.stopLocationUpdates()
        bluetoothService.stopDiscovery()
    }

    private fun trackLocation() {
        val gpsData = locationService.getLocation()
        if (gpsData.first == null && gpsData.second == null) {
            Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
            return
        }
        val selectedLocation = getSelectedLocation()
        val dateFormat = SimpleDateFormat("HH:mm:ss")

        val cellStrength = telephoneService.getSignalStrength()
        val cellType = telephoneService.getNetworkType(this)

        val timeStampNetwork = gpsData.first?.time?.let { dateFormat.format(it) } ?: "N/A"
        val hAccuracyNetwork = gpsData.first?.accuracy ?: 0.0f
        val vAccuracyNetwork = gpsData.first?.verticalAccuracyMeters ?: 0.0f
        val bAccuracyNetwork = gpsData.first?.bearingAccuracyDegrees ?: 0.0f //probably not needed for our model
        val speedAccuracyNetwork = gpsData.first?.speedAccuracyMetersPerSecond ?: 0.0f //probably not needed for our model
        val networkExtras = bundleToMap(gpsData.first?.extras ?: Bundle())
        val networkLocationType = networkExtras["networkLocationType"] ?: "N/A"

        val timeStampGPS = gpsData.second?.time?.let { dateFormat.format(it) } ?: "N/A"
        val hAccuracyGPS = gpsData.second?.accuracy ?: 0.0f
        val vAccuracyGPS = gpsData.second?.verticalAccuracyMeters ?: 0.0f
        val bAccuracyGPS = gpsData.second?.bearingAccuracyDegrees ?: 0.0f //probably not needed for our model
        val speedAccuracyGPS = gpsData.second?.speedAccuracyMetersPerSecond ?: 0.0f //probably not needed for our model

        val gpsExtras = bundleToMap(gpsData.second?.extras ?: Bundle())
        val satellites = locationService.getSatelliteInfoJSON()
        val nrSatellitesInFix = gpsExtras["satellites"] ?: satellites.length()
        val nrSatellitesInView = (locationService as LocationManagerService).getSatellitesInView()
        val minCn0GPS = (locationService as LocationManagerService).getMinCn0()
        val meanCn0GPS = gpsExtras["meanCn0"] ?: 0.0f
        val maxCn0GPS = gpsExtras["maxCn0"] ?: 0.0f
        // if needed we will also calculate the mean and max Cn0 of the top 7 satellites


        bluetoothService.startDiscovery()
        val blDevices = bluetoothService.getDevicesJSON()

        var nrBlDevices = bluetoothService.getNrDevices()
        var minCn0Bl = bluetoothService.getMinCn0()
        var meanCn0Bl = bluetoothService.getMeanCn0()
        var maxCn0Bl = bluetoothService.getMaxCn0()

        val wifiNetworks = wifiService.getWifiNetworksJSON()
        var nrWifiDevices = wifiNetworks.length()
        var minCn0Wifi = wifiService.getMinCn0(wifiNetworks)
        var meanCn0Wifi = wifiService.getMeanCn0(wifiNetworks)
        var maxCn0Wifi = wifiService.getMaxCn0(wifiNetworks)


        val gpsEntry = GpsEntry(
            selectedLocation,
            cellStrength,
            cellType,
            timeStampNetwork,
            hAccuracyNetwork,
            vAccuracyNetwork,
            bAccuracyNetwork,
            speedAccuracyNetwork,
            networkLocationType,
            timeStampGPS,
            hAccuracyGPS,
            vAccuracyGPS,
            bAccuracyGPS,
            speedAccuracyGPS,
            nrSatellitesInView,
            nrSatellitesInFix,
            minCn0GPS,
            meanCn0GPS,
            maxCn0GPS,
            satellites.toString(),
            nrBlDevices,
            minCn0Bl,
            meanCn0Bl,
            maxCn0Bl,
            blDevices.toString(),
            nrWifiDevices,
            minCn0Wifi,
            meanCn0Wifi,
            maxCn0Wifi,
            wifiNetworks.toString()
        )
        gpsEntries.add(gpsEntry)
        adapter.notifyDataSetChanged()
        saveEntriesToSharedPreferences()
    }

    fun getSelectedLocation(): String {
        return findViewById<Spinner>(R.id.spinnerLocation).selectedItem.toString()
    }

    private fun saveEntriesToSharedPreferences() {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor: SharedPreferences.Editor = prefs.edit()
        val gson = Gson()
        val json = gson.toJson(gpsEntries)
        editor.putString("entries", json)
        editor.apply()
    }

    private fun loadEntriesFromSharedPreferences(): List<GpsEntry> {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString("entries", "")
        val type = object : TypeToken<ArrayList<GpsEntry>>() {}.type
        val savedEntries: List<GpsEntry> = gson.fromJson(json, type) ?: return emptyList()
        gpsEntries.addAll(savedEntries)
        adapter.notifyDataSetChanged()
        return savedEntries
    }

    private fun clearSharedPreferences() {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }

    private fun exportData(entries: List<GpsEntry>) {
        val resolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "${getSelectedLocation()}_${formatTimestamp(System.currentTimeMillis())}.csv")
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
        }

        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)


        resolver.openOutputStream(uri!!).use { outputStream ->
            OutputStreamWriter(outputStream).use { writer ->
                val header = entries[0].toCSVHeader()
                writer.write(header)
                writer.write("\n") // New line for each entry

                entries.forEach { entry ->
                    writer.write(entry.toCSV())
                    writer.write("\n") // New line for each entry
                }
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("label", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
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

    fun formatTimestamp(timestamp: Long): String {
        //day and time
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return dateFormat.format(timestamp)
    }
}
