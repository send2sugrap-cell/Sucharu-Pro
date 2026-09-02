package com.sucharu.sucharupro.ui.features.communication.automation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AutomationAnalyticsScreen(
    viewModel: AutomationAnalyticsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize().background(AutoBg)) {
        AutomationTopBar(
            title = "Automation Analytics",
            onBack = onNavigateBack
        )

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AutoAccent)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Throughput & Outcomes", color = AutoTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AutomationMetricCard(
                        title = "Generated",
                        value = "${state.summary.notificationsGenerated}",
                        icon = Icons.Default.DoneAll,
                        accentColor = AutoAccentGreen,
                        modifier = Modifier.weight(1f)
                    )
                    AutomationMetricCard(
                        title = "Suppressed",
                        value = "${state.summary.notificationsSuppressed}",
                        icon = Icons.Default.Shield,
                        accentColor = AutoAccentPurple,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AutomationMetricCard(
                        title = "Scheduled",
                        value = "${state.summary.scheduledCount}",
                        icon = Icons.Default.Schedule,
                        accentColor = AutoAccentAmber,
                        modifier = Modifier.weight(1f)
                    )
                    AutomationMetricCard(
                        title = "Escalated",
                        value = "${state.summary.escalatedCount}",
                        icon = Icons.Default.TrendingUp,
                        accentColor = AutoAccentRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AutoSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Suppression & Filter Breakdown", color = AutoTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Duplicate Triggers Blocked", color = AutoTextSecondary, fontSize = 13.sp)
                            Text("${state.summary.duplicateBlockedCount}", color = AutoTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("User Preference Blocked", color = AutoTextSecondary, fontSize = 13.sp)
                            Text("${state.summary.preferenceBlockedCount}", color = AutoTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Active Automation Rules", color = AutoTextSecondary, fontSize = 13.sp)
                            Text("${state.summary.activeRules}", color = AutoAccentGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
