package com.example.volbright.ui

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.volbright.AppForegroundTracker
import com.example.volbright.FloatingWindowService
import com.example.volbright.data.PreferencesManager

fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
    val expectedComponentName = ComponentName(context, service)
    val enabledServicesSetting = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    val colonSplitter = TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServicesSetting)
    while (colonSplitter.hasNext()) {
        val componentNameString = colonSplitter.next()
        val enabledService = ComponentName.unflattenFromString(componentNameString)
        if (enabledService != null && enabledService == expectedComponentName) return true
    }
    return false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    prefs: PreferencesManager,
    onNavigateToExcludedApps: () -> Unit
) {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(prefs.isEnabled) }
    var opacity by remember { mutableFloatStateOf(prefs.overlayOpacity) }
    var size by remember { mutableFloatStateOf(prefs.overlaySize.toFloat()) }
    var sensitivity by remember { mutableFloatStateOf(prefs.dragSensitivity) }
    var volumeStream by remember { mutableIntStateOf(prefs.volumeStream) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var isWriteSettingsEnabled by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityEnabled = isAccessibilityServiceEnabled(context, AppForegroundTracker::class.java)
                isWriteSettingsEnabled = Settings.System.canWrite(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(opacity, size, sensitivity, volumeStream) {
        prefs.overlayOpacity = opacity
        prefs.overlaySize = size.toInt()
        prefs.dragSensitivity = sensitivity
        prefs.volumeStream = volumeStream
        if (isEnabled) {
            context.startService(Intent(context, FloatingWindowService::class.java).apply {
                action = FloatingWindowService.ACTION_SETTINGS_UPDATED
            })
        }
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF1A1C29), Color(0xFF0F1016))
    )
    
    val cardColor = Color(0xFF232536)
    val accentOrange = Color(0xFFFF9800)
    val accentCyan = Color(0xFF00E5FF)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VolBright", fontWeight = FontWeight.ExtraBold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                
                // Enable Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable VolBright", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { checked ->
                                if (checked && !Settings.canDrawOverlays(context)) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                } else {
                                    isEnabled = checked
                                    prefs.isEnabled = checked
                                    val intent = Intent(context, FloatingWindowService::class.java).apply {
                                        action = if (checked) FloatingWindowService.ACTION_START else FloatingWindowService.ACTION_STOP
                                    }
                                    if (checked) {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            context.startForegroundService(intent)
                                        } else {
                                            context.startService(intent)
                                        }
                                    } else {
                                        context.startService(intent)
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = accentCyan,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.DarkGray
                            )
                        )
                    }
                }

                // Customization Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Appearance & Feel", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = accentCyan)
                        
                        Column {
                            Text("Visibility (Opacity)", style = MaterialTheme.typography.bodyLarge, color = Color.LightGray)
                            Slider(
                                value = opacity,
                                onValueChange = { opacity = it },
                                valueRange = 0.1f..1.0f,
                                colors = SliderDefaults.colors(thumbColor = accentCyan, activeTrackColor = accentCyan)
                            )
                        }

                        Column {
                            Text("Button Size", style = MaterialTheme.typography.bodyLarge, color = Color.LightGray)
                            Slider(
                                value = size,
                                onValueChange = { size = it },
                                valueRange = 80f..300f,
                                colors = SliderDefaults.colors(thumbColor = accentCyan, activeTrackColor = accentCyan)
                            )
                        }
                        
                        Column {
                            Text("Drag Sensitivity (Speed)", style = MaterialTheme.typography.bodyLarge, color = Color.LightGray)
                            Text("Lower value means faster changes", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Slider(
                                value = sensitivity,
                                onValueChange = { sensitivity = it },
                                valueRange = 200f..2000f,
                                colors = SliderDefaults.colors(thumbColor = accentOrange, activeTrackColor = accentOrange)
                            )
                        }

                        Column {
                            Text("Volume Control Target", style = MaterialTheme.typography.bodyLarge, color = Color.LightGray)
                            Spacer(modifier = Modifier.height(8.dp))
                            val streams = listOf(
                                android.media.AudioManager.STREAM_MUSIC to "Media",
                                android.media.AudioManager.STREAM_RING to "Ringtone",
                                android.media.AudioManager.STREAM_ALARM to "Alarm",
                                android.media.AudioManager.STREAM_VOICE_CALL to "Call"
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                streams.forEach { (streamVal, label) ->
                                    FilterChip(
                                        selected = volumeStream == streamVal,
                                        onClick = { volumeStream = streamVal },
                                        label = { Text(label, fontWeight = if (volumeStream == streamVal) FontWeight.Bold else FontWeight.Normal) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = accentCyan,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Permissions Card (Brightness & Accessibility) - Conditionally shown
                if (!isAccessibilityEnabled || !isWriteSettingsEnabled) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3F2B2B)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Required Permissions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                            
                            if (!isAccessibilityEnabled) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Accessibility Service", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                        Text("Required to auto-hide buttons over excluded apps and keyboard.", style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                                            colors = ButtonDefaults.buttonColors(containerColor = accentOrange),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Grant Access", color = Color.White)
                                        }
                                    }
                                }
                            }
                            
                            if (!isWriteSettingsEnabled) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Modify System Settings", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                        Text("Required to allow adjusting screen brightness natively.", style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { 
                                                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                                    data = Uri.parse("package:${context.packageName}")
                                                }
                                                context.startActivity(intent) 
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = accentCyan),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Grant Access", color = Color.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Excluded Apps Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("App Exceptions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = accentOrange)
                        Text(
                            "Select apps where the floating buttons should automatically disappear (like games or video players).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onNavigateToExcludedApps,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF33364D)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Manage Excluded Apps", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
