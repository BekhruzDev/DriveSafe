package com.bekhruzdev.drivesafe.ui

import android.os.Bundle
import android.preference.PreferenceFragment
import androidx.appcompat.app.AppCompatActivity
import com.bekhruzdev.drivesafe.R
import com.bekhruzdev.drivesafe.preference.CameraXLivePreviewPreferenceFragment
import com.bekhruzdev.drivesafe.preference.LivePreviewPreferenceFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * Hosts the preference fragment to configure settings for a demo activity that specified by the
 * [LaunchSource].
 */
@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    enum class LaunchSource(
        val titleResId: Int,
        val prefFragmentClass: Class<out PreferenceFragment>
    ) {
        LIVE_PREVIEW(
            R.string.pref_screen_title_live_preview,
            LivePreviewPreferenceFragment::class.java
        ),
        CAMERAX_LIVE_PREVIEW(
            R.string.pref_screen_title_camerax_live_preview,
            CameraXLivePreviewPreferenceFragment::class.java
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val launchSource = intent.getSerializableExtra(EXTRA_LAUNCH_SOURCE) as LaunchSource
        supportActionBar?.setTitle(launchSource.titleResId)
        try {
            fragmentManager
                .beginTransaction()
                .replace(
                    R.id.settings_container,
                    launchSource.prefFragmentClass.getDeclaredConstructor().newInstance()
                )
                .commit()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    companion object {
        const val EXTRA_LAUNCH_SOURCE = "extra_launch_source"
    }
}
