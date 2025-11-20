package com.example.android_project_onwe.view.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android_project_onwe.viewmodel.FinalizedBillViewModel
import com.example.android_project_onwe.model.Expense
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalizedBillScreen(
    groupId: String,
    onBack: () -> Unit,
    viewModel: FinalizedBillViewModel = viewModel()
) {
    LaunchedEffect(groupId) { viewModel.start(groupId) }

    val expenses by viewModel.expenses.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val members by viewModel.members.collectAsState()
    val groupName by viewModel.groupName.collectAsState()

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    val totalExpenses = expenses.sumOf { it.amount }

    val perPayerTotals = expenses
        .groupBy { it.payerId }
        .mapValues { (_, list) -> list.sumOf { it.amount } }

    val groupedReceipt: Map<String, List<Expense>> =
        expenses.groupBy { it.payerId }.mapKeys { (payerId, _) ->
            if (payerId == currentUserId) "You" else members[payerId] ?: "Unknown"
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Finalized", style = MaterialTheme.typography.titleMedium)
                        Text(groupName, style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { viewModel.reopenBill(groupId) { onBack() } },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Reopen")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {

            Surface(
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {

                    Text("Summary of expenses", style = MaterialTheme.typography.titleMedium)

                    Spacer(Modifier.height(12.dp))

                    groupedReceipt.forEach { (payerLabel, list) ->
                        Text(payerLabel, fontWeight = FontWeight.SemiBold)

                        list.forEach { exp ->
                            Text(
                                "• ${exp.description} — %.2f kr".format(exp.amount),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }


                        Spacer(Modifier.height(10.dp))
                    }
                    Divider()
                    Spacer(Modifier.height(10.dp))
                    SummaryRow("Total expenses:", "%.2f kr".format(totalExpenses))
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Payments to be made", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(payments, key = { it.id }) { p ->

                    val fromId = p.fromUser?.id
                    val toId = p.toUser?.id

                    val fromLabel =
                        if (fromId == currentUserId) "You" else members[fromId] ?: "Unknown"
                    val toLabel =
                        if (toId == currentUserId) "You" else members[toId] ?: "Unknown"

                    val phrase = when {
                        fromLabel == "You" -> "You owe $toLabel"
                        toLabel == "You" -> "$fromLabel owes you"
                        else -> "$fromLabel owes $toLabel"
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Column {
                                Text(phrase, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "%.2f kr".format(p.amount),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Checkbox(
                                checked = p.isPaid,
                                onCheckedChange = { checked ->
                                    viewModel.togglePaid(groupId, p.id, checked)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF2E7D32),
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    checkmarkColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
