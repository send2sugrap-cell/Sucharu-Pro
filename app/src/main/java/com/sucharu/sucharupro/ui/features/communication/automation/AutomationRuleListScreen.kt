package com.sucharu.sucharupro.ui.features.communication.automation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.communication.automation.CommunicationAutomationEventType

@Composable
fun AutomationRuleListScreen(
    viewModel: AutomationRuleListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onSelectRule: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            AutomationTopBar(
                title = "Automation Rules",
                onBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = AutoAccent,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Rule", tint = AutoBg)
            }
        },
        containerColor = AutoBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Event Type Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedEventType == null,
                        onClick = { viewModel.setFilterEventType(null) },
                        label = { Text("All Events", fontSize = 12.sp) }
                    )
                }
                items(CommunicationAutomationEventType.entries) { evt ->
                    FilterChip(
                        selected = state.selectedEventType == evt,
                        onClick = { viewModel.setFilterEventType(evt) },
                        label = { Text(evt.defaultLabel, fontSize = 12.sp) }
                    )
                }
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AutoAccent)
                }
                return@Column
            }

            if (state.rules.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Rule, contentDescription = null, tint = AutoTextSecondary, modifier = Modifier.size(48.dp))
                        Text("No automation rules configured for this filter.", color = AutoTextSecondary, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.rules) { rule ->
                        AutomationRuleCard(
                            rule = rule,
                            onClick = { onSelectRule(rule.ruleId) }
                        )
                    }
                }
            }
        }
    }
}
