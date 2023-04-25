package com.example.drivesafe.mlkit_utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.drivesafe.R
import com.example.drivesafe.camerax_live_preview.CameraXLivePreviewActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.ArrayList

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        lifecycleScope.launch(Dispatchers.Main) {
            delay(4000)
            initCameraXActivity()
            finish()
        }
    }

    private fun initCameraXActivity() {
        val intent = Intent(this, CameraXLivePreviewActivity::class.java)
        startActivity(intent)
        if (!allRuntimePermissionsGranted()) {
            getRuntimePermissions()
        }
    }

    private fun allRuntimePermissionsGranted(): Boolean {
            for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
                permission.let {
                    if (!isPermissionGranted(this, it)) {
                        return false
                    }
                }
            }
            return true
        }

        private fun getRuntimePermissions() {
            val permissionsToRequest = ArrayList<String>()
            for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
                permission.let {
                    if (!isPermissionGranted(this, it)) {
                        permissionsToRequest.add(permission)
                    }
                }
            }

            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toTypedArray(),
                    PERMISSION_REQUESTS
                )
            }
        }

        private fun isPermissionGranted(context: Context, permission: String): Boolean {
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.i(TAG, "Permission granted: $permission")
                return true
            }
            Log.i(TAG, "Permission NOT granted: $permission")
            return false
        }

        companion object {
            private const val TAG = "MainActivity"
            private const val PERMISSION_REQUESTS = 1
            private val REQUIRED_RUNTIME_PERMISSIONS =
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
        }

}