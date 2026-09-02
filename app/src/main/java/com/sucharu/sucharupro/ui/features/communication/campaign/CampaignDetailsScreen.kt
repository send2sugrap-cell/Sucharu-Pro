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
import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignStatus
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CampaignDetailsScreen(
    campaignId: String,
    viewModel: CampaignDetailsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(campaignId) { viewModel.load(campaignId = campaignId) }

    Column(modifier = Modifier.fillMaxSize().background(CampaignBg)) {
        CampaignTopBar(
            title = state.campaign?.campaignNo ?: "Campaign Details",
            onBack = onNavigateBack
        )

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CampaignAccent)
            }
            return@Column
        }

        val campaign = state.campaign
        if (campaign == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error ?: "Campaign not found.", color = CampaignAccentRed)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CampaignSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CampaignStatusBadge(campaign.status)
                            CampaignPriorityBadge(campaign.priority)
                        }

                        Text(campaign.title, color = CampaignTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                        if (campaign.description.isNotBlank()) {
                            Text(campaign.description, color = CampaignTextSecondary, fontSize = 14.sp)
                        }

                        HorizontalDivider(color = CampaignBorder)

                        Text("Content / Message:", color = CampaignTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(campaign.content, color = CampaignTextPrimary, fontSize = 14.sp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CampaignAudienceChip(campaign.audienceType)
                            CampaignTypeBadge(campaign.campaignType)
                        }
                    }
                }
            }

            // Lifecycle Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (campaign.status) {
                        CampaignStatus.DRAFT -> {
                            Button(
                                onClick = { viewModel.submitForApproval(campaign.projectId, campaign.campaignId) },
                                colors = ButtonDefaults.buttonColors(containerColor = CampaignAccent),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Submit for Approval", color = Color.Black)
                            }
                        }
                        CampaignStatus.PENDING_APPROVAL -> {
                            Button(
                                onClick = { viewModel.approve(campaign.projectId, campaign.campaignId) },
                                colors = ButtonDefaults.buttonColors(containerColor = CampaignAccentGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Approve")
                            }
                        }
                        CampaignStatus.APPROVED, CampaignStatus.SCHEDULED -> {
                            Button(
                                onClick = { viewModel.publish(campaign.projectId, campaign.campaignId) },
                                colors = ButtonDefaults.buttonColors(containerColor = CampaignAccentGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Publish Now")
                            }
                        }
                        else -> {}
                    }

                    if (!campaign.isTerminal) {
                        OutlinedButton(
                            onClick = { viewModel.cancel(campaign.projectId, campaign.campaignId) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CampaignAccentRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }

            // Delivery & Engagement Breakdown
            item {
                Text("Delivery & Engagement Metrics", color = CampaignTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CampaignMetricCard("Total", "${state.deliverySummary.totalRecipients}", Icons.Default.Group, CampaignAccent, Modifier.weight(1f))
                    CampaignMetricCard("Delivered", "${state.deliverySummary.delivered}", Icons.Default.DoneAll, CampaignAccentGreen, Modifier.weight(1f))
                    CampaignMetricCard("Read", "${state.deliverySummary.read}", Icons.Default.Visibility, CampaignAccentPurple, Modifier.weight(1f))
                }
            }

            // Activity Timeline Section
            item {
                Text("Audit & Activity Trail", color = CampaignTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            if (state.activityEvents.isEmpty()) {
                item {
                    Text("No activity logged yet.", color = CampaignTextSecondary, fontSize = 12.sp)
                }
            } else {
                items(state.activityEvents) { event ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = CampaignSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = CampaignAccent, modifier = Modifier.size(16.dp))
                            Column {
                                Text(event.summary, color = CampaignTextPrimary, fontSize = 13.sp)
                                val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(event.timestamp))
                                Text("$dateStr by ${event.actorUserId}", color = CampaignTextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
