package com.sucharu.sucharupro.ui.features.communication.campaign

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.communication.campaign.*

// ─── Theme Palette ───
val CampaignBg = Color(0xFF0F172A)
val CampaignSurface = Color(0xFF1E293B)
val CampaignAccent = Color(0xFF38BDF8)
val CampaignAccentGreen = Color(0xFF22C55E)
val CampaignAccentAmber = Color(0xFFF59E0B)
val CampaignAccentRed = Color(0xFFEF4444)
val CampaignAccentPurple = Color(0xFFA855F7)
val CampaignTextPrimary = Color(0xFFF1F5F9)
val CampaignTextSecondary = Color(0xFF94A3B8)
val CampaignBorder = Color(0xFF334155)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(title, color = CampaignTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CampaignTextPrimary)
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = CampaignSurface)
    )
}

@Composable
fun CampaignStatusBadge(status: CampaignStatus) {
    val (bg, fg) = when (status) {
        CampaignStatus.DRAFT -> Color(0xFF334155) to Color(0xFF94A3B8)
        CampaignStatus.PENDING_APPROVAL -> Color(0xFF78350F) to Color(0xFFFBBF24)
        CampaignStatus.APPROVED -> Color(0xFF064E3B) to Color(0xFF34D399)
        CampaignStatus.SCHEDULED -> Color(0xFF1E3A8A) to Color(0xFF60A5FA)
        CampaignStatus.PUBLISHED -> Color(0xFF065F46) to Color(0xFF10B981)
        CampaignStatus.COMPLETED -> Color(0xFF312E81) to Color(0xFFA5B4FC)
        CampaignStatus.REJECTED -> Color(0xFF7F1D1D) to Color(0xFFF87171)
        CampaignStatus.CANCELLED -> Color(0xFF4B5563) to Color(0xFF9CA3AF)
    }

    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(status.defaultLabel, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CampaignPriorityBadge(priority: CampaignPriority) {
    val (bg, fg) = when (priority) {
        CampaignPriority.LOW -> Color(0xFF1E293B) to Color(0xFF94A3B8)
        CampaignPriority.NORMAL -> Color(0xFF0C4A6E) to Color(0xFF38BDF8)
        CampaignPriority.HIGH -> Color(0xFF78350F) to Color(0xFFFBBF24)
        CampaignPriority.URGENT -> Color(0xFF7F1D1D) to Color(0xFFF87171)
    }

    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(priority.defaultLabel, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CampaignTypeBadge(type: CampaignType) {
    Box(
        modifier = Modifier
            .background(Color(0xFF334155), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(type.defaultLabel, color = CampaignTextSecondary, fontSize = 11.sp)
    }
}

@Composable
fun CampaignAudienceChip(audienceType: CampaignAudienceType) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(Icons.Default.Group, contentDescription = null, tint = CampaignAccent, modifier = Modifier.size(14.dp))
        Text(audienceType.defaultLabel, color = CampaignTextPrimary, fontSize = 12.sp)
    }
}

@Composable
fun CampaignMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CampaignSurface),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(title, color = CampaignTextSecondary, fontSize = 12.sp)
                Text(value, color = CampaignTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CampaignCard(
    campaign: Campaign,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CampaignSurface),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(campaign.campaignNo, color = CampaignTextSecondary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CampaignPriorityBadge(campaign.priority)
                    CampaignStatusBadge(campaign.status)
                }
            }

            Text(campaign.title, color = CampaignTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

            if (campaign.description.isNotBlank()) {
                Text(campaign.description, color = CampaignTextSecondary, fontSize = 13.sp, maxLines = 2)
            }

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
