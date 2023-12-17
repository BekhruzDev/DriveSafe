package com.bekhruzdev.drivesafe.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ExperimentalGetImage
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bekhruzdev.drivesafe.databinding.ActivityMainBinding
import com.bekhruzdev.drivesafe.ui.camerax_live_preview.CameraXLivePreviewActivity
import com.bekhruzdev.drivesafe.ui.live_preview.LivePreviewActivity
import com.bekhruzdev.drivesafe.utils.view_utils.manageVisibility
import com.bekhruzdev.drivesafe.utils.view_utils.showAlertDialog
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.serenegiant.utils.PermissionCheck.openSettings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@ExperimentalGetImage
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding:ActivityMainBinding
    @RequiresApi(Build.VERSION_CODES.M)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        when (isGranted) {
            true -> {
                //permission granted
                initCameraXActivity()
            }
            false -> {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    //when denied for one time
                    showAlertDialog(
                        this,
                        "Attention",
                        "This app is heavily dependant on the device's camera to track sleepiness.\n" +
                                "Please, grant camera permission to app.",
                        yesClicked = ::requestCallPermissionWithSystemDialog,
                        noClicked = ::finish
                    )
                } else {
                    //when denied forever, open settings
                    showAlertDialog(
                        this,
                        "Attention",
                        "This app is heavily dependant on the device's camera to track sleepiness.\n" +
                                "Please, open settings and grant camera permission to app.",
                        yesText = "Open settings",
                        yesClicked = {
                            openSettings(this)
                            finish()
                        },
                        noClicked = ::finish
                    )

                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        Firebase.analytics.logEvent("open_app", null)
        setContentView(binding.root)
        lifecycleScope.launch(Dispatchers.Main) {
            delay(3000)
            requestCameraPermission(
                onGranted = ::initCameraXActivity,
                onDenied = {
                    binding.progressBar.manageVisibility(false)
                    requestCallPermissionWithSystemDialog()
                }
            )
        }
    }

    private fun initCameraXActivity() {
        val intent = Intent(this, CameraXLivePreviewActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun initTestPreviewActivity() {
        val intent = Intent(this, TestPreviewActivity::class.java)
        startActivity(intent)
    }

    private fun initLivePreviewActivity() {
        val intent = Intent(this, LivePreviewActivity::class.java)
        startActivity(intent)
    }

    private fun requestCameraPermission(
        onGranted: () -> Unit = {},
        onDenied: () -> Unit = {}
    ) {
        if (ContextCompat.checkSelfPermission(
                this,
                CAMERA_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onGranted.invoke()
        } else {
            onDenied.invoke()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestCallPermissionWithSystemDialog() {
        requestPermissionLauncher.launch(CAMERA_PERMISSION)
    }

    companion object {
        const val TAG = "MainActivity"
        const val CAMERA_PERMISSION = Manifest.permission.CAMERA
    }

}