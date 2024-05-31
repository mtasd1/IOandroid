package com.example.ioandroid.services

import android.app.Activity
import android.content.Context
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class PredictService(private val context: Context, private val activity: Activity) {
    private val py = Python.getInstance()
    private val module = py.getModule("script")

    private lateinit var interpreter: Interpreter
    private var inputSize = 0
    private var outputSize = 0
    private lateinit var input : ByteBuffer
    private lateinit var output : ByteBuffer

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    fun startService() {
        coroutineScope.launch(Dispatchers.Default) {
            if(!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }
        }

        interpreter = Interpreter(loadModelFile(activity, "lstm_classifier.tflite"))
        inputSize = interpreter.getInputTensor(0).shape().size
        outputSize = interpreter.getOutputTensor(0).shape().size
        input = ByteBuffer.allocateDirect(4 * inputSize)
        output = ByteBuffer.allocateDirect(4 * outputSize)
    }
    fun stopService() {
        interpreter.close()
    }

    fun predictWithLSTM(file: File): FloatArray {
        clearBuffers()

        val preprocessed1D = preprocessLSTM(file).toJava(Array<Float>::class.java).map { it }.toFloatArray()

        input = ByteBuffer.allocateDirect(4 * preprocessed1D.size)
        input.order(ByteOrder.LITTLE_ENDIAN) // Set the byte order of input buffer to little-endian

        input.rewind()
        for (value in preprocessed1D) {
            input.putFloat(value)
        }
        input.rewind()

        interpreter.run(input, output)

        output.rewind()
        output.order(ByteOrder.LITTLE_ENDIAN) // Set the byte order to little-endian

        return FloatArray(outputSize) { output.float }
    }

    fun predictWithRFC(file: File): FloatArray {
        val preprocessed = preprocessRFC(file)
        val prediction = module.callAttr("predict_rfc", preprocessed)
        val list = prediction.asList()
        return list.map { it.asList() }  // Assuming prediction is a list of lists
            .flatten()  // Flatten the list of lists to a single list
            .map { it.toFloat() }  // Convert each PyObject to Float
            .toFloatArray()  // Convert the List<Float> to FloatArray
    }

    fun preprocessRFC(file: File): PyObject {
        return module.callAttr("preprocess_single_entry", file.absolutePath)
    }

    fun preprocessLSTM(file: File): PyObject {
        return module.callAttr("preprocess_lstm", file.absolutePath)
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