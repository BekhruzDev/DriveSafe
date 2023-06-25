package com.example.drivesafe.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

open class BaseActivity<VB:ViewBinding>(val inflater:(LayoutInflater) -> VB) : AppCompatActivity() {

    lateinit var binding: VB
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val workerScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = inflater(layoutInflater)
        setContentView(binding.root)
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        // Cancel all coroutines
        uiScope.cancel()
        workerScope.cancel()
        super.onDestroy()
    }


    fun runOnUIThread(duration: Long, task: suspend () -> Unit) {
        uiScope.launch {
            delay(duration)
            task()
        }
    }


    fun queueEvent( delayMillis: Long = 0L, task: suspend () -> Unit,) {
        workerScope.launch {
            delay(delayMillis)
            task()
        }
    }

    companion object{
        const val TAG = "BaseActivity"

    }
}
