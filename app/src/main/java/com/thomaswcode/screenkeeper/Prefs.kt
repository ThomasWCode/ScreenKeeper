package com.thomaswcode.screenkeeper

import android.content.Context
import android.content.SharedPreferences

// Persisted user choices
object Prefs {
    private const val FILE = "screenkeeper"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_START_ON_BOOT = "start_on_boot"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // True while the user wants the screen kept on (survives process death / reboot)
    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, value).apply()
    }

    fun isStartOnBoot(context: Context): Boolean = prefs(context).getBoolean(KEY_START_ON_BOOT, true)

    fun setStartOnBoot(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_START_ON_BOOT, value).apply()
    }
}
