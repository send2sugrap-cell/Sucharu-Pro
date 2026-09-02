package com.sucharu.sucharupro.ui.features.customercommunication

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.communication.customer.CustomerCommunicationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerCommunicationCenterScreen(
    viewModel: CustomerCommunicationCenterViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Customer Messages & Alerts", color = Color(0xFFF8FAFC), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("${state.unreadCount} unread notices", color = Color(0xFF38BDF8), fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFF8FAFC))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A),
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search updates, orders, receipts...", color = Color(0xFF64748B), fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF94A3B8)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color(0xFFF8FAFC),
                    unfocusedTextColor = Color(0xFFF8FAFC)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CustomerCommunicationFilter.values().forEach { filter ->
                    FilterChip(
                        selected = state.selectedFilter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filter.defaultLabel, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF38BDF8).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF38BDF8),
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = state.selectedFilter == filter,
                            borderColor = Color(0xFF334155),
                            selectedBorderColor = Color(0xFF38BDF8)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val filteredList = state.communications.filter { comm ->
                val matchesFilter = when (state.selectedFilter) {
                    CustomerCommunicationFilter.ALL -> true
                    CustomerCommunicationFilter.UNREAD -> !comm.isRead && comm.status != CustomerCommunicationStatus.CANCELLED
                    CustomerCommunicationFilter.ORDERS -> comm.communicationType.name.startsWith("ORDER")
                    CustomerCommunicationFilter.PRODUCTION -> comm.communicationType.name.startsWith("PRODUCTION") || comm.communicationType.name.startsWith("DESIGN")
                    CustomerCommunicationFilter.DELIVERY -> comm.communicationType.name.startsWith("DELIVERY")
                    CustomerCommunicationFilter.FINANCE -> comm.communicationType.name.startsWith("PAYMENT")
                    CustomerCommunicationFilter.ANNOUNCEMENTS -> comm.communicationType.name.contains("ANNOUNCEMENT") || comm.communicationType.name.contains("NOTICE")
                    CustomerCommunicationFilter.OFFERS -> comm.communicationType.name.contains("OFFER") || comm.communicationType.name.contains("PROMOTION")
                }
                val matchesSearch = state.searchQuery.isBlank() ||
                        comm.title.contains(state.searchQuery, ignoreCase = true) ||
                        comm.message.contains(state.searchQuery, ignoreCase = true) ||
                        comm.communicationNo.contains(state.searchQuery, ignoreCase = true)
                matchesFilter && matchesSearch
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No communications found.", color = Color(0xFF64748B), fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredList, key = { it.communicationId }) { item ->
                        CustomerCommunicationItemCard(
                            communication = item,
                            onClick = { onNavigateToDetails(item.communicationId) },
                            onMarkReadClick = if (!item.isRead) { { viewModel.markAsRead(item.communicationId) } } else null
                        )
                    }
                }
            }
        }
    }
}
