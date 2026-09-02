package com.sucharu.sucharupro.ui.features.communication.automation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AutomationRuleDetailsScreen(
    ruleId: String,
    viewModel: AutomationRuleDetailsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(ruleId) { viewModel.load(ruleId = ruleId) }

    Column(modifier = Modifier.fillMaxSize().background(AutoBg)) {
        AutomationTopBar(
            title = state.rule?.ruleNo ?: "Rule Details",
            onBack = onNavigateBack
        )

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AutoAccent)
            }
            return@Column
        }

        val rule = state.rule
        if (rule == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error ?: "Rule not found.", color = AutoAccentRed)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Rule Overview Card
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AutoSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(rule.eventType.defaultLabel, color = AutoAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Switch(
                                checked = rule.enabled,
                                onCheckedChange = { viewModel.toggleEnabled(rule.projectId, rule.ruleId, it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = AutoAccentGreen)
                            )
                        }

                        Text(rule.name, color = AutoTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        if (rule.description.isNotBlank()) {
                            Text(rule.description, color = AutoTextSecondary, fontSize = 14.sp)
                        }

                        HorizontalDivider(color = AutoBorder)

                        Text("Notification Template:", color = AutoTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(rule.titleTemplate, color = AutoTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(rule.messageTemplate, color = AutoTextSecondary, fontSize = 13.sp)

                        HorizontalDivider(color = AutoBorder)

                        Text("Conditions (${rule.conditions.size}):", color = AutoTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        if (rule.conditions.isEmpty()) {
                            Text("Always matches for this event (No additional condition filter).", color = AutoTextSecondary, fontSize = 12.sp)
                        } else {
                            rule.conditions.forEach { cond ->
                                Text("• ${cond.field} ${cond.operator.defaultLabel} '${cond.expectedValue}'", color = AutoTextPrimary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Executions for this Rule
            item {
                Text("Executions (${state.executions.size})", color = AutoTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            if (state.executions.isEmpty()) {
                item {
                    Text("No executions triggered by this rule yet.", color = AutoTextSecondary, fontSize = 12.sp)
                }
            } else {
                items(state.executions) { exec ->
                    AutomationExecutionCard(execution = exec)
                }
            }
        }
    }
}
