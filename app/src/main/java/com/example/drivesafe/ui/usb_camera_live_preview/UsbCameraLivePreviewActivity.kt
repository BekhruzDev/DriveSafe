package com.example.drivesafe.ui.usb_camera_live_preview

import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.view.Surface
import com.example.drivesafe.databinding.ActivityUsbCameraLivePreviewBinding
import com.example.drivesafe.ui.base.BaseActivity
import com.example.drivesafe.utils.view_utils.showToast
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener
import com.serenegiant.usbcameracommon.UVCCameraHandler
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UsbCameraLivePreviewActivity :
    BaseActivity<ActivityUsbCameraLivePreviewBinding>(ActivityUsbCameraLivePreviewBinding::inflate) {

    private var usbMonitor: USBMonitor? = null
    private var cameraUvcHandler: UVCCameraHandler? = null
    private lateinit var onDeviceConnectListener:OnDeviceConnectListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initOnDeviceConnectedListener()
        binding.cameraView.aspectRatio = PREVIEW_WIDTH * 1.0 / PREVIEW_HEIGHT

        usbMonitor = USBMonitor(this, onDeviceConnectListener)
        cameraUvcHandler = UVCCameraHandler.createHandler(
            this,
            binding.cameraView,
            if (USE_SURFACE_ENCODER) 0 else 1,
            PREVIEW_WIDTH,
            PREVIEW_HEIGHT,
            PREVIEW_MODE
        )

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
                queueEvent(0){ cameraUvcHandler?.close() }
            }

            override fun onCancel(device: UsbDevice?) {
                showToast("onCancel: ${device?.deviceName} device")
            }

        }
    }

    private fun startPreview() {
        val st: SurfaceTexture? = binding.cameraView.surfaceTexture
        cameraUvcHandler?.startPreview(Surface(st))
        //runOnUiThread { mCaptureButton.setVisibility(View.VISIBLE) }
       // updateItems()
    }

    companion object {
            private const val USE_SURFACE_ENCODER = false
            private const val PREVIEW_WIDTH = 640
            private const val PREVIEW_HEIGHT = 480
            private const val PREVIEW_MODE = 1
            const val SETTINGS_HIDE_DELAY_MS = 2500
        }


}