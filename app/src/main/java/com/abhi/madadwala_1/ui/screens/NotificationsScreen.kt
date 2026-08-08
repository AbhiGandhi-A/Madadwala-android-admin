package com.abhi.madadwala_1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.madadwala_1.data.NotificationData
import com.abhi.madadwala_1.data.PreferenceManager
import com.abhi.madadwala_1.ui.theme.MadadwalaColors
import com.abhi.madadwala_1.ui.navigation.Screen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class NotificationType {
    ALL, MESSAGES, CALLS, UPDATES, SYSTEM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit = {}) {
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    val notifications by preferenceManager.savedNotifications.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    
    var activeChatBookingId by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf(NotificationType.ALL) }

    val filteredNotifications = remember(notifications, selectedFilter) {
        if (selectedFilter == NotificationType.ALL) notifications
        else notifications.filter { getNotificationType(it) == selectedFilter }
    }

    LaunchedEffect(Unit) {
        preferenceManager.setUnreadNotificationsCount(0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Notifications", 
                        fontWeight = FontWeight.Bold,
                        color = MadadwalaColors.Ink
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = MadadwalaColors.Ink
                        )
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = { scope.launch { preferenceManager.clearNotifications() } },
                        modifier = Modifier.padding(end = 8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MadadwalaColors.Green
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear All", color = MadadwalaColors.Green, fontSize = 14.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                val items = listOf(
                    Triple("Home", Icons.Default.Home, Screen.Home.route),
                    Triple("Bookings", Icons.AutoMirrored.Filled.Assignment, Screen.Home.route), // In HomeScreen it's a tab
                    Triple("Notifications", Icons.Default.Notifications, Screen.Notifications.route),
                    Triple("Profile", Icons.Default.Person, Screen.Home.route) // In HomeScreen it's a tab
                )
                items.forEach { (label, icon, route) ->
                    val isSelected = label == "Notifications"
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { 
                            if (!isSelected) {
                                if (route == Screen.Home.route) {
                                    // For simplicity, we just go back to home. 
                                    // If we want to go to a specific tab, we'd need more logic.
                                    onBack() 
                                } else {
                                    onNavigate(route)
                                }
                            }
                        },
                        label = { 
                            Text(
                                text = label, 
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MadadwalaColors.Green else Color.Gray
                            ) 
                        },
                        icon = {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MadadwalaColors.Green.copy(alpha = 0.1f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(icon, contentDescription = label, tint = MadadwalaColors.Green)
                                }
                            } else {
                                Icon(icon, contentDescription = label, tint = Color.Gray)
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        },
        containerColor = MadadwalaColors.Cream
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            FilterRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )
            
            if (filteredNotifications.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No notifications found", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredNotifications) { notification ->
                        NotificationItem(notification, onClick = {
                            if (!notification.bookingId.isNullOrEmpty()) {
                                activeChatBookingId = notification.bookingId
                            }
                        })
                    }
                }
            }
        }
    }

    if (activeChatBookingId != null) {
        com.abhi.madadwala_1.ui.components.BookingChatBottomSheet(
            bookingId = activeChatBookingId!!,
            onDismiss = { activeChatBookingId = null }
        )
    }
}

@Composable
fun FilterRow(selectedFilter: NotificationType, onFilterSelected: (NotificationType) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            FilterChipItem(
                text = "All",
                isSelected = selectedFilter == NotificationType.ALL,
                onClick = { onFilterSelected(NotificationType.ALL) },
                icon = Icons.Default.Notifications
            )
        }
        item {
            FilterChipItem(
                text = "Messages",
                isSelected = selectedFilter == NotificationType.MESSAGES,
                onClick = { onFilterSelected(NotificationType.MESSAGES) },
                icon = Icons.Default.Chat
            )
        }
        item {
            FilterChipItem(
                text = "Calls",
                isSelected = selectedFilter == NotificationType.CALLS,
                onClick = { onFilterSelected(NotificationType.CALLS) },
                icon = Icons.Default.Call
            )
        }
        item {
            FilterChipItem(
                text = "Updates",
                isSelected = selectedFilter == NotificationType.UPDATES,
                onClick = { onFilterSelected(NotificationType.UPDATES) },
                icon = Icons.Default.DirectionsCar
            )
        }
        item {
            FilterChipItem(
                text = "System",
                isSelected = selectedFilter == NotificationType.SYSTEM,
                onClick = { onFilterSelected(NotificationType.SYSTEM) },
                icon = Icons.Default.Settings
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChipItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { 
            Text(
                text = text, 
                fontSize = 13.sp,
                color = if (isSelected) Color.White else Color.DarkGray
            ) 
        },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) Color.White else Color.DarkGray
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MadadwalaColors.Green,
            containerColor = Color.White,
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = if (isSelected) Color.Transparent else Color.LightGray.copy(alpha = 0.5f),
            enabled = true,
            selected = isSelected,
            borderWidth = 1.dp
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun NotificationItem(notification: NotificationData, onClick: () -> Unit = {}) {
    val sdf = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val date = remember(notification.timestamp) { sdf.format(Date(notification.timestamp)) }
    val type = getNotificationType(notification)
    
    val themeColor = when {
        notification.title.contains("Arrived", ignoreCase = true) -> MadadwalaColors.Orange
        notification.title.contains("way", ignoreCase = true) -> MadadwalaColors.Blue
        type == NotificationType.MESSAGES -> MadadwalaColors.Green
        type == NotificationType.CALLS -> MadadwalaColors.Purple
        type == NotificationType.SYSTEM -> Color.Gray
        else -> MadadwalaColors.Green
    }

    val lightColor = when {
        notification.title.contains("Arrived", ignoreCase = true) -> MadadwalaColors.LightOrange
        notification.title.contains("way", ignoreCase = true) -> MadadwalaColors.LightBlue
        type == NotificationType.MESSAGES -> MadadwalaColors.LightGreen
        type == NotificationType.CALLS -> MadadwalaColors.LightPurple
        type == NotificationType.SYSTEM -> Color.LightGray.copy(alpha = 0.2f)
        else -> MadadwalaColors.LightGreen
    }

    val icon = when {
        notification.title.contains("Arrived", ignoreCase = true) -> Icons.Default.LocationOn
        notification.title.contains("way", ignoreCase = true) -> Icons.Default.Navigation
        type == NotificationType.MESSAGES -> Icons.Default.Chat
        type == NotificationType.CALLS -> Icons.Default.Call
        type == NotificationType.SYSTEM -> Icons.Default.Settings
        else -> Icons.Default.Notifications
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon section
            Box(modifier = Modifier.size(48.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(lightColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                // Status dot on icon
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(1.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(themeColor)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Content section
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MadadwalaColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp
                            ),
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(themeColor)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    ),
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun getNotificationType(notification: NotificationData): NotificationType {
    val title = notification.title.lowercase()
    val message = notification.message.lowercase()
    return when {
        title.contains("message") || message.contains("message") -> NotificationType.MESSAGES
        title.contains("call") || message.contains("call") -> NotificationType.CALLS
        title.contains("arrived") || title.contains("way") || title.contains("partner") || 
        message.contains("arrived") || message.contains("way") || message.contains("location") -> NotificationType.UPDATES
        else -> NotificationType.SYSTEM
    }
}
