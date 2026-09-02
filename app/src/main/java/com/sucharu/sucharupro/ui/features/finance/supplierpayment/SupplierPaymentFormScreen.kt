package com.sucharu.sucharupro.ui.features.finance.supplierpayment

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentMethod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierPaymentFormScreen(
    viewModel: SupplierPaymentFormViewModel,
    onNavigateBack: () -> Unit,
    onPaymentCreated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.createdPayment) {
        state.createdPayment?.let {
            onPaymentCreated(it.paymentId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Supplier Payment") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            state.errorMessage?.let { error ->
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }

            OutlinedTextField(
                value = state.vendorId,
                onValueChange = viewModel::onVendorIdChanged,
                label = { Text("Vendor ID *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.payableId,
                onValueChange = viewModel::onPayableIdChanged,
                label = { Text("Payable ID *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            state.selectedPayable?.let { payable ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Payable #${payable.payableNo}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = "Total Liability: ${payable.originalAmount.formatted()} ${payable.currency}", fontSize = 12.sp)
                        Text(
                            text = "Current Outstanding Due: ${payable.outstandingAmount.formatted()} ${payable.currency}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }

            Text(text = "Disbursement Method", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SupplierPaymentMethod.values().forEach { method ->
                    FilterChip(
                        selected = state.paymentMethod == method,
                        onClick = { viewModel.onPaymentMethodChanged(method) },
                        label = { Text(method.defaultLabel) }
                    )
                }
            }

            if (state.paymentMethod.requiresReference) {
                OutlinedTextField(
                    value = state.paymentReference,
                    onValueChange = viewModel::onPaymentReferenceChanged,
                    label = { Text("Payment Reference (Cheque No, EFT Trx ID, etc.) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = state.amountText,
                onValueChange = viewModel::onAmountChanged,
                label = { Text("Payment Amount (BDT) *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChanged,
                label = { Text("Notes (Optional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = viewModel::submit,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Disburse Supplier Payment")
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
