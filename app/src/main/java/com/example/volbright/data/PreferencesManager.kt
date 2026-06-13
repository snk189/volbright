package com.example.volbright.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("volbright_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_EXCLUDED_APPS = "excluded_apps"
        private const val KEY_OVERLAY_OPACITY = "overlay_opacity"
        private const val KEY_OVERLAY_SIZE = "overlay_size"
        private const val KEY_IS_ENABLED = "is_enabled"
        private const val KEY_OVERLAY_X_PERCENT = "overlay_x_percent"
        private const val KEY_OVERLAY_Y_PERCENT = "overlay_y_percent"
        private const val KEY_OVERLAY2_X_PERCENT = "overlay2_x_percent"
        private const val KEY_OVERLAY2_Y_PERCENT = "overlay2_y_percent"
        private const val KEY_DRAG_SENSITIVITY = "drag_sensitivity"
        private const val KEY_VOLUME_STREAM = "volume_stream"
    }

    var excludedApps: Set<String>
        get() = prefs.getStringSet(KEY_EXCLUDED_APPS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_EXCLUDED_APPS, value).apply()

    var overlayOpacity: Float
        get() = prefs.getFloat(KEY_OVERLAY_OPACITY, 0.8f)
        set(value) = prefs.edit().putFloat(KEY_OVERLAY_OPACITY, value).apply()

    var overlaySize: Int
        get() = prefs.getInt(KEY_OVERLAY_SIZE, 150) // Default size 150px
        set(value) = prefs.edit().putInt(KEY_OVERLAY_SIZE, value).apply()

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_IS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_ENABLED, value).apply()
        
    var overlayXPercent: Float
        get() = prefs.getFloat(KEY_OVERLAY_X_PERCENT, 0.1f)
        set(value) = prefs.edit().putFloat(KEY_OVERLAY_X_PERCENT, value).apply()
        
    var overlayYPercent: Float
        get() = prefs.getFloat(KEY_OVERLAY_Y_PERCENT, 0.2f)
        set(value) = prefs.edit().putFloat(KEY_OVERLAY_Y_PERCENT, value).apply()

    var overlay2XPercent: Float
        get() = prefs.getFloat(KEY_OVERLAY2_X_PERCENT, 0.1f)
        set(value) = prefs.edit().putFloat(KEY_OVERLAY2_X_PERCENT, value).apply()
        
    var overlay2YPercent: Float
        get() = prefs.getFloat(KEY_OVERLAY2_Y_PERCENT, 0.5f)
        set(value) = prefs.edit().putFloat(KEY_OVERLAY2_Y_PERCENT, value).apply()

    var dragSensitivity: Float
        get() = prefs.getFloat(KEY_DRAG_SENSITIVITY, 1000f) // default to 1000px for full sweep
        set(value) = prefs.edit().putFloat(KEY_DRAG_SENSITIVITY, value).apply()

    var volumeStream: Int
        get() = prefs.getInt(KEY_VOLUME_STREAM, android.media.AudioManager.STREAM_MUSIC)
        set(value) = prefs.edit().putInt(KEY_VOLUME_STREAM, value).apply()
}
