package com.sucharu.sucharupro.ui.features.communication.campaign

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignStatus

@Composable
fun CampaignListScreen(
    viewModel: CampaignListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onSelectCampaign: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            CampaignTopBar(
                title = "Campaigns",
                onBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = CampaignAccent,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Campaign", tint = CampaignBg)
            }
        },
        containerColor = CampaignBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Status Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.filterStatus == null,
                        onClick = { viewModel.setFilterStatus(null) },
                        label = { Text("All", fontSize = 12.sp) }
                    )
                }
                items(CampaignStatus.entries) { status ->
                    FilterChip(
                        selected = state.filterStatus == status,
                        onClick = { viewModel.setFilterStatus(status) },
                        label = { Text(status.defaultLabel, fontSize = 12.sp) }
                    )
                }
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CampaignAccent)
                }
                return@Column
            }

            if (state.campaigns.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = null, tint = CampaignTextSecondary, modifier = Modifier.size(48.dp))
                        Text("No campaigns match the selected filters.", color = CampaignTextSecondary, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.campaigns) { campaign ->
                        CampaignCard(
                            campaign = campaign,
                            onClick = { onSelectCampaign(campaign.campaignId) }
                        )
                    }
                }
            }
        }
    }
}
