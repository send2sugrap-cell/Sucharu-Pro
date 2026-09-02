package com.sucharu.sucharupro.ui.features.communication.campaign

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CampaignAnalyticsScreen(
    viewModel: CampaignAnalyticsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize().background(CampaignBg)) {
        CampaignTopBar(
            title = "Communication Analytics",
            onBack = onNavigateBack
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
            item {
                Text("Performance Overview", color = CampaignTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CampaignMetricCard(
                        title = "Delivery Rate",
                        value = "${String.format("%.1f", state.engagementSummary.deliveryRate)}%",
                        icon = Icons.Default.DoneAll,
                        accentColor = CampaignAccentGreen,
                        modifier = Modifier.weight(1f)
                    )
                    CampaignMetricCard(
                        title = "Read Rate",
                        value = "${String.format("%.1f", state.engagementSummary.readRate)}%",
                        icon = Icons.Default.Visibility,
                        accentColor = CampaignAccentPurple,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CampaignMetricCard(
                        title = "Ack Rate",
                        value = "${String.format("%.1f", state.engagementSummary.acknowledgementRate)}%",
                        icon = Icons.Default.TaskAlt,
                        accentColor = CampaignAccent,
                        modifier = Modifier.weight(1f)
                    )
                    CampaignMetricCard(
                        title = "Failure Rate",
                        value = "${String.format("%.1f", state.engagementSummary.failureRate)}%",
                        icon = Icons.Default.ErrorOutline,
                        accentColor = CampaignAccentRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CampaignSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Campaign Lifecycle Totals", color = CampaignTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Initiated", color = CampaignTextSecondary, fontSize = 13.sp)
                            Text("${state.summary.totalCampaigns}", color = CampaignTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Active / Running", color = CampaignTextSecondary, fontSize = 13.sp)
                            Text("${state.summary.activeCampaigns}", color = CampaignAccentGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Scheduled", color = CampaignTextSecondary, fontSize = 13.sp)
                            Text("${state.summary.scheduledCampaigns}", color = CampaignAccentAmber, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Completed", color = CampaignTextSecondary, fontSize = 13.sp)
                            Text("${state.summary.completedCampaigns}", color = CampaignAccentPurple, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cancelled", color = CampaignTextSecondary, fontSize = 13.sp)
                            Text("${state.summary.cancelledCampaigns}", color = CampaignTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
