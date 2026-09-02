package com.sucharu.sucharupro.ui.features.finance.reporting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.finance.FinancialReportFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportFilterBottomSheet(
    filter: FinancialReportFilter,
    onApplyFilter: (FinancialReportFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var customerId by remember { mutableStateOf(filter.customerId ?: "") }
    var vendorId by remember { mutableStateOf(filter.vendorId ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Report Filters",
                color = Color(0xFFF8FAFC),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = customerId,
                onValueChange = { customerId = it },
                label = { Text("Customer ID Filter (Optional)", color = Color(0xFF94A3B8)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFFF8FAFC),
                    unfocusedTextColor = Color(0xFFF8FAFC),
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = vendorId,
                onValueChange = { vendorId = it },
                label = { Text("Vendor ID Filter (Optional)", color = Color(0xFF94A3B8)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFFF8FAFC),
                    unfocusedTextColor = Color(0xFFF8FAFC),
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onApplyFilter(filter.copy(customerId = null, vendorId = null))
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Reset", color = Color(0xFF94A3B8))
                }

                Button(
                    onClick = {
                        val newFilter = filter.copy(
                            customerId = customerId.trim().ifBlank { null },
                            vendorId = vendorId.trim().ifBlank { null }
                        )
                        onApplyFilter(newFilter)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Apply Filter", color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
