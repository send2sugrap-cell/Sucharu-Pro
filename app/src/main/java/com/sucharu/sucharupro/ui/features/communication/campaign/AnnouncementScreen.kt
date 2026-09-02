package com.sucharu.sucharupro.ui.features.communication.campaign

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Announcement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.communication.campaign.Announcement

@Composable
fun AnnouncementScreen(
    viewModel: AnnouncementViewModel,
    onNavigateBack: () -> Unit,
    onSelectAnnouncement: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize().background(CampaignBg)) {
        CampaignTopBar(
            title = "Announcements",
            onBack = onNavigateBack
        )

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CampaignAccent)
            }
            return@Column
        }

        if (state.announcements.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Announcement, contentDescription = null, tint = CampaignTextSecondary, modifier = Modifier.size(48.dp))
                    Text("No announcements published.", color = CampaignTextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.announcements) { ann ->
                    AnnouncementCard(
                        announcement = ann,
                        onClick = { onSelectAnnouncement(ann.announcementId) }
                    )
                }
            }
        }
    }
}

@Composable
fun AnnouncementCard(
    announcement: Announcement,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CampaignSurface),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(announcement.announcementNo, color = CampaignTextSecondary, fontSize = 12.sp)
                CampaignPriorityBadge(announcement.priority)
            }

            Text(announcement.title, color = CampaignTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(announcement.content, color = CampaignTextSecondary, fontSize = 13.sp, maxLines = 3)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CampaignAudienceChip(announcement.audienceType)
                CampaignStatusBadge(announcement.status)
            }
        }
    }
}
