package com.example.drivesafe.facedetector

import com.google.mlkit.vision.face.Face

interface OnFaceActions {
    fun onFaceAvailable(face: Face)
}