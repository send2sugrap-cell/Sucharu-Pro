package com.sucharu.sucharupro.ui.features.communication.vendor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.communication.vendor.VendorCommunicationStatus
import com.sucharu.sucharupro.domain.model.communication.vendor.VendorCommunicationType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorCommunicationCenterScreen(
    viewModel: VendorCommunicationListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToCompose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Vendor Communications Center", color = Color(0xFFF8FAFC), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("All Vendor & Supplier Messages", color = Color(0xFF38BDF8), fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFF8FAFC))
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = Color(0xFF38BDF8))
                    }
                    IconButton(onClick = { viewModel.loadCommunications() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF38BDF8))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCompose, containerColor = Color(0xFF38BDF8), contentColor = Color(0xFF0F172A)) {
                Icon(Icons.Default.Add, contentDescription = "Compose")
            }
        },
        containerColor = Color(0xFF0F172A),
        modifier = modifier
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::updateSearch,
                placeholder = { Text("Search communications…", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF38BDF8)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color(0xFFF8FAFC),
                    unfocusedTextColor = Color(0xFFF8FAFC),
                    cursorColor = Color(0xFF38BDF8)
                ),
                singleLine = true
            )

            // Active filters display
            if (state.filterType != null || state.filterStatus != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filters:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    state.filterType?.let { VendorCommunicationTypeChip(it) }
                    state.filterStatus?.let { VendorCommunicationStatusChip(it) }
                    TextButton(onClick = { viewModel.filterByType(null); viewModel.filterByStatus(null) }) {
                        Text("Clear", color = Color(0xFF38BDF8), fontSize = 12.sp)
                    }
                }
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF38BDF8))
                }
            } else {
                val filtered = state.communications.filter { comm ->
                    (state.searchQuery.isBlank() || comm.subject.contains(state.searchQuery, true) || comm.message.contains(state.searchQuery, true)) &&
                    (state.filterType == null || comm.communicationType == state.filterType) &&
                    (state.filterStatus == null || comm.status == state.filterStatus)
                }

                if (filtered.isEmpty()) {
                    EmptyVendorCommunicationState("No communications found.", modifier = Modifier.fillMaxSize())
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(filtered, key = { it.communicationId }) { comm ->
                            VendorCommunicationCard(communication = comm, onClick = { onNavigateToDetails(comm.communicationId) })
                        }
                    }
                }
            }
        }
    }
}
