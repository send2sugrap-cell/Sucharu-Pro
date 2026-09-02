package com.sucharu.sucharupro.ui.features.finance.reconciliation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationType

@Composable
fun FinancialReconciliationDashboardScreen(
    viewModel: FinancialReconciliationViewModel,
    onNavigateToExecute: (String) -> Unit,
    onNavigateToDiscrepancies: () -> Unit,
    onNavigateToPeriodClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.dashboardState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0F172A)
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Financial Control & Reconciliation",
                                color = Color(0xFFF8FAFC),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Period closing, cash/bank reconciliation & discrepancy tracking",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Control Summary Cards
                item {
                    val summary = state.summary
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FinancialControlStatCard(
                                title = "Active Period",
                                value = summary?.activePeriod?.periodName ?: "No Open Period",
                                subtitle = summary?.activePeriod?.status?.defaultLabel ?: "Inactive",
                                badgeText = summary?.activePeriod?.status?.defaultLabel,
                                badgeColor = Color(0xFF38BDF8),
                                modifier = Modifier.weight(1f)
                            )
                            FinancialControlStatCard(
                                title = "Closing Readiness",
                                value = summary?.closingReadiness?.defaultLabel ?: "NOT READY",
                                subtitle = "${summary?.criticalDiscrepanciesCount ?: 0} Critical Blockers",
                                badgeText = summary?.closingReadiness?.name,
                                badgeColor = if (summary?.closingReadiness?.canProceedWithClosing == true) Color(0xFF10B981) else Color(0xFFEF4444),
                                isWarning = summary?.closingReadiness?.canProceedWithClosing != true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FinancialControlStatCard(
                                title = "Reconciled Cash",
                                value = summary?.cashInHandBalance?.formatted() ?: "৳ 0",
                                subtitle = "Physical counted cash",
                                badgeText = "CASH",
                                badgeColor = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                            FinancialControlStatCard(
                                title = "Reconciled Bank",
                                value = summary?.bankBalance?.formatted() ?: "৳ 0",
                                subtitle = "Bank statement balance",
                                badgeText = "BANK",
                                badgeColor = Color(0xFF6366F1),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FinancialControlStatCard(
                                title = "Open Discrepancies",
                                value = "${summary?.openDiscrepanciesCount ?: 0}",
                                subtitle = "${summary?.criticalDiscrepanciesCount ?: 0} critical severity",
                                isWarning = (summary?.openDiscrepanciesCount ?: 0) > 0,
                                modifier = Modifier.weight(1f)
                            )
                            FinancialControlStatCard(
                                title = "Reconciliations",
                                value = "${summary?.matchedReconciliationsCount ?: 0} / ${summary?.totalReconciliationsCount ?: 0}",
                                subtitle = "Completed / Total",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Quick Navigation Actions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val currentPeriodId = state.summary?.activePeriod?.periodId ?: "DEFAULT_PERIOD"
                                onNavigateToExecute(currentPeriodId)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reconcile Cash / Bank", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = onNavigateToDiscrepancies,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Discrepancies (${state.discrepancies.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = onNavigateToPeriodClose,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Period Closing", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Search & Filter
                item {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search by reference or number...", color = Color(0xFF64748B), fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedTextColor = Color(0xFFF8FAFC),
                            unfocusedTextColor = Color(0xFFF8FAFC),
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Filter Chips
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = state.selectedStatusFilter == null,
                                onClick = { viewModel.setStatusFilter(null) },
                                label = { Text("All Statuses", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF38BDF8),
                                    selectedLabelColor = Color(0xFF0F172A),
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                        items(FinancialReconciliationStatus.entries) { status ->
                            FilterChip(
                                selected = state.selectedStatusFilter == status,
                                onClick = { viewModel.setStatusFilter(status) },
                                label = { Text(status.defaultLabel, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF38BDF8),
                                    selectedLabelColor = Color(0xFF0F172A),
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }

                // Reconciliations List
                item {
                    Text(
                        text = "Reconciliation Records (${state.reconciliations.size})",
                        color = Color(0xFFF8FAFC),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val filtered = state.reconciliations.filter { item ->
                    (state.selectedStatusFilter == null || item.status == state.selectedStatusFilter) &&
                    (state.selectedTypeFilter == null || item.reconciliationType == state.selectedTypeFilter) &&
                    (state.searchQuery.isBlank() || item.reconciliationNo.contains(state.searchQuery, ignoreCase = true) || (item.referenceId?.contains(state.searchQuery, ignoreCase = true) == true))
                }

                if (filtered.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No reconciliation records found", color = Color(0xFF94A3B8), fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    items(filtered) { rec ->
                        ReconciliationItemCard(
                            reconciliation = rec,
                            onClick = { onNavigateToExecute(rec.periodId) }
                        )
                    }
                }
            }
        }
    }
}
