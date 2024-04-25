package com.example.ioandroid

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
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

        buttonPredict.setOnClickListener {
            interpreter = Interpreter(loadModelFile(this, "lstm_classifier.tflite"), options)
            inputSize = interpreter.getInputTensor(0).shape().size
            outputSize = interpreter.getOutputTensor(0).shape().size
            input = ByteBuffer.allocateDirect(4 * inputSize)
            output = ByteBuffer.allocateDirect(4 * outputSize)

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
                println(preprocessLSTM.toString())
                // Update the UI with the current location

                val predictionRfc = predictWithRFC(preprocess)
                val predictionLstm = predictWithLSTM(interpreter, preprocessLSTM)
                for (i in 0 until 2) {
                    println(predictionLstm[i])
                }
                findViewById<TextView>(R.id.locationTextView).text = "RFC: $predictionRfc \n LSTM: ${predictionLstm[0]} ${predictionLstm[1]}"
            }
        }

        trackService = TrackService(this)
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

    private fun predictWithRFC(data: PyObject): PyObject {
        return module.callAttr("predict_rfc", data)
    }

    /*private fun predictWithLSTM(interpreter: Interpreter, data: PyObject): Float {
        // Clear all buffers
        clearBuffers()

        // Convert PyObject to 1D array of floats
        val data1D = data.toJava(Array<Float>::class.java).map { it }.toFloatArray()

        // Ensure the ByteBuffer can hold all the data
        input = ByteBuffer.allocateDirect(4 * data1D.size)

        // Load data into input buffer
        input.rewind()
        for (value in data1D) {
            input.putFloat(value)
        }

        // Run the interpreter
        interpreter.run(input, output)

        // Extract the prediction
        output.rewind()
        return output.float
        // print first entry of data


        //return 0.0f
    }*/

    private fun predictWithLSTM(interpreter: Interpreter, data: PyObject): FloatArray {
        // Clear all buffers
        clearBuffers()

        // Convert PyObject to 1D array of floats
        val data1D = data.toJava(Array<Float>::class.java).map { it }.toFloatArray()

        // Ensure the ByteBuffer can hold all the data
        input = ByteBuffer.allocateDirect(4 * data1D.size)

        // Load data into input buffer
        input.rewind()
        for (value in data1D) {
            input.putFloat(value)
        }

        // Run the interpreter
        interpreter.run(input, output)

        // Extract the prediction
        output.rewind()
        output.order(ByteOrder.LITTLE_ENDIAN) // Set the byte order to little-endian


        return FloatArray(outputSize) { output.float }
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