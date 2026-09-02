package com.sucharu.sucharupro.ui.features.inventory.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryException
import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryExceptionStatus
import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryExceptionType

/**
 * Screen for managing inventory governance exceptions (Module 07 Step 10).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryGovernanceScreen(
    viewModel: InventoryGovernanceViewModel,
    onNavigateToDetails: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory Governance") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.runGovernanceCheck() }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Run Check")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
        ) {
            // Severity Filters
            Text(text = "Filter by Severity", style = MaterialTheme.typography.labelMedium)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedSeverity == null,
                        onClick = { viewModel.onSeverityFilterChanged(null) },
                        label = { Text("All") }
                    )
                }
                items(InventoryException.Severity.entries) { severity ->
                    FilterChip(
                        selected = uiState.selectedSeverity == severity,
                        onClick = { viewModel.onSeverityFilterChanged(severity) },
                        label = { Text(severity.name) }
                    )
                }
            }

            // Type Filters
            Text(text = "Filter by Type", style = MaterialTheme.typography.labelMedium)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedType == null,
                        onClick = { viewModel.onTypeFilterChanged(null) },
                        label = { Text("All") }
                    )
                }
                items(InventoryExceptionType.entries) { type ->
                    FilterChip(
                        selected = uiState.selectedType == type,
                        onClick = { viewModel.onTypeFilterChanged(type) },
                        label = { Text(type.name.replace("_", " ")) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.filteredExceptions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle, 
                            contentDescription = null, 
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No open governance issues found.")
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(uiState.filteredExceptions) { exception ->
                        ExceptionListItem(
                            exception = exception,
                            onAcknowledge = { viewModel.acknowledgeException(exception.exceptionId) },
                            onResolve = { viewModel.resolveException(exception.exceptionId) },
                            onClick = { onNavigateToDetails(exception.exceptionId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExceptionListItem(
    exception: InventoryException,
    onAcknowledge: () -> Unit,
    onResolve: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                InventorySeverityBadge(severity = exception.severity)
                Text(text = exception.detectedAt, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = exception.type.name.replace("_", " "),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Target: ${exception.targetType} ${exception.targetId}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (exception.status == InventoryExceptionStatus.OPEN) {
                    OutlinedButton(
                        onClick = onAcknowledge,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Acknowledge", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Button(
                    onClick = onResolve,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Resolve", fontSize = 12.sp)
                }
            }
        }
    }
}
