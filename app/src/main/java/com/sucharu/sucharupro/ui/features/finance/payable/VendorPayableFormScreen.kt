package com.sucharu.sucharupro.ui.features.finance.payable

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPayableFormScreen(
    viewModel: VendorPayableFormViewModel,
    onNavigateBack: () -> Unit,
    onPayableCreated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.createdPayable) {
        state.createdPayable?.let {
            onPayableCreated(it.payableId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Supplier Payable / Bill") },
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

            Text(text = "Reference Type", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    FinancialReferenceType.PURCHASE,
                    FinancialReferenceType.PURCHASE_ORDER,
                    FinancialReferenceType.SUPPLIER_INVOICE,
                    FinancialReferenceType.VENDOR_BILL,
                    FinancialReferenceType.STOCK_RECEIPT,
                    FinancialReferenceType.MANUAL
                ).forEach { type ->
                    FilterChip(
                        selected = state.referenceType == type,
                        onClick = { viewModel.onReferenceTypeChanged(type) },
                        label = { Text(type.name) }
                    )
                }
            }

            OutlinedTextField(
                value = state.referenceId,
                onValueChange = viewModel::onReferenceIdChanged,
                label = { Text("Reference ID (e.g. PO-001, GRN-001) *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.supplierInvoiceNo,
                onValueChange = viewModel::onSupplierInvoiceNoChanged,
                label = { Text("Supplier Bill / Invoice No (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.amountText,
                onValueChange = viewModel::onAmountChanged,
                label = { Text("Payable Amount (BDT) *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChanged,
                label = { Text("Description *") },
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
                    Text("Create Payable Obligation")
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
