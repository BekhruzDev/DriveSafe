package com.example.drivesafe.camerax_live_preview

import android.app.Application
import android.util.Log
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.ExecutionException
import javax.inject.Inject

/** View model for interacting with CameraX. */
@HiltViewModel
class CameraXViewModel
    @Inject constructor(application: Application) : AndroidViewModel(application) {

    private val TAG = "CameraXViewModel"
    private var cameraProviderLiveData: MutableLiveData<ProcessCameraProvider>? = null
    /**
     * Create an instance which interacts with the camera service via the given application context.
     */
    fun getProcessCameraProvider(): LiveData<ProcessCameraProvider> {
        if (cameraProviderLiveData == null) {
            cameraProviderLiveData = MutableLiveData()

            val cameraProviderFuture = ProcessCameraProvider.getInstance(getApplication())
            cameraProviderFuture.addListener(
                {
                    try {
                        cameraProviderLiveData?.setValue(cameraProviderFuture.get())
                    } catch (e: ExecutionException) {
                        // Handle any errors (including cancellation) here.
                        Log.e(TAG, "Unhandled exception", e)
                    } catch (e: InterruptedException) {
                        Log.e(TAG, "Unhandled exception", e)
                    }
                },
                ContextCompat.getMainExecutor(getApplication())
            )
        }

        return cameraProviderLiveData!!
    }
}
