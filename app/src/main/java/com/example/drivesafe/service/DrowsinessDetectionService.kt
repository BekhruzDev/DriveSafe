package com.example.drivesafe.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RawRes
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.example.drivesafe.R
import com.example.drivesafe.base.BaseComponent.handleSleeping
import com.example.drivesafe.base.BaseComponent.startFlashlight
import com.example.drivesafe.base.BaseComponent.stopFlashlight
import com.example.drivesafe.facedetector.OnFaceActions
import com.example.drivesafe.preference.AppPreferences
import com.example.drivesafe.preference.PreferenceUtils
import com.example.drivesafe.ui.camerax_live_preview.CameraXLivePreviewActivity
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.*
import java.util.concurrent.ExecutionException


@ExperimentalGetImage
class DrowsinessDetectionService : LifecycleService() {

    private val binder = LocalBinder()
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var imageAnalysis: ImageAnalysis? = null
    private var cameraSelector: CameraSelector? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var faceDetector: FaceDetector
    private var onFaceActions: OnFaceActions? = null
    private val workerScope = CoroutineScope(Dispatchers.Default)
    private var camera: Camera? = null
    private var cameraManager: CameraManager? = null
    private var mediaPlayer:MediaPlayer? = null
    @RawRes private var currentSound: Int = 0
    private var isPlaying = false

    inner class LocalBinder : Binder() {
        fun getService(): DrowsinessDetectionService = this@DrowsinessDetectionService
    }

    override fun onCreate() {
        super.onCreate()
        initFaceDetector()
        initFaceActions()
        initSound()
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
            } catch (e: ExecutionException) {
                Log.e(TAG, "Unhandled exception", e)
            }
            initAllUseCases()
        }, ContextCompat.getMainExecutor(this))


    }

    private fun initSound() {
        currentSound = when(AppPreferences.sound){
            AppPreferences.SOUND_TRUCK_HONK -> R.raw.sound_truck_horn
            AppPreferences.SOUND_SIREN -> R.raw.sound_siren
            else -> R.raw.sound_police_siren
        }
    }

    private fun initForegroundService() {
        val notificationIntent = Intent(applicationContext, CameraXLivePreviewActivity::class.java)
        /*notificationIntent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        val contentIntent = PendingIntent.getActivity(applicationContext, 0, notificationIntent, 0)*/
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Anti-Sleep is running...")
            //.setContentText("Service is running...")
            .setSmallIcon(R.drawable.ic_videocam)
           // .setContentIntent(contentIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_MIN
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        // Start the service in the foreground
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun initAllUseCases() {
        if (cameraProvider != null) {
            cameraProvider!!.unbindAll()
            bindAnalysisUseCase()
        }

    }

    private fun bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return
        }
        if (imageAnalysis != null) {
            cameraProvider!!.unbind(imageAnalysis)
        }

        val builder = ImageAnalysis.Builder()
        val targetResolution =
            PreferenceUtils.getCameraXTargetResolution(this, CameraSelector.LENS_FACING_FRONT)
        if (targetResolution != null) {
            builder.setTargetResolution(targetResolution)
        }
        imageAnalysis = builder.build()
        imageAnalysis?.setAnalyzer(
            // imageProcessor.processImageProxy will use another thread to run the detection underneath,
            // thus we can just run the analyzer itself on main thread.
            ContextCompat.getMainExecutor(this)
        ) { imageProxy: ImageProxy ->
            runFaceDetector(imageProxy)
        }

        camera = cameraProvider!!.bindToLifecycle(
            this@DrowsinessDetectionService,
            cameraSelector!!,
            imageAnalysis
        )
    }

    private fun initFaceActions() {
        onFaceActions = object : OnFaceActions {
            @RequiresApi(Build.VERSION_CODES.M)
            override fun onFaceAvailable(face: Face) {
                workerScope.launch(SupervisorJob()) {
                    face.handleSleeping { isSleeping ->
                        if (isSleeping) {
                            Log.d(TAG, "SLEEPING!!!!!")
                                playSound()
                            launch{
                                if(AppPreferences.useFlashlight){
                                    cameraManager?.let{startFlashlight(it)}
                                }
                            }
                        } else {
                            Log.d(TAG, "NOT SLEEPING!!!!!")
                            /*synchronized(face){
                                stopSound(soundPool)
                            }*/
                            launch {
                              //  stopSound(soundPool)
                                if(AppPreferences.useFlashlight) {
                                    cameraManager?.let { stopFlashlight(it) }
                                }
                            }
                        }
                    }
                }
            }

        }
    }


    private fun initFaceDetector() {
        val options = FaceDetectorOptions.Builder()
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .enableTracking()
            .build()
        faceDetector = FaceDetection.getClient(options)
    }

    private fun runFaceDetector(
        imageProxy: ImageProxy,
        onFaceActions: OnFaceActions? = this.onFaceActions
    ) {
        val mediaImage = imageProxy.image ?: return
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        faceDetector.process(inputImage).addOnSuccessListener { faces ->
            faces.forEach {
                onFaceActions?.onFaceAvailable(it)
            }
        }.addOnCanceledListener {
            Log.d(TAG, "FACE DETECTION CANCEL ")
        }.addOnFailureListener {
            Log.d(TAG, "FACE DETECTION FAIL ")
        }.addOnCompleteListener {
            imageProxy.close()
        }

    }



    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        initForegroundService()
        return binder

    }
    override fun onUnbind(intent: Intent?): Boolean {
        // Stop the service from being in the foreground
        stopForeground(true)
        return super.onUnbind(intent)
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraProvider?.unbindAll()
        mediaPlayer?.release()
    }

    private fun playSound() {
       if(mediaPlayer ==null){
           mediaPlayer = MediaPlayer.create(this, currentSound)
           mediaPlayer!!.setOnCompletionListener { stopPlayer() }
       }
        mediaPlayer!!.start()
    }

    fun stopPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }

    companion object {
        const val TAG = "DrowsinessDetection"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "channel_id"
    }
}