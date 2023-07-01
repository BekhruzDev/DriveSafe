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

package com.example.drivesafe.ui.live_preview

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.drivesafe.R
import com.example.drivesafe.databinding.ActivityVisionLivePreviewBinding
import com.example.drivesafe.facedetector.FaceDetectorProcessor
import com.example.drivesafe.mlkit_utils.CameraSource
import com.example.drivesafe.mlkit_utils.CameraSourcePreview
import com.example.drivesafe.mlkit_utils.GraphicOverlay
import com.example.drivesafe.preference.PreferenceUtils
import com.example.drivesafe.base.BaseActivity
import com.google.android.gms.common.annotation.KeepName
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException

/** Live preview demo for ML Kit APIs. */
@AndroidEntryPoint
@KeepName
class LivePreviewActivity :
  BaseActivity<ActivityVisionLivePreviewBinding>(ActivityVisionLivePreviewBinding::inflate) {

  private var cameraSource: CameraSource? = null
  private var preview: CameraSourcePreview? = null
  private var graphicOverlay: GraphicOverlay? = null
  private var selectedModel = FACE_DETECTION

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    preview = findViewById(R.id.preview_view)
    graphicOverlay = findViewById(R.id.graphic_overlay)
    createCameraSource(selectedModel)
  }


  private fun createCameraSource(model: String) {
    // If there's no existing cameraSource, create one.
    if (cameraSource == null) {
      cameraSource = CameraSource(this, graphicOverlay)
      cameraSource?.setFacing(CameraSource.CAMERA_FACING_FRONT)
    }
    try {
          Log.i(TAG, "Using Face Detector Processor")
          val faceDetectorOptions = PreferenceUtils.getFaceDetectorOptions(this)
          cameraSource!!.setMachineLearningFrameProcessor(
            FaceDetectorProcessor(this, faceDetectorOptions)
          )
    } catch (e: Exception) {
      Log.e(TAG, "Can not create image processor: $model", e)
      Toast.makeText(
          applicationContext,
          "Can not create image processor: " + e.message,
          Toast.LENGTH_LONG
        )
        .show()
    }
  }

  /**
   * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
   * (e.g., because onResume was called before the camera source was created), this will be called
   * again when the camera source is created.
   */
  private fun startCameraSource() {
    if (cameraSource != null) {
      try {
        if (preview == null) {
          Log.d(TAG, "resume: Preview is null")
        }
        if (graphicOverlay == null) {
          Log.d(TAG, "resume: graphOverlay is null")
        }
        preview!!.start(cameraSource, graphicOverlay)
      } catch (e: IOException) {
        Log.e(TAG, "Unable to start camera source.", e)
        cameraSource!!.release()
        cameraSource = null
      }
    }
  }

  public override fun onResume() {
    super.onResume()
    Log.d(TAG, "onResume")
    createCameraSource(selectedModel)
    startCameraSource()
  }

  /** Stops the camera. */
  override fun onPause() {
    super.onPause()
    preview?.stop()
  }

  public override fun onDestroy() {
    super.onDestroy()
    if (cameraSource != null) {
      cameraSource?.release()
    }
  }

  companion object {
    private const val FACE_DETECTION = "Face Detection"
    private const val TAG = "LivePreviewActivity"
  }
}
