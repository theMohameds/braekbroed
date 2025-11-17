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
    // Current screen state
    var currentScreenState by remember { mutableStateOf<Screen>(Screen.Home) }

    // ViewModel for screens
    val viewModel: GroupViewModel = viewModel()

    // Determine which bottom nav item is selected
    val selectedItem = when (currentScreenState) {
        is Screen.Home -> "home"
        is Screen.CreateGroup -> "add"
        else -> "home"
    }

    Scaffold(
        bottomBar = {
            if (currentScreenState.showBottomBar) {
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
        }
    ) { paddingValues ->
        when (val screen = currentScreenState) {
            is Screen.Home -> {
                HomeScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues),
                    onGroupClick = { group ->
                        // handle group click
                    }
                )
            }
            is Screen.CreateGroup -> {
                CreateGroupScreen(
                    viewModel = viewModel,
                    onGroupCreated = { newGroup ->
                        currentScreenState = Screen.Home
                    }
                )
            }
        }
    }
}

// Sealed class for screens
sealed class Screen(val showBottomBar: Boolean = true) {
    object Home : Screen(showBottomBar = true)
    object CreateGroup : Screen(showBottomBar = true)
}
