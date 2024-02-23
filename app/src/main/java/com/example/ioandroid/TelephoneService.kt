package com.example.ioandroid

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat

class TelephoneService(context: Context) {
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private var signalStrengthDbm = 0

    @RequiresApi(Build.VERSION_CODES.R)
    private val telephonyCallback = @RequiresApi(Build.VERSION_CODES.S)
    object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
            signalStrengthDbm = signalStrength.level
        }
    }

    private val phoneStateListener = object : PhoneStateListener() {
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
            super.onSignalStrengthsChanged(signalStrength)
            signalStrengthDbm = signalStrength.level
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            telephonyManager.registerTelephonyCallback(context.mainExecutor, telephonyCallback)
        } else {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
        }
    }

    fun getSignalStrength(): Int {
        return signalStrengthDbm
    }

    fun getNetworkType(context: Context): Int {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context as MainActivity,
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                1
            )
        }
        return telephonyManager.networkType
    }
}