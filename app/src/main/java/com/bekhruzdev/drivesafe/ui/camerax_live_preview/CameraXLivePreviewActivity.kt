/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bekhruzdev.drivesafe.ui.camerax_live_preview

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.camera.core.ExperimentalGetImage
import androidx.core.view.forEach
import androidx.lifecycle.lifecycleScope
import com.bekhruzdev.drivesafe.R
import com.bekhruzdev.drivesafe.base.BaseActivity
import com.bekhruzdev.drivesafe.databinding.MainLayoutBinding
import com.bekhruzdev.drivesafe.preference.AppPreferences
import com.bekhruzdev.drivesafe.service.DrowsinessDetectionService
import com.bekhruzdev.drivesafe.ui.TestPreviewActivity
import com.bekhruzdev.drivesafe.ui.usb_camera_live_preview.UsbCameraLivePreviewActivity
import com.bekhruzdev.drivesafe.utils.view_utils.selected
import com.bekhruzdev.drivesafe.utils.view_utils.showSnackBar
import com.bekhruzdev.drivesafe.utils.view_utils.showToast
import com.google.android.gms.common.annotation.KeepName
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@ExperimentalGetImage
/** Live preview demo app for ML Kit APIs using CameraX. */
@KeepName
@AndroidEntryPoint
class CameraXLivePreviewActivity :
    BaseActivity<MainLayoutBinding>(MainLayoutBinding::inflate) {

    private val cameraXViewModel: CameraXViewModel by viewModels()
    private var drowsinessDetectionService: Service? = null
    private var isPlaying = false
    private var currentSound = 0
    private var isPressedBackOnce = true
    private var isBound = false

    //connection with the Bound Service
    private val connection = @ExperimentalGetImage object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder = service as DrowsinessDetectionService.LocalBinder
            drowsinessDetectionService = binder.getService()
            cameraXViewModel.setServiceBound(true)
        }

        override fun onServiceDisconnected(className: ComponentName?) {
            cameraXViewModel.setServiceBound(false)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleBackPressed()
        cameraXViewModel.isServiceBound.observe(this) { bound ->
            Firebase.analytics.logEvent("detection") {
                param("running", if (bound) "true" else "false")
            }
            if (bound) {
                binding.lvPowerBtn.setAnimation(R.raw.lottie_power_on)
                binding.tvAntiSleepStatus.text = "running..."
                binding.tvAntiSleepStatus.setTextColor(resources.getColor(R.color.green_600))
            } else {
                binding.lvPowerBtn.setAnimation(R.raw.lottie_power_off_v3)
                binding.tvAntiSleepStatus.text = "not active"
                binding.tvAntiSleepStatus.setTextColor(resources.getColor(R.color.orange_red_500))
            }
            binding.lvPowerBtn.playAnimation()
            isBound = bound
        }
    }

    override fun onInitUi() {
        super.onInitUi()
        binding.llPreview.setOnClickListener {
            stopPlayerAndDetection()
            val intent = Intent(this@CameraXLivePreviewActivity, TestPreviewActivity::class.java)
            startActivity(intent)
        }
        binding.lvPowerBtn.apply {
            playAnimation()
            if (isBound) {
                binding.lvPowerBtn.setAnimation(R.raw.lottie_power_on)
            } else {
                binding.lvPowerBtn.setAnimation(R.raw.lottie_power_off_v3)
            }
            setOnClickListener {
                stopPlayerAndDetection()
                if (isBound) {
                    this.setAnimation(R.raw.lottie_power_off_v3)
                } else {
                    startDetectionService()
                    this.setAnimation(R.raw.lottie_power_on)
                }
                resumeAnimation()
            }
        }
        binding.llEcoMode.setOnClickListener {
            if (isBound) {
                moveTaskToBack(true)
            } else {
                showSnackBar(
                    view = binding.root,
                    message = "You haven't started Anti-Sleep detection!",
                    buttonText = "Start",
                    buttonTextColor = resources.getColor(R.color.green_600)
                ) {
                    startDetectionService()
                }
            }
        }
        binding.llUsbCam.setOnClickListener {
            if (isBound) {
                stopPlayerAndDetection()
            }
            val intent = Intent(this, UsbCameraLivePreviewActivity::class.java)
            startActivity(intent)
        }
        binding.include.switchFlashlightBlink.apply {
            isChecked = AppPreferences.useFlashlight
            setOnCheckedChangeListener { switch, isChecked ->
                switch.isChecked = isChecked
                AppPreferences.useFlashlight = isChecked
            }
        }
        when (AppPreferences.sleepTimeOut) {
            500 -> binding.include.millis500.selected()
            1000 -> binding.include.millis1000.selected()
            1500 -> binding.include.millis1500.selected()
        }
        binding.include.millis500.setOnClickListener {
            binding.include.llSleepTimeOut.forEach { view ->
                view.isSelected = false
            }
            it.selected()
            AppPreferences.sleepTimeOut = 500
        }
        binding.include.millis1000.setOnClickListener {
            binding.include.llSleepTimeOut.forEach { view ->
                view.isSelected = false
            }
            it.selected()
            AppPreferences.sleepTimeOut = 1000
        }
        binding.include.millis1500.setOnClickListener {
            binding.include.llSleepTimeOut.forEach { view ->
                view.isSelected = false
            }
            it.selected()
            AppPreferences.sleepTimeOut = 1500
        }


        when (AppPreferences.sound) {
            AppPreferences.SOUND_SIREN -> binding.include.btnAlarmSiren.selected()
            AppPreferences.SOUND_POLICE_SIREN -> binding.include.btnPoliceSiren.selected()
            AppPreferences.SOUND_TRUCK_HONK -> binding.include.btnTruckSiren.selected()
        }
        binding.include.btnAlarmSiren.setOnClickListener {
            binding.include.llSound.forEach { view ->
                view.isSelected = false
            }
            it.selected()
            AppPreferences.sound = AppPreferences.SOUND_SIREN

            if (isPlaying && currentSound == R.raw.sound_siren) {
                stopPlayerAndDetection()
                isPlaying = false
            } else {
                stopPlayerAndDetection()
                playSound(R.raw.sound_siren)
                currentSound = R.raw.sound_siren
                isPlaying = true
            }

        }
        binding.include.btnPoliceSiren.setOnClickListener {
            binding.include.llSound.forEach { view ->
                view.isSelected = false
            }
            it.selected()
            AppPreferences.sound = AppPreferences.SOUND_POLICE_SIREN
            if (isPlaying && currentSound == R.raw.sound_police_siren) {
                stopPlayerAndDetection()
                isPlaying = false
            } else {
                stopPlayerAndDetection()
                playSound(R.raw.sound_police_siren)
                currentSound = R.raw.sound_police_siren
                isPlaying = true
            }

        }
        binding.include.btnTruckSiren.setOnClickListener {
            binding.include.llSound.forEach { view ->
                view.isSelected = false
            }
            it.selected()
            AppPreferences.sound = AppPreferences.SOUND_TRUCK_HONK
            if (isPlaying && currentSound == R.raw.sound_truck_horn) {
                stopPlayerAndDetection()
                isPlaying = false
            } else {
                stopPlayerAndDetection()
                playSound(R.raw.sound_truck_horn)
                currentSound = R.raw.sound_truck_horn
                isPlaying = true
            }
        }
    }

    private fun stopDetectionService() {
        if (isBound) {
            unbindService(connection)
            cameraXViewModel.setServiceBound(false)
        }
    }

    private fun startDetectionService() {
        val intent = Intent(
            this@CameraXLivePreviewActivity,
            DrowsinessDetectionService::class.java
        )
        bindService(intent, connection, BIND_AUTO_CREATE)
        cameraXViewModel.setServiceBound(true)
    }

    public override fun onDestroy() {
        super.onDestroy()
        isBound = false
    }


    private fun handleBackPressed() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isPressedBackOnce) {
                    isPressedBackOnce = false
                    showToast("Press back button again to exit")
                    queueEvent(2500) {
                        isPressedBackOnce = true
                    }
                } else {
                    finish()
                }
            }
        }
        this.onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun stopPlayerAndDetection() {
        stopPlayer {
            stopDetectionService()
        }
    }

    companion object {
        private const val TAG = "CameraXLivePreview"
    }

    /*
    private fun bindAllCameraUseCases() {
        if (cameraProvider != null) {
            // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
            cameraProvider!!.unbindAll()
            //bindPreviewUseCase()
            bindAnalysisUseCase()
        }
    }

    private fun bindPreviewUseCase() {
        if (!PreferenceUtils.isCameraLiveViewportEnabled(this)) {
            return
        }
        if (cameraProvider == null) {
            return
        }
        if (previewUseCase != null) {
            cameraProvider!!.unbind(previewUseCase)
        }

        val builder = Preview.Builder()
        builder.setTargetResolution(Size(640, 480))
        previewUseCase = builder.build()
        previewUseCase!!.setSurfaceProvider(previewView!!.surfaceProvider)
        cameraProvider!!.bindToLifecycle(
            this,
            cameraSelector!!,
            previewUseCase
        )
    }

    private fun bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return
        }
        if (analysisUseCase != null) {
            cameraProvider!!.unbind(analysisUseCase)
        }
        if (imageProcessor != null) {
            imageProcessor!!.stop()
        }
        imageProcessor =
            try {
                Log.i(TAG, "Using Face Detector Processor")
                val faceDetectorOptions = PreferenceUtils.getFaceDetectorOptions(this)
                FaceDetectorProcessor(this, faceDetectorOptions, onFaceActions)
            } catch (e: Exception) {
                Log.e(TAG, "Can not create image processor: $selectedModel", e)
                showToastLongTime("Can not create image processor: ")
                return
            }

        val builder = ImageAnalysis.Builder()
        val targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing)
        if (targetResolution != null) {
            builder.setTargetResolution(targetResolution)
        }
        analysisUseCase = builder.build()

        needUpdateGraphicOverlayImageSourceInfo = true

        analysisUseCase?.setAnalyzer(
            // imageProcessor.processImageProxy will use another thread to run the detection underneath,
            // thus we can just run the analyzer itself on main thread.
            ContextCompat.getMainExecutor(this),
            ImageAnalysis.Analyzer { imageProxy: ImageProxy ->
                if (needUpdateGraphicOverlayImageSourceInfo) {
                    val isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    if (rotationDegrees == 0 || rotationDegrees == 180) {
                        graphicOverlay!!.setImageSourceInfo(
                            imageProxy.width,
                            imageProxy.height,
                            isImageFlipped
                        )
                    } else {
                        graphicOverlay!!.setImageSourceInfo(
                            imageProxy.height,
                            imageProxy.width,
                            isImageFlipped
                        )
                    }
                    needUpdateGraphicOverlayImageSourceInfo = false
                }
                try {
                    imageProcessor!!.processImageProxy(imageProxy, graphicOverlay)
                } catch (e: MlKitException) {
                    Log.e(TAG, "Failed to process image. Error: " + e.localizedMessage)
                    Toast.makeText(applicationContext, e.localizedMessage, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        )
        cameraProvider!!.bindToLifecycle(*/
/* lifecycleOwner = *//*
 this,
            cameraSelector!!,
            analysisUseCase
        )
    }

*/

}
