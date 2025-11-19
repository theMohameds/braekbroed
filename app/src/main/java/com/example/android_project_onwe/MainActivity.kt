package com.example.android_project_onwe

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android_project_onwe.ui.theme.AndroidProjectOnWeTheme
import com.example.android_project_onwe.view.LoginScreen
import com.example.android_project_onwe.viewmodel.AuthViewModel

// ----- NOTIFICATION IMPORTS -----
import com.example.android_project_onwe.utils.NotificationUtils
import com.example.android_project_onwe.viewmodel.NotificationViewModel
// ----- END NOTIFICATION IMPORTS -----

// Profile Management Screen import
import com.example.android_project_onwe.view.ProfileScreen


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

@Composable
fun AndroidProjectOnWeApp(authViewModel: AuthViewModel) {
    val isUserLoggedIn by authViewModel.isLoggedIn.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (!isUserLoggedIn) {
            // Show login screen if not logged in
            LoginScreen(authViewModel)
        } else {
            // Show the main app with bottom navigation
            LoggedInApp()
        }
    }
}

@Composable
fun LoggedInApp() {
    val currentDestination = rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    // Wrap in Surface to apply theme background
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    AppDestinations.entries.forEach { destination ->
                        NavigationBarItem(
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                            selected = destination == currentDestination.value,
                            onClick = { currentDestination.value = destination }
                        )
                    }
                }
            }
        ) { innerPadding ->
            // Apply innerPadding inside a Column so background is correct
            Column(modifier = Modifier.padding(innerPadding)) {
                when (currentDestination.value) {
                    AppDestinations.HOME -> Greeting("Home")
                    AppDestinations.FAVORITES -> Greeting("Favorites")
                    AppDestinations.PROFILE ->{ Greeting("profil")
                    ProfileScreen() // viser profilskærm
                    }
                }
            }
        }
    }
}


enum class AppDestinations(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    FAVORITES("Favorites", Icons.Default.Favorite),
    PROFILE("Profile", Icons.Default.AccountBox),
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AndroidProjectOnWeTheme {
        Greeting("Android")
    }
}
