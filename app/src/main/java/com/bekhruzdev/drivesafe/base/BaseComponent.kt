package com.bekhruzdev.drivesafe.base

import android.graphics.Color
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Build
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import com.bekhruzdev.drivesafe.preference.AppPreferences
import com.bekhruzdev.drivesafe.utils.view_utils.gone
import com.bekhruzdev.drivesafe.utils.view_utils.show
import com.google.mlkit.vision.face.Face
import kotlinx.coroutines.*

object BaseComponent {
    private var sleepStartTime = 0L
    private var awakeStartTime = 0L
    private const val SLEEP_TIMEOUT = 1000L
    private const val AWAKE_TIMEOUT = 1000L

    suspend fun Face.handleSleeping(action: (Boolean) -> Unit) {
        withContext(Dispatchers.Default) {
            if (leftEyeOpenProbability != null && rightEyeOpenProbability != null) {
                if (leftEyeOpenProbability!! <= 0.20f && rightEyeOpenProbability!! <= 0.20f) {
                    awakeStartTime = 0L
                    if (sleepStartTime == 0L) {
                        sleepStartTime = System.currentTimeMillis()
                    } else if (System.currentTimeMillis() - sleepStartTime >= AppPreferences.sleepTimeOut) { //TODO; USE CACHING WITH APP_PREFERENCES
                        // User's eyes have been closed for at least 1 second
                        // Do something here
                        action.invoke(true)
                    }
                } else {
                    sleepStartTime = 0L
                    if (awakeStartTime == 0L) {
                        awakeStartTime = System.currentTimeMillis()
                    } else if (System.currentTimeMillis() - awakeStartTime >= AWAKE_TIMEOUT) {
                        // User's eyes have been open for at least 1 second
                        // Do something here
                        action.invoke(false)
                    }
                    //stop Player
                }
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.M)
    suspend fun startFlashlight(cameraManager: CameraManager) {
        val cameraId = cameraManager.cameraIdList[0]
        withContext(Dispatchers.Default) {
            try {
                cameraManager.setTorchMode(cameraId, true)
                delay(500)
                cameraManager.setTorchMode(cameraId, false)
                delay(200)
            } catch (e: CameraAccessException) {

            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.M)
    suspend fun stopFlashlight(cameraManager: CameraManager) {
        val cameraId = cameraManager.cameraIdList[0]
        withContext(Dispatchers.Default) {
            try {
                cameraManager.setTorchMode(cameraId, false)
            } catch (e: CameraAccessException) {
            }
        }
    }

    suspend fun startScreenLight(view: View, window: Window) {
        withContext(Dispatchers.Main) {
            view.show()
            val params = window.attributes
            params.screenBrightness = 1.0f // Set the screen brightness to maximum
            window.attributes = params
            launch {
                var isScreenOn = false
                while (true) {
                    if (!isScreenOn) {
                        view.setBackgroundColor(Color.WHITE) // Set the background color to white
                        isScreenOn = true
                    } else {
                        view.setBackgroundColor(Color.BLACK) // Set the background color to black
                        isScreenOn = false
                    }
                    delay(1000) // Blink every one second
                }
            }
        }
    }

    fun stopScreenLight(view: View){
        view.gone()
    }



}
