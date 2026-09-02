package com.sucharu.sucharupro.ui.features.finance.analytics

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseAnalyticsScreen(
    viewModel: FinanceAnalyticsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val exp = state.expense

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Analytics & Burn", color = Color(0xFFF8FAFC), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFFF8FAFC))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A),
        modifier = modifier
    ) { padding ->
        if (exp == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No expense analytics available.", color = Color(0xFF94A3B8))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    ExecutiveKpiCard(
                        "Total Posted Operating Expenses",
                        exp.totalPostedExpenses,
                        subtitle = "Approved Vouchers: ${exp.approvedExpenses.formatted()}",
                        accentColor = Color(0xFFFCA5A5),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (exp.topExpenseCategories.isNotEmpty()) {
                    item {
                        Text("Top Expense Categories", color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    items(exp.topExpenseCategories) { cat ->
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(cat.categoryName, color = Color(0xFFF8FAFC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("${cat.expenseCount} vouchers • ${cat.percentageOfTotal}% of total", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                }
                                Text("${cat.totalAmount.formatted()} BDT", color = Color(0xFFFCA5A5), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
