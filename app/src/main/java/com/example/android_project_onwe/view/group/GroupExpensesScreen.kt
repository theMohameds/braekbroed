package com.example.android_project_onwe.view.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android_project_onwe.AppNotificationManager
import com.example.android_project_onwe.model.Expense
import com.example.android_project_onwe.viewmodel.FinalizedBillViewModel
import com.example.android_project_onwe.viewmodel.GroupChatViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupExpensesScreen(
    groupId: String,
    onBack: () -> Unit,
    notificationManager: AppNotificationManager,
    onFinalizeBill: (String) -> Unit,
    viewModel: GroupChatViewModel = viewModel(),
    viewModel2: FinalizedBillViewModel = viewModel()
) {

    // Start listeners
    LaunchedEffect(groupId) {
        viewModel.listenForExpenses(groupId)
        viewModel.listenForBillFinalized(groupId)
    }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val expenses by viewModel.expenses.collectAsState()
    val membersMap by viewModel.membersMap.collectAsState()
    val groupName by viewModel.groupName.collectAsState()
    val billFinalized by viewModel.billFinalized.collectAsState()

    var showAddExpense by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var deletingExpense by remember { mutableStateOf<Expense?>(null) }
    var showFinalizedDialog by remember { mutableStateOf(false) }

    val memberCount = membersMap.size.takeIf { it > 0 } ?: 1
    val totalExpenses = expenses.sumOf { it.amount }
    val equalShare = totalExpenses / memberCount
    val userPaid = expenses.filter { it.payerId == currentUserId }.sumOf { it.amount }
    val userBalance = userPaid - equalShare
    val balanceColor = when {
        userBalance > 0 -> Color(0xFF2E7D32) // green
        userBalance < 0 -> Color(0xFFD32F2F) // red
        else -> Color(0xFF616161)            // neutral gray
    }


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        groupName + " Expenses",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        notificationManager.setCurrentOpenGroup(groupId)
                        viewModel.resetValueGroupState()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(.1.dp)
                ),
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            // Summary Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryRow(
                        label = "Total Expenses",
                        value = "%.2f kr".format(totalExpenses),
                        valueColor = MaterialTheme.colorScheme.onSurface
                    )
                    SummaryRow(
                        label = if (userBalance > 0) "You are owed" else if (userBalance < 0) "You owe" else "Balanced",
                        value = "%.2f kr".format(kotlin.math.abs(userBalance)),
                        valueColor = balanceColor,
                        bold = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        if (billFinalized) {
                            showFinalizedDialog = true
                        } else {
                            showAddExpense = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    if (!billFinalized) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (billFinalized) "Open Bill" else "Add Expense")
                }


                Button(
                    onClick = {
                        viewModel.finalizeBill(groupId)
                        onFinalizeBill(groupId)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    if (!billFinalized) {
                        Icon(Icons.Default.Done, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(if (billFinalized) "View Bill" else "Finalize Bill")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Expenses List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(expenses) { exp ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 1.dp,
                        shadowElevation = 2.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ExpenseCardRow(
                            expense = exp,
                            payerName = membersMap[exp.payerId] ?: "Unknown",
                            onEditClick = { editingExpense = it },
                            onDeleteClick = { deletingExpense = it }
                        )
                    }
                }
            }
        }
    }

    // Add Expense Dialog
    if (showAddExpense) {
        AddExpenseDialog(
            onDismiss = { showAddExpense = false },
            onConfirm = { amount, desc ->
                viewModel.addExpense(groupId, amount, desc)
                showAddExpense = false
            }
        )
    }

    // Edit Expense Dialog
    editingExpense?.let { exp ->
        EditExpenseDialog(
            expense = exp,
            onDismiss = { editingExpense = null },
            onConfirm = { amt, desc ->
                viewModel.editExpense(groupId, exp.id, amt, desc)
                editingExpense = null
            }
        )
    }

    // Delete Expense Dialog
    deletingExpense?.let { exp ->
        AlertDialog(
            onDismissRequest = { deletingExpense = null },
            title = { Text("Delete Expense") },
            text = { Text("Delete \"${exp.description}\" (${String.format("%.2f", exp.amount)} kr)?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteExpense(groupId, exp.id)
                    deletingExpense = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingExpense = null }) { Text("Cancel") }
            }
        )
    }

    // Finalized Bill Dialog
    if (showFinalizedDialog) {
        AlertDialog(
            onDismissRequest = { showFinalizedDialog = false },
            title = { Text("Bill Finalized") },
            text = { Text("This bill is finalized. Do you want to reopen it to add a new expense?") },
            confirmButton = {
                Button(onClick = {
                    viewModel2.reopenBill(groupId) {
                        CoroutineScope(Dispatchers.Main).launch {
                            viewModel.loadGroupName(groupId)
                            showFinalizedDialog = false
                            showAddExpense = true
                        }
                    }
                }) {
                    Text("Reopen Bill")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinalizedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ExpenseCardRow(
    expense: Expense,
    payerName: String,
    onEditClick: (Expense) -> Unit,
    onDeleteClick: (Expense) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("$payerName paid", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("${expense.description} — ${String.format("%.2f", expense.amount)} kr", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }

                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = {
                        expanded = false
                        onEditClick(expense)
                    })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = {
                        expanded = false
                        onDeleteClick(expense)
                    })
                    DropdownMenuItem(text = { Text("Cancel") }, onClick = { expanded = false })
                }
            }
        }
    }
}

@Composable
private fun EditExpenseDialog(
    expense: Expense,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var desc by remember { mutableStateOf(expense.description) }
    var amount by remember { mutableStateOf(expense.amount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Expense") },
        text = {
            Column {
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (kr)") })
            }
        },
        confirmButton = {
            Button(onClick = {
                val v = amount.toDoubleOrNull()
                if (v != null && desc.isNotBlank()) {
                    onConfirm(v, desc)
                }
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense") },
        text = {
            Column {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (kr)") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = amount.toDoubleOrNull()
                    if (value != null && description.isNotBlank()) {
                        onConfirm(value, description)
                    }
                    onDismiss()
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color, bold: Boolean = false) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            color = valueColor,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium
        )
    }
}

