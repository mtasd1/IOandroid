package com.example.ioandroid

import android.Manifest
import android.bluetooth.BluetoothAdapter
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
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    val bluetoothDevices = ArrayMap<BluetoothDevice, Int>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                                        val deviceRSSI =
                        intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                    if (!bluetoothDevices.containsKey(device)) {
                        bluetoothDevices[device] = deviceRSSI.toInt()
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Toast.makeText(context, "Discovery finished", Toast.LENGTH_SHORT).show()
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
        Toast.makeText(context, "Starting discovery", Toast.LENGTH_SHORT).show()

        bluetoothAdapter.startDiscovery()
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

        bluetoothAdapter?.cancelDiscovery()
        context.unregisterReceiver(receiver)
    }

    //create a function that returns all devices found and their RSSI
    fun getDevices(): ArrayMap<BluetoothDevice, Int> {
        if(bluetoothDevices.isEmpty()) {
            return ArrayMap()
        }
        return bluetoothDevices
    }

}