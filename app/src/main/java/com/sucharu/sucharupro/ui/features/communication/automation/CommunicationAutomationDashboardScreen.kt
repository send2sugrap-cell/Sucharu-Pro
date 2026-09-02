package com.sucharu.sucharupro.ui.features.communication.automation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CommunicationAutomationDashboardScreen(
    viewModel: AutomationDashboardViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRules: () -> Unit,
    onNavigateToCreateRule: () -> Unit,
    onNavigateToExecutions: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onSelectRule: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize().background(AutoBg)) {
        AutomationTopBar(
            title = "Automation & Smart Triggers",
            onBack = onNavigateBack,
            actions = {
                IconButton(onClick = onNavigateToAnalytics) {
                    Icon(Icons.Default.Analytics, contentDescription = "Analytics", tint = AutoTextPrimary)
                }
            }
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
            // Metrics Row 1
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AutomationMetricCard(
                        title = "Active Rules",
                        value = "${state.summary.activeRules} / ${state.summary.totalRules}",
                        icon = Icons.Default.Rule,
                        accentColor = AutoAccentGreen,
                        modifier = Modifier.weight(1f)
                    )
                    AutomationMetricCard(
                        title = "Trigger Volume",
                        value = "${state.summary.totalTriggers}",
                        icon = Icons.Default.ElectricBolt,
                        accentColor = AutoAccentAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Metrics Row 2
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AutomationMetricCard(
                        title = "Dispatched",
                        value = "${state.summary.notificationsGenerated}",
                        icon = Icons.Default.DoneAll,
                        accentColor = AutoAccent,
                        modifier = Modifier.weight(1f)
                    )
                    AutomationMetricCard(
                        title = "Suppressed / Anti-Spam",
                        value = "${state.summary.notificationsSuppressed}",
                        icon = Icons.Default.Shield,
                        accentColor = AutoAccentPurple,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Actions
            item {
                Text("Management Hub", color = AutoTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onNavigateToCreateRule,
                        colors = ButtonDefaults.buttonColors(containerColor = AutoAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                        Spacer(Modifier.width(6.dp))
                        Text("New Rule", color = Color.Black, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onNavigateToExecutions,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AutoTextPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = AutoAccentAmber)
                        Spacer(Modifier.width(6.dp))
                        Text("Execution Log")
                    }
                }
            }

            // Active Rules Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Configured Rules", color = AutoTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = onNavigateToRules) {
                        Text("View All (${state.summary.totalRules})", color = AutoAccent, fontSize = 12.sp)
                    }
                }
            }

            if (state.recentRules.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AutoSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No automation rules configured.", color = AutoTextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(state.recentRules) { rule ->
                    AutomationRuleCard(
                        rule = rule,
                        onClick = { onSelectRule(rule.ruleId) }
                    )
                }
            }

            // Recent Executions Section
            item {
                Text("Recent Automation Executions", color = AutoTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            if (state.recentExecutions.isEmpty()) {
                item {
                    Text("No executions recorded yet.", color = AutoTextSecondary, fontSize = 12.sp)
                }
            } else {
                items(state.recentExecutions) { exec ->
                    AutomationExecutionCard(execution = exec)
                }
            }
        }
    }
}
