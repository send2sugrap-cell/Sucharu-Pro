package com.sucharu.sucharupro.ui.features.communication.campaign

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CampaignDashboardScreen(
    viewModel: CampaignDashboardViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToList: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToAnnouncements: () -> Unit,
    onNavigateToBroadcasts: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onSelectCampaign: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize().background(CampaignBg)) {
        CampaignTopBar(
            title = "Campaigns & Broadcasts",
            onBack = onNavigateBack,
            actions = {
                IconButton(onClick = onNavigateToAnalytics) {
                    Icon(Icons.Default.Analytics, contentDescription = "Analytics", tint = CampaignTextPrimary)
                }
            }
        )

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CampaignAccent)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Metrics Row 1
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CampaignMetricCard(
                        title = "Total Campaigns",
                        value = "${state.summary.totalCampaigns}",
                        icon = Icons.Default.Campaign,
                        accentColor = CampaignAccent,
                        modifier = Modifier.weight(1f)
                    )
                    CampaignMetricCard(
                        title = "Active",
                        value = "${state.summary.activeCampaigns}",
                        icon = Icons.Default.CheckCircle,
                        accentColor = CampaignAccentGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Metrics Row 2
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CampaignMetricCard(
                        title = "Scheduled",
                        value = "${state.summary.scheduledCampaigns}",
                        icon = Icons.Default.Schedule,
                        accentColor = CampaignAccentAmber,
                        modifier = Modifier.weight(1f)
                    )
                    CampaignMetricCard(
                        title = "Completed",
                        value = "${state.summary.completedCampaigns}",
                        icon = Icons.Default.TaskAlt,
                        accentColor = CampaignAccentPurple,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Actions
            item {
                Text("Quick Actions", color = CampaignTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onNavigateToCreate,
                        colors = ButtonDefaults.buttonColors(containerColor = CampaignAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                        Spacer(Modifier.width(6.dp))
                        Text("New Campaign", color = Color.Black, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onNavigateToBroadcasts,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CampaignTextPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Podcasts, contentDescription = null, tint = CampaignAccentAmber)
                        Spacer(Modifier.width(6.dp))
                        Text("Broadcast")
                    }
                }
            }

            // Hub Navigation Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToList,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CampaignTextPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.List, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("All Campaigns")
                    }

                    OutlinedButton(
                        onClick = onNavigateToAnnouncements,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CampaignTextPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Announcement, contentDescription = null, tint = CampaignAccentGreen)
                        Spacer(Modifier.width(6.dp))
                        Text("Announcements")
                    }
                }
            }

            // Recent Campaigns Section
            item {
                Text("Recent Campaigns", color = CampaignTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            if (state.recentCampaigns.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CampaignSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No campaigns found. Create your first campaign above.", color = CampaignTextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(state.recentCampaigns) { campaign ->
                    CampaignCard(
                        campaign = campaign,
                        onClick = { onSelectCampaign(campaign.campaignId) }
                    )
                }
            }
        }
    }
}
