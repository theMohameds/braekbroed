package com.example.android_project_onwe.view.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Create New Group", style = MaterialTheme.typography.titleLarge)
        }

        item {
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = groupDescription,
                onValueChange = { groupDescription = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newMember,
                    onValueChange = {
                        newMember = it
                        duplicateMemberError = false
                    },
                    label = { Text("Add Member") },
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
        }

        if (members.isNotEmpty()) {
            item {
                Text("Members:")
            }

            items(members) { member ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
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

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (groupName.isNotBlank() && members.isNotEmpty()) {
                        isSubmitting = true

                        // Call ViewModel, converting member Strings to DocumentReferences in VM
                        viewModel.createGroup(
                            name = groupName,
                            description = groupDescription,
                            members = emptyList() // replace with conversion in ViewModel
                        )

                        // Invoke callback (could be after actual success in VM)
                        // onGroupCreated(group)  // you need the created group object

                        // Reset UI
                        groupName = ""
                        groupDescription = ""
                        members = emptyList()
                        duplicateMemberError = false
                        isSubmitting = false
                    }
                },
                enabled = groupName.isNotBlank() && members.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSubmitting) "Creating..." else "Create Group")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
