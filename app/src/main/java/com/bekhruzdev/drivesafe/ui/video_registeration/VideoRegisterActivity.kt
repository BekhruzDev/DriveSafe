package com.bekhruzdev.drivesafe.ui.video_registeration

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bekhruzdev.drivesafe.R
import com.bekhruzdev.drivesafe.base.BaseActivity
import com.bekhruzdev.drivesafe.databinding.ActivityVideoRegisterBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class VideoRegisterActivity :
    BaseActivity<ActivityVideoRegisterBinding>(ActivityVideoRegisterBinding::inflate) {
    var service: ExecutorService? = null
    var recording: Recording? = null
    var videoCapture: VideoCapture<Recorder>? = null
    var capture: ImageButton? = null
    var toggleFlash:ImageButton? = null
    var flipCamera:ImageButton? = null
    var previewView: PreviewView? = null
    var cameraFacing = CameraSelector.LENS_FACING_BACK
    private val activityResultLauncher = registerForActivityResult<String, Boolean>(
        ActivityResultContracts.RequestPermission()
    ) { result: Boolean? ->
        if (ActivityCompat.checkSelfPermission(
                this@VideoRegisterActivity,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera(cameraFacing)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        previewView = binding.viewFinder
        capture = binding.capture
        flipCamera = binding.flipCamera
        capture!!.setOnClickListener(View.OnClickListener { view: View? ->
            if (ActivityCompat.checkSelfPermission(
                    this@VideoRegisterActivity,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                activityResultLauncher.launch(Manifest.permission.CAMERA)
            } else if (ActivityCompat.checkSelfPermission(
                    this@VideoRegisterActivity,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                activityResultLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && ActivityCompat.checkSelfPermission(
                    this@VideoRegisterActivity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                activityResultLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                captureVideo()
            }
        })
        if (ActivityCompat.checkSelfPermission(
                this@VideoRegisterActivity,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            activityResultLauncher.launch(Manifest.permission.CAMERA)
        } else {
            startCamera(cameraFacing)
        }
        flipCamera!!.setOnClickListener(View.OnClickListener {
            cameraFacing = if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            startCamera(cameraFacing)
        })
        service = Executors.newSingleThreadExecutor()
    }

    fun captureVideo() {
        capture!!.setImageResource(R.drawable.round_stop_circle_24)
        val recording1 = recording
        if (recording1 != null) {
            recording1.stop()
            recording = null
            return
        }
        val name = SimpleDateFormat(
            "yyyy-MM-dd-HH-mm-ss-SSS",
            Locale.getDefault()
        ).format(System.currentTimeMillis())
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
        val options = MediaStoreOutputOptions.Builder(
            contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
            .setContentValues(contentValues).build()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        recording =
            videoCapture!!.output.prepareRecording(this@VideoRegisterActivity, options).withAudioEnabled()
                .start(
                    ContextCompat.getMainExecutor(this@VideoRegisterActivity)
                ) { videoRecordEvent: VideoRecordEvent? ->
                    if (videoRecordEvent is VideoRecordEvent.Start) {
                        capture!!.isEnabled = true
                    } else if (videoRecordEvent is VideoRecordEvent.Finalize) {
                        if (!videoRecordEvent.hasError()) {
                            val msg =
                                "Video capture succeeded: " + videoRecordEvent.outputResults
                                    .outputUri
                            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                        } else {
                            recording!!.close()
                            recording = null
                            val msg = "Error: ${videoRecordEvent.error}"
                            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                            Log.e("VideoCapture", "Error: ${videoRecordEvent.error}")
                        }
                        capture!!.setImageResource(R.drawable.round_fiber_manual_record_24)
                    }
                }
    }

    fun startCamera(cameraFacing: Int) {
        val processCameraProvider = ProcessCameraProvider.getInstance(this@VideoRegisterActivity)
        processCameraProvider.addListener({
            try {
                val cameraProvider = processCameraProvider.get()
                val preview =
                    Preview.Builder().build()
                preview.setSurfaceProvider(previewView!!.surfaceProvider)
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                    .build()
                videoCapture = VideoCapture.withOutput(recorder)
                cameraProvider.unbindAll()
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(cameraFacing).build()
                val camera =
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture)
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this@VideoRegisterActivity))
    }

    override fun onDestroy() {
        super.onDestroy()
        service!!.shutdown()
    }

}