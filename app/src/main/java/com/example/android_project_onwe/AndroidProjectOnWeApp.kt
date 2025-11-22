package com.example.android_project_onwe

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.android_project_onwe.view.startUpScreens.LoginScreen
import com.example.android_project_onwe.view.startUpScreens.SignUpScreen
import com.example.android_project_onwe.viewmodel.AuthViewModel

@Composable
fun AndroidProjectOnWeApp(authViewModel: AuthViewModel) {

    val isUserLoggedIn by authViewModel.isLoggedIn.collectAsState(initial = false)
    var currentScreen by remember { mutableStateOf("login") }
    val context = LocalContext.current
    val notificationManager = remember { AppNotificationManager(context) }

    LaunchedEffect(isUserLoggedIn) {
        if (isUserLoggedIn) {
            notificationManager.start()
        }
    }

    if (!isUserLoggedIn) {
        when (currentScreen) {
            "login" -> LoginScreen(
                authViewModel = authViewModel,
                onSignUpClick = { currentScreen = "signup" }
            )
            "signup" -> SignUpScreen(
                authViewModel = authViewModel,
                onLoginClick = { currentScreen = "login" }
            )
        }
    } else {
        AppNavigation(
            notificationManager = notificationManager
        )
    }
}



