package com.example.android_project_onwe

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android_project_onwe.ui.theme.AndroidProjectOnWeTheme
import com.example.android_project_onwe.viewmodel.AuthViewModel

// ----- NOTIFICATION IMPORTS -----
import com.example.android_project_onwe.utils.NotificationUtils
import com.example.android_project_onwe.viewmodel.NotificationViewModel
// ----- END NOTIFICATION IMPORTS -----

class MainActivity : ComponentActivity() {

    // ----- NOTIFICATION RELATED -----
    private val notificationViewModel by lazy {
        NotificationViewModel(applicationContext)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                testNotification()
            }
        }
    // ----- END NOTIFICATION RELATED -----

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ----- NOTIFICATION RELATED -----
        // Create notification channel (only once)
        NotificationUtils.createNotificationChannel(this)

        // Check/request notification permission
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                testNotification() // optional: for testing
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        // ----- END NOTIFICATION RELATED -----

        // Compose UI
        setContent {
            AndroidProjectOnWeTheme {
                val authViewModel: AuthViewModel = viewModel()
                AndroidProjectOnWeApp(authViewModel)
            }
        }
    }

    // ----- NOTIFICATION RELATED -----
    private fun testNotification() {
        // Delay by 3 seconds for testing
        Handler(Looper.getMainLooper()).postDelayed({
            notificationViewModel.triggerNotification(
                "Hello from Compose!",
                "This is a test notification from your MVVM setup."
            )
        }, 3000)
    }
    // ----- END NOTIFICATION RELATED -----
}
