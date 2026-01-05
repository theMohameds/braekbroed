package com.example.android_project_onwe.view.group

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android_project_onwe.AppNotificationManager
import com.example.android_project_onwe.Screen
import com.example.android_project_onwe.model.Expense
import com.example.android_project_onwe.model.Message
import com.example.android_project_onwe.viewmodel.GroupChatViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@RequiresApi(Build.VERSION_CODES.O)
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

    // Load all initial data
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

    // Merge messages and expenses safely
    val merged by remember(messages, expenses, membersMap) {
        derivedStateOf {
            if (membersMap.isEmpty()) emptyList()
            else (messages + expenses).sortedBy {
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

    // Flag to scroll after sending a message
    val shouldScroll = remember { mutableStateOf(false) }

    // Scroll when a new message appears
    LaunchedEffect(merged.size) {
        if (merged.isNotEmpty()) {
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
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(.1.dp)
                ),
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 18.dp)
                    .imePadding()
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.Bottom
            ) {
                CompactMessageField(
                    input = input,
                    onValueChange = { input = it },
                    onSend = {
                        viewModel.sendMessage(groupId, input)
                        input = ""
                        shouldScroll.value = true
                    },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (input.isNotBlank()) {
                            viewModel.sendMessage(groupId, input)
                            input = ""
                            shouldScroll.value = true
                        }
                    },
                    modifier = Modifier.height(43.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send"
                    )
                }
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {

            Spacer(Modifier.height(10.dp))

            // Navigation buttons
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

            // Chat and expense list
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
                ) { currentItem ->

                    val prevItem = merged.getOrNull(merged.indexOf(currentItem) - 1)
                    val showName = shouldShowName(prevItem, currentItem)
                    val showTime = shouldShowTimestamp(prevItem, currentItem)

                    when (currentItem) {
                        is Message -> {
                            val isMe = currentItem.senderId == currentUserId
                            ChatMessageBubble(
                                isMe = isMe,
                                senderName = if (isMe) "You" else membersMap[currentItem.senderId] ?: "Unknown",
                                text = currentItem.text,
                                showName = showName,
                                time = currentItem.timestamp,
                                showTime = showTime
                            )
                        }

                        is Expense -> {
                            val isMe = currentItem.payerId == currentUserId
                            ExpenseBubble(
                                isMe = isMe,
                                senderName = if (isMe) "You" else membersMap[currentItem.payerId] ?: "Unknown",
                                text = "${currentItem.description} (${String.format("%.2f", currentItem.amount)} kr)",
                                showName = showName,
                                time = currentItem.timestamp,
                                showTime = showTime
                            )
                        }
                    }
                }

            }
            Spacer(Modifier.height(10.dp))

        }
    }
}

private fun shouldShowName(prev: Any?, current: Any): Boolean {
    if (prev == null) return true

    return when {
        prev is Message && current is Message ->
            prev.senderId != current.senderId

        prev is Expense && current is Expense ->
            prev.payerId != current.payerId

        prev is Message && current is Expense ->
            prev.senderId != current.payerId

        prev is Expense && current is Message ->
            prev.payerId != current.senderId

        else -> true
    }
}

private fun shouldShowTimestamp(prev: Any?, current: Any): Boolean {
    if (prev == null) return true

    val fiveMinutes = 5 * 60 * 1000L // Five minutes

    // Get timestamps
    val currentTime = when (current) {
        is Message -> current.timestamp
        is Expense -> current.timestamp
        else -> return true
    }

    val prevTime = when (prev) {
        is Message -> prev.timestamp
        is Expense -> prev.timestamp
        else -> return true
    }

    return (currentTime - prevTime) >= fiveMinutes
}
@Composable
private fun CompactMessageField(
    input: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val fontSize = 16.sp
    val lineHeight = 20.sp
    val maxLines = 5
    val minHeight = 43.dp
    val maxHeight = lineHeight.value.dp * maxLines + 16.dp
    val textPadding = 3.dp

    val roundedCorners = 20.dp

    var textFieldHeight by remember { mutableStateOf(minHeight) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(roundedCorners))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(roundedCorners)
            )
            .padding(horizontal = 12.dp)
            .heightIn(min = minHeight, max = maxHeight),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(textFieldHeight),
            contentAlignment = Alignment.CenterStart
        ) {
            if (input.isEmpty()) {
                Text(
                    "Type a message…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    modifier = Modifier
                        .padding(textPadding)
                )
            }

            BasicTextField(
                value = input,
                onValueChange = onValueChange,
                maxLines = maxLines,
                textStyle = TextStyle(
                    fontSize = fontSize,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = lineHeight,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (input.isNotBlank()) onSend()
                    }
                ),
                interactionSource = interactionSource,
                onTextLayout = { layoutResult ->
                    val heightPx = layoutResult.size.height
                    val heightDp: Dp = with(density) { heightPx.toDp() } + 16.dp
                    textFieldHeight = heightDp.coerceIn(minHeight, maxHeight)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart)
                    .padding(horizontal = textPadding)
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun Long.toFormattedChatTime(): String {
    val dateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(this),
        ZoneId.systemDefault()
    )
    val now = LocalDateTime.now()

    return when {
        dateTime.toLocalDate() == now.toLocalDate() -> {
            // Same day → only time
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            dateTime.format(formatter)
        }
        // Same week but different day
        dateTime.get(java.time.temporal.WeekFields.of(java.util.Locale.getDefault())
            .weekOfWeekBasedYear()) ==
                now.get(java.time.temporal.WeekFields.of(java.util.Locale.getDefault())
                    .weekOfWeekBasedYear()) &&
                dateTime.year == now.year -> {
            val formatter = DateTimeFormatter.ofPattern("EEEE HH:mm") // Monday 14:35
            dateTime.format(formatter)
        }
        else -> {
            // Older → full date + time
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            dateTime.format(formatter)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ChatMessageBubble(
    isMe: Boolean,
    senderName: String,
    text: String,
    showName: Boolean,
    time: Long,
    showTime: Boolean
) {
    Column(Modifier.fillMaxWidth()) {

        if (showTime && time != null) {
            Row(
                Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(time.toFormattedChatTime(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (showName && senderName.isNotBlank() || showTime && senderName.isNotBlank()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 5.dp),
                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
            ) {
                Text(
                    text = senderName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(max = 140.dp)
                )

            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            Surface(
                color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.animateContentSize()
            ) {
                Text(
                    text,
                    Modifier.padding(12.dp, 8.dp),
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ExpenseBubble(
    isMe: Boolean,
    senderName: String,
    text: String,
    showName: Boolean,
    time: Long,
    showTime: Boolean
) {

    val darkMode = isSystemInDarkTheme()

    val bg = if (isMe) {
        if (darkMode) Color(0xFF3168AF) else Color(0xFFBBDEFB)
    } else {
        if (darkMode) Color(0xFFE1AE4F) else Color(0xFFFFF9C4)
    }

    val content = if (isMe) {
        if (darkMode) Color.White else Color.Black
    } else {
        Color.Black
    }

    Column(Modifier.fillMaxWidth()) {

        if (showTime && time != null) {
            Row(
                Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(time.toFormattedChatTime(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (showName && senderName.isNotBlank() || showTime && senderName.isNotBlank()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 5.dp),
                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
            ) {
                Text(
                    text = senderName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(max = 140.dp)
                )
            }
        }

        // Expense bubble
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            Surface(
                color = bg,
                contentColor = content,
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 1.dp,
                modifier = Modifier.animateContentSize()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        if (isMe) "You added an expense" else "Expense",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(text)
                }
            }
        }
    }
}



