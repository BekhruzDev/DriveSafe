package com.bekhruzdev.drivesafe.service

import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.MutableLiveData

object DrowsinessDetectionRepository {
    var cameraProvider: MutableLiveData<ProcessCameraProvider>? = null


}