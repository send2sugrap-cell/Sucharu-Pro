package com.sucharu.sucharupro.ui.features.finance.payment

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPaymentListScreen(
    viewModel: CustomerPaymentListViewModel,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    canCreatePayment: Boolean = true,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Payments & Receipts", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            if (canCreatePayment) {
                FloatingActionButton(
                    onClick = onNavigateToCreate,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Record Payment")
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = { Text("Search payment no, customer, ref...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Status filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedStatusFilter == null,
                    onClick = { viewModel.onStatusFilterSelected(null) },
                    label = { Text("All Status") }
                )
                CustomerPaymentStatus.entries.forEach { status ->
                    FilterChip(
                        selected = uiState.selectedStatusFilter == status,
                        onClick = { viewModel.onStatusFilterSelected(status) },
                        label = { Text(status.defaultLabel) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Method filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedMethodFilter == null,
                    onClick = { viewModel.onMethodFilterSelected(null) },
                    label = { Text("All Methods") }
                )
                CustomerPaymentMethod.entries.forEach { method ->
                    FilterChip(
                        selected = uiState.selectedMethodFilter == method,
                        onClick = { viewModel.onMethodFilterSelected(method) },
                        label = { Text(method.defaultLabel) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val filtered = uiState.payments.filter { payment ->
                    val matchesStatus = uiState.selectedStatusFilter == null || payment.status == uiState.selectedStatusFilter
                    val matchesMethod = uiState.selectedMethodFilter == null || payment.paymentMethod == uiState.selectedMethodFilter
                    val matchesQuery = uiState.searchQuery.isBlank() ||
                            payment.paymentNo.contains(uiState.searchQuery, ignoreCase = true) ||
                            payment.customerId.contains(uiState.searchQuery, ignoreCase = true) ||
                            (payment.paymentReference?.contains(uiState.searchQuery, ignoreCase = true) == true)
                    matchesStatus && matchesMethod && matchesQuery
                }

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No customer payments recorded.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.paymentId }) { payment ->
                            CustomerPaymentCard(
                                payment = payment,
                                onClick = { onNavigateToDetails(payment.paymentId) }
                            )
                        }
                    }
                }
            }
        }
    }
}
