package com.example.volbright

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.volbright.data.PreferencesManager
import com.example.volbright.ui.ExcludedAppsScreen
import com.example.volbright.ui.MainScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Main)
  val context = LocalContext.current
  val prefs = remember { PreferencesManager(context) }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(
            prefs = prefs,
            onNavigateToExcludedApps = { backStack.add(ExcludedApps) }
          )
        }
        entry<ExcludedApps> {
          ExcludedAppsScreen(
            prefs = prefs,
            onBack = { backStack.removeLastOrNull() }
          )
        }
      },
  )
}
