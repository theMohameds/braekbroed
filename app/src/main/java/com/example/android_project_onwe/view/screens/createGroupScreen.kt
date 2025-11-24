package com.example.android_project_onwe.view.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android_project_onwe.Screen
import com.example.android_project_onwe.viewmodel.GroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    viewModel: GroupViewModel = viewModel(),
    onNavigate: (Screen) -> Unit,
) {
    val context = LocalContext.current

    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var newMember by remember { mutableStateOf("") }
    var members by remember { mutableStateOf(listOf<String>()) }
    var duplicateMemberError by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val groupEvent by viewModel.groupEvent

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Create New Group",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                ),
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(
                top = 0.dp,
                bottom = 96.dp
            )
        ) {

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting  // DISABLE when submitting
                    )

                    OutlinedTextField(
                        value = groupDescription,
                        onValueChange = { groupDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting  // DISABLE when submitting
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Add Members", style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newMember,
                            onValueChange = {
                                newMember = it
                                duplicateMemberError = false
                            },
                            label = { Text("Member Email") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            isError = duplicateMemberError,
                            enabled = !isSubmitting  // DISABLE when submitting
                        )

                        SmallFloatingActionButton(
                            onClick = {
                                if (newMember.isNotBlank()) {
                                    if (!members.contains(newMember)) {
                                        members = members + newMember
                                        newMember = ""
                                    } else {
                                        duplicateMemberError = true
                                    }
                                }
                            },
                            modifier = Modifier.size(40.dp),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,

                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add member")
                        }
                    }

                    if (duplicateMemberError) {
                        Text("Member already added", color = MaterialTheme.colorScheme.error)
                    }

                    if (members.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            members.forEach { member ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(member, modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = { members = members - member },
                                        enabled = !isSubmitting  // DISABLE when submitting
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove member"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        isSubmitting = true
                        viewModel.createGroup(
                            name = groupName,
                            description = groupDescription,
                            memberEmails = members
                        )
                    },
                    enabled = groupName.isNotBlank() && members.isNotEmpty() && !isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Creating...")
                    } else {
                        Text("Create Group")
                    }
                }
            }

        }
    }

    LaunchedEffect(groupEvent) {
        if (groupEvent.isNotBlank()) {
            Toast.makeText(context, groupEvent, Toast.LENGTH_LONG).show()
            // Reset form only if success
            if (groupEvent.contains("success", ignoreCase = true)) {
                groupName = ""
                groupDescription = ""
                members = emptyList()
                newMember = ""
                duplicateMemberError = false
                onNavigate(Screen.Home)
            }
            isSubmitting = false
            // Clear the event after showing toast
            viewModel.groupEvent.value = ""
        }
    }
}
