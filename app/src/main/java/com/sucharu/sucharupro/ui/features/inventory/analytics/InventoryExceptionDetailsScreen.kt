package com.sucharu.sucharupro.ui.features.inventory.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryException

/**
 * Screen displaying granular details for a specific inventory exception (Module 07 Step 10).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryExceptionDetailsScreen(
    exceptionId: String,
    viewModel: InventoryGovernanceViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val exception = uiState.exceptions.find { it.exceptionId == exceptionId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exception Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (exception == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("Exception not found.")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InventorySeverityBadge(severity = exception.severity)
                        Text(
                            text = exception.status.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = exception.type.name.replace("_", " "),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Detected at: ${exception.detectedAt}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            DetailSection(label = "Target Details") {
                Text(text = "Type: ${exception.targetType}", fontSize = 14.sp)
                Text(text = "ID: ${exception.targetId}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            exception.details?.let {
                DetailSection(label = "Description & Logs") {
                    Text(text = it, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.acknowledgeException(exceptionId) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Acknowledge")
                }
                Button(
                    onClick = { viewModel.resolveException(exceptionId) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Resolve")
                }
            }
        }
    }
}

@Composable
private fun DetailSection(
    label: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                content()
            }
        }
    }
}
