package com.example.ioandroid.UI

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.ioandroid.R
import com.example.ioandroid.models.GpsEntry
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class PredictActivity : AppCompatActivity() {
    private lateinit var switchPredict: SwitchCompat
    private lateinit var buttonPredict: Button

    private lateinit var currentLocation: GpsEntry

    private lateinit var trackService: TrackService
    private lateinit var predictService: PredictService

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val nWarmUp = 3
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_predict)


        switchPredict = findViewById(R.id.switchPredict)
        buttonPredict = findViewById(R.id.btnTrack)


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

            coroutineScope.launch(Dispatchers.IO) {
                var gpsEntry = trackLocation()
                val file = writeEntryToFile(gpsEntry)
                val predictionRfc = predictService.predictWithRFC(file)
                val predictionLstm = predictService.predictWithLSTM(file)

                withContext(Dispatchers.Main) {
                    updateBarChart(findViewById(R.id.barChartRFC), predictionRfc, "RFC")
                    updateBarChart(findViewById(R.id.barChartLSTM), predictionLstm, "LSTM")
                }
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        trackService.stopService()
    }

    private suspend fun trackLocation(): GpsEntry {
         return withContext(Dispatchers.Main) {
            trackService.getGpsEntry("not", "relevant", "anymore")
        }
    }

    private fun writeEntryToFile(gpsEntry: GpsEntry): File {
        val file = File.createTempFile("gpsEntry", ".csv", cacheDir)
        file.writeText(gpsEntry.toCSVHeader())
        file.appendText("\n")
        file.appendText(gpsEntry.toCSV())
        return file
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