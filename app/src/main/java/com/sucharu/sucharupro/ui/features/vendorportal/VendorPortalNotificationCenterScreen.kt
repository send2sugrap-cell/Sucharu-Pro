package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.VendorPortalNotificationDto
import com.sucharu.sucharupro.data.api.model.VendorPortalNotificationUnreadCountDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalNotificationCenterScreen(
    notifications: List<VendorPortalNotificationDto>,
    unreadCounts: VendorPortalNotificationUnreadCountDto,
    onNotificationClick: (String) -> Unit = {},
    onMarkAllReadClick: () -> Unit = {},
    onPreferencesClick: () -> Unit = {},
    onCategoryFilterSelected: (String?) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notification Center (${unreadCounts.totalUnread} Unread)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    TextButton(onClick = onMarkAllReadClick) {
                        Text(text = "Mark All Read", color = Color(0xFF60A5FA))
                    }
                    TextButton(onClick = onPreferencesClick) {
                        Text(text = "Preferences", color = Color(0xFFA78BFA))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(notifications) { notif ->
                val isUnread = notif.status == "UNREAD"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNotificationClick(notif.notificationId) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUnread) Color(0xFF1E293B) else Color(0xFF0F172A)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Badge(
                                containerColor = when (notif.severity) {
                                    "CRITICAL", "URGENT" -> Color(0xFFEF4444)
                                    "HIGH" -> Color(0xFFF59E0B)
                                    else -> Color(0xFF3B82F6)
                                }
                            ) {
                                Text(
                                    text = notif.category,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (isUnread) {
                                Badge(containerColor = Color(0xFF10B981)) {
                                    Text(
                                        text = "NEW",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = notif.title,
                            fontSize = 15.sp,
                            fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = notif.message,
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}
