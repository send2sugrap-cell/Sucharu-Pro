package com.sucharu.sucharupro.ui.features.production.monitoring.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentLate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.job.ProductionMonitoringSnapshot
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Responsive top metric summary grid displaying key production indicators.
 */
@Composable
fun ProductionMetricsSummarySection(
    snapshot: ProductionMonitoringSnapshot,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            MetricCard(
                title = "Active Jobs",
                count = snapshot.activeJobs,
                icon = Icons.Default.Work,
                iconColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "In Production",
                count = snapshot.inProgressJobs,
                icon = Icons.Default.PlayCircle,
                iconColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            MetricCard(
                title = "Ready to Start",
                count = snapshot.readyForProductionJobs,
                icon = Icons.Default.HourglassTop,
                iconColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "On Hold",
                count = snapshot.onHoldJobs,
                icon = Icons.Default.PauseCircle,
                iconColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            MetricCard(
                title = "Ready",
                count = snapshot.readyJobs,
                icon = Icons.Default.CheckCircle,
                iconColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Active Stages",
                count = snapshot.activeStageCount,
                icon = Icons.Default.Engineering,
                iconColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            MetricCard(
                title = "Unassigned Work",
                count = snapshot.unassignedPendingStageCount,
                icon = Icons.Default.AssignmentLate,
                iconColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Attention Queue",
                count = snapshot.attentionRequiredCount,
                icon = Icons.Default.NotificationsActive,
                iconColor = if (snapshot.attentionRequiredCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    count: Int,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
