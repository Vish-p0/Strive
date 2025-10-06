package com.example.strive.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class WidgetConfigActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.example.strive.R.layout.activity_widget_config)

        val appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Implement UI: list habits and allow selection (single/double).
        // On save, persist config JSON to SharedPreferences key "pref_widget_config_<appWidgetId>"
        // Set result Intent with appWidgetId and finish to finalize widget addition.
        
        // For now, just complete the widget configuration with default
        val prefs = getSharedPreferences("pulse_prefs", MODE_PRIVATE)
        prefs.edit().putString("pref_widget_config_$appWidgetId", "{}").apply()
        
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }
}