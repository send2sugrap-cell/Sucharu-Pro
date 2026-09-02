package com.sucharu.sucharupro.ui.features.finance.periodclose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Scaffold
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
import com.sucharu.sucharupro.domain.model.finance.AccountingPeriodStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AccountingPeriodScreen(
    viewModel: AccountingPeriodViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChecklist: (String) -> Unit,
    onNavigateToSnapshot: (String) -> Unit,
    onNavigateToReopenRequests: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.listState.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

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
                Text(
                    text = "Accounting Periods & Period Lock",
                    color = Color(0xFFF8FAFC),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF38BDF8))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Active Period Overview
                    val active = state.activePeriod
                    if (active != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("CURRENT ACTIVE PERIOD", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text(active.periodName, color = Color(0xFFF8FAFC), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        }
                                        AccountingPeriodStatusBadge(status = active.status)
                                    }

                                    Text(
                                        text = "${dateFormat.format(Date(active.startDate))} — ${dateFormat.format(Date(active.endDate))}",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 13.sp
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onNavigateToChecklist(active.periodId) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Closing Review", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }

                                        if (active.status == AccountingPeriodStatus.CLOSED) {
                                            Button(
                                                onClick = { onNavigateToSnapshot(active.periodId) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("View Snapshot", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Period List
                    item {
                        Text(
                            text = "Fiscal Periods History",
                            color = Color(0xFFF8FAFC),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(state.periods) { period ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToChecklist(period.periodId) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(period.periodName, color = Color(0xFFF8FAFC), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${dateFormat.format(Date(period.startDate))} - ${dateFormat.format(Date(period.endDate))}",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                    if (period.closingReference != null) {
                                        Text("Snapshot: ${period.closingReference}", color = Color(0xFF64748B), fontSize = 10.sp)
                                    }
                                }
                                AccountingPeriodStatusBadge(status = period.status)
                            }
                        }
                    }
                }
            }
        }
    }
}
