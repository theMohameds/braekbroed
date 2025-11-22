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
import com.example.android_project_onwe.view.ProfileScreen
import com.example.android_project_onwe.view.group.FinalizedBillScreen

@Composable
fun AppNavigation(notificationManager: AppNotificationManager) {
    var currentScreenState by remember { mutableStateOf<Screen>(Screen.Home) }
    val groupViewModel: GroupViewModel = viewModel()

    val selectedItem = when (currentScreenState) {
        is Screen.Home -> "home"
        is Screen.CreateGroup -> "add"
        is Screen.GroupChat -> ""
        is Screen.Profile -> "profile"
        is Screen.FinalizedBill -> ""
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
                            "profile" -> Screen.Profile
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
                    viewModel = groupViewModel,
                    modifier = Modifier.padding(paddingValues),
                    onGroupClick = { group ->
                        currentScreenState = Screen.GroupChat(group.id)
                    }
                )
            }

            is Screen.CreateGroup -> {
                CreateGroupScreen(
                    viewModel = groupViewModel,
                    onGroupCreated = {
                        currentScreenState = Screen.Home
                    }
                )
            }

            is Screen.GroupChat -> {
                GroupChatView(
                    groupId = screen.groupId,
                    onBack = {
                        notificationManager.setCurrentOpenGroup(null) // clear current group
                        currentScreenState = Screen.Home
                    },
                    onFinalizeBill = { groupId ->
                        notificationManager.setCurrentOpenGroup(null) // clear current group
                        currentScreenState = Screen.FinalizedBill(groupId)
                    },
                    notificationManager = notificationManager // pass it here
                )
            }

            is Screen.FinalizedBill -> {
                FinalizedBillScreen(
                    groupId = screen.groupId,
                    onBack = { currentScreenState = Screen.Home }
                )
            }

            is Screen.Profile -> {
                ProfileScreen(
                    modifier = Modifier.padding(paddingValues)
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
    data class FinalizedBill(val groupId: String) : Screen()
    object Profile : Screen()
}

