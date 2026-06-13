package com.example.volbright

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.volbright.data.PreferencesManager

class AppForegroundTracker : AccessibilityService() {

    private lateinit var prefs: PreferencesManager

    private var lastVisibilityState = true
    private var lastKnownAppPackage = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = PreferencesManager(this)
        Log.d("VolBright", "AccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        var isKeyboardVisible = false
        var currentAppPackage = lastKnownAppPackage

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            val windowsList = windows
            for (window in windowsList) {
                if (window.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                    isKeyboardVisible = true
                }
                if (window.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION && window.isActive) {
                    val pkg = window.root?.packageName?.toString()
                    if (!pkg.isNullOrEmpty()) {
                        currentAppPackage = pkg
                    }
                }
            }
        }
        
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString()
            if (!pkg.isNullOrEmpty() && !pkg.contains("systemui") && !pkg.contains("inputmethod")) {
                currentAppPackage = pkg
            }
        }
        
        lastKnownAppPackage = currentAppPackage
        
        val shouldHide = isKeyboardVisible || (currentAppPackage.isNotEmpty() && prefs.excludedApps.contains(currentAppPackage))
        val shouldShow = !shouldHide
        
        if (shouldShow != lastVisibilityState) {
            lastVisibilityState = shouldShow
            val intent = Intent(this, FloatingWindowService::class.java).apply {
                action = FloatingWindowService.ACTION_UPDATE_VISIBILITY
                putExtra(FloatingWindowService.EXTRA_IS_VISIBLE, shouldShow)
            }
            try {
                startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onInterrupt() {
        Log.d("VolBright", "AccessibilityService interrupted")
    }
}
