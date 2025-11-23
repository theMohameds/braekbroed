package com.example.android_project_onwe.view.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android_project_onwe.model.Group
import com.example.android_project_onwe.viewmodel.GroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    viewModel: GroupViewModel = viewModel(),
    onGroupCreated: (Group) -> Unit = {}
) {
    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var newMember by remember { mutableStateOf("") }
    var members by remember { mutableStateOf(listOf<String>()) }
    var duplicateMemberError by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Create New Group",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background, // same as background
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.shadow(
                    elevation = 4.dp // subtle floating effect
                )
            )
        }

    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp) // space between sections
        ) {
            // Group Info
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = groupDescription,
                        onValueChange = { groupDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Add Members
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Add Members", style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically // center button
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
                            isError = duplicateMemberError
                        )

                        Button(onClick = {
                            if (newMember.isNotBlank()) {
                                if (!members.contains(newMember)) {
                                    members = members + newMember
                                    newMember = ""
                                } else {
                                    duplicateMemberError = true
                                }
                            }
                        }) {
                            Text("Add")
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
                                    IconButton(onClick = { members = members - member }) {
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

            // Submit Button
            item {
                Button(
                    onClick = {
                        if (groupName.isNotBlank() && members.isNotEmpty()) {
                            isSubmitting = true
                            viewModel.createGroup(
                                name = groupName,
                                description = groupDescription,
                                memberEmailsOrRefs = members
                            )
                            groupName = ""
                            groupDescription = ""
                            members = emptyList()
                            newMember = ""
                            duplicateMemberError = false
                            isSubmitting = false
                        }
                    },
                    enabled = groupName.isNotBlank() && members.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isSubmitting) "Creating..." else "Create Group")
                }
            }
        }
    }
}
