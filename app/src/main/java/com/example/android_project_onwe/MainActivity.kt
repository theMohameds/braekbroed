package com.example.android_project_onwe

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android_project_onwe.ui.theme.AndroidProjectOnWeTheme
import com.example.android_project_onwe.viewmodel.AuthViewModel
import com.example.android_project_onwe.utils.NotificationUtils

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.setNavigationBarContrastEnforced(false)

        NotificationUtils.createNotificationChannel(this)

        // Look into lowering the API level so more uses can use thew app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        // Compose UI
        setContent {
            AndroidProjectOnWeTheme {
                val authViewModel: AuthViewModel = viewModel()
                AndroidProjectOnWeApp(authViewModel)
            }
        }
    }
}
