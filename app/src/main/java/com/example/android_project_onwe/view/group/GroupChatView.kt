package com.example.android_project_onwe.view.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android_project_onwe.model.Expense
import com.example.android_project_onwe.model.Message
import com.example.android_project_onwe.viewmodel.GroupChatViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatView(
    groupId: String,
    onBack: () -> Unit,
    viewModel: GroupChatViewModel = viewModel()
) {
    // DEV MODE
    val DEV_MODE = false
    val DEV_USER_ID = "b1aGkqyYBqR9GSIEB1FnbjBMrWt1"
    val currentUserId = if (DEV_MODE) DEV_USER_ID else FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(groupId) {
        viewModel.listenForMessages(groupId)
        viewModel.listenForExpenses(groupId)
        viewModel.loadGroupMembersAndNames(groupId)
        viewModel.loadGroupName(groupId)
    }

    val messages by viewModel.messages.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val membersMap by viewModel.membersMap.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val groupName by viewModel.groupName.collectAsState()

    var inputMessage by remember { mutableStateOf("") }

    var showExpenseDialog by remember { mutableStateOf(false) }
    var expenseDescription by remember { mutableStateOf("") }
    var expenseAmount by remember { mutableStateOf("") }

    var showExpenseList by remember { mutableStateOf(false) }

    val memberCount = membersMap.keys.size.takeIf { it > 0 } ?: 1
    val totalExpenses = expenses.sumOf { it.amount }
    val userPaid = expenses.filter { it.payerId == currentUserId }.sumOf { it.amount }
    val equalShare = totalExpenses / memberCount
    val userBalance = userPaid - equalShare

    val balanceColor = if (userBalance >= 0.0) Color(0xFF2E7D32) else Color(0xFFD32F2F)

    val combinedTimeline = (messages + expenses).sortedBy {
        when (it) {
            is Message -> it.timestamp
            is Expense -> it.timestamp
            else -> Long.MAX_VALUE
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(combinedTimeline.size) {
        if (combinedTimeline.isNotEmpty()) {
            listState.animateScrollToItem(combinedTimeline.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(groupName.ifEmpty { "Group Chat" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showExpenseList = true }) {
                        Icon(Icons.Default.List, contentDescription = "List Expenses")
                    }
                    Button(onClick = { showExpenseDialog = true }) {
                        Text("Add Expense")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            SummaryBox(
                totalExpenses = totalExpenses,
                userBalance = userBalance,
                balanceColor = balanceColor,
                debts = debts,
                membersMap = membersMap,
                currentUserId = currentUserId
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                items(combinedTimeline) { item ->
                    when (item) {
                        is Message -> {
                            val name = if (item.senderId == currentUserId) "You"
                            else membersMap[item.senderId] ?: "Unknown"
                            MessageRow(name, item.text, item.senderId == currentUserId)
                        }

                        is Expense -> {
                            val payerName = if (item.payerId == currentUserId) "You"
                            else membersMap[item.payerId] ?: "Unknown"
                            MessageRow(
                                name = payerName,
                                text = "${item.description} (%.2f kr)".format(item.amount),
                                isMe = item.payerId == currentUserId,
                                isExpense = true
                            )
                        }
                    }
                }
            }

            MessageInputBar(
                inputMessage = inputMessage,
                onMessageChange = { inputMessage = it },
                onSend = {
                    if (inputMessage.isNotBlank()) {
                        viewModel.sendMessage(groupId, inputMessage)
                        inputMessage = ""
                    }
                }
            )
        }
    }

    if (showExpenseDialog) {
        ExpenseDialog(
            onDismiss = { showExpenseDialog = false },
            description = expenseDescription,
            amount = expenseAmount,
            onDescriptionChange = { expenseDescription = it },
            onAmountChange = { expenseAmount = it },
            onConfirm = {
                val amount = expenseAmount.toDoubleOrNull()
                if (amount != null && expenseDescription.isNotBlank()) {
                    viewModel.addExpense(groupId, amount, expenseDescription)
                }
                showExpenseDialog = false
                expenseDescription = ""
                expenseAmount = ""
            }
        )
    }

    if (showExpenseList) {
        ExpenseListDialog(expenses, membersMap) {
            showExpenseList = false
        }
    }
}

@Composable
private fun SummaryBox(
    totalExpenses: Double,
    userBalance: Double,
    balanceColor: Color,
    debts: List<com.example.android_project_onwe.model.Debt>,
    membersMap: Map<String, String>,
    currentUserId: String?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        Column {

            Text("Total spent: %.2f kr".format(totalExpenses), fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(10.dp))

            Text(
                if (userBalance >= 0) "You are owed %.2f kr".format(userBalance)
                else "You owe %.2f kr".format(-userBalance),
                color = balanceColor,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(16.dp))

            Divider(color = Color.LightGray, thickness = 1.dp)

            Spacer(Modifier.height(12.dp))

            val owesYou = debts.filter { it.toUserId == currentUserId }
            val youOwe = debts.filter { it.fromUserId == currentUserId }

            if (owesYou.isEmpty() && youOwe.isEmpty()) {
                Text("No outstanding debts", color = Color.Gray)
                return@Column
            }

            Text("Breakdown:", fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(6.dp))

            youOwe.forEach { d ->
                val name = membersMap[d.toUserId] ?: d.toUserId
                Text("You owe $name %.2f kr".format(d.amount), color = Color(0xFFD32F2F))
            }

            owesYou.forEach { d ->
                val name = membersMap[d.fromUserId] ?: d.fromUserId
                Text("$name owes you %.2f kr".format(d.amount), color = Color(0xFF2E7D32))
            }
        }
    }
}


@Composable
private fun MessageRow(name: String, text: String, isMe: Boolean, isExpense: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            Text(name, color = Color.Gray, fontWeight = FontWeight.Medium)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            if (isExpense) ExpenseBubble(text)
            else ChatBubble(text, isMe)
        }
    }
}

@Composable
private fun ChatBubble(text: String, isMe: Boolean) {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isMe) Color(0xFF2196F3) else Color(0xFFE0E0E0))
            .padding(14.dp)
            .widthIn(max = 260.dp)
    ) {
        Text(text, color = if (isMe) Color.White else Color.Black)
    }
}

@Composable
private fun ExpenseBubble(text: String) {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE3F2FD))
            .padding(14.dp)
            .widthIn(max = 300.dp)
    ) {
        Column {
            Text("Expense added", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
            Spacer(Modifier.height(4.dp))
            Text(text, color = Color(0xFF0D47A1))
        }
    }
}

@Composable
private fun MessageInputBar(
    inputMessage: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = inputMessage,
            onValueChange = onMessageChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Type a message…") }
        )
        Spacer(Modifier.width(8.dp))
        Button(onClick = onSend) { Text("Send") }
    }
}

@Composable
private fun ExpenseDialog(
    onDismiss: () -> Unit,
    description: String,
    amount: String,
    onDescriptionChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense") },
        text = {
            Column {
                OutlinedTextField(description, onDescriptionChange, label = { Text("Description") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(amount, onAmountChange, label = { Text("Amount") })
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Add") } },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ExpenseListDialog(
    expenses: List<Expense>,
    membersMap: Map<String, String>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("All Expenses") },
        text = {
            Column(modifier = Modifier.height(300.dp).padding(8.dp)) {
                expenses.sortedBy { it.timestamp }.forEach { exp ->
                    val payer = membersMap[exp.payerId] ?: "Unknown"
                    Text("- $payer: ${exp.description} (%.2f kr)".format(exp.amount))
                    Spacer(Modifier.height(6.dp))
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Close") } }
    )
}
