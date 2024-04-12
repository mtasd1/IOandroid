package com.example.ioandroid

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class PredictActivity : AppCompatActivity() {
    private lateinit var switchPredict: SwitchCompat
    private lateinit var buttonPredict: Button

    private lateinit var currentLocation: GpsEntry

    private lateinit var trackService: TrackService

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

        trackService = TrackService(this)
        trackService.startService()


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
        currentLocation = trackService.getGpsEntry("not", "relevant", "anymore")
    }
}