package com.example.android_project_onwe.view.group

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android_project_onwe.model.Expense
import com.example.android_project_onwe.model.Message
import com.example.android_project_onwe.viewmodel.GroupChatViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatView(
    groupId: String,
    onBack: () -> Unit,
    onFinalizeBill: (String) -> Unit,
    viewModel: GroupChatViewModel = viewModel()
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(groupId) {
        viewModel.loadGroupName(groupId)
        viewModel.loadGroupMembersAndNames(groupId)
        viewModel.listenForMessages(groupId)
        viewModel.listenForExpenses(groupId)
        viewModel.listenForPayments(groupId)
    }

    val groupName by viewModel.groupName.collectAsState()
    val billFinalized by viewModel.billFinalized.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val membersMap by viewModel.membersMap.collectAsState()

    val memberCount = membersMap.size.takeIf { it > 0 } ?: 1
    val totalExpenses = expenses.sumOf { it.amount }
    val equalShare = totalExpenses / memberCount
    val userPaid = expenses.filter { it.payerId == currentUserId }.sumOf { it.amount }
    val userBalance = userPaid - equalShare

    val balanceColor =
        if (userBalance >= 0) Color(0xFF2E7D32)
        else Color(0xFFD32F2F)

    val merged = remember(messages, expenses) {
        (messages + expenses)
            .sortedBy {
                when (it) {
                    is Message -> it.timestamp
                    is Expense -> it.timestamp
                    else -> Long.MAX_VALUE
                }
            }
    }

    var input by remember { mutableStateOf("") }
    var showAddExpense by remember { mutableStateOf(false) }

    var showExpenseOverview by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var deletingExpense by remember { mutableStateOf<Expense?>(null) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current

    LaunchedEffect(merged.size) {
        if (merged.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(merged.size - 1) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(groupName) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetValueGroupState()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { showExpenseOverview = true },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Expenses")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SummaryRow(
                        label = "Total expenses",
                        value = "%.2f kr".format(totalExpenses),
                        valueColor = MaterialTheme.colorScheme.onSurface
                    )

                    SummaryRow(
                        label = if (userBalance >= 0) "You are owed" else "You owe",
                        value = "%.2f kr".format(kotlin.math.abs(userBalance)),
                        valueColor = balanceColor,
                        bold = true
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = { showAddExpense = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add Expense")
                }

                Button(
                    onClick = {
                        viewModel.finalizeBill(groupId)
                        onFinalizeBill(groupId)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Done, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (billFinalized) "View Bill" else "Finalize Bill")
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(merged) { item ->
                    when (item) {
                        is Message -> {
                            val isMe = item.senderId == currentUserId
                            ChatMessageBubble(
                                isMe = isMe,
                                senderName = if (isMe) "You" else membersMap[item.senderId] ?: "Unknown",
                                text = item.text
                            )
                        }

                        is Expense -> {
                            val isMe = item.payerId == currentUserId
                            ExpenseBubble(
                                isMe = isMe,
                                senderName = if (isMe) "You" else membersMap[item.payerId] ?: "Unknown",
                                text = "${item.description} (${String.format("%.2f", item.amount)} kr)"
                            )
                        }
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message…") },
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (input.isNotBlank()) {
                            viewModel.sendMessage(groupId, input)
                            input = ""
                            focus.clearFocus()
                            scope.launch { listState.animateScrollToItem(merged.size) }
                        }
                    }
                ) {
                    Text("Send")
                }
            }
        }
    }

    if (showAddExpense) {
        AddExpenseDialog(
            onDismiss = { showAddExpense = false },
            onConfirm = { amount, desc ->
                viewModel.addExpense(groupId, amount, desc)
            }
        )
    }

    if (showExpenseOverview) {
        ExpenseOverviewDialog(
            expenses = expenses,
            memberNames = membersMap,
            onDismiss = { showExpenseOverview = false },
            onEditClick = { expense ->
                editingExpense = expense
            },
            onDeleteClick = { expense ->
                deletingExpense = expense
            }
        )
    }

    editingExpense?.let { exp ->
        EditExpenseDialog(
            expense = exp,
            onDismiss = { editingExpense = null },
            onConfirm = { newAmount, newDesc ->
                viewModel.editExpense(groupId, exp.id, newAmount, newDesc)
                editingExpense = null
            }
        )
    }

    deletingExpense?.let { exp ->
        AlertDialog(
            onDismissRequest = { deletingExpense = null },
            title = { Text("Delete expense") },
            text = { Text("Are you sure you want to delete \"${exp.description}\" (${String.format("%.2f", exp.amount)} kr)? This cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteExpense(groupId, exp.id)
                    deletingExpense = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingExpense = null }) { Text("Cancel") }
            }
        )
    }
}


// Composables
@Composable
fun SummaryRow(label: String, value: String, valueColor: Color, bold: Boolean = false) {
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

@Composable
fun ChatMessageBubble(isMe: Boolean, senderName: String, text: String) {
    Column(Modifier.fillMaxWidth()) {

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            Text(senderName, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            Surface(
                color = if (isMe) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.animateContentSize()
            ) {
                Text(
                    text,
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = if (isMe) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ExpenseBubble(isMe: Boolean, senderName: String, text: String) {
    val bg = if (isMe) Color(0xFFBBDEFB) else Color(0xFFFFF9C4)

    Column(Modifier.fillMaxWidth()) {

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            Text(senderName, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            Surface(
                color = bg,
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 1.dp,
                modifier = Modifier.animateContentSize()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(if (isMe) "You added an expense" else "Expense", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(text)
                }
            }
        }
    }
}

@Composable
fun ExpenseOverviewDialog(
    expenses: List<Expense>,
    memberNames: Map<String, String>,
    onDismiss: () -> Unit,
    onEditClick: (Expense) -> Unit,
    onDeleteClick: (Expense) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Expenses") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (expenses.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                        Text("No expenses yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                        items(expenses) { exp ->
                            ExpenseCardRow(exp, memberNames[exp.payerId] ?: "Unknown", onEditClick, onDeleteClick)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun ExpenseCardRow(
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
fun EditExpenseDialog(
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
fun AddExpenseDialog(
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

