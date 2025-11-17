// File: AppNavigation.kt
package com.example.android_project_onwe

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.Scaffold
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android_project_onwe.view.BottomNavigationBar
import com.example.android_project_onwe.view.screens.CreateGroupScreen
import com.example.android_project_onwe.view.screens.HomeScreen
import com.example.android_project_onwe.viewmodel.GroupViewModel

@Composable
fun AppNavigation() {
    var currentScreenState by remember { mutableStateOf<Screen>(Screen.CreateGroup) }
    val viewModel: GroupViewModel = viewModel()

    // Determine which bottom nav item is selected
    val selectedItem = when (currentScreenState) {
        is Screen.Home -> "home"
        is Screen.CreateGroup -> "add"
        else -> "home"
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedItem = selectedItem,
                onItemSelected = { item ->
                    currentScreenState = when (item) {
                        "home" -> Screen.Home
                        "add" -> Screen.CreateGroup
                        else -> currentScreenState
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val screen = currentScreenState) {
            is Screen.Home -> {
                HomeScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues),
                    onGroupClick = { group ->
                    }
                )
            }
            is Screen.CreateGroup -> {
                CreateGroupScreen(
                    viewModel = viewModel,
                    onGroupCreated = { newGroup ->
                        // After creation, navigate back to Home
                        currentScreenState = Screen.Home
                    }
                )
            }
        }
    }
}

// Screen sealed class
sealed class Screen {
    object Home : Screen()
    object CreateGroup : Screen()
}
