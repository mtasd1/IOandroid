package com.example.ioandroid

import android.location.Location

interface LocationService {
    fun getLocation(): Location?
}