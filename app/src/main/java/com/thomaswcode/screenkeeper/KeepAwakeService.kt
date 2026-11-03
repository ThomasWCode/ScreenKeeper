package com.thomaswcode.screenkeeper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.service.quicksettings.TileService
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager

// Foreground service that holds an invisible 1x1 overlay window with FLAG_KEEP_SCREEN_ON.
// While that window is attached, the display will not time out, whichever app is in front.
class KeepAwakeService : Service() {

    companion object {
        private const val TAG = "KeepAwakeService"
        private const val CHANNEL_ID = "keep_awake"
        private const val NOTIFICATION_ID = 1

        @Volatile
        var isRunning = false
            private set

        // Called on the main thread whenever isRunning changes (used by MainActivity)
        var stateListener: (() -> Unit)? = null

        // Returns false if Android refused to start the service from the background
        fun start(context: Context): Boolean {
            Prefs.setEnabled(context, true)
            return try {
                context.startForegroundService(Intent(context, KeepAwakeService::class.java))
                true
            } catch (e: IllegalStateException) {
                // ForegroundServiceStartNotAllowedException (API 31+) extends IllegalStateException
                Log.w(TAG, "Foreground service start refused", e)
                false
            }
        }

        fun stop(context: Context) {
            Prefs.setEnabled(context, false)
            context.stopService(Intent(context, KeepAwakeService::class.java))
        }
    }

    private var overlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, buildNotification())
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
            stopSelf()
            return START_NOT_STICKY
        }

        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission missing; stopping")
            stopSelf()
            return START_NOT_STICKY
        }

        addOverlay()
        setRunning(overlayView != null)
        if (overlayView == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Ask Android to recreate the service if it is killed
        return START_STICKY
    }

    override fun onDestroy() {
        removeOverlay()
        setRunning(false)
        super.onDestroy()
    }

    private fun addOverlay() {
        if (overlayView != null) return
        val windowManager = getSystemService(WindowManager::class.java)
        val params = WindowManager.LayoutParams(
            1,
            1,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // At or below 0.8 the window never counts as obscuring touches (Android 12+)
            alpha = 0.8f
            title = "ScreenKeeper"
        }
        val view = View(this)
        try {
            windowManager.addView(view, params)
            overlayView = view
        } catch (e: Exception) {
            Log.e(TAG, "Could not add overlay window", e)
        }
    }

    private fun removeOverlay() {
        val view = overlayView ?: return
        try {
            getSystemService(WindowManager::class.java).removeView(view)
        } catch (e: Exception) {
            Log.w(TAG, "Could not remove overlay window", e)
        }
        overlayView = null
    }

    private fun setRunning(running: Boolean) {
        isRunning = running
        stateListener?.invoke()
        TileService.requestListeningState(this, ComponentName(this, KeepAwakeTileService::class.java))
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_description)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(openApp)
            .setOngoing(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return builder.build()
    }
}
