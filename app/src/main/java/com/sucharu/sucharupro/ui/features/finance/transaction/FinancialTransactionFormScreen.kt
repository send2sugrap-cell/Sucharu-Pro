package com.sucharu.sucharupro.ui.features.finance.transaction

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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialTransactionFormScreen(
    viewModel: FinancialTransactionFormViewModel,
    onNavigateBack: () -> Unit,
    onSuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.successTransactionId) {
        uiState.successTransactionId?.let { onSuccess(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Financial Transaction", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Transaction Type Selection
            Text("Transaction Type", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinancialTransactionType.entries.forEach { type ->
                    FilterChip(
                        selected = uiState.transactionType == type,
                        onClick = { viewModel.onTransactionTypeChanged(type) },
                        label = { Text(type.defaultLabel) }
                    )
                }
            }

            // Entry Type (Debit / Credit)
            Text("Entry Type", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FinancialEntryType.entries.forEach { entryType ->
                    FilterChip(
                        selected = uiState.entryType == entryType,
                        onClick = { viewModel.onEntryTypeChanged(entryType) },
                        label = { Text(entryType.defaultLabel) }
                    )
                }
            }

            // Amount Input
            OutlinedTextField(
                value = uiState.amountInput,
                onValueChange = viewModel::onAmountChanged,
                label = { Text("Amount (BDT) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Reference Type Selection
            Text("Reference Type", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinancialReferenceType.entries.forEach { refType ->
                    FilterChip(
                        selected = uiState.referenceType == refType,
                        onClick = { viewModel.onReferenceTypeChanged(refType) },
                        label = { Text(refType.defaultLabel) }
                    )
                }
            }

            OutlinedTextField(
                value = uiState.referenceIdInput,
                onValueChange = viewModel::onReferenceIdChanged,
                label = { Text("Reference ID (e.g. Order ID, Invoice ID) *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.customerIdInput,
                onValueChange = viewModel::onCustomerIdChanged,
                label = { Text("Customer ID (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.vendorIdInput,
                onValueChange = viewModel::onVendorIdChanged,
                label = { Text("Vendor ID (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.descriptionInput,
                onValueChange = viewModel::onDescriptionChanged,
                label = { Text("Description *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.notesInput,
                onValueChange = viewModel::onNotesChanged,
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { viewModel.submitForm() },
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Create Draft Transaction", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
