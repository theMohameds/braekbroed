package com.example.android_project_onwe.view.group

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
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

    val groupedReceipt: Map<String, List<Expense>> =
        expenses.groupBy { it.payerId }.mapKeys { (payerId, _) ->
            if (payerId == currentUserId) "You" else members[payerId] ?: "Unknown"
        }

    val paymentsToBeMade = payments.filter { it.fromUser?.id == currentUserId }
    val paymentsToBeReceived = payments.filter { it.toUser?.id == currentUserId }

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
                items(paymentsToBeMade, key = { it.id }) { p ->

                    val toLabel = members[p.toUser?.id] ?: "Unknown"

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
                                Text("You owe $toLabel", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "%.2f kr".format(p.amount),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            SlideToPay(
                                isPaid = p.isPaid,
                                onSlideComplete = {
                                    viewModel.togglePaid(groupId, p.id, true)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))


            Text("Payments to be received", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(paymentsToBeReceived, key = { it.id }) { p ->

                    val fromLabel = members[p.fromUser?.id] ?: "Unknown"

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
                                Text("$fromLabel owes you", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "%.2f kr".format(p.amount),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (p.isPaid) {
                                Text(
                                    text = "Payment completed ✔",
                                    color = Color(0xFF2E7D32),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                IconButton(
                                    onClick = {
                                        viewModel.sendPaymentReminder(
                                            toUserId = p.fromUser?.id ?: "",
                                            amount = p.amount
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Notifications,
                                        contentDescription = "Send reminder",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
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

@Composable
fun SlideToPay(
    isPaid: Boolean,
    onSlideComplete: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var isSliding by remember { mutableStateOf(false) }

    val sliderHeight = 40.dp
    val thumbSize = 40.dp
    val corner = RoundedCornerShape(20.dp)
    val bgColor = MaterialTheme.colorScheme.surfaceVariant
    val activeColor = Color(0xFF58A85C)
    val thumbColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface
    val completedTextColor = Color.White

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .height(sliderHeight)
            .clip(corner)
            .background(bgColor)
    ) {
        val maxWidthPx = constraints.maxWidth.toFloat() - with(LocalDensity.current) { thumbSize.toPx() }

        // Lock thumb at end if payment is done
        val currentOffset = if (isPaid) maxWidthPx else offsetX
        val progress = currentOffset / maxWidthPx

        // Active track fills up to thumb
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(with(LocalDensity.current) { (currentOffset + thumbSize.toPx()).toDp() })
                .clip(corner)
                .background(activeColor)
        )

        // Center text
        Text(
            text = if (isPaid) "Payment completed ✔" else "Slide to pay",
            modifier = Modifier.align(Alignment.Center),
            color = if (isPaid) completedTextColor else textColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )

        // Thumb (ball), vertically centered
        Box(
            modifier = Modifier
                .size(thumbSize)
                .offset { IntOffset(currentOffset.toInt(), ((sliderHeight - thumbSize) / 2).roundToPx()) }
                .clip(CircleShape)
                .background(thumbColor)
                .pointerInput(isPaid) {
                    if (!isPaid) {
                        detectDragGestures(
                            onDragStart = { isSliding = true },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetX = (offsetX + dragAmount.x).coerceIn(0f, maxWidthPx)
                            },
                            onDragEnd = {
                                isSliding = false
                                if (offsetX >= maxWidthPx * 0.95f) {
                                    offsetX = maxWidthPx
                                    onSlideComplete()
                                } else {
                                    offsetX = 0f
                                }
                            }
                        )
                    }
                }
        )
    }
}








