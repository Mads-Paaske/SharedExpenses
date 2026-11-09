package com.example.sharedexpenses

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.example.sharedexpenses.ui.navigation.AppNavigation
import com.example.sharedexpenses.ui.theme.SharedExpensesTheme

/** Entry point (Lecture 7). Wraps the whole app in our theme and starts the navigation graph (Lecture 9). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SharedExpensesTheme {
                RequestNotificationPermission()
                AppNavigation(navController = rememberNavController())
            }
        }
    }
}

/** Android 13+ asks the user before an app may show notifications (F12, NF05). */
@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
}
