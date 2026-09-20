package com.example.volbright

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.ContextCompat
import com.example.volbright.data.PreferencesManager

class AppForegroundTracker : AccessibilityService() {

    private lateinit var prefs: PreferencesManager

    private var lastVisibilityState = true
    private var lastKnownAppPackage = ""
    
    private var automationState = 0
    private val handler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable { 
        if (automationState > 0) {
            android.widget.Toast.makeText(this, "Network Automation failed or timed out.", android.widget.Toast.LENGTH_SHORT).show()
        }
        automationState = 0 
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.volbright.ACTION_START_AUTOMATION") {
                startAutomation()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = PreferencesManager(this)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, IntentFilter("com.example.volbright.ACTION_START_AUTOMATION"), Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, IntentFilter("com.example.volbright.ACTION_START_AUTOMATION"))
        }
        
        Log.d("VolBright", "AccessibilityService connected")
    }

    private fun startAutomation() {
        automationState = 1
        handler.removeCallbacks(timeoutRunnable)
        handler.postDelayed(timeoutRunnable, 10000) // 10 seconds timeout
        
        val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
            startActivity(fallback)
        }
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
        
        // Ensure floating button doesn't hide when Settings app is open
        val shouldHide = isKeyboardVisible || (currentAppPackage.isNotEmpty() && currentAppPackage != "com.android.settings" && prefs.excludedApps.contains(currentAppPackage))
        val shouldShow = if (automationState > 0) true else !shouldHide
        
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
        
        if (automationState > 0) {
            handleAutomation()
        }

        // Real-time tracking: If the user manually clicks 4G or 5G inside Settings, sync the state!
        if (event?.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED && 
            (currentAppPackage == "com.android.settings" || currentAppPackage == "com.android.phone")) {
            val node = event.source
            if (node != null) {
                if (doesNodeContainText(node, prefs.network5gName)) {
                    prefs.is5GEnabled = true
                    startService(Intent(this, FloatingWindowService::class.java).apply {
                        action = FloatingWindowService.ACTION_SETTINGS_UPDATED
                    })
                } else if (doesNodeContainText(node, prefs.network4gName)) {
                    prefs.is5GEnabled = false
                    startService(Intent(this, FloatingWindowService::class.java).apply {
                        action = FloatingWindowService.ACTION_SETTINGS_UPDATED
                    })
                }
            }
        }
    }

    private fun doesNodeContainText(node: AccessibilityNodeInfo?, text: String): Boolean {
        if (node == null) return false
        if (node.text?.toString()?.contains(text, ignoreCase = true) == true || 
            node.contentDescription?.toString()?.contains(text, ignoreCase = true) == true) {
            return true
        }
        for (i in 0 until node.childCount) {
            if (doesNodeContainText(node.getChild(i), text)) return true
        }
        return false
    }

    private fun handleAutomation() {
        val rootNode = rootInActiveWindow ?: return

        when (automationState) {
            1 -> {
                if (clickNodeWithText(rootNode, prefs.simName)) {
                    automationState = 2
                }
            }
            2 -> {
                if (clickNodeWithText(rootNode, prefs.networkMenuName)) {
                    automationState = 3
                }
            }
            3 -> {
                // Toggle between 4G and 5G
                val nextMode = !prefs.is5GEnabled
                val targetText = if (nextMode) prefs.network5gName else prefs.network4gName
                if (clickNodeWithText(rootNode, targetText)) {
                    prefs.is5GEnabled = nextMode
                    automationState = 4
                    handler.removeCallbacks(timeoutRunnable)
                    
                    android.widget.Toast.makeText(this, "Switched to ${if (nextMode) "5G" else "4G"} successfully!", android.widget.Toast.LENGTH_SHORT).show()
                    
                    // Update FloatingWindowService UI
                    startService(Intent(this, FloatingWindowService::class.java).apply {
                        action = FloatingWindowService.ACTION_SETTINGS_UPDATED
                    })
                    
                    // Go back completely to previous app
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_BACK) }, 400)
                    handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_BACK) }, 800)
                    handler.postDelayed({ automationState = 0 }, 1000)
                }
            }
        }
    }

    private fun clickNodeWithText(node: AccessibilityNodeInfo, text: String): Boolean {
        if (node.text?.toString()?.contains(text, ignoreCase = true) == true || 
            node.contentDescription?.toString()?.contains(text, ignoreCase = true) == true) {
            
            var clickableNode: AccessibilityNodeInfo? = node
            while (clickableNode != null && !clickableNode.isClickable) {
                clickableNode = clickableNode.parent
            }
            if (clickableNode != null && clickableNode.isClickable) {
                if (clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true
                }
            }
            
            val rect = android.graphics.Rect()
            node.getBoundsInScreen(rect)
            if (!rect.isEmpty) {
                val path = android.graphics.Path()
                path.moveTo(rect.exactCenterX(), rect.exactCenterY())
                val gestureBuilder = android.accessibilityservice.GestureDescription.Builder()
                gestureBuilder.addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 50))
                dispatchGesture(gestureBuilder.build(), null, null)
                return true
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null && clickNodeWithText(child, text)) {
                return true
            }
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    override fun onInterrupt() {
        Log.d("VolBright", "AccessibilityService interrupted")
    }
}
