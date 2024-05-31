package com.example.ioandroid.UI

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.ioandroid.R
import com.example.ioandroid.models.GpsEntry
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
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel


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
    private val py = Python.getInstance()
    private val module = py.getModule("script")

    // Variables for the LSTM model
    private lateinit var interpreter: Interpreter
    private var inputSize = 0
    private var outputSize = 0
    private lateinit var input : ByteBuffer
    private lateinit var output : ByteBuffer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_predict) // You need to create this layout file


        switchPredict = findViewById(R.id.switchPredict)
        buttonPredict = findViewById(R.id.btnTrack)

        coroutineScope.launch(Dispatchers.Default) {
            if(!Python.isStarted()) {
                Python.start(AndroidPlatform(this@PredictActivity))
            }
        }


        switchPredict.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                startActivity(Intent(this, MainActivity::class.java))
            }
        }

        // Initialize the LSTM model
        val options = Interpreter.Options()
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            options.useNNAPI = true
        } else {
            options.useNNAPI = false
        }*/
        options.useNNAPI = false

        interpreter = Interpreter(loadModelFile(this, "lstm_classifier.tflite"))
        inputSize = interpreter.getInputTensor(0).shape().size
        outputSize = interpreter.getOutputTensor(0).shape().size
        input = ByteBuffer.allocateDirect(4 * inputSize)
        output = ByteBuffer.allocateDirect(4 * outputSize)

        buttonPredict.setOnClickListener {

            coroutineScope.launch {
                var gpsEntry = withContext(Dispatchers.Main) {
                    trackLocation()
                }

                val file = File.createTempFile("gpsEntry", ".csv", cacheDir)
                file.writeText(gpsEntry.toCSVHeader())
                file.appendText("\n")
                file.appendText(gpsEntry.toCSV())

                val preprocess = preprocessSingleEntry(file)
                val preprocessLSTM = preprocessLSTM(file)

                coroutineScope.launch(Dispatchers.IO) {
                    val predictionRfc = predictWithRFC(preprocess)
                    val predictionLstm = predictWithLSTM(interpreter, preprocessLSTM)

                    withContext(Dispatchers.Main) {
                        updateBarChart(findViewById(R.id.barChartRFC), predictionRfc, "RFC")
                        updateBarChart(findViewById(R.id.barChartLSTM), predictionLstm, "LSTM")
                    }
                }
            }
        }

        trackService = TrackService(this, true)
        trackService.startService()

        // fetch the current location three times for warm-up immediately after the service is started
        coroutineScope.launch {
            repeat(3) {
                currentLocation = trackLocation()
            }
        }
    }

    private suspend fun trackLocation(): GpsEntry {
         return withContext(Dispatchers.Main) {
            trackService.getGpsEntry("not", "relevant", "anymore")
        }
    }

    private fun preprocessSingleEntry(file: File): PyObject {
        return module.callAttr("preprocess_single_entry", file.absolutePath)
    }

    private fun preprocessLSTM(file: File): PyObject {
        return module.callAttr("preprocess_lstm", file.absolutePath)
    }

    private fun predictWithRFC(data: PyObject): FloatArray {
        val prediction = module.callAttr("predict_rfc", data)
        val list = prediction.asList()  // Converts PyObject containing a Python list to List<PyObject>
        return list.map { it.asList() }  // Assuming prediction is a list of lists
            .flatten()  // Flatten the list of lists to a single list
            .map { it.toFloat() }  // Convert each PyObject to Float
            .toFloatArray()  // Convert the List<Float> to FloatArray
    }

    private fun predictWithLSTM(interpreter: Interpreter, data: PyObject): FloatArray {
        // Clear all buffers
        clearBuffers()

        // Convert PyObject to 1D array of floats
        val data1D = data.toJava(Array<Float>::class.java).map { it }.toFloatArray()

        // Ensure the ByteBuffer can hold all the data
        input = ByteBuffer.allocateDirect(4 * data1D.size)
        input.order(ByteOrder.LITTLE_ENDIAN) // Set the byte order of input buffer to little-endian

        // Load data into input buffer
        input.rewind()
        for (value in data1D) {
            input.putFloat(value)
        }
        input.rewind()

        // Run the interpreter
        interpreter.run(input, output)

        // Extract the prediction
        output.rewind()
        output.order(ByteOrder.LITTLE_ENDIAN) // Set the byte order to little-endian


        return FloatArray(outputSize) { output.float }
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
        val labels = arrayOf("Indoor", "Outdoor")  // Your custom labels

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

    /** Memory-map the model file in Assets.  */
    @Throws(IOException::class)
    private fun loadModelFile(activity: Activity, filename: String): ByteBuffer {
        val fileDescriptor = activity.assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun clearBuffers() {
        input.clear()
        output.clear()
    }
}