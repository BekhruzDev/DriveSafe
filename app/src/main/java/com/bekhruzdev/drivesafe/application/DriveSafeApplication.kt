package com.bekhruzdev.drivesafe.application

import android.app.Application
import com.bekhruzdev.drivesafe.preference.AppPreferences
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DriveSafeApplication:Application(){
    override fun onCreate() {
        super.onCreate()
        AppPreferences.init(this)
    }
}