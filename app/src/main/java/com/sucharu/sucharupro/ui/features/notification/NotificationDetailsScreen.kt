package com.sucharu.sucharupro.ui.features.notification

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.notification.NotificationStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailsScreen(
    notificationId: String,
    viewModel: NotificationDetailsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(notificationId) {
        viewModel.loadNotification(notificationId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Details", color = Color(0xFFF8FAFC), fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFF8FAFC))
                    }
                },
                actions = {
                    val notif = state.notification
                    if (notif != null && notif.status == NotificationStatus.FAILED) {
                        IconButton(onClick = { viewModel.retryDelivery(notificationId) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color(0xFFFBBF24))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A),
        modifier = modifier
    ) { padding ->
        val notif = state.notification

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
            }
        } else if (notif == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(state.errorMessage ?: "Notification not found.", color = Color(0xFF94A3B8))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header & Status Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(notif.notificationNo, color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    NotificationPriorityBadge(priority = notif.priority)
                                    NotificationStatusBadge(status = notif.status)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(notif.title, color = Color(0xFFF8FAFC), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(notif.message, color = Color(0xFFCBD5E1), fontSize = 13.sp)

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color(0xFF334155).copy(alpha = 0.5f), thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Channel: ${notif.channel.defaultLabel}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text(formatDetailTime(notif.createdAt), color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Reference Info (if any)
                if (notif.referenceType != null && notif.referenceId != null) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Business Reference", color = Color(0xFF64748B), fontSize = 10.sp)
                                    Text("${notif.referenceType}: ${notif.referenceId}", color = Color(0xFFF8FAFC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Delivery Attempts Section
                if (state.attempts.isNotEmpty()) {
                    item {
                        Text("Delivery Attempts (${state.attempts.size})", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    items(state.attempts) { attempt ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF334155).copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Attempt #${attempt.attemptNumber} via ${attempt.channel.defaultLabel}", color = Color(0xFFF8FAFC), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Provider: ${attempt.provider}", color = Color(0xFF64748B), fontSize = 10.sp)
                                }
                                NotificationStatusBadge(status = attempt.status)
                            }
                        }
                    }
                }

                // Audit History Section
                if (state.activityEvents.isNotEmpty()) {
                    item {
                        Text("Audit Activity Trail (${state.activityEvents.size})", color = Color(0xFF34D399), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    items(state.activityEvents) { event ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(event.eventType.defaultLabel, color = Color(0xFFF8FAFC), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text(event.description, color = Color(0xFF94A3B8), fontSize = 10.sp)
                            }
                            Text(formatDetailTime(event.timestamp), color = Color(0xFF64748B), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun formatDetailTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
