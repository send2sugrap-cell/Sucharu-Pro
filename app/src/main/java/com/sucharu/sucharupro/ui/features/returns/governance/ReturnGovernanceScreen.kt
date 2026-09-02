package com.sucharu.sucharupro.ui.features.returns.governance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.returns.ReturnExceptionStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.ui.features.returns.analytics.ReturnExceptionCard

/**
 * Governance monitoring and policy exception center screen (Module 11 Step 06).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnGovernanceScreen(
    projectId: String,
    viewModel: ReturnGovernanceViewModel,
    userRole: UserRole? = null,
    actorId: String = "CURRENT_USER",
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.loadExceptions(projectId = projectId, callerRole = userRole)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Return Governance Center") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.runInspection(actorId, userRole) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Run Governance Scan")
                    }
                    IconButton(onClick = { viewModel.loadExceptions(projectId, callerRole = userRole) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
        ) {
            // Filter chips by status
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.statusFilter == null,
                        onClick = { viewModel.selectStatusFilter(null) },
                        label = { Text("All") }
                    )
                }
                items(ReturnExceptionStatus.entries) { status ->
                    FilterChip(
                        selected = uiState.statusFilter == status,
                        onClick = { viewModel.selectStatusFilter(status) },
                        label = { Text(status.displayName) }
                    )
                }
            }

            if (uiState.isLoading || uiState.isActionInProgress) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.errorMessage ?: "Error loading exceptions",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else if (uiState.exceptions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No governance exceptions detected.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.exceptions, key = { it.exceptionId }) { exception ->
                        ReturnExceptionCard(
                            exception = exception,
                            onAcknowledge = { viewModel.acknowledgeException(exception.exceptionId, actorId, userRole) },
                            onResolve = { notes -> viewModel.resolveException(exception.exceptionId, actorId, notes, userRole) }
                        )
                    }
                }
            }
        }
    }
}
