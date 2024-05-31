package com.example.ioandroid.UI

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
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
import androidx.appcompat.widget.SwitchCompat
import com.example.ioandroid.adapter.ExpandableListAdapter
import com.example.ioandroid.models.GpsEntry
import com.example.ioandroid.R
import com.example.ioandroid.services.TrackService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity() {

    private val gpsEntries = mutableListOf<GpsEntry>()
    private lateinit var adapter: ExpandableListAdapter

    private lateinit var trackService: TrackService

    private val handler = Handler(Looper.getMainLooper())
    private val delay = 2000L
    private val runnable = object : Runnable {
        override fun run() {
            trackLocation()
            handler.postDelayed(this, delay)
        }
    }

    private val PREFS_NAME = "MyPrefsFile"

    private lateinit var switchPredict: SwitchCompat


    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        switchPredict = findViewById(R.id.switchPredict)

        val spinnerLocation: Spinner = findViewById(R.id.spinnerLocation)
        val spinnerDescription: Spinner = findViewById(R.id.spinnerDescription)
        val spinnerPeople: Spinner = findViewById(R.id.spinnerPeople)
        val btnTrack: Button = findViewById(R.id.btnTrack)
        val btnStopTrack: Button = findViewById(R.id.btnStopTrack)
        val listView: ExpandableListView = findViewById(R.id.expandableListView)
        val btnDelete: Button = findViewById(R.id.btnDelete)
        val btnDeleteAll: Button = findViewById(R.id.btnDeleteAll)
        val btnExport: Button = findViewById(R.id.btnExport)
        var selectedGroupPosition = AdapterView.INVALID_POSITION

        switchPredict.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Start the PredictActivity
                val intent = Intent(this, PredictActivity::class.java)
                startActivity(intent)
            } else {
                // Stop the PredictActivity
            }
        }

        trackService = TrackService(this, false)
        trackService.startService()

        btnTrack.isEnabled = false
        btnStopTrack.isEnabled = false
        btnDelete.isEnabled = false


        // Adapter for the Location Spinner
        val labelAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.location_options,
            android.R.layout.simple_spinner_item
        )
        labelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLocation.adapter = labelAdapter

        // Adapter for the Description Spinner
        val descriptionAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.description_options,
            android.R.layout.simple_spinner_item
        )
        descriptionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDescription.adapter = descriptionAdapter

        // Adapter for the People Spinner
        val peopleAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.people_options,
            android.R.layout.simple_spinner_item
        )
        peopleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPeople.adapter = peopleAdapter


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
            handler.post(runnable) // start the tracking
            btnStopTrack.isEnabled = true
        }

        btnStopTrack.setOnClickListener {
            handler.removeCallbacks(runnable) // stop the tracking
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
        trackService.stopService()
    }

    fun trackLocation() {
        val selectedLocation = getSelectedLocation()
        val selectedDescription = getSelectedDescription()
        val selectedPeople = getSelectedPeople()

        val gpsEntry = trackService.getGpsEntry(selectedLocation, selectedDescription, selectedPeople)

        gpsEntries.add(gpsEntry)
        adapter.notifyDataSetChanged()
        saveEntriesToSharedPreferences()
    }

    fun getSelectedLocation(): String {
        return findViewById<Spinner>(R.id.spinnerLocation).selectedItem.toString()
    }

    fun getSelectedDescription(): String {
        return findViewById<Spinner>(R.id.spinnerDescription).selectedItem.toString()
    }

    fun getSelectedPeople(): String {
        return findViewById<Spinner>(R.id.spinnerPeople).selectedItem.toString()
    }

    private fun saveEntriesToSharedPreferences() {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor: SharedPreferences.Editor = prefs.edit()
        val gson = GsonBuilder().serializeSpecialFloatingPointValues().create()
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

    fun formatTimestamp(timestamp: Long): String {
        //day and time
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return dateFormat.format(timestamp)
    }
}
