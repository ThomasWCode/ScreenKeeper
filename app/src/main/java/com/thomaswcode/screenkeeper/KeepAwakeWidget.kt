package com.thomaswcode.screenkeeper

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

// Home screen widget: one button that toggles KeepAwakeService and shows whether it is on
class KeepAwakeWidget : AppWidgetProvider() {

    companion object {
        // Redraws every placed widget (called whenever KeepAwakeService.isRunning changes)
        fun update(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, KeepAwakeWidget::class.java))
            if (ids.isNotEmpty()) manager.updateAppWidget(ids, buildViews(context))
        }

        private fun buildViews(context: Context): RemoteViews {
            val active = KeepAwakeService.isRunning
            val state = context.getString(if (active) R.string.tile_on else R.string.tile_off)
            // A tap opens the invisible ToggleActivity: a service started from an activity is a
            // foreground start, which a widget's broadcast is not guaranteed to be
            val toggle = PendingIntent.getActivity(
                context,
                0,
                Intent(context, ToggleActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            return RemoteViews(context.packageName, R.layout.widget_toggle).apply {
                setInt(
                    android.R.id.background,
                    "setBackgroundResource",
                    if (active) R.drawable.widget_background_on else R.drawable.widget_background_off
                )
                setTextViewText(R.id.widget_state, state)
                setContentDescription(android.R.id.background, context.getString(R.string.widget_state, state))
                setOnClickPendingIntent(android.R.id.background, toggle)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetManager.updateAppWidget(appWidgetIds, buildViews(context))
    }
}
