package com.example.volbright.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.volbright.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val isExcluded: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcludedAppsScreen(
    prefs: PreferencesManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    
    var appsList by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val excludedSet = prefs.excludedApps
            val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            
            val mappedApps = installedApps
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || excludedSet.contains(it.packageName) }
                .map { appInfo ->
                    AppInfo(
                        packageName = appInfo.packageName,
                        appName = packageManager.getApplicationLabel(appInfo).toString(),
                        icon = packageManager.getApplicationIcon(appInfo),
                        isExcluded = excludedSet.contains(appInfo.packageName)
                    )
                }
                .sortedBy { it.appName }

            appsList = mappedApps
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Excluded Apps") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("<", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(appsList, key = { it.packageName }) { app ->
                    AppListItem(
                        appInfo = app,
                        onToggle = { isChecked ->
                            val newSet = prefs.excludedApps.toMutableSet()
                            if (isChecked) {
                                newSet.add(app.packageName)
                            } else {
                                newSet.remove(app.packageName)
                            }
                            prefs.excludedApps = newSet
                            
                            appsList = appsList.map { 
                                if (it.packageName == app.packageName) it.copy(isExcluded = isChecked) else it
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppListItem(appInfo: AppInfo, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                bitmap = appInfo.icon.toBitmap().asImageBitmap(),
                contentDescription = appInfo.appName,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = appInfo.appName, 
                style = MaterialTheme.typography.titleMedium, 
                modifier = Modifier.weight(1f)
            )
            Checkbox(
                checked = appInfo.isExcluded,
                onCheckedChange = onToggle
            )
        }
    }
}
