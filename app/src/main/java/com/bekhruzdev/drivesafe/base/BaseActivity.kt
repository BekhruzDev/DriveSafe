package com.bekhruzdev.drivesafe.base

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.*

open class BaseActivity<VB:ViewBinding>(val inflater:(LayoutInflater) -> VB) : AppCompatActivity() {

    lateinit var binding: VB
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val workerScope = CoroutineScope(Dispatchers.Default)
    private var mediaPlayer:MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = inflater(layoutInflater)
        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()
        onInitUi()
    }
    override fun onPause() {
        super.onPause()
        stopPlayer()
    }

    override fun onStop() {
        super.onStop()
        stopPlayer()
    }
    open fun onInitUi(){}

    override fun onDestroy() {
        // Cancel all coroutines
        uiScope.cancel()
        workerScope.cancel()
        stopPlayer()
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

     fun playSound(soundRawRes:Int) {
        if(mediaPlayer ==null){
            mediaPlayer = MediaPlayer.create(this, soundRawRes)
            mediaPlayer!!.setOnCompletionListener { stopPlayer() }
        }
        mediaPlayer!!.start()
    }

    fun stopPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }
    companion object{
        const val TAG = "BaseActivity"

    }
}
