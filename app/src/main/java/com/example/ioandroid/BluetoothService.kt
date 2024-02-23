package com.example.ioandroid

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.ArrayMap
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult

class BluetoothService(private val context: Context) {
    private val bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothDevices = ArrayMap<BluetoothDevice, Triple<Int, Pair<Int, String>, String>>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val deviceRSSI =
                        intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)

                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            context as MainActivity,
                            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION),
                            1
                        )
                    }
                    val deviceClassInt = device?.bluetoothClass?.deviceClass ?: -1
                    val deviceClassString = getDeviceType(deviceClassInt)
                    val deviceName = getDeviceName(device)

                    if (!bluetoothDevices.containsKey(device)) {
                        bluetoothDevices[device] = Triple(deviceRSSI.toInt(), Pair(deviceClassInt, deviceClassString), deviceName)
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    stopDiscovery()
                }
            }
        }
    }

    fun startDiscovery() {
        if(bluetoothAdapter == null) {
            Toast.makeText(context, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            return
        }
        if(!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(context as MainActivity, enableBtIntent, 1, null)
        }
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context as MainActivity,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                1
            )
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(receiver, filter)

        while (bluetoothAdapter.isDiscovering) {
            // do nothing
        }
    }

    fun stopDiscovery() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context as MainActivity,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                1
            )
            return
        }
        Toast.makeText(context, "Stopping discovery with ${bluetoothDevices.size} devices found", Toast.LENGTH_SHORT).show()
        bluetoothAdapter?.cancelDiscovery()
        context.unregisterReceiver(receiver)
    }

    fun getDevices(): ArrayMap<BluetoothDevice, Triple<Int, Pair<Int, String>, String>> {
        Toast.makeText(context, "Bluetooth devices: $bluetoothDevices", Toast.LENGTH_SHORT).show()
        return bluetoothDevices
    }

    fun getDeviceType(deviceClassInt: Int?): String {
        return when (deviceClassInt) {
            BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE -> "Audio Video Handsfree"
            BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES -> "Audio Video Headphones"
            BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER -> "Audio Video Loudspeaker"
            BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO -> "Audio Video Portable Audio"
            BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX -> "Audio Video Set Top Box"
            BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED -> "Audio Video Uncategorised"
            BluetoothClass.Device.AUDIO_VIDEO_VCR -> "Audio Video VCR"
            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CAMERA -> "Audio Video Video Camera"
            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CONFERENCING -> "Audio Video Video Conferencing"
            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER -> "Audio Video Video Display and Loudspeaker"
            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY -> "Audio Video Video Gaming Toy"
            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_MONITOR -> "Audio Video Video Monitor"
            BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET -> "Audio Video Wearable Headset"
            BluetoothClass.Device.COMPUTER_DESKTOP -> "Computer Desktop"
            BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA -> "Computer Handheld PC PDA"
            BluetoothClass.Device.COMPUTER_LAPTOP -> "Computer Laptop"
            BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA -> "Computer Palm Size PC PDA"
            BluetoothClass.Device.COMPUTER_SERVER -> "Computer Server"
            BluetoothClass.Device.COMPUTER_UNCATEGORIZED -> "Computer Uncategorised"
            BluetoothClass.Device.COMPUTER_WEARABLE -> "Computer Wearable"
            BluetoothClass.Device.HEALTH_BLOOD_PRESSURE -> "Health Blood Pressure"
            BluetoothClass.Device.HEALTH_DATA_DISPLAY -> "Health Data Display"
            BluetoothClass.Device.HEALTH_GLUCOSE -> "Health Glucose"
            BluetoothClass.Device.HEALTH_PULSE_OXIMETER -> "Health Pulse Oximeter"
            BluetoothClass.Device.HEALTH_PULSE_RATE -> "Health Pulse Rate"
            BluetoothClass.Device.HEALTH_THERMOMETER -> "Health Thermometer"
            BluetoothClass.Device.HEALTH_UNCATEGORIZED -> "Health Uncategorised"
            BluetoothClass.Device.HEALTH_WEIGHING -> "Health Weighing"
            else -> "Unknown Device Class"
        }
    }

    fun getDeviceName(device: BluetoothDevice?): String {
        return device?.name ?: ""
    }

}