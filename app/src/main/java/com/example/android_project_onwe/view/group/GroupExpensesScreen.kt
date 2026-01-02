    package com.example.android_project_onwe.view.group

    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.lazy.LazyColumn
    import androidx.compose.foundation.lazy.items
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.automirrored.filled.ArrowBack
    import androidx.compose.material.icons.filled.Add
    import androidx.compose.material.icons.filled.Done
    import androidx.compose.material3.*
    import androidx.compose.runtime.*
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
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            notificationManager.setCurrentOpenGroup(groupId)
                            viewModel.resetValueGroupState()
                            onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
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
                })
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

