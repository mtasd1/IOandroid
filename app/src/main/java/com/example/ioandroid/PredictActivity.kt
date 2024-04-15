package com.example.ioandroid

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PredictActivity : AppCompatActivity() {
    private lateinit var switchPredict: SwitchCompat
    private lateinit var buttonPredict: Button

    private lateinit var currentLocation: GpsEntry

    private lateinit var trackService: TrackService

    /*private val handler = Handler(Looper.getMainLooper())
    private val delay = 2000L
    private val runnable = object : Runnable {
        override fun run() {
            trackLocation()
            handler.postDelayed(this, delay)
        }
    }*/

    private val coroutineScope = CoroutineScope(Dispatchers.Main)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_predict) // You need to create this layout file


        switchPredict = findViewById(R.id.switchPredict)
        buttonPredict = findViewById(R.id.btnTrack)

        val py = Python.getInstance()
        val module = py.getModule("script")

        coroutineScope.launch(Dispatchers.Default) {
            if(!Python.isStarted()) {
                Python.start(AndroidPlatform(this@PredictActivity))
            }

            val result = module.callAttr("double", 21)
            println(result)
        }


        switchPredict.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                startActivity(Intent(this, MainActivity::class.java))
            }
        }

        buttonPredict.setOnClickListener {
            coroutineScope.launch {
                var gpsEntry = withContext(Dispatchers.Main) {
                    trackLocation()
                }

                val file = File.createTempFile("gpsEntry", ".csv", cacheDir)
                file.writeText(gpsEntry.toCSVHeader())
                file.appendText("\n")
                file.appendText(gpsEntry.toCSV())

                val preprocess = module.callAttr("preprocess_single_entry", file.absolutePath)
                // Update the UI with the current location

                val prediction = module.callAttr("predict_rfc", preprocess)
                findViewById<TextView>(R.id.locationTextView).text = prediction.toString()
                println(preprocess)
                //At this point we have the statistical figures computed and added to the csv file, which still contains the headers


            }
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

    suspend fun trackLocation(): GpsEntry {
         return withContext(Dispatchers.Main) {
            trackService.getGpsEntry("not", "relevant", "anymore")
        }
    }


}