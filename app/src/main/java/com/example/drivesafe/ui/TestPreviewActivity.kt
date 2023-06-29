package com.example.drivesafe.ui

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
import com.example.drivesafe.databinding.ActivityTestPreviewBinding
import com.example.drivesafe.facedetector.FaceDetectorProcessor
import com.example.drivesafe.mlkit_utils.GraphicOverlay
import com.example.drivesafe.mlkit_utils.VisionImageProcessor
import com.example.drivesafe.preference.PreferenceUtils
import com.example.drivesafe.base.BaseActivity
import com.example.drivesafe.ui.camerax_live_preview.CameraXViewModel
import com.example.drivesafe.utils.view_utils.showToastLongTime
import com.google.android.gms.common.annotation.KeepName
import com.google.mlkit.common.MlKitException
import dagger.hilt.android.AndroidEntryPoint

@KeepName
@AndroidEntryPoint
class TestPreviewActivity: BaseActivity<ActivityTestPreviewBinding>(ActivityTestPreviewBinding::inflate) {

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        previewView = binding.previewView
        graphicOverlay = binding.graphicOverlay
        initCameraProvider()
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
        cameraXViewModel.getProcessCameraProvider()
    }

    private fun bindAllCameraUseCases() {
        if (cameraProvider != null) {
            // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
            cameraProvider!!.unbindAll()
            bindPreviewUseCase()
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
                FaceDetectorProcessor(this, faceDetectorOptions)
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
    }
}
