package com.example.ioandroid

import android.app.Activity
import android.location.Location

class LocationManagerService(private val activity: Activity): LocationService {
    override fun getLocation(): Location? {
        TODO("Not yet implemented")
    }
}
