package com.bekhruzdev.drivesafe.ui.camerax_live_preview

import android.content.Context
import android.os.Bundle
import com.bekhruzdev.drivesafe.R
import com.google.android.material.bottomsheet.BottomSheetDialog

class SettingsBottomSheet(context: Context): BottomSheetDialog(context){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutInflater.inflate(R.layout.bottom_sheet_settings, null))
    }
}