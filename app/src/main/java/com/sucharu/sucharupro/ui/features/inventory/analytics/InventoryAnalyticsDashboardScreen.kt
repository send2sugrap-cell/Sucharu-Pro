package com.sucharu.sucharupro.ui.features.inventory.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryAnalyticsActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryAnalyticsPeriod
import java.util.Locale

/**
 * Main dashboard for Inventory Analytics (Module 07 Step 10).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryAnalyticsDashboardScreen(
    viewModel: InventoryAnalyticsDashboardViewModel,
    onNavigateToGovernance: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory Analytics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadDashboard(uiState.projectId) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
        ) {
            // Period Selector
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(InventoryAnalyticsPeriod.entries) { period ->
                    FilterChip(
                        selected = uiState.selectedPeriod == period,
                        onClick = { viewModel.onPeriodChanged(period) },
                        label = { Text(period.name.replace("_", " ")) }
                    )
                }
            }

            if (uiState.isLoading && uiState.summary == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        KpiSection(uiState)
                    }

                    item {
                        InventoryTrendCard(
                            title = "Stock Quantity Trend",
                            trends = uiState.trends
                        )
                    }

                    item {
                        Button(
                            onClick = onNavigateToGovernance,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Go to Governance & Exceptions")
                        }
                    }

                    item {
                        Text(
                            text = "Recent Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (uiState.activityEvents.isEmpty()) {
                        item {
                            Text(
                                text = "No recent activity recorded.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(uiState.activityEvents) { event ->
                            ActivityEventItem(event = event)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiSection(uiState: InventoryAnalyticsDashboardUiState) {
    val summary = uiState.summary ?: return

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InventoryKpiCard(
                label = "Total Stock",
                value = String.format(Locale.getDefault(), "%.0f", summary.totalStockQuantity),
                modifier = Modifier.weight(1f)
            )
            if (uiState.hasFinancialAccess) {
                InventoryKpiCard(
                    label = "Stock Value",
                    value = summary.totalStockValue?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "N/A",
                    subValue = summary.valuationStatus.name,
                    valueColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InventoryKpiCard(
                label = "Alerts",
                value = "${summary.lowStockCount + summary.criticalStockCount}",
                subValue = "${summary.outOfStockCount} Out of Stock",
                valueColor = if (summary.criticalStockCount > 0) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            InventoryKpiCard(
                label = "Open Exceptions",
                value = "${summary.openExceptionsCount}",
                valueColor = if (summary.openExceptionsCount > 0) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ActivityEventItem(event: InventoryAnalyticsActivityEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = event.eventType.name.replace("_", " "),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = event.timestamp, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = event.description, fontSize = 13.sp)
            event.actorName?.let {
                Text(text = "by $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
