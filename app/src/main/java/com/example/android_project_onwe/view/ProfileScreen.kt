package com.example.android_project_onwe.view

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android_project_onwe.Screen
import com.example.android_project_onwe.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onNavigate: (Screen) -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val profile = viewModel.profile.value
    val originalProfile = viewModel.originalProfile.value
    var isSaved = viewModel.isSaved.value
    val errorMessage = viewModel.errorMessage.value
    val isLoading = viewModel.isLoading.value
    val context = LocalContext.current

    val firstNameError = profile.firstName.isBlank()
    val lastNameError = profile.lastName.isBlank()
    val emailError = profile.email.isBlank()
    val phoneError = profile.phone.isBlank()

    val hasChanges by remember(profile, originalProfile) {
        mutableStateOf(
            profile.firstName != originalProfile.firstName ||
                    profile.lastName != originalProfile.lastName ||
                    profile.email != originalProfile.email ||
                    profile.phone != originalProfile.phone ||
                    profile.notificationsEnabled != originalProfile.notificationsEnabled
        )
    }

    val canSave = hasChanges && !firstNameError && !lastNameError && !emailError && !phoneError

    DisposableEffect(Unit) {
        onDispose { viewModel.resetProfile() }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(.1.dp)
                )
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // scaffold top/bottom insets
                .padding(horizontal = 16.dp)
                .statusBarsPadding(),  // optional: ensures content starts below status bar
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally, // <- add this
            contentPadding = WindowInsets.navigationBars.asPaddingValues() // dynamic bottom padding
        ) {

            item() {
                // Profile icon
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }

            item {
                // First Name
                OutlinedTextField(
                    value = profile.firstName,
                    onValueChange = { viewModel.updateProfileField("firstName", it) },
                    label = { Text("First Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = firstNameError
                )
                if (firstNameError) {
                    Text("First name cannot be empty", color = MaterialTheme.colorScheme.error)
                }
            }

            item {
                // Last Name
                OutlinedTextField(
                    value = profile.lastName,
                    onValueChange = { viewModel.updateProfileField("lastName", it) },
                    label = { Text("Last Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = lastNameError
                )
                if (lastNameError) {
                    Text("Last name cannot be empty", color = MaterialTheme.colorScheme.error)
                }
            }

            item {
                // Email
                OutlinedTextField(
                    value = profile.email,
                    onValueChange = { viewModel.updateProfileField("email", it) },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                )
                if (emailError) {
                    Text("Email cannot be empty", color = MaterialTheme.colorScheme.error)
                }
            }

            item {
                // Phone
                OutlinedTextField(
                    value = profile.phone,
                    onValueChange = { viewModel.updateProfileField("phone", it) },
                    label = { Text("Phone") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Phone
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    isError = phoneError
                )
                if (phoneError) {
                    Text("Phone cannot be empty", color = MaterialTheme.colorScheme.error)
                }
            }

            item {
                // Notifications toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enable Notifications", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = profile.notificationsEnabled,
                        onCheckedChange = { viewModel.toggleNotifications(it) }
                    )
                }
            }

            if (canSave) {
                item {
                    // Save button
                    Button(
                        onClick = { viewModel.saveProfile() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Done, contentDescription = "Save", tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Save", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            item {
                // Logout button
                Button(
                    onClick = {
                        viewModel.logout()
                        onNavigate(Screen.login)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Logout", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onError)
                }
            }

            if (errorMessage != null) {
                item {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp)) // extra space so buttons scroll above nav bar
            }
        }

        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Toast
        if (isSaved) {
            Toast.makeText(context, "Profile Saved", Toast.LENGTH_LONG).show()
            isSaved = false
        }
    }
}
