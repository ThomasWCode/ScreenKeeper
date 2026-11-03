package com.thomaswcode.screenkeeper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

// Restores the keep-on state after a reboot (if enabled) or after the app is updated
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!Prefs.isEnabled(context) || !Settings.canDrawOverlays(context)) return
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                if (Prefs.isStartOnBoot(context)) {
                    KeepAwakeService.start(context)
                } else {
                    Prefs.setEnabled(context, false)
                }
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> KeepAwakeService.start(context)
        }
    }
}
