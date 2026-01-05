package com.example.android_project_onwe.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android_project_onwe.model.Group
import com.example.android_project_onwe.viewmodel.GroupViewModel
import com.example.android_project_onwe.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: GroupViewModel = viewModel(),
    viewModel2: ProfileViewModel = viewModel(),
    onGroupClick: (Group) -> Unit = {},
) {
    val groups = viewModel.groups.value
    var searchQuery by remember { mutableStateOf("") }
    val filteredGroups = groups.filter { it.name.contains(searchQuery, ignoreCase = true) }

    LaunchedEffect(Unit) {
        viewModel.loadGroupsForCurrentUser()
        viewModel.startListeningToGroups()
        viewModel2.loadProfile()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Groups",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            CompactSearchField(
                searchQuery = searchQuery,
                onValueChange = { searchQuery = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredGroups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 50.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        "This user is not part of any groups yet.",
                        color = Color.Gray,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                            top = 1.dp,
                        bottom = 96.dp
                )

                ) {
                    items(filteredGroups) { group ->
                        GroupCard(group = group, onClick = { onGroupClick(group) })
                    }
                }
            }
        }
    }
}

@Composable
fun GroupCard(group: Group, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val cardBackground = if (isSystemInDarkTheme()) {
        Color(0xFF262629)
    } else {
        Color(0xFFF7F7F9)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            val avatarBackground = colors.onSurface.copy(alpha = 0.1f)

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(avatarBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSystemInDarkTheme()) Color.White else colors.onBackground,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Des: ${group.description}",
                    fontSize = 12.sp,
                    color = if (isSystemInDarkTheme()) Color(0xFFB0B0B0) else colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Members: ${group.members.size}",
                    fontSize = 12.sp,
                    color = if (isSystemInDarkTheme()) Color(0xFFB0B0B0) else colors.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
fun CompactSearchField(
    searchQuery: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val fontSize = 16.sp
    val lineHeight = 20.sp
    val maxLines = 5
    val minHeight = 45.dp
    val maxHeight = lineHeight.value.dp * maxLines + 16.dp

    val roundedCorners = 12.dp

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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(textFieldHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {

                if (searchQuery.isEmpty()) {
                    Text(
                        "Search",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = fontSize,
                        lineHeight = lineHeight

                    )
                }

                BasicTextField(
                    value = searchQuery,
                    onValueChange = onValueChange,
                    maxLines = maxLines,
                    textStyle = TextStyle(
                        fontSize = fontSize,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = lineHeight
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Search
                    ),
                    interactionSource = interactionSource,
                    onTextLayout = { layoutResult ->
                        val heightDp = with(density) { layoutResult.size.height.toDp() } + 16.dp
                        textFieldHeight = heightDp.coerceIn(minHeight, maxHeight)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}





