package com.sucharu.sucharupro.ui.features.returns.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.returns.ReturnAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Main dashboard screen for Return Analytics & KPIs (Module 11 Step 06).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnAnalyticsDashboardScreen(
    projectId: String,
    viewModel: ReturnAnalyticsDashboardViewModel,
    userRole: UserRole? = null,
    onNavigateToGovernance: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.loadAnalytics(projectId = projectId, callerRole = userRole)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Return Analytics & KPIs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToGovernance) {
                        Icon(Icons.Default.Warning, contentDescription = "Governance Center")
                    }
                    IconButton(onClick = { viewModel.refresh(userRole) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
        ) {
            // Period Filter Selector
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(ReturnAnalyticsPeriod.entries) { period ->
                    FilterChip(
                        selected = uiState.selectedPeriod == period,
                        onClick = { viewModel.onPeriodChanged(period, userRole) },
                        label = { Text(period.displayName) }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.errorMessage ?: "Error loading analytics",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                val summary = uiState.summary
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    if (summary != null) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ReturnKpiCard(
                                    title = "Total Returns",
                                    value = summary.totalReturns.toString(),
                                    subtitle = "Rate: ${summary.returnRate}%",
                                    modifier = Modifier.weight(1f)
                                )
                                ReturnKpiCard(
                                    title = "Settled Value",
                                    value = summary.totalSettledValue.formatted(),
                                    subtitle = "${summary.settledReturns} Settled",
                                    badgeColor = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ReturnKpiCard(
                                    title = "Accepted Qty",
                                    value = summary.totalAcceptedQuantity.toString(),
                                    subtitle = "of ${summary.totalRequestedQuantity} requested",
                                    modifier = Modifier.weight(1f)
                                )
                                ReturnKpiCard(
                                    title = "Avg Turnaround",
                                    value = "${summary.averageTurnaroundDays}d",
                                    subtitle = "${summary.openReturns} Open Returns",
                                    badgeColor = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    item {
                        ReturnDefectBreakdownCard(breakdowns = uiState.defectBreakdown)
                    }

                    item {
                        ReturnFinancialBreakdownCard(breakdowns = uiState.financialBreakdown)
                    }
                }
            }
        }
    }
}
