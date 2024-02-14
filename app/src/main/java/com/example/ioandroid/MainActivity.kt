package com.example.ioandroid

import android.content.ClipData
import android.content.ClipboardManager
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
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
    private lateinit var adapter: ArrayAdapter<GpsEntry>

    // The app will work with the FusedLocationProviderClient or the LocationManager depending on the value of this variable
    private val isFusedLocationProvider = false
    private lateinit var selectedService: LocationService


    private val PREFS_NAME = "MyPrefsFile"

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val spinnerLocation: Spinner = findViewById(R.id.spinnerLocation)
        val btnTrack: Button = findViewById(R.id.btnTrack)
        val listView: ListView = findViewById(R.id.listView)
        val btnDelete: Button = findViewById(R.id.btnDelete)

        // Initialize the selected service
        /*selectedService = if (isFusedLocationProvider) {
            FusedLocationService(this)
        } else {
            LocationManagerService(this)
        }*/
        selectedService = LocationManagerService(this)
        selectedService.getLocation()

        btnTrack.isEnabled = false


        // Adapter for the Spinner
        val labelAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.location_options,
            android.R.layout.simple_spinner_item
        )
        labelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLocation.adapter = labelAdapter

        // Adapter for the ListView
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, gpsEntries)
        listView.adapter = adapter
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

        listView.setOnItemClickListener { _, _, _, _ ->
            btnDelete.isEnabled = true
        }

        listView.setOnItemLongClickListener { parent, _, position, _ ->
            val selectedItem = parent.getItemAtPosition(position) as GpsEntry
            copyToClipboard(selectedItem.toString())
            true
        }

        // Set a listener for the Track button
        btnTrack.setOnClickListener {
            val gpsData = selectedService.getLocation()
            val satellites = selectedService.getSatelliteInfo()
            if (gpsData.first == null && gpsData.second == null) {
                Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selectedLocation = spinnerLocation.selectedItem.toString()
            val dateFormat = SimpleDateFormat("HH:mm:ss")
            Toast.makeText(this, "vAccuracy: ${gpsData.first?.verticalAccuracyMeters} ", Toast.LENGTH_SHORT).show()
            val gpsEntry = GpsEntry(selectedLocation, gpsData.first?.time?.let { dateFormat.format(it) } ?: "N/A", gpsData.first.toString(), gpsData.second?.time?.let { dateFormat.format(it) } ?: "N/A", gpsData.second.toString(), satellites)
            gpsEntries.add(gpsEntry)
            adapter.notifyDataSetChanged()
            saveEntriesToSharedPreferences()
        }

        // Set a listener for the Delete button
        btnDelete.setOnClickListener {

            val selectedPosition = listView.checkedItemPosition
            if (selectedPosition != AdapterView.INVALID_POSITION) {
                gpsEntries.removeAt(selectedPosition)
                adapter.notifyDataSetChanged()
                if(gpsEntries.isEmpty()) btnDelete.isEnabled = false

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

}
