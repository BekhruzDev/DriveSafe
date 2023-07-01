package com.example.drivesafe.ui

import android.os.Bundle
import com.example.drivesafe.R
import com.example.drivesafe.databinding.MainLayoutBinding
import com.example.drivesafe.base.BaseActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SampleActivity : BaseActivity<MainLayoutBinding>(MainLayoutBinding::inflate) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

}