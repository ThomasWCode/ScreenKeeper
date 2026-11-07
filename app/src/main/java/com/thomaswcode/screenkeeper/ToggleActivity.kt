package com.thomaswcode.screenkeeper

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings

// Invisible activity opened by the home screen widget: toggles KeepAwakeService, then closes.
// Starting the service from here counts as a foreground start
class ToggleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when {
            KeepAwakeService.isRunning -> KeepAwakeService.stop(this)
            // Permission must be granted from the app first
            !Settings.canDrawOverlays(this) -> startActivity(Intent(this, MainActivity::class.java))
            else -> KeepAwakeService.start(this)
        }
        finish()
    }
}
