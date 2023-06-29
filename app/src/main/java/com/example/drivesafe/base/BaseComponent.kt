package com.example.drivesafe.base

import com.google.mlkit.vision.face.Face

object BaseComponent {
    private var sleepStartTime = 0L
    private var awakeStartTime = 0L
    private val SLEEP_TIMEOUT = 1000L
    private val AWAKE_TIMEOUT = 1000L

    fun Face.handleSleeping(action: (Boolean) -> Unit) {
            if (leftEyeOpenProbability != null && rightEyeOpenProbability != null) {
                if (leftEyeOpenProbability!! <= 0.20f && rightEyeOpenProbability!! <= 0.20f) {
                    awakeStartTime = 0L
                    if (sleepStartTime == 0L) {
                        sleepStartTime = System.currentTimeMillis()
                    } else if (System.currentTimeMillis() - sleepStartTime >= SLEEP_TIMEOUT) {
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