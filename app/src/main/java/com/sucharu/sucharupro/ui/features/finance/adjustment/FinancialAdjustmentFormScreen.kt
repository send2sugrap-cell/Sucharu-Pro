package com.sucharu.sucharupro.ui.features.finance.adjustment

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
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentDirection
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialAdjustmentFormScreen(
    viewModel: FinancialAdjustmentFormViewModel,
    onNavigateBack: () -> Unit,
    onAdjustmentCreated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.createdAdjustment) {
        state.createdAdjustment?.let {
            onAdjustmentCreated(it.adjustmentId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Financial Adjustment") },
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

            Text(text = "Adjustment Type *", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinancialAdjustmentType.values().forEach { type ->
                    FilterChip(
                        selected = state.adjustmentType == type,
                        onClick = { viewModel.onTypeChanged(type) },
                        label = { Text(type.defaultLabel) }
                    )
                }
            }

            Text(text = "Direction *", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinancialAdjustmentDirection.values().forEach { dir ->
                    FilterChip(
                        selected = state.direction == dir,
                        onClick = { viewModel.onDirectionChanged(dir) },
                        label = { Text(dir.defaultLabel) }
                    )
                }
            }

            OutlinedTextField(
                value = state.amountText,
                onValueChange = viewModel::onAmountChanged,
                label = { Text("Amount (BDT) *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (state.adjustmentType.isCustomerFacing) {
                OutlinedTextField(
                    value = state.customerId,
                    onValueChange = viewModel::onCustomerIdChanged,
                    label = { Text("Customer ID *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.relatedReceivableId,
                    onValueChange = viewModel::onRelatedReceivableChanged,
                    label = { Text("Related Receivable ID (Optional - Auto settles)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (state.adjustmentType.isVendorFacing) {
                OutlinedTextField(
                    value = state.vendorId,
                    onValueChange = viewModel::onVendorIdChanged,
                    label = { Text("Vendor ID *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.relatedPayableId,
                    onValueChange = viewModel::onRelatedPayableChanged,
                    label = { Text("Related Payable ID (Optional - Auto settles)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = state.referenceId,
                onValueChange = viewModel::onReferenceIdChanged,
                label = { Text("Reference ID (e.g. Invoice #, Bill #, Order #) *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.reasonCode,
                onValueChange = viewModel::onReasonCodeChanged,
                label = { Text("Reason Code *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.reason,
                onValueChange = viewModel::onReasonChanged,
                label = { Text("Reason (e.g. Goods Return, Discount) *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChanged,
                label = { Text("Description / Details *") },
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
                    Text("Save Adjustment")
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
