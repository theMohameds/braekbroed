package com.example.android_project_onwe.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun BottomNavigationBar(
    selectedItem: String,
    onItemSelected: (String) -> Unit  // callback when an item is tapped
) {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(
            selected = selectedItem == "chat",
            onClick = { onItemSelected("chat") },
            icon = { Icon(Icons.Default.MailOutline, contentDescription = "Chat") }
        )
        NavigationBarItem(
            selected = selectedItem == "add",
            onClick = { onItemSelected("add") },
            icon = { Icon(Icons.Default.Add, contentDescription = "Add") }
        )
        NavigationBarItem(
            selected = selectedItem == "home",
            onClick = { onItemSelected("home") },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") }
        )
        NavigationBarItem(
            selected = selectedItem == "profile",
            onClick = { onItemSelected("profile") },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") }
        )
    }
}
