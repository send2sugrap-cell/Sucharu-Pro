package com.sucharu.sucharupro.ui.features.internalcommunication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InternalCommunicationDepartmentScreen(
    departmentId: String,
    viewModel: InternalCommunicationDepartmentViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(departmentId) {
        viewModel.loadDepartmentMessages(departmentId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Department Notices ($departmentId)", color = Color(0xFFF8FAFC), fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFF8FAFC))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadDepartmentMessages(departmentId) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF38BDF8))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A),
        modifier = modifier
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
            }
        } else if (state.departmentMessages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No department notices found for $departmentId.", color = Color(0xFF94A3B8), fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.departmentMessages, key = { it.communicationId }) { comm ->
                    InternalCommunicationCard(
                        communication = comm,
                        onClick = { onNavigateToDetails(comm.communicationId) }
                    )
                }
            }
        }
    }
}
