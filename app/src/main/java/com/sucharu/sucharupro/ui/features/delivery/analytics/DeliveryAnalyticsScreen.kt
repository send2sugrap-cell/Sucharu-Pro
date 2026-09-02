package com.sucharu.sucharupro.ui.features.delivery.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsPeriod
import com.sucharu.sucharupro.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryAnalyticsScreen(
    viewModel: DeliveryAnalyticsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToGovernance: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Delivery Analytics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate Back"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is DeliveryAnalyticsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is DeliveryAnalyticsUiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No Delivery Data Available",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No delivery or dispatch records match the selected criteria.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is DeliveryAnalyticsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Failed to load delivery analytics",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = state.message, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.refresh() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is DeliveryAnalyticsUiState.Success -> {
                    val summary = state.summary
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(MaterialTheme.spacing.screenPadding)
                    ) {
                        // Period Filter Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DeliveryAnalyticsPeriod.entries.filter { it != DeliveryAnalyticsPeriod.CUSTOM }.forEach { p ->
                                FilterChip(
                                    selected = state.filter.period == p,
                                    onClick = { viewModel.setPeriod(p) },
                                    label = { Text(p.label) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Key Performance Metric Cards
                        Text(
                            text = "Delivery Performance & Rates",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DeliveryAnalyticsMetricCard(
                                title = "Success Rate",
                                value = "%.1f%%".format(summary.deliverySuccessRate),
                                subtitle = "${summary.totalDelivered} of ${summary.totalDispatches} Dispatched",
                                icon = Icons.Default.CheckCircle,
                                modifier = Modifier.weight(1f)
                            )
                            DeliveryAnalyticsMetricCard(
                                title = "POD Acceptance",
                                value = "%.1f%%".format(summary.podAcceptanceRate),
                                subtitle = "${summary.totalAcceptedPod} Accepted PODs",
                                icon = Icons.Default.FactCheck,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DeliveryAnalyticsMetricCard(
                                title = "Return Rate",
                                value = "%.1f%%".format(summary.returnRate),
                                subtitle = "${summary.totalReturned} Returns",
                                icon = Icons.Default.Replay,
                                modifier = Modifier.weight(1f)
                            )
                            DeliveryAnalyticsMetricCard(
                                title = "Discrepancy Rate",
                                value = "%.1f%%".format(summary.discrepancyRate),
                                subtitle = "${summary.totalDiscrepancies} Discrepancies",
                                icon = Icons.Default.Warning,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Operational Volume KPIs
                        Text(
                            text = "Operational Deliveries & Quantities",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DeliveryAnalyticsMetricCard(
                                title = "Total Orders",
                                value = "${summary.totalDeliveryOrders}",
                                subtitle = "Ordered: %.0f units".format(summary.totalOrderedQuantity),
                                icon = Icons.Default.Assignment,
                                modifier = Modifier.weight(1f)
                            )
                            DeliveryAnalyticsMetricCard(
                                title = "Delivered Qty",
                                value = "%.0f".format(summary.totalDeliveredQuantity),
                                subtitle = "Outstanding: %.0f".format(summary.totalOutstandingQuantity),
                                icon = Icons.Default.LocalShipping,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onNavigateToGovernance,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("View Governance & Risk Exceptions")
                        }
                    }
                }
            }
        }
    }
}
