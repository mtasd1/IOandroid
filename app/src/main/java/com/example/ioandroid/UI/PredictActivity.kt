package com.example.ioandroid.UI

import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
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
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.ioandroid.R
import com.example.ioandroid.models.DataEntry
import com.example.ioandroid.services.PredictService
import com.example.ioandroid.services.TrackService
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class PredictActivity : AppCompatActivity() {
    private lateinit var switchPredict: SwitchCompat
    private lateinit var buttonPredict: Button
    private lateinit var intervalSpinner: Spinner
    private lateinit var stopButton: Button

    private var interval: Long = 0
    private var csvFile: File? = null
    private var predictionJob: Job? = null
    private var isPredicting = false

    private lateinit var handler: Handler
    private var runnable: Runnable? = null

    private lateinit var currentLocation: DataEntry

    private lateinit var trackService: TrackService
    private lateinit var predictService: PredictService

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val nWarmUp = 3
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_predict)


        switchPredict = findViewById(R.id.switchPredict)
        buttonPredict = findViewById(R.id.btnTrack)
        intervalSpinner = findViewById<Spinner>(R.id.interval_spinner)
        stopButton = findViewById<Button>(R.id.stop_button)

        handler = Handler()

        val intervals = arrayOf("-", "1", "2", "5", "10")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, intervals)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        intervalSpinner.adapter = adapter

        intervalSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selected = intervals[position]
                interval = if (selected == "-") 0 else selected.toLong() * 1000
                stopButton.isEnabled = isPredicting
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        switchPredict.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                startActivity(Intent(this, CollectActivity::class.java))
            }
        }

        trackService = TrackService(this, true)
        trackService.startService(nWarmUp)

        predictService = PredictService(this, this)
        predictService.startService()

        buttonPredict.setOnClickListener {
            //after refactor, we should only call function to get data and function to get prediction and to update the UI chart

            if(interval == 0L) {
                coroutineScope.launch(Dispatchers.IO) {
                performPrediction()
                }
            } else {
                startIntervalPrediction()
            }

            /* coroutineScope.launch(Dispatchers.IO) {
                var dataEntry = trackLocation()
                val file = writeEntryToFile(dataEntry)
                val predictionRfc = predictService.predictWithRFC(file)
                val predictionLstm = predictService.predictWithLSTM(file)

                withContext(Dispatchers.Main) {
                    updateBarChart(findViewById(R.id.barChartRFC), predictionRfc, "RFC")
                    updateBarChart(findViewById(R.id.barChartLSTM), predictionLstm, "LSTM")
                }
            } */
        }

        stopButton.setOnClickListener {
            stopIntervalPrediction()
            stopButton.isEnabled = false
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        trackService.stopService()
        predictService.stopService()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private suspend fun trackLocation(): DataEntry {
         return withContext(Dispatchers.Main) {
            trackService.getDataEntry("not", "relevant", "anymore")
        }
    }

    private fun writeEntryToFile(dataEntry: DataEntry, file: File): File {
        file.writeText(dataEntry.toCSVHeader())
        file.appendText("\n")
        file.appendText(dataEntry.toCSV())
        return file
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun startIntervalPrediction() {
        if (isPredicting) return
        isPredicting = true
        stopButton.isEnabled = true

        csvFile = File(filesDir, "predictions.csv")
        csvFile?.writeText("timestamp,predictionRFC_Indoor,predictionRFC_Outdoor,predictionLSTM_Indoor,predictionLSTM_Outdoor\n")

        handler = Handler(Looper.getMainLooper())
        predictionJob = coroutineScope.launch(Dispatchers.Main) {
            while (isActive) {
                val predictions = performPrediction()
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val predictionRFC = predictions.first
                val predictionLSTM = predictions.second
                Toast.makeText(this@PredictActivity, "Predictions: ${predictionRFC.joinToString(",")}", Toast.LENGTH_SHORT).show()

                try {
                    FileWriter(csvFile, true).use { writer ->
                        writer.append("$timestamp,${predictionRFC.joinToString(",")},${predictionLSTM.joinToString(",")}\n")
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                delay(interval)
            }
        }
    }

    private fun stopIntervalPrediction() {
        if (!isPredicting) return
        isPredicting = false
        stopButton.isEnabled = false

        predictionJob?.cancel()
        csvFile?.let {
            exportCSV(it)
            Toast.makeText(this, "Predictions exported to CSV file!", Toast.LENGTH_LONG).show()
        } ?: run {
            Toast.makeText(this, "No predictions to export.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportCSV(sourceFile: File) {
        val resolver = contentResolver
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "predictions_$timestamp.csv")
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
        }

        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

        uri?.let {
            resolver.openOutputStream(it).use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    sourceFile.forEachLine { line ->
                        writer.write(line)
                        writer.write("\n")
                    }
                }
            }
        } ?: run {
            Toast.makeText(this, "Failed to export CSV: URI is null", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private suspend fun performPrediction(): Pair<FloatArray, FloatArray> {
        val predictions = coroutineScope.async(Dispatchers.IO) {
            val dataEntry = trackLocation()
            var locationFile = File.createTempFile("gpsEntry", ".csv", cacheDir)
            locationFile = writeEntryToFile(dataEntry, locationFile)
            val predictionRfc = predictService.predictWithRFC(locationFile)
            val predictionLstm = predictService.predictWithLSTM(locationFile)

            Pair(predictionRfc, predictionLstm)
        }.await()

        withContext(Dispatchers.Main) {
            updateBarChart(findViewById(R.id.barChartRFC), predictions.first, "RFC")
            updateBarChart(findViewById(R.id.barChartLSTM), predictions.second, "LSTM")
        }

        return predictions
    }

    private fun updateBarChart(chart: HorizontalBarChart, data: FloatArray, chartLabel: String) {
        val entries = data.mapIndexed { index, fl ->
            BarEntry(index.toFloat(), fl)
        }
        val dataSet = BarDataSet(entries, chartLabel)
        dataSet.color = ColorTemplate.getHoloBlue()
        dataSet.valueTextSize = 16f

        dataSet.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${"%.2f".format(value * 100)}%"
            }
        }
        dataSet.setDrawValues(true)
        dataSet.valueTextColor = Color.BLACK

        val barData = BarData(dataSet)
        barData.barWidth = 0.1f

        chart.data = barData
        chart.description.isEnabled = false
        chart.setFitBars(true)
        chart.animateY(200)

        // Customize the x-axis
        val labels = arrayOf("Indoor", "Outdoor")

        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.setDrawAxisLine(false)
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM_INSIDE
        chart.xAxis.labelRotationAngle = 270f
        chart.xAxis.granularity = 1f
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chart.xAxis.textSize = 16f

        // y-axis should always be between 0 and 1
        chart.axisLeft.axisMinimum = 0f
        chart.axisLeft.axisMaximum = 1f
        chart.axisRight.axisMinimum = 0f
        chart.axisRight.axisMaximum = 1f
        chart.axisLeft.maxWidth = 1f
        chart.axisLeft.minWidth = 1f
        chart.axisRight.maxWidth = 1f
        chart.axisRight.minWidth = 1f



        // Remove background color and border lines
        chart.setDrawBorders(false)  // No borders
        chart.setDrawGridBackground(false)  // No grid background

        // Set extra offsets
        chart.setExtraOffsets(0f, 0f, 70f, 0f)  // The third argument is the extra offset on the right side

        chart.legend.isEnabled = false

        // Zoom and view settings
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)

        chart.notifyDataSetChanged()  // Notify the chart that data has changed
        chart.invalidate() // Refresh the chart
    }

}