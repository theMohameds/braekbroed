package com.example.android_project_onwe.view.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android_project_onwe.model.Group
import com.example.android_project_onwe.viewmodel.GroupViewModel
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    viewModel: GroupViewModel = viewModel(),
    onGroupCreated: (Group) -> Unit = {}
) {
    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var totalExpenseText by remember { mutableStateOf("") }
    var newMember by remember { mutableStateOf("") }
    var members by remember { mutableStateOf(listOf<String>()) }
    var splitOption by remember { mutableStateOf("Even") }
    var manualSplits by remember { mutableStateOf(mapOf<String, String>()) }
    var duplicateMemberError by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Total expense validation
    val totalExpense = totalExpenseText.toDoubleOrNull() ?: 0.0
    val totalExpenseError = totalExpenseText.isNotEmpty() && totalExpense == 0.0

    // Compute splits efficiently
    val splits by remember(members, manualSplits, splitOption, totalExpense) {
        derivedStateOf {
            when (splitOption) {
                "Even" -> if (members.isNotEmpty()) members.associateWith { totalExpense / members.size } else emptyMap()
                "Manual" -> manualSplits.mapNotNull { (member, amtText) ->
                    val amt = amtText.toDoubleOrNull()
                    if (amt != null && amt <= totalExpense) member to amt else null
                }.toMap()
                "Percentage" -> manualSplits.mapNotNull { (member, pctText) ->
                    val pct = pctText.toDoubleOrNull()
                    if (pct != null && pct in 0.0..100.0) member to totalExpense * (pct / 100.0) else null
                }.toMap()
                else -> emptyMap()
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Text("Create New Group", style = MaterialTheme.typography.titleLarge)
        }

        // Group Name
        item {
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Description
        item {
            OutlinedTextField(
                value = groupDescription,
                onValueChange = { groupDescription = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Total Expense
        item {
            OutlinedTextField(
                value = totalExpenseText,
                onValueChange = {
                    totalExpenseText = it.filter { c -> c.isDigit() || c == '.' }
                },
                label = { Text("Total Expense") },
                singleLine = true,
                isError = totalExpenseError,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
            )
            if (totalExpenseError) {
                Text("Enter a valid total expense", color = MaterialTheme.colorScheme.error)
            }
        }

        // Add Member Section
        item {
            var addMemberText by remember { mutableStateOf(newMember) }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = addMemberText,
                    onValueChange = {
                        addMemberText = it
                        duplicateMemberError = false
                    },
                    label = { Text("Add Member") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    isError = duplicateMemberError
                )
                Button(onClick = {
                    if (addMemberText.isNotBlank()) {
                        if (!members.contains(addMemberText)) {
                            members = members + addMemberText
                            addMemberText = ""
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

        // Split Options
        if (members.isNotEmpty()) {
            item {
                Text("Split Option:")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Even", "Manual", "Percentage").forEach { option ->
                        FilterChip(
                            selected = splitOption == option,
                            onClick = { splitOption = option },
                            label = { Text(option) }
                        )
                    }
                }
            }

            // Member List
            items(members) { member ->
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (splitOption) {
                            "Even" -> {
                                val amount = splits[member] ?: 0.0
                                Text("- $member pays $${"%.2f".format(amount)}", modifier = Modifier.weight(1f))
                            }
                            "Manual" -> {
                                val amtText = manualSplits[member] ?: ""
                                val amt = amtText.toDoubleOrNull() ?: 0.0
                                val isError = amt > totalExpense
                                OutlinedTextField(
                                    value = amtText,
                                    onValueChange = { input ->
                                        manualSplits = manualSplits + (member to input.filter { it.isDigit() || it == '.' })
                                    },
                                    label = { Text("$member pays") },
                                    singleLine = true,
                                    isError = isError,
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                                )
                                if (amt > totalExpense) {
                                    Text("Cannot exceed total expense", color = MaterialTheme.colorScheme.error)
                                }
                            }
                            "Percentage" -> {
                                val pctText = manualSplits[member] ?: ""
                                val pct = pctText.toDoubleOrNull() ?: 0.0
                                val isError = pct !in 0.0..100.0
                                OutlinedTextField(
                                    value = pctText,
                                    onValueChange = { input ->
                                        manualSplits = manualSplits + (member to input.filter { it.isDigit() || it == '.' })
                                    },
                                    label = { Text("$member %") },
                                    singleLine = true,
                                    isError = isError,
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                                )
                                Text("Pays $${"%.2f".format(totalExpense * pct / 100)}")
                                if (isError) Text("Must be 0-100%", color = MaterialTheme.colorScheme.error)
                            }
                        }

                        // Remove member button
                        IconButton(onClick = {
                            members = members - member
                            manualSplits = manualSplits - member
                        }) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Close,
                                contentDescription = "Remove member"
                            )
                        }
                    }
                }
            }
        }

        // Create Group Button
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val finalSplits = splits
                    if (groupName.isNotBlank() && members.isNotEmpty() && finalSplits.isNotEmpty() && !totalExpenseError) {


                        viewModel.createGroup(groupName, groupDescription, members, "John",
                            totalExpense, finalSplits)

                        // Reset
                        groupName = ""
                        groupDescription = ""
                        totalExpenseText = ""
                        members = emptyList()
                        manualSplits = emptyMap()
                        duplicateMemberError = false
                        splitOption = "Even"
                    }
                },
                enabled = groupName.isNotBlank() && members.isNotEmpty() && !totalExpenseError,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSubmitting) "Creating..." else "Create Group")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
