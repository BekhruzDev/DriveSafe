package com.bekhruzdev.drivesafe.ui

import android.os.Bundle
import com.bekhruzdev.drivesafe.databinding.MainLayoutBinding
import com.bekhruzdev.drivesafe.base.BaseActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SampleActivity : BaseActivity<MainLayoutBinding>(MainLayoutBinding::inflate) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

}