package com.sucharu.sucharupro.ui.features.notification

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.notification.NotificationCategory
import com.sucharu.sucharupro.domain.model.notification.NotificationPriority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    viewModel: NotificationCenterViewModel,
    onNavigateBack: () -> Unit,
    onNotificationClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Notification Center", color = Color(0xFFF8FAFC), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("${state.unreadCount} unread alerts", color = Color(0xFF38BDF8), fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFF8FAFC))
                    }
                },
                actions = {
                    if (state.unreadCount > 0) {
                        IconButton(onClick = { viewModel.markAllAsRead() }) {
                            Icon(Icons.Default.DoneAll, contentDescription = "Mark all read", tint = Color(0xFF34D399))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A),
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Input
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search notifications...", color = Color(0xFF64748B), fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B),
                    focusedTextColor = Color(0xFFF8FAFC),
                    unfocusedTextColor = Color(0xFFF8FAFC)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Category Filter Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(NotificationCenterFilter.values()) { f ->
                    val isSelected = state.selectedFilter == f
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B),
                        modifier = Modifier.clickable { viewModel.setFilter(f) }
                    ) {
                        Text(
                            text = f.defaultLabel,
                            color = if (isSelected) Color(0xFF0F172A) else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Notification List Filter Logic
            val filteredNotifications = state.notifications.filter { n ->
                val matchesSearch = state.searchQuery.isBlank() ||
                        n.title.contains(state.searchQuery, ignoreCase = true) ||
                        n.message.contains(state.searchQuery, ignoreCase = true)

                val matchesFilter = when (state.selectedFilter) {
                    NotificationCenterFilter.ALL -> true
                    NotificationCenterFilter.UNREAD -> !n.isRead
                    NotificationCenterFilter.HIGH_PRIORITY -> n.priority == NotificationPriority.HIGH || n.priority == NotificationPriority.URGENT
                    NotificationCenterFilter.ORDERS -> n.notificationType.category == NotificationCategory.ORDER
                    NotificationCenterFilter.PRODUCTION -> n.notificationType.category == NotificationCategory.PRODUCTION
                    NotificationCenterFilter.DELIVERY -> n.notificationType.category == NotificationCategory.DELIVERY
                    NotificationCenterFilter.FINANCE -> n.notificationType.category == NotificationCategory.FINANCE
                    NotificationCenterFilter.INVENTORY -> n.notificationType.category == NotificationCategory.INVENTORY
                    NotificationCenterFilter.SYSTEM -> n.notificationType.category == NotificationCategory.SYSTEM
                }

                matchesSearch && matchesFilter
            }

            if (filteredNotifications.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No notifications found.", color = Color(0xFF64748B), fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredNotifications, key = { it.notificationId }) { notification ->
                        NotificationItemCard(
                            notification = notification,
                            onClick = { onNotificationClick(notification.notificationId) },
                            onMarkAsRead = { viewModel.markAsRead(notification.notificationId) }
                        )
                    }
                }
            }
        }
    }
}
