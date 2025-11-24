package com.example.android_project_onwe.view.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android_project_onwe.AppNotificationManager
import com.example.android_project_onwe.Screen
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
    onNavigate: (Screen) -> Unit,
    viewModel: GroupChatViewModel = viewModel(),
    notificationManager: AppNotificationManager,
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(groupId) {
        viewModel.loadGroupName(groupId)
        viewModel.loadGroupMembersAndNames(groupId)
        viewModel.listenForMessages(groupId)
        viewModel.listenForExpenses(groupId)
        viewModel.listenForPayments(groupId)
        notificationManager.setCurrentOpenGroup(groupId)
    }

    val groupName by viewModel.groupName.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val membersMap by viewModel.membersMap.collectAsState()

    // Optimized merged list
    val merged by remember(messages, expenses) {
        derivedStateOf {
            (messages + expenses).sortedBy {
                when (it) {
                    is Message -> it.timestamp
                    is Expense -> it.timestamp
                    else -> Long.MAX_VALUE
                }
            }
        }
    }

    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current

    LaunchedEffect(merged.size) {
        if (merged.isNotEmpty() && listState.firstVisibleItemIndex >= merged.size - 2) {
            listState.scrollToItem(merged.size - 1)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        groupName,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        notificationManager.setCurrentOpenGroup(null)
                        viewModel.resetValueGroupState()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                ),
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            )
        },
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        notificationManager.setCurrentOpenGroup(groupId)
                        onNavigate(Screen.GroupExpenses(groupId))
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Expenses") }

                FilledTonalButton(
                    onClick = {
                        notificationManager.setCurrentOpenGroup(groupId)
                        onNavigate(Screen.GroupSettings(groupId))
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Group settings") }
            }

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = merged,
                    key = {
                        when (it) {
                            is Message -> "msg-${it.id}"
                            is Expense -> "exp-${it.id}"
                            else -> it.hashCode()
                        }
                    }
                ) { item ->
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (input.isNotBlank()) {
                                viewModel.sendMessage(groupId, input)
                                input = ""
                                focus.clearFocus()
                                scope.launch { listState.scrollToItem(merged.size - 1) }
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (input.isNotBlank()) {
                            viewModel.sendMessage(groupId, input)
                            input = ""
                            focus.clearFocus()
                            scope.launch { listState.scrollToItem(merged.size - 1) }
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) { Text("Send") }
            }
        }
    }
}
