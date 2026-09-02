package com.sucharu.sucharupro.ui.features.notification

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.notification.NotificationChannel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPreferenceScreen(
    viewModel: NotificationPreferenceViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Preferences", color = Color(0xFFF8FAFC), fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFF8FAFC))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A),
        modifier = modifier
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "Manage your notification delivery channel preferences. Note: Mandatory system and security alerts cannot be disabled.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }

                if (state.preferences.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Text("All standard channels (In-App, Push, Email, SMS, WhatsApp) enabled by default.", color = Color(0xFF38BDF8), fontSize = 12.sp, modifier = Modifier.padding(14.dp))
                        }
                    }
                } else {
                    items(state.preferences, key = { it.preferenceId }) { pref ->
                        val isMandatory = pref.channel == NotificationChannel.IN_APP && pref.notificationType.isMandatory
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pref.notificationType.defaultLabel, color = Color(0xFFF8FAFC), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Channel: ${pref.channel.defaultLabel}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                                Switch(
                                    checked = pref.enabled,
                                    onCheckedChange = { enabled ->
                                        viewModel.updatePreference(pref.preferenceId, enabled)
                                    },
                                    enabled = !isMandatory,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFF8FAFC),
                                        checkedTrackColor = Color(0xFF34D399),
                                        uncheckedThumbColor = Color(0xFF64748B),
                                        uncheckedTrackColor = Color(0xFF334155)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
