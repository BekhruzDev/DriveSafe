package com.example.drivesafe.ui.usb_camera_live_preview

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import android.view.Surface
import com.example.drivesafe.databinding.ActivityUsbCameraLivePreviewBinding
import com.example.drivesafe.facedetector.FaceDetectorProcessor
import com.example.drivesafe.mlkit_utils.FrameMetadata
import com.example.drivesafe.mlkit_utils.GraphicOverlay
import com.example.drivesafe.mlkit_utils.VisionImageProcessor
import com.example.drivesafe.preference.PreferenceUtils
import com.example.drivesafe.ui.base.BaseActivity
import com.example.drivesafe.utils.view_utils.showToast
import com.serenegiant.usb.IFrameCallback
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener
import com.serenegiant.usbcameracommon.UVCCameraHandler
import dagger.hilt.android.AndroidEntryPoint
import java.nio.ByteBuffer

@AndroidEntryPoint
class UsbCameraLivePreviewActivity :
    BaseActivity<ActivityUsbCameraLivePreviewBinding>(ActivityUsbCameraLivePreviewBinding::inflate) {

    private var usbMonitor: USBMonitor? = null
    private var cameraUvcHandler: UVCCameraHandler? = null
    private var frameCallback: IFrameCallback? = null
    private var graphicOverlay: GraphicOverlay? = null
    private lateinit var onDeviceConnectListener: OnDeviceConnectListener

    //FrameProcessing
    private var processingThread: Thread? = null
    private var processingRunnable: FrameProcessingRunnable? = null
    private val processorLock = Object()
    private var frameProcessor: VisionImageProcessor? = null
    private var buf_sz = 4096
    private var buf = ByteArray(buf_sz)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initOnDeviceConnectedListener()
        initFrameCallback()
        graphicOverlay = binding.graphicOverlay
        processingRunnable = FrameProcessingRunnable()
        binding.cameraView.aspectRatio = PREVIEW_WIDTH * 1.0 / PREVIEW_HEIGHT

        usbMonitor = USBMonitor(this, onDeviceConnectListener)
        cameraUvcHandler = UVCCameraHandler.createHandler(
            this,
            binding.cameraView,
            frameCallback,
            if (USE_SURFACE_ENCODER) 0 else 1,
            PREVIEW_WIDTH,
            PREVIEW_HEIGHT,
            PREVIEW_MODE
        )
        val faceDetectorOptions = PreferenceUtils.getFaceDetectorOptions(this)
        setMachineLearningFrameProcessor(FaceDetectorProcessor(this, faceDetectorOptions))

    }

    override fun onStart() {
        super.onStart()
        usbMonitor?.register()
        binding.cameraView.onResume()
    }

    override fun onStop() {
        cameraUvcHandler?.close()
        binding.cameraView.onPause()
        //setCameraButton(false)
        super.onStop()
    }

    override fun onDestroy() {
        cameraUvcHandler?.release()
        cameraUvcHandler = null
        if (usbMonitor != null) {
            usbMonitor!!.destroy()
            usbMonitor = null
        }
        ///binding.cameraView = null
        super.onDestroy()
    }

    private fun initOnDeviceConnectedListener() {
        onDeviceConnectListener = object : OnDeviceConnectListener {
            override fun onAttach(device: UsbDevice?) {
                showToast("onAttach: ${device?.deviceName} device")
            }

            override fun onDetach(device: UsbDevice?) {
                showToast("onDetach: ${device?.deviceName} device")
            }

            override fun onConnect(
                device: UsbDevice?,
                ctrlBlock: USBMonitor.UsbControlBlock?,
                createNew: Boolean
            ) {
                showToast("onConnect: ${device?.deviceName} device")
                cameraUvcHandler?.open(ctrlBlock)
                startPreview()

            }

            override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                showToast("onDisconnect: ${device?.deviceName} device")
                queueEvent(0) { cameraUvcHandler?.close() }
            }

            override fun onCancel(device: UsbDevice?) {
                showToast("onCancel: ${device?.deviceName} device")
            }

        }
    }

    fun initFrameCallback() {
        frameCallback = IFrameCallback { frame: ByteBuffer ->
            //process the ByteBuffer
            val n = frame.limit()
            if (buf_sz < n) {
                buf_sz = n
                buf =  ByteArray(n)
            }
            frame.get(buf, 0, n)
            processingRunnable?.setNextFrame(ByteBuffer.wrap(buf))
        }
    }

    fun setMachineLearningFrameProcessor(processor: VisionImageProcessor) {
        synchronized(processorLock) {
            cleanScreen()
            frameProcessor?.stop()
            frameProcessor = processor
        }
    }

    private fun startPreview() {
        val st: SurfaceTexture? = binding.cameraView.surfaceTexture
        cameraUvcHandler?.startPreview(Surface(st))
            processingThread = Thread(processingRunnable)
            processingRunnable!!.setActive(true)
            processingThread!!.start()
    }

    private fun cleanScreen() {
        graphicOverlay?.clear()
    }


    companion object {
        private const val USE_SURFACE_ENCODER = false
        private const val PREVIEW_WIDTH = 640
        private const val PREVIEW_HEIGHT = 480
        private const val PREVIEW_MODE = 1
        const val SETTINGS_HIDE_DELAY_MS = 2500
    }

    private inner class FrameProcessingRunnable : Runnable {

        // This lock guards all of the member variables below.
        private val lock = Object()
        private var active = true

        // These pending variables hold the state associated with the new frame awaiting processing.
        private var pendingFrameData: ByteBuffer? = null

        /** Marks the runnable as active/not active. Signals any blocked threads to continue. */
        fun setActive(active: Boolean) {
            synchronized(lock) {
                this.active = active
                lock.notifyAll()
            }
        }

        /**
         * Sets the frame data received from the camera. This adds the previous unused frame buffer (if
         * present) back to the camera, and keeps a pending reference to the frame data for future use.
         */
        @Suppress("ByteBufferBackingArray")
        fun setNextFrame(data: ByteBuffer) {
            synchronized(lock) {
                if (pendingFrameData != null) {
                    // camera.addCallbackBuffer(pendingFrameData!!.array())
                    pendingFrameData = null
                }
                pendingFrameData = data
                // Notify the processor thread if it is waiting on the next frame (see below).
                lock.notifyAll()
            }
        }

        /**
         * As long as the processing thread is active, this executes detection on frames continuously.
         * The next pending frame is either immediately available or hasn't been received yet. Once it
         * is available, we transfer the frame info to local variables and run detection on that frame.
         * It immediately loops back for the next frame without pausing.
         *
         * <p>If detection takes longer than the time in between new frames from the camera, this will
         * mean that this loop will run without ever waiting on a frame, avoiding any context switching
         * or frame acquisition time latency.
         *
         * <p>If you find that this is using more CPU than you'd like, you should probably decrease the
         * FPS setting above to allow for some idle time in between frames.
         */
        @SuppressLint("InlinedApi")
        @Suppress("GuardedBy", "ByteBufferBackingArray")
        override fun run() {
            var data: ByteBuffer

            while (true) {
                synchronized(lock) {
                    while (active && (pendingFrameData == null)) {
                        try {
                            // Wait for the next frame to be received from the camera, since we
                            // don't have it yet.
                            lock.wait()
                        } catch (e: InterruptedException) {
                            Log.d(TAG, "Frame processing loop terminated.", e)
                            return
                        }
                    }

                    if (!active) {
                        // Exit the loop once this camera source is stopped or released.  We check
                        // this here, immediately after the wait() above, to handle the case where
                        // setActive(false) had been called, triggering the termination of this
                        // loop.
                        return
                    }

                    // Hold onto the frame data locally, so that we can use this for detection
                    // below.  We need to clear pendingFrameData to ensure that this buffer isn't
                    // recycled back to the camera before we are done using that data.
                    data = pendingFrameData!!
                    pendingFrameData = null
                }

                // The code below needs to run outside of synchronization, because this will allow
                // the camera to add pending frame(s) while we are running detection on the current
                // frame.

                try {
                    synchronized(processorLock) {
                        frameProcessor?.processByteBuffer(
                            data,
                            FrameMetadata.Builder()
                                .setWidth(cameraUvcHandler?.width!!)
                                .setHeight(cameraUvcHandler?.height!!)
                                .setRotation(0)
                                .build(),
                            graphicOverlay
                        )
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "Exception thrown from receiver.", t)
                }
            }
        }
    }


}