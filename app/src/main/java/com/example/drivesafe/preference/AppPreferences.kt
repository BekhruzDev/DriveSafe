package com.example.drivesafe.preference

import android.content.Context
import android.content.SharedPreferences

object AppPreferences{
    private lateinit var devicePreferences: SharedPreferences
    private const val DEVICE_PREFERENCES = "device_preferences"
    fun init(context:Context){
        devicePreferences = context.getSharedPreferences(DEVICE_PREFERENCES, Context.MODE_PRIVATE)
    }

    var useFlashlight :Boolean
    set(value) = devicePreferences.edit().putBoolean(::useFlashlight.name, value).apply()
    get() = devicePreferences.getBoolean(::useFlashlight.name, true)
}