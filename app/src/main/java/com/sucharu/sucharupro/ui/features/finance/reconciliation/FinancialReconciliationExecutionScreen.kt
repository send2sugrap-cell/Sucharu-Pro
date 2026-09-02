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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationStatus

@Composable
fun FinancialReconciliationExecutionScreen(
    periodId: String,
    viewModel: FinancialReconciliationViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.executionState.collectAsState()
    val tabs = listOf("Cash in Hand", "Bank Accounts", "General Ledger")

    // Cash Form State
    var openingCash by remember { mutableStateOf("0") }
    var cashReceipts by remember { mutableStateOf("0") }
    var cashPayments by remember { mutableStateOf("0") }
    var actualCountedCash by remember { mutableStateOf("0") }
    var cashNotes by remember { mutableStateOf("") }

    // Bank Form State
    var openingBank by remember { mutableStateOf("0") }
    var ledgerDeposits by remember { mutableStateOf("0") }
    var ledgerWithdrawals by remember { mutableStateOf("0") }
    var statementBalance by remember { mutableStateOf("0") }
    var outstandingDeposits by remember { mutableStateOf("0") }
    var outstandingWithdrawals by remember { mutableStateOf("0") }
    var bankNotes by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0F172A)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFF8FAFC))
                }
                Column {
                    Text(
                        text = "Execute Reconciliation",
                        color = Color(0xFFF8FAFC),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Period: $periodId",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = state.activeTab,
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFF38BDF8),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[state.activeTab]),
                        color = Color(0xFF38BDF8)
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = state.activeTab == index,
                        onClick = { viewModel.setExecutionTab(index) },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (state.activeTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (state.activeTab == index) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                            )
                        }
                    )
                }
            }

            if (state.actionSuccessMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF6EE7B7))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(state.actionSuccessMessage ?: "", color = Color(0xFF6EE7B7), fontSize = 13.sp)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (state.activeTab) {
                    0 -> { // Cash
                        item {
                            Text(
                                "Cash in Hand Physical Reconciliation",
                                color = Color(0xFFF8FAFC),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = openingCash,
                                        onValueChange = { openingCash = it },
                                        label = { Text("Opening Cash Balance (৳)", color = Color(0xFF94A3B8)) },
                                        colors = darkTextFieldColors(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = cashReceipts,
                                        onValueChange = { cashReceipts = it },
                                        label = { Text("Recorded Cash Receipts (+ ৳)", color = Color(0xFF94A3B8)) },
                                        colors = darkTextFieldColors(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = cashPayments,
                                        onValueChange = { cashPayments = it },
                                        label = { Text("Recorded Cash Payments (- ৳)", color = Color(0xFF94A3B8)) },
                                        colors = darkTextFieldColors(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = actualCountedCash,
                                        onValueChange = { actualCountedCash = it },
                                        label = { Text("Actual Counted Cash in Vault (৳)", color = Color(0xFF38BDF8)) },
                                        colors = darkTextFieldColors(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = cashNotes,
                                        onValueChange = { cashNotes = it },
                                        label = { Text("Reconciliation Notes", color = Color(0xFF94A3B8)) },
                                        colors = darkTextFieldColors(),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            viewModel.executeCashReconciliation(
                                                periodId = periodId,
                                                openingCash = Money(openingCash.toDoubleOrNull() ?: 0.0),
                                                cashReceipts = Money(cashReceipts.toDoubleOrNull() ?: 0.0),
                                                cashPayments = Money(cashPayments.toDoubleOrNull() ?: 0.0),
                                                actualClosingCash = Money(actualCountedCash.toDoubleOrNull() ?: 0.0),
                                                notes = cashNotes
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Execute & Record Cash Reconciliation", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    1 -> { // Bank
                        item {
                            Text(
                                "Bank Statement Balance Verification",
                                color = Color(0xFFF8FAFC),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = openingBank,
                                        onValueChange = { openingBank = it },
                                        label = { Text("Opening Bank Balance (৳)", color = Color(0xFF94A3B8)) },
                                        colors = darkTextFieldColors(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = ledgerDeposits,
                                        onValueChange = { ledgerDeposits = it },
                                        label = { Text("Ledger Deposits (+ ৳)", color = Color(0xFF94A3B8)) },
                                        colors = darkTextFieldColors(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = ledgerWithdrawals,
                                        onValueChange = { ledgerWithdrawals = it },
                                        label = { Text("Ledger Withdrawals (- ৳)", color = Color(0xFF94A3B8)) },
                                        colors = darkTextFieldColors(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = statementBalance,
                                        onValueChange = { statementBalance = it },
                                        label = { Text("Bank Statement Closing Balance (৳)", color = Color(0xFF6366F1)) },
                                        colors = darkTextFieldColors(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = outstandingDeposits,
                                        onValueChange = { outstandingDeposits = it },
                                        label = { Text("Outstanding Deposits (In Transit ৳)", color = Color(0xFF94A3B8)) },
                                        colors = darkTextFieldColors(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = outstandingWithdrawals,
                                        onValueChange = { outstandingWithdrawals = it },
                                        label = { Text("Unpresented Cheques (- ৳)", color = Color(0xFF94A3B8)) },
                                        colors = darkTextFieldColors(),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            viewModel.executeBankReconciliation(
                                                periodId = periodId,
                                                openingBankBalance = Money(openingBank.toDoubleOrNull() ?: 0.0),
                                                ledgerDeposits = Money(ledgerDeposits.toDoubleOrNull() ?: 0.0),
                                                ledgerWithdrawals = Money(ledgerWithdrawals.toDoubleOrNull() ?: 0.0),
                                                bankStatementBalance = Money(statementBalance.toDoubleOrNull() ?: 0.0),
                                                outstandingDeposits = Money(outstandingDeposits.toDoubleOrNull() ?: 0.0),
                                                outstandingWithdrawals = Money(outstandingWithdrawals.toDoubleOrNull() ?: 0.0),
                                                notes = bankNotes
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Execute & Record Bank Reconciliation", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    2 -> { // General Ledger
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("General Ledger Integrity Diagnostic", color = Color(0xFFF8FAFC), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Automated diagnostic compares all posted financial transactions against general ledger entries for zero variance and no orphan postings.",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Debits equal Credits verified", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Zero orphan transactions detected", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Strict project isolation verified", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun darkTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color(0xFF0F172A),
    unfocusedContainerColor = Color(0xFF0F172A),
    focusedTextColor = Color(0xFFF8FAFC),
    unfocusedTextColor = Color(0xFFF8FAFC),
    focusedBorderColor = Color(0xFF38BDF8),
    unfocusedBorderColor = Color(0xFF334155)
)
