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

package com.example.drivesafe.ui.camerax_live_preview

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.drivesafe.R
import com.example.drivesafe.databinding.ActivityVisionCameraxLivePreviewBinding
import com.example.drivesafe.databinding.MainLayoutBinding
import com.example.drivesafe.facedetector.FaceDetectorProcessor
import com.example.drivesafe.facedetector.OnFaceActions
import com.example.drivesafe.mlkit_utils.GraphicOverlay
import com.example.drivesafe.mlkit_utils.VisionImageProcessor
import com.example.drivesafe.preference.PreferenceUtils
import com.example.drivesafe.ui.TestPreviewActivity
import com.example.drivesafe.ui.base.BaseActivity
import com.example.drivesafe.utils.view_utils.showToast
import com.example.drivesafe.utils.view_utils.showToastLongTime
import com.google.android.gms.common.annotation.KeepName
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.face.Face
import com.serenegiant.utils.ThreadPool.queueEvent
import dagger.hilt.android.AndroidEntryPoint

/** Live preview demo app for ML Kit APIs using CameraX. */
@KeepName
@AndroidEntryPoint
class CameraXLivePreviewActivity :
    BaseActivity<MainLayoutBinding>(MainLayoutBinding::inflate) {

    private val cameraXViewModel: CameraXViewModel by viewModels()
    private var previewView: PreviewView? = null
    private var graphicOverlay: GraphicOverlay? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var previewUseCase: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null
    private var imageProcessor: VisionImageProcessor? = null
    private var needUpdateGraphicOverlayImageSourceInfo = false
    private var selectedModel = FACE_DETECTION
    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private var cameraSelector: CameraSelector? = null
    private var onFaceActions: OnFaceActions? = null
    private var sleepStartTime = 0L
    private var awakeStartTime = 0L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        graphicOverlay = binding.graphicOverlay
        initFaceActions()
        handleBackPressed()

    }

    override fun onInitUi() {
        super.onInitUi()
        binding.llPreview.setOnClickListener {
            val intent = Intent(this, TestPreviewActivity::class.java)
            startActivity(intent)
        }
        binding.lvPowerBtn.apply {
            playAnimation()
            if(cameraProvider == null){
                this.setAnimation(R.raw.lottie_power_off_v3)
            }else{
                this.setAnimation(R.raw.lottie_power_on)
            }
            setOnClickListener {
                if(cameraProvider == null){
                    initCameraProvider()
                    this.setAnimation(R.raw.lottie_power_on)
                }else{
                    cameraProvider!!.unbindAll()
                    imageProcessor?.run { this.stop() }
                    cameraProvider = null
                    this.setAnimation(R.raw.lottie_power_off_v3)
                }
                resumeAnimation()

            }
        }
    }

    private fun initFaceActions() {
        onFaceActions = object : OnFaceActions {
            override fun onFaceAvailable(face: Face) {
                face.handleSleeping { isSleeping ->
                    if (isSleeping) {
                        Log.d("EYE_ACTIVITY", "SLEEPING!!!!!")
                    } else {
                        Log.d("EYE_ACTIVITY", "NOT SLEEPING!!!!!")
                    }
                }
            }

        }
    }


    private fun initCameraProvider() {
        cameraXViewModel.getProcessCameraProvider().observe(this) {
            cameraProvider = it
            bindAllCameraUseCases()
        }
        Log.d(TAG, "Initialized CameraProvider")
    }

    public override fun onResume() {
        super.onResume()
        bindAllCameraUseCases()
    }

    override fun onPause() {
        super.onPause()
        imageProcessor?.run { this.stop() }
    }

    public override fun onDestroy() {
        super.onDestroy()
        imageProcessor?.run { this.stop() }
    }

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
        cameraProvider!!.bindToLifecycle(/* lifecycleOwner = */ this,
            cameraSelector!!,
            analysisUseCase
        )
    }


    private fun Face.handleSleeping(action: (Boolean) -> Unit) {
        queueEvent {
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

    private fun handleBackPressed() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        }
        this.onBackPressedDispatcher.addCallback(this, callback)
    }

    companion object {
        private const val TAG = "CameraXLivePreview"
        private const val FACE_DETECTION = "Face Detection"
        private const val SLEEP_TIMEOUT = 1000L
        private const val AWAKE_TIMEOUT = 1000L
    }
}
