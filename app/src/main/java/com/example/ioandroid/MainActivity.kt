package com.example.ioandroid

import android.content.ClipData
import android.content.ClipboardManager
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
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
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity() {

    private val gpsEntries = mutableListOf<GpsEntry>()
    //private lateinit var adapter: ArrayAdapter<GpsEntry>
    private lateinit var adapter: ExpandableListAdapter;

    private lateinit var locationService: LocationService
    private lateinit var telephoneService: TelephoneService
    private lateinit var wifiService: WifiService
    private lateinit var bluetoothService: BluetoothService

    private val PREFS_NAME = "MyPrefsFile"

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val spinnerLocation: Spinner = findViewById(R.id.spinnerLocation)
        val btnTrack: Button = findViewById(R.id.btnTrack)
        val listView: ExpandableListView = findViewById(R.id.expandableListView)
        val btnDelete: Button = findViewById(R.id.btnDelete)
        var selectedGroupPosition = AdapterView.INVALID_POSITION



        locationService = LocationManagerService(this)
        locationService.getLocation() // call the function when creating to initialize the Location

        bluetoothService = BluetoothService(this)
        bluetoothService.startDiscovery()

        wifiService = WifiService(this)
        wifiService.enableWifi()

        telephoneService = TelephoneService(this)

        btnTrack.isEnabled = false
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

            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                btnTrack.isEnabled = false
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
            val gpsData = locationService.getLocation()
            val satellites = locationService.getSatelliteInfo()
            if (gpsData.first == null && gpsData.second == null) {
                Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selectedLocation = spinnerLocation.selectedItem.toString()
            val dateFormat = SimpleDateFormat("HH:mm:ss")

            bluetoothService.startDiscovery()
            val blDevices = bluetoothService.getDevices()

            val wifiNetworks = wifiService.getWifiNetworks()

            val cellStrength = telephoneService.getSignalStrength()
            val cellType = telephoneService.getNetworkType(this)
            //Toast.makeText(this, "Bluetooth devices: ${bluetoothService.getDevices()}", Toast.LENGTH_SHORT).show()
            val networkExtras = bundleToMap(gpsData.first?.extras ?: Bundle())
            val gpsExtras = bundleToMap(gpsData.second?.extras ?: Bundle())

            val gpsEntry = GpsEntry(selectedLocation, cellStrength.toString(),  gpsData.first?.time?.let { dateFormat.format(it) } ?: "N/A", gpsData.first.toString(), networkExtras.toString(), gpsData.second?.time?.let { dateFormat.format(it) } ?: "N/A", gpsData.second.toString(), gpsExtras.toString(),satellites, blDevices.toString(), wifiNetworks.toString(), wifiNetworks.size)
            gpsEntries.add(gpsEntry)
            adapter.notifyDataSetChanged()
            saveEntriesToSharedPreferences()
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
    }


    private fun saveEntriesToSharedPreferences() {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor: SharedPreferences.Editor = prefs.edit()
        val gson = Gson()
        val json = gson.toJson(gpsEntries)
        editor.putString("entries", json)
        editor.apply()
    }

    private fun loadEntriesFromSharedPreferences() {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString("entries", "")
        val type = object : TypeToken<ArrayList<GpsEntry>>() {}.type
        val savedEntries: List<GpsEntry> = gson.fromJson(json, type) ?: return
        gpsEntries.addAll(savedEntries)
        adapter.notifyDataSetChanged()
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
}
