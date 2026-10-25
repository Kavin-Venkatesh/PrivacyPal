package com.privacypal.app.data.watchdog

import android.content.Context
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Whether the screen is on (interactive). Use while the screen is off is the most suspicious kind. */
fun interface ScreenState {
    fun isOn(): Boolean
}

class PowerManagerScreenState @Inject constructor(@ApplicationContext private val context: Context) : ScreenState {
    override fun isOn(): Boolean = context.getSystemService(PowerManager::class.java).isInteractive
}
