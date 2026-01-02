package com.example.android_project_onwe.view.group

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android_project_onwe.AppNotificationManager
import com.example.android_project_onwe.Screen
import com.example.android_project_onwe.viewmodel.GroupSettingsViewModel
import com.google.firebase.firestore.DocumentReference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSettingsScreen(
    groupId: String,
    onBack: () -> Unit,
    notificationManager: AppNotificationManager,
    onNavigate: (Screen) -> Unit,
) {
    val viewModel: GroupSettingsViewModel = viewModel()
    val context = LocalContext.current

    // After (for mutableStateOf):
    val group = viewModel.group.value
    val members = viewModel.membersData.value
    val isLoading = viewModel.isLoading.value
    val isSaved = viewModel.isSaved.value
    val errorMessage = viewModel.errorMessage.value

    var memberToRemove by remember { mutableStateOf<DocumentReference?>(null) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var newMemberEmail by remember { mutableStateOf("") }
    var addMemberError by remember { mutableStateOf<String?>(null) }

    var editableName by remember { mutableStateOf("") }
    var editableDescription by remember { mutableStateOf("") }
    var isEdited by remember { mutableStateOf(false) }

    // Fetch group data
    LaunchedEffect(groupId) {
        viewModel.fetchGroup(groupId)
    }

    // Populate editable fields when group loads
    LaunchedEffect(group) {
        group?.let {
            editableName = it.name
            editableDescription = it.description
        }
    }

    val nameError = editableName.isBlank()
    val descriptionError = editableDescription.isBlank()

    val hasChanges by remember(group, editableName, editableDescription) {
        mutableStateOf(
            group != null &&
                    (editableName != group.name || editableDescription != group.description)
        )
    }

    val canSave by remember(nameError, descriptionError, hasChanges, isEdited) {
        mutableStateOf(isEdited && hasChanges && !nameError && !descriptionError)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Group Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        notificationManager.setCurrentOpenGroup(groupId)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            )
        }
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (group != null) {

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(60.dp)
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // Editable fields
                    OutlinedTextField(
                        value = editableName,
                        onValueChange = {
                            editableName = it
                            isEdited = true
                        },
                        label = { Text("Group Name") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = nameError
                    )

                    OutlinedTextField(
                        value = editableDescription,
                        onValueChange = {
                            editableDescription = it
                            isEdited = true
                        },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = descriptionError
                    )

                    if (descriptionError) Text(
                        "Description cannot be empty",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Members header + add button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Members", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { showAddMemberDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Member")
                    }
                }

                LazyColumn {
                    items(members) { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(member.first, style = MaterialTheme.typography.titleMedium)
                                Text(member.second, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                            }
                            if (!viewModel.isCurrentUser(member.third)) {
                                TextButton(onClick = { memberToRemove = member.third }) {
                                    Text("Remove", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Save button
                    if (canSave) {
                        Button(
                            onClick = {
                                group?.let {
                                    viewModel.saveGroupChanges(editableName,editableDescription)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Done, contentDescription = "Save Changes", tint = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("Save Changes", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium)
                        }
                    }

                Spacer(Modifier.height(24.dp))

                // Leave group
                Button(
                    onClick = { showLeaveDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Leave Group", tint = MaterialTheme.colorScheme.onError)
                    Spacer(Modifier.width(8.dp))
                    Text("Leave Group", color = MaterialTheme.colorScheme.onError, style = MaterialTheme.typography.titleMedium)
                }

                // Show error message
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
                }

                // Show toast on save
                if (isSaved) {
                    Toast.makeText(context, "Group settings saved", Toast.LENGTH_SHORT).show()
                    viewModel.clearSavedFlag()
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
        }
    }


    // Add Member
    if (showAddMemberDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddMemberDialog = false
                newMemberEmail = ""
                addMemberError = null
            },
            title = { Text("Add Member") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newMemberEmail,
                        onValueChange = {
                            newMemberEmail = it
                            addMemberError = null
                        },
                        label = { Text("Email") },
                        singleLine = true,
                        isError = addMemberError != null
                    )
                    addMemberError?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newMemberEmail.isBlank()) {
                        addMemberError = "Email cannot be empty"
                        return@TextButton
                    }
                    viewModel.addMemberByEmail(newMemberEmail)
                    showAddMemberDialog = false
                    newMemberEmail = ""
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddMemberDialog = false }) { Text("Cancel") } }
        )
    }

    // Remove Member
    if (memberToRemove != null) {
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text("Remove Member") },
            text = { Text("Are you sure you want to remove this member from the group?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeMember(memberToRemove!!)
                    memberToRemove = null
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { memberToRemove = null }) { Text("Cancel") } }
        )
    }

    // Leave Group
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave Group") },
            text = { Text("Are you sure you want to leave this group?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.leaveGroup()
                    notificationManager.setCurrentOpenGroup(null)
                    onNavigate(Screen.Home)
                }) { Text("Leave", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showLeaveDialog = false }) { Text("Cancel") } }
        )
    }
}
