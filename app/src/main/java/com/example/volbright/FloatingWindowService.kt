package com.example.volbright

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.example.volbright.data.PreferencesManager

class FloatingWindowService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_HIDE = "ACTION_HIDE"
        const val ACTION_SHOW = "ACTION_SHOW"
        const val ACTION_UPDATE_VISIBILITY = "ACTION_UPDATE_VISIBILITY"
        const val EXTRA_IS_VISIBLE = "EXTRA_IS_VISIBLE"
        const val ACTION_SETTINGS_UPDATED = "ACTION_SETTINGS_UPDATED"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "FloatingWindowServiceChannel"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var audioManager: AudioManager
    private lateinit var prefs: PreferencesManager
    
    private var volumeView: FrameLayout? = null
    private var volumeButton: ImageView? = null
    private lateinit var volumeParams: WindowManager.LayoutParams
    
    private var brightnessView: FrameLayout? = null
    private var brightnessButton: ImageView? = null
    private lateinit var brightnessParams: WindowManager.LayoutParams
    
    private var sliderView: FrameLayout? = null
    private var sliderProgressBar: android.widget.ProgressBar? = null
    private lateinit var sliderParams: WindowManager.LayoutParams
    
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        prefs = PreferencesManager(this)
        
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        createNotificationChannel()
        addFloatingWindows()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification())
                showWindows()
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_HIDE -> hideWindows()
            ACTION_SHOW -> showWindows()
            ACTION_UPDATE_VISIBILITY -> {
                val isVisible = intent.getBooleanExtra(EXTRA_IS_VISIBLE, true)
                if (isVisible) showWindows() else hideWindows()
            }
            ACTION_SETTINGS_UPDATED -> updateViewAppearance()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Window Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VolBright is running")
            .setContentText("Tap to open settings")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    private fun createLayoutParams(xPos: Int, yPos: Int): WindowManager.LayoutParams {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = xPos
        params.y = yPos
        return params
    }

    private fun getScreenSize(): Pair<Int, Int> {
        val displayMetrics = resources.displayMetrics
        return Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addFloatingWindows() {
        val (screenWidth, screenHeight) = getScreenSize()
        
        // --- Volume Button ---
        volumeView = FrameLayout(this)
        volumeButton = ImageView(this).apply {
            setImageResource(R.drawable.ic_volume)
            val pad = 24
            setPadding(pad, pad, pad, pad)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        volumeView?.addView(volumeButton)
        
        val x1 = (screenWidth * prefs.overlayXPercent).toInt()
        val y1 = (screenHeight * prefs.overlayYPercent).toInt()
        volumeParams = createLayoutParams(x1, y1)
        setupTouchListeners(volumeButton, volumeParams, volumeView, isVolume = true)

        // --- Brightness Button ---
        brightnessView = FrameLayout(this)
        brightnessButton = ImageView(this).apply {
            setImageResource(R.drawable.ic_brightness)
            val pad = 24
            setPadding(pad, pad, pad, pad)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        brightnessView?.addView(brightnessButton)
        
        val x2 = (screenWidth * prefs.overlay2XPercent).toInt()
        val y2 = (screenHeight * prefs.overlay2YPercent).toInt()
        brightnessParams = createLayoutParams(x2, y2)
        setupTouchListeners(brightnessButton, brightnessParams, brightnessView, isVolume = false)

        // --- Custom Brightness Slider ---
        sliderView = FrameLayout(this).apply {
            val pad = 64
            setPadding(pad, pad * 2, pad, pad)
            visibility = View.GONE
        }
        sliderProgressBar = android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 255
            progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FF9800"))
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.DKGRAY)
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 50)
            scaleY = 3f // Thick slider
        }
        sliderView?.addView(sliderProgressBar)
        
        sliderParams = createLayoutParams(0, 0)
        sliderParams.width = WindowManager.LayoutParams.MATCH_PARENT
        sliderParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL

        updateViewAppearance()
        
        try {
            windowManager.addView(volumeView, volumeParams)
            windowManager.addView(brightnessView, brightnessParams)
            windowManager.addView(sliderView, sliderParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateViewAppearance() {
        val size = prefs.overlaySize
        val opacity = prefs.overlayOpacity

        val buttonParams = FrameLayout.LayoutParams(size, size)
        
        volumeButton?.layoutParams = buttonParams
        volumeButton?.alpha = opacity
        setButtonColor(volumeButton, Color.parseColor("#80000000"))
        
        brightnessButton?.layoutParams = buttonParams
        brightnessButton?.alpha = opacity
        setButtonColor(brightnessButton, Color.parseColor("#80000000"))

        try {
            if (volumeView?.isAttachedToWindow == true) {
                windowManager.updateViewLayout(volumeView, volumeParams)
            }
            if (brightnessView?.isAttachedToWindow == true) {
                windowManager.updateViewLayout(brightnessView, brightnessParams)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setButtonColor(button: ImageView?, color: Int) {
        val drawable = android.graphics.drawable.GradientDrawable()
        drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
        drawable.setColor(color)
        button?.background = drawable
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListeners(
        button: ImageView?, 
        params: WindowManager.LayoutParams, 
        rootView: FrameLayout?, 
        isVolume: Boolean
    ) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var startValue = 0

        var isMoveMode = false
        val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
        val handler = Handler(Looper.getMainLooper())
        var longPressRunnable: Runnable? = null
        var isLongPressStarted = false

        button?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    
                    if (isVolume) {
                        startValue = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    } else {
                        try {
                            startValue = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                        } catch (e: Settings.SettingNotFoundException) {
                            startValue = 125
                        }
                    }
                    
                    isMoveMode = false
                    isLongPressStarted = false

                    // Highlight Green/Orange when touched for adjustment
                    val activeColor = if (isVolume) Color.parseColor("#4CAF50") else Color.parseColor("#FF9800")
                    setButtonColor(button, activeColor)

                    longPressRunnable = Runnable {
                        isMoveMode = true
                        isLongPressStarted = true
                        setButtonColor(button, Color.parseColor("#2196F3")) // Blue for move mode
                        vibrate()
                    }
                    handler.postDelayed(longPressRunnable!!, longPressTimeout)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY

                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        if (!isLongPressStarted) {
                            handler.removeCallbacks(longPressRunnable!!)
                        }
                    }

                    if (isMoveMode) {
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        windowManager.updateViewLayout(rootView, params)
                    } else if (!isLongPressStarted) {
                        // Adjustment mode
                        val sensitivity = prefs.dragSensitivity // default 1000f
                        // Drag up (-dy) or drag right (+dx) increases the value
                        val deltaFraction = (-dy + dx) / sensitivity

                        if (isVolume) {
                            val stream = prefs.volumeStream
                            val maxVol = audioManager.getStreamMaxVolume(stream)
                            val deltaVol = (deltaFraction * maxVol).toInt()
                            val newVol = (startValue + deltaVol).coerceIn(0, maxVol)
                            
                            audioManager.setStreamVolume(
                                stream,
                                newVol,
                                AudioManager.FLAG_SHOW_UI
                            )
                        } else {
                            if (Settings.System.canWrite(this)) {
                                sliderView?.visibility = View.VISIBLE
                                val maxBright = 255
                                val deltaBright = (deltaFraction * maxBright).toInt()
                                val newBright = (startValue + deltaBright).coerceIn(0, maxBright)
                                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, newBright)
                                sliderProgressBar?.progress = newBright
                            }
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    longPressRunnable?.let { handler.removeCallbacks(it) }
                    if (isMoveMode) {
                        val (screenWidth, screenHeight) = getScreenSize()
                        if (isVolume) {
                            prefs.overlayXPercent = params.x.toFloat() / screenWidth
                            prefs.overlayYPercent = params.y.toFloat() / screenHeight
                        } else {
                            prefs.overlay2XPercent = params.x.toFloat() / screenWidth
                            prefs.overlay2YPercent = params.y.toFloat() / screenHeight
                        }
                    }
                    isMoveMode = false
                    sliderView?.visibility = View.GONE
                    setButtonColor(button, Color.parseColor("#80000000")) // Revert to idle
                    true
                }
                else -> false
            }
        }
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(50)
        }
    }

    private fun hideWindows() {
        volumeView?.visibility = View.GONE
        brightnessView?.visibility = View.GONE
    }

    private fun showWindows() {
        volumeView?.visibility = View.VISIBLE
        brightnessView?.visibility = View.VISIBLE
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        val (screenWidth, screenHeight) = getScreenSize()
        
        if (::volumeParams.isInitialized && volumeView?.isAttachedToWindow == true) {
            volumeParams.x = (screenWidth * prefs.overlayXPercent).toInt()
            volumeParams.y = (screenHeight * prefs.overlayYPercent).toInt()
            windowManager.updateViewLayout(volumeView, volumeParams)
        }
        
        if (::brightnessParams.isInitialized && brightnessView?.isAttachedToWindow == true) {
            brightnessParams.x = (screenWidth * prefs.overlay2XPercent).toInt()
            brightnessParams.y = (screenHeight * prefs.overlay2YPercent).toInt()
            windowManager.updateViewLayout(brightnessView, brightnessParams)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (volumeView != null) windowManager.removeView(volumeView)
        if (brightnessView != null) windowManager.removeView(brightnessView)
        if (sliderView != null) windowManager.removeView(sliderView)
    }
}
