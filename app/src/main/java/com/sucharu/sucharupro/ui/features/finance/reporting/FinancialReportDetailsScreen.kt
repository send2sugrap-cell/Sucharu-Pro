package com.sucharu.sucharupro.ui.features.finance.reporting

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.finance.FinancialReportType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialReportDetailsScreen(
    reportType: FinancialReportType,
    viewModel: FinancialReportingViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (reportType) {
        FinancialReportType.DASHBOARD,
        FinancialReportType.KPI_SUMMARY -> FinancialReportingDashboardScreen(viewModel = viewModel, onNavigateToReport = { viewModel.selectReportType(it) }, modifier = modifier)
        FinancialReportType.PROFIT_AND_LOSS -> ProfitLossReportScreen(viewModel = viewModel, onNavigateBack = onNavigateBack, modifier = modifier)
        FinancialReportType.BALANCE_SHEET -> BalanceSheetReportScreen(viewModel = viewModel, onNavigateBack = onNavigateBack, modifier = modifier)
        FinancialReportType.CASH_FLOW -> CashFlowReportScreen(viewModel = viewModel, onNavigateBack = onNavigateBack, modifier = modifier)
        FinancialReportType.TRIAL_BALANCE -> TrialBalanceScreen(viewModel = viewModel, onNavigateBack = onNavigateBack, modifier = modifier)
        FinancialReportType.GENERAL_LEDGER -> GeneralLedgerReportScreen(viewModel = viewModel, onNavigateBack = onNavigateBack, modifier = modifier)
        FinancialReportType.ACCOUNTS_RECEIVABLE -> AccountsReceivableReportScreen(viewModel = viewModel, onNavigateBack = onNavigateBack, modifier = modifier)
        FinancialReportType.ACCOUNTS_PAYABLE -> AccountsPayableReportScreen(viewModel = viewModel, onNavigateBack = onNavigateBack, modifier = modifier)
        FinancialReportType.EXPENSE_ANALYSIS -> ExpenseAnalysisScreen(viewModel = viewModel, onNavigateBack = onNavigateBack, modifier = modifier)
        FinancialReportType.PERIOD_COMPARISON -> PeriodComparisonScreen(viewModel = viewModel, onNavigateBack = onNavigateBack, modifier = modifier)
        else -> ProfitLossReportScreen(viewModel = viewModel, onNavigateBack = onNavigateBack, modifier = modifier)
    }
}
