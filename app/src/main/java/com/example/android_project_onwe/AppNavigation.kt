package com.example.android_project_onwe

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.Scaffold
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android_project_onwe.Screen.*
import com.example.android_project_onwe.view.BottomNavigationBar
import com.example.android_project_onwe.view.group.GroupChatView
import com.example.android_project_onwe.view.screens.CreateGroupScreen
import com.example.android_project_onwe.view.screens.HomeScreen
import com.example.android_project_onwe.viewmodel.GroupViewModel
import com.example.android_project_onwe.view.ProfileScreen
import com.example.android_project_onwe.view.group.FinalizedBillScreen
import com.example.android_project_onwe.view.group.GroupExpensesScreen
import com.example.android_project_onwe.view.group.GroupSettingsScreen

@Composable
fun AppNavigation(notificationManager: AppNotificationManager) {
    var currentScreenState by remember { mutableStateOf<Screen>(Home) }
    val groupViewModel: GroupViewModel = viewModel()

    // Determine the selected item for the nav bars
    val selectedItem = when (currentScreenState) {
        is Home -> "home"
        is CreateGroup -> "add"
        is GroupChat -> ""
        is Profile -> "profile"
        is FinalizedBill -> ""
        is GroupExpenses -> ""
        is GroupSettings -> ""
    }

    Scaffold(
        bottomBar = {
            if (currentScreenState !is GroupChat && currentScreenState !is GroupExpenses
                && currentScreenState !is FinalizedBill
                && currentScreenState !is GroupSettings) {
                BottomNavigationBar(
                    selectedItem = selectedItem,
                    onItemSelected = { item ->
                        currentScreenState = when (item) {
                            "home" -> Home
                            "add" -> CreateGroup
                            "profile" -> Profile
                            else -> currentScreenState
                        }
                    }
                )
            }
        }
    ) { paddingValues ->

        when (val screen = currentScreenState) {
            is Home -> {
                HomeScreen(
                    viewModel = groupViewModel,
                    modifier = Modifier.padding(paddingValues),
                    onGroupClick = { group ->
                        currentScreenState = GroupChat(group.id)
                    }
                )
            }

            is CreateGroup -> {
                CreateGroupScreen(
                    viewModel = groupViewModel,
                    onGroupCreated = { currentScreenState = Home }
                )
            }

            is GroupChat -> {
                GroupChatView(
                    groupId = screen.groupId,
                    onBack = {
                        notificationManager.setCurrentOpenGroup(null)
                        currentScreenState = Home
                    },
                    onNavigate = { screen -> currentScreenState = screen },
                    notificationManager = notificationManager,
                )
            }

            is FinalizedBill -> {
                FinalizedBillScreen(
                    groupId = screen.groupId,
                    onBack = {
                        notificationManager.setCurrentOpenGroup(screen.groupId)
                        currentScreenState = GroupExpenses(screen.groupId)
                    }
                )
            }

            is Profile -> {
                ProfileScreen(modifier = Modifier.padding(paddingValues))
            }

            is GroupExpenses -> {
                GroupExpensesScreen(
                    groupId = screen.groupId,
                    onBack = {
                        notificationManager.setCurrentOpenGroup(screen.groupId)
                        currentScreenState = GroupChat(screen.groupId)
                    },
                    onFinalizeBill = { groupId ->
                        notificationManager.setCurrentOpenGroup(null)
                        currentScreenState = FinalizedBill(groupId)
                    },
                    notificationManager = notificationManager
                )
            }

            is GroupSettings -> {
                GroupSettingsScreen(
                    groupId = screen.groupId,
                    onBack = {
                        notificationManager.setCurrentOpenGroup(screen.groupId)
                        currentScreenState = GroupChat(screen.groupId)
                    },
                    notificationManager = notificationManager,
                    onNavigate = { screen -> currentScreenState = screen },
                    )
            }
        }
    }
}

// Screens sealed class
sealed class Screen {
    object Home : Screen()
    object CreateGroup : Screen()
    data class GroupChat(val groupId: String) : Screen()
    data class FinalizedBill(val groupId: String) : Screen()
    object Profile : Screen()
    data class GroupExpenses(val groupId: String) : Screen()
    data class GroupSettings(val groupId: String) : Screen()
}
