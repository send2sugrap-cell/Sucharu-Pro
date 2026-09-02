package com.sucharu.sucharupro.ui.features.finance.reporting

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.sucharu.sucharupro.domain.model.finance.FinancialReportExportFormat
import com.sucharu.sucharupro.domain.model.finance.FinancialReportType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialReportingDashboardScreen(
    viewModel: FinancialReportingViewModel,
    onNavigateToReport: (FinancialReportType) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Financial Reporting & Analytics",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC)
                        )
                        Text(
                            text = "Module 09 Step 09 • Executive Accounting Statements",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFilterSheet(true) }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = Color(0xFF60A5FA))
                    }
                    IconButton(onClick = { viewModel.createSnapshot(FinancialReportType.DASHBOARD) }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Snapshot", tint = Color(0xFF34D399))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A),
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                ReportPeriodSelector(
                    selectedPeriod = state.selectedPeriod,
                    onPeriodSelected = { viewModel.selectPeriod(it) }
                )
            }

            // Error or Control Warning
            state.errorMessage?.let { err ->
                item {
                    ControlExceptionBanner(exceptions = listOf(err))
                }
            }

            state.successMessage?.let { msg ->
                item {
                    Surface(
                        color = Color(0xFF064E3B),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF6EE7B7))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(msg, color = Color(0xFF6EE7B7), fontSize = 12.sp)
                        }
                    }
                }
            }

            // Top KPI Cards Grid
            item {
                val kpi = state.kpiSummary
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FinancialKpiCard(
                            title = "Total Revenue",
                            amount = kpi?.totalRevenue ?: com.sucharu.sucharupro.domain.model.common.Money.ZERO,
                            subtitle = "Inflow Revenue",
                            icon = Icons.Default.TrendingUp,
                            iconBgColor = Color(0xFF064E3B),
                            iconTint = Color(0xFF6EE7B7),
                            modifier = Modifier.weight(1f)
                        )
                        FinancialKpiCard(
                            title = "Total Expenses",
                            amount = kpi?.totalExpenses ?: com.sucharu.sucharupro.domain.model.common.Money.ZERO,
                            subtitle = "Disbursed",
                            icon = Icons.Default.TrendingDown,
                            iconBgColor = Color(0xFF7F1D1D),
                            iconTint = Color(0xFFFCA5A5),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FinancialKpiCard(
                            title = "Net Profit / Margin",
                            amount = kpi?.netProfit ?: com.sucharu.sucharupro.domain.model.common.Money.ZERO,
                            percentage = kpi?.netProfitMarginPercent,
                            subtitle = "Net Margin",
                            icon = Icons.Default.AttachMoney,
                            iconBgColor = Color(0xFF1E3A8A),
                            iconTint = Color(0xFF60A5FA),
                            modifier = Modifier.weight(1f)
                        )
                        FinancialKpiCard(
                            title = "Cash Position",
                            amount = kpi?.cashPosition ?: com.sucharu.sucharupro.domain.model.common.Money.ZERO,
                            subtitle = "Liquid Cash In Hand",
                            icon = Icons.Default.AccountBalanceWallet,
                            iconBgColor = Color(0xFF581C87),
                            iconTint = Color(0xFFD8B4FE),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FinancialKpiCard(
                            title = "Receivable Due",
                            amount = kpi?.totalReceivableOutstanding ?: com.sucharu.sucharupro.domain.model.common.Money.ZERO,
                            percentage = kpi?.overdueReceivableRatioPercent,
                            subtitle = "Customer Due",
                            icon = Icons.Default.Receipt,
                            iconBgColor = Color(0xFF78350F),
                            iconTint = Color(0xFFFDE68A),
                            modifier = Modifier.weight(1f)
                        )
                        FinancialKpiCard(
                            title = "Payable Due",
                            amount = kpi?.totalPayableOutstanding ?: com.sucharu.sucharupro.domain.model.common.Money.ZERO,
                            percentage = kpi?.overduePayableRatioPercent,
                            subtitle = "Vendor Due",
                            icon = Icons.Default.LocalShipping,
                            iconBgColor = Color(0xFF334155),
                            iconTint = Color(0xFF94A3B8),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Financial Statements & Reports Directory
            item {
                Text(
                    text = "Financial Statements & Management Reports",
                    color = Color(0xFFF8FAFC),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            val reportCards = listOf(
                FinancialReportType.PROFIT_AND_LOSS to Icons.Default.Assessment,
                FinancialReportType.BALANCE_SHEET to Icons.Default.AccountBalance,
                FinancialReportType.CASH_FLOW to Icons.Default.SwapHoriz,
                FinancialReportType.TRIAL_BALANCE to Icons.Default.Balance,
                FinancialReportType.GENERAL_LEDGER to Icons.Default.Book,
                FinancialReportType.ACCOUNTS_RECEIVABLE to Icons.Default.ReceiptLong,
                FinancialReportType.ACCOUNTS_PAYABLE to Icons.Default.Payment,
                FinancialReportType.EXPENSE_ANALYSIS to Icons.Default.PieChart,
                FinancialReportType.CUSTOMER_PAYMENT to Icons.Default.People,
                FinancialReportType.SUPPLIER_PAYMENT to Icons.Default.Store,
                FinancialReportType.ADJUSTMENT to Icons.Default.Tune,
                FinancialReportType.PERIOD_COMPARISON to Icons.Default.CompareArrows
            )

            items(reportCards.size) { idx ->
                val (type, icon) = reportCards[idx]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { onNavigateToReport(type) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = type.defaultLabel,
                                color = Color(0xFFF8FAFC),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "View detailed statement & export",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF64748B)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (state.isFilterSheetVisible) {
            ReportFilterBottomSheet(
                filter = state.filter,
                onApplyFilter = { viewModel.applyFilter(it) },
                onDismiss = { viewModel.toggleFilterSheet(false) }
            )
        }
    }
}
