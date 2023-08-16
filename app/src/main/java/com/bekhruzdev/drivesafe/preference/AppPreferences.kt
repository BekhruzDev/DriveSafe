package com.bekhruzdev.drivesafe.preference

import android.content.Context
import android.content.SharedPreferences

object AppPreferences {
    private lateinit var devicePreferences: SharedPreferences
    private const val DEVICE_PREFERENCES = "device_preferences"
    const val SOUND_TRUCK_HONK = "truck_honk"
    const val SOUND_POLICE_SIREN = "police_siren"
    const val SOUND_SIREN = "siren"
    fun init(context: Context) {
        devicePreferences = context.getSharedPreferences(DEVICE_PREFERENCES, Context.MODE_PRIVATE)
    }

    var useFlashlight: Boolean
        set(value) = devicePreferences.edit().putBoolean(::useFlashlight.name, value).apply()
        get() = devicePreferences.getBoolean(::useFlashlight.name, true)

    var sleepTimeOut: Int
        set(value) = devicePreferences.edit().putInt(::sleepTimeOut.name, value).apply()
    get() = devicePreferences.getInt(::sleepTimeOut.name, 500)

    var sound:String?
        set(value) = devicePreferences.edit().putString(::sound.name, value).apply()
        get() = devicePreferences.getString(::sound.name, SOUND_SIREN)





}