package com.example.android_project_onwe

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.Scaffold
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android_project_onwe.view.BottomNavigationBar
import com.example.android_project_onwe.view.group.GroupChatView
import com.example.android_project_onwe.view.screens.CreateGroupScreen
import com.example.android_project_onwe.view.screens.HomeScreen
import com.example.android_project_onwe.viewmodel.GroupViewModel

@Composable
fun AppNavigation() {
    var currentScreenState by remember { mutableStateOf<Screen>(Screen.Home) }
    val viewModel: GroupViewModel = viewModel()

    // Determine which bottom nav item is selected
    val selectedItem = when (currentScreenState) {
        is Screen.Home -> "home"
        is Screen.CreateGroup -> "add"
        is Screen.GroupChat -> ""
    }

    Scaffold(
        bottomBar = {
            if (currentScreenState !is Screen.GroupChat) {
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
                        currentScreenState = Screen.GroupChat(group.id)
                    }
                )
            }

            is Screen.CreateGroup -> {
                CreateGroupScreen(
                    viewModel = viewModel,
                    onGroupCreated = {
                        currentScreenState = Screen.Home
                    }
                )
            }

            is Screen.GroupChat -> {
                GroupChatView(
                    groupId = screen.groupId,
                    onBack = {
                        currentScreenState = Screen.Home
                    }
                )
            }

        }
    }
}

// Screens
sealed class Screen {
    object Home : Screen()
    object CreateGroup : Screen()
    data class GroupChat(val groupId: String) : Screen()
}
