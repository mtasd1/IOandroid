package com.example.ioandroid

import android.content.Context
import android.telephony.TelephonyManager

class TelephoneService(context: Context) {
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    /*fun getSignalStrength(): Int {
        return telephonyManager.signalStrength?.level ?: 0
    }*/



}