package com.example.android_project_onwe.view

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavigationBar(
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface, // slightly different from background

    )
    {

        // Add tab
        NavigationBarItem(
            selected = selectedItem == "add",
            onClick = { onItemSelected("add") },
            icon = { Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(24.dp)) },
            label = { Text("Add") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onBackground,
                unselectedIconColor = Color.Gray,
                selectedTextColor = MaterialTheme.colorScheme.onBackground,
                unselectedTextColor = Color.Gray,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        )

        // Home tab
        NavigationBarItem(
            selected = selectedItem == "home",
            onClick = { onItemSelected("home") },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(24.dp)) },
            label = { Text("Home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onBackground,
                unselectedIconColor = Color.Gray,
                selectedTextColor = MaterialTheme.colorScheme.onBackground,
                unselectedTextColor = Color.Gray,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            )
        )

        // Profile tab
        NavigationBarItem(
            selected = selectedItem == "profile",
            onClick = { onItemSelected("profile") },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile", modifier = Modifier.size(24.dp)) },
            label = { Text("Profile") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onBackground,
                unselectedIconColor = Color.Gray,
                selectedTextColor = MaterialTheme.colorScheme.onBackground,
                unselectedTextColor = Color.Gray,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        )
    }
}
