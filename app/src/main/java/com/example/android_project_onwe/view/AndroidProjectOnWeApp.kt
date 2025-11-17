package com.example.android_project_onwe

import androidx.compose.runtime.*
import com.example.android_project_onwe.view.startUpScreens.LoginScreen
import com.example.android_project_onwe.view.startUpScreens.SignUpScreen
import com.example.android_project_onwe.viewmodel.AuthViewModel

@Composable
fun AndroidProjectOnWeApp(authViewModel: AuthViewModel) {
    // Temporary: Always show main app for testing
    AppNavigation()

    // Original code (commented out for now):
    /*val isUserLoggedIn by authViewModel.isLoggedIn.collectAsState()

    // ----- SWITCH LOGIN/SIGNUP SCREEN -----
    var currentScreen by remember { mutableStateOf("login") }
    // ----- END SWITCH LOGIN/SIGNUP SCREEN -----

    if (!isUserLoggedIn) {
        when (currentScreen) {
            "login" -> LoginScreen(
                authViewModel = authViewModel,
                onSignUpClick = { currentScreen = "signup" } // Switch to sign-up
            )
            "signup" -> SignUpScreen(
                authViewModel = authViewModel,
                onLoginClick = { currentScreen = "login" } // Switch to login
            )
        }
    } else {
        AppNavigation()
    }

     */
}
