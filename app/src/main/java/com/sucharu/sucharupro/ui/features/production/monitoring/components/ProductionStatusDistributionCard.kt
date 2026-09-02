package com.sucharu.sucharupro.ui.features.production.monitoring.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.job.ProductionMonitoringSnapshot
import com.sucharu.sucharupro.ui.features.orders.components.DetailSectionCard
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Card displaying the complete breakdown of jobs by status.
 */
@Composable
fun ProductionStatusDistributionCard(
    snapshot: ProductionMonitoringSnapshot,
    modifier: Modifier = Modifier
) {
    DetailSectionCard(
        title = "Job Status Distribution",
        icon = Icons.Default.PieChart,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
        ) {
            StatusRow(status = ProductionJobStatus.DRAFT, count = snapshot.draftJobs)
            StatusRow(status = ProductionJobStatus.READY_FOR_PRODUCTION, count = snapshot.readyForProductionJobs)
            StatusRow(status = ProductionJobStatus.IN_PROGRESS, count = snapshot.inProgressJobs)
            StatusRow(status = ProductionJobStatus.ON_HOLD, count = snapshot.onHoldJobs)
            StatusRow(status = ProductionJobStatus.READY, count = snapshot.readyJobs)
            StatusRow(status = ProductionJobStatus.DELIVERED, count = snapshot.deliveredJobs)
            StatusRow(status = ProductionJobStatus.CANCELLED, count = snapshot.cancelledJobs)
        }
    }
}

@Composable
private fun StatusRow(
    status: ProductionJobStatus,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = status.defaultLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Surface(
            shape = MaterialTheme.shapes.small,
            color = when (status) {
                ProductionJobStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiaryContainer
                ProductionJobStatus.READY -> MaterialTheme.colorScheme.primaryContainer
                ProductionJobStatus.DELIVERED -> MaterialTheme.colorScheme.primaryContainer
                ProductionJobStatus.ON_HOLD -> MaterialTheme.colorScheme.errorContainer
                ProductionJobStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.secondaryContainer
            }
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}
