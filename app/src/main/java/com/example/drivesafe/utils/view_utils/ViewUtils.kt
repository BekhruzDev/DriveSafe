package com.example.drivesafe.utils.view_utils

import android.content.Context
import android.widget.Toast

fun Context.showToast(message:String = "This is a toast message"){
    Toast.makeText(this,message,Toast.LENGTH_SHORT).show()
}

fun Context.showToastLongTime(message:String = "This is a toast message"){
    Toast.makeText(this,message,Toast.LENGTH_LONG).show()
}