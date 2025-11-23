package com.example.android_project_onwe.view.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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

    // Load group
    LaunchedEffect(groupId) { viewModel.fetchGroup(groupId) }

    val group by viewModel.group.collectAsState()
    val members by viewModel.membersData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var memberToRemove by remember { mutableStateOf<DocumentReference?>(null) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }

    var newMemberEmail by remember { mutableStateOf("") }
    var addMemberError by remember { mutableStateOf<String?>(null) }

    // Local state for editable fields
    var editableName by remember { mutableStateOf("") }
    var editableDescription by remember { mutableStateOf("") }

    // Update local state when group loads
    LaunchedEffect(group) {
        group?.let {
            editableName = it.name
            editableDescription = it.description
        }
    }

    val currentGroup = group
    val hasChanges = currentGroup != null &&
            (editableName != currentGroup.name || editableDescription != currentGroup.description)

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Group Settings",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        notificationManager.setCurrentOpenGroup(groupId)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                modifier = Modifier.shadow(4.dp)
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            if (currentGroup != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icon above group name with adaptive background
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

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = editableName,
                        onValueChange = { editableName = it },
                        label = { Text("Group Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editableDescription,
                        onValueChange = { editableDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }



            Spacer(Modifier.height(16.dp))

            // Members header with subtle + button
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

            // MEMBERS LIST
            LazyColumn {
                items(members) { member ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(member.first, style = MaterialTheme.typography.titleMedium)
                            Text(
                                member.second,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }

                        if (!viewModel.isCurrentUser(member.third)) {
                            TextButton(onClick = { memberToRemove = member.third }) {
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            if (hasChanges) {
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        currentGroup?.let {
                            viewModel.updateGroupName(editableName)
                            viewModel.updateGroupDescription(editableDescription)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = "Save Changes",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Save Changes",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Leave group button
            Button(
                onClick = { showLeaveDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Leave Group",
                    tint = MaterialTheme.colorScheme.onError
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Leave Group",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onError
                )
            }
        }
    }

    // ==================================
    // DIALOG: ADD MEMBER
    // ==================================
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
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemberDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ==================================
    // DIALOG: REMOVE MEMBER
    // ==================================
    if (memberToRemove != null) {
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text("Remove Member") },
            text = { Text("Are you sure you want to remove this member from the group?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeMember(memberToRemove!!)
                    memberToRemove = null
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToRemove = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ==================================
    // DIALOG: LEAVE GROUP
    // ==================================
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
                }) {
                    Text("Leave", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
