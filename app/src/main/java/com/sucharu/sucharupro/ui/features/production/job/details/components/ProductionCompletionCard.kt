package com.sucharu.sucharupro.ui.features.production.job.details.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.job.ProductionCompletionChecklist
import com.sucharu.sucharupro.domain.model.job.ProductionCompletionChecklistItem
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * UI Card rendering the Production Completion Readiness Checklist and Action Trigger.
 */
@Composable
fun ProductionCompletionCard(
    job: ProductionJob,
    checklist: ProductionCompletionChecklist?,
    isActionInProgress: Boolean,
    onConfirmCompletionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = null,
                        tint = if (job.status == ProductionJobStatus.READY) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "উৎপাদন সমাপ্তি ও প্রস্তুতি গেট",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Production Completion & Readiness Gate",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val statusBadgeText = when (job.status) {
                    ProductionJobStatus.READY -> "উৎপাদন সম্পন্ন"
                    ProductionJobStatus.DELIVERED -> "ডেলিভারি সম্পন্ন"
                    ProductionJobStatus.CANCELLED -> "বাতিল"
                    ProductionJobStatus.ON_HOLD -> "স্থগিত"
                    else -> if (checklist?.isEligible == true) "প্রস্তুত" else "চলমান"
                }
                val badgeBg = when (job.status) {
                    ProductionJobStatus.READY, ProductionJobStatus.DELIVERED -> Color(0xFFE8F5E9)
                    ProductionJobStatus.CANCELLED -> Color(0xFFFFEBEE)
                    ProductionJobStatus.ON_HOLD -> Color(0xFFFFF3E0)
                    else -> if (checklist?.isEligible == true) Color(0xFFE3F2FD) else Color(0xFFF5F5F5)
                }
                val badgeTextColor = when (job.status) {
                    ProductionJobStatus.READY, ProductionJobStatus.DELIVERED -> Color(0xFF2E7D32)
                    ProductionJobStatus.CANCELLED -> Color(0xFFC62828)
                    ProductionJobStatus.ON_HOLD -> Color(0xFFEF6C00)
                    else -> if (checklist?.isEligible == true) Color(0xFF1565C0) else Color(0xFF757575)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(badgeBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusBadgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Checklist Items
            if (checklist != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    checklist.items.forEach { item ->
                        ChecklistRow(item = item)
                    }
                }

                // Over-production Notice
                if (checklist.isOverProduced) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFF8E1))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFF57F17),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "লক্ষ্যমাত্রার চেয়ে +${checklist.overProductionQuantity} ${job.unit} অতিরিক্ত উৎপাদন রেকর্ড হয়েছে।",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE65100),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Action Button
                if (job.status == ProductionJobStatus.IN_PROGRESS || job.status == ProductionJobStatus.READY_FOR_PRODUCTION) {
                    Spacer(modifier = Modifier.height(4.dp))
                    AppButton(
                        text = "উৎপাদন সম্পন্ন নিশ্চিত করুন",
                        onClick = onConfirmCompletionClick,
                        enabled = checklist.isEligible && !isActionInProgress,
                        isLoading = isActionInProgress,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ChecklistRow(item: ProductionCompletionChecklistItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val icon = if (item.isPassed) Icons.Default.CheckCircle else Icons.Default.Close
        val iconTint = if (item.isPassed) Color(0xFF2E7D32) else Color(0xFFD32F2F)

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = item.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
