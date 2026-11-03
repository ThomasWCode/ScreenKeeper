package com.thomaswcode.screenkeeper

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    companion object {
        private const val REQUEST_NOTIFICATIONS = 1
    }

    private lateinit var statusText: TextView
    private lateinit var toggleButton: Button
    private lateinit var bootSwitch: Switch
    private lateinit var overlayStatus: TextView
    private lateinit var overlayButton: Button
    private lateinit var notificationStatus: TextView
    private lateinit var notificationButton: Button
    private lateinit var batteryStatus: TextView
    private lateinit var batteryButton: Button

    private var notificationsDenied = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.status_text)
        toggleButton = findViewById(R.id.toggle_button)
        bootSwitch = findViewById(R.id.boot_switch)
        overlayStatus = findViewById(R.id.overlay_status)
        overlayButton = findViewById(R.id.overlay_button)
        notificationStatus = findViewById(R.id.notification_status)
        notificationButton = findViewById(R.id.notification_button)
        batteryStatus = findViewById(R.id.battery_status)
        batteryButton = findViewById(R.id.battery_button)

        toggleButton.setOnClickListener { onToggle() }
        bootSwitch.setOnCheckedChangeListener { _, checked -> Prefs.setStartOnBoot(this, checked) }
        overlayButton.setOnClickListener { openOverlaySettings() }
        notificationButton.setOnClickListener { requestNotifications() }
        batteryButton.setOnClickListener { requestBatteryExemption() }
    }

    override fun onResume() {
        super.onResume()
        KeepAwakeService.stateListener = { runOnUiThread { refresh() } }
        // Restart if the user wants it on but the service died (e.g. force-stopped)
        if (Prefs.isEnabled(this) && !KeepAwakeService.isRunning && Settings.canDrawOverlays(this)) {
            KeepAwakeService.start(this)
        }
        refresh()
    }

    override fun onPause() {
        KeepAwakeService.stateListener = null
        super.onPause()
    }

    private fun onToggle() {
        if (KeepAwakeService.isRunning) {
            KeepAwakeService.stop(this)
        } else if (!Settings.canDrawOverlays(this)) {
            openOverlaySettings()
        } else {
            KeepAwakeService.start(this)
        }
        refresh()
    }

    private fun refresh() {
        val running = KeepAwakeService.isRunning
        statusText.setText(if (running) R.string.status_on else R.string.status_off)
        toggleButton.setText(if (running) R.string.action_turn_off else R.string.action_turn_on)
        bootSwitch.isChecked = Prefs.isStartOnBoot(this)

        val overlayOk = Settings.canDrawOverlays(this)
        overlayStatus.setText(if (overlayOk) R.string.granted else R.string.overlay_required)
        overlayButton.visibility = if (overlayOk) View.GONE else View.VISIBLE

        val notificationsOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        notificationStatus.setText(if (notificationsOk) R.string.granted else R.string.notification_recommended)
        notificationButton.visibility = if (notificationsOk) View.GONE else View.VISIBLE

        val batteryOk = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
        batteryStatus.setText(if (batteryOk) R.string.battery_unrestricted else R.string.battery_recommended)
        batteryButton.visibility = if (batteryOk) View.GONE else View.VISIBLE
    }

    private fun openOverlaySettings() {
        startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        )
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsDenied) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        } else {
            // Dialog no longer shown after repeated denials; send the user to settings
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATIONS) {
            notificationsDenied = grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED
            refresh()
        }
    }

    private fun requestBatteryExemption() {
        try {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
            )
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }
}
