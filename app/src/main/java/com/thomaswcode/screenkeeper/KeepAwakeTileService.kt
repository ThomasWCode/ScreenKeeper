package com.thomaswcode.screenkeeper

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

// Quick Settings tile that toggles KeepAwakeService
class KeepAwakeTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile(KeepAwakeService.isRunning)
    }

    override fun onClick() {
        super.onClick()
        if (KeepAwakeService.isRunning) {
            KeepAwakeService.stop(this)
            updateTile(false)
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            // Permission must be granted from the app first
            launch(MainActivity::class.java)
            return
        }
        if (KeepAwakeService.start(this)) {
            updateTile(true)
        } else {
            // Background start refused: start it from a (foreground) invisible activity instead
            launch(StartActivity::class.java)
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun launch(activity: Class<*>) {
        val intent = Intent(this, activity).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTile(active: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = getString(if (active) R.string.tile_on else R.string.tile_off)
        tile.updateTile()
    }
}
