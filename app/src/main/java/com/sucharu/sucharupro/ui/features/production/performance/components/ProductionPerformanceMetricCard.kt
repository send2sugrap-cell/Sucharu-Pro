package com.sucharu.sucharupro.ui.features.production.performance.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.job.ProductionPerformanceMetrics

/**
 * KPI Overview Card displaying high-level statistical production metrics.
 */
@Composable
fun ProductionPerformanceMetricCard(
    metrics: ProductionPerformanceMetrics,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Completion Rate & Total Jobs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricTile(
                icon = Icons.Default.Speed,
                label = "Completion Rate",
                value = "${(metrics.completionRate * 100).toInt()}%",
                subtitle = "${metrics.completedJobs}/${metrics.totalHistoricalJobs} Jobs Finished",
                valueColor = Color(0xFF2E7D32),
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                icon = Icons.Default.AssignmentTurnedIn,
                label = "Total Jobs",
                value = "${metrics.totalHistoricalJobs}",
                subtitle = "${metrics.currentlyActiveJobs} Active / ${metrics.deliveredJobs} Delivered",
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Average Stage Duration & Recorded Output
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricTile(
                icon = Icons.Default.Schedule,
                label = "Avg Stage Duration",
                value = metrics.formattedAverageStageDuration,
                subtitle = "Longest: ${metrics.formattedLongestStageDuration}",
                valueColor = Color(0xFFE65100),
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                icon = Icons.Default.Inventory2,
                label = "Output Rate",
                value = "${(metrics.outputCompletionRate * 100).toInt()}%",
                subtitle = "${metrics.recordedOutput}/${metrics.plannedQuantity} Units",
                valueColor = Color(0xFF1565C0),
                modifier = Modifier.weight(1f)
            )
        }

        // Row 3: Delivered vs Cancelled
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricTile(
                icon = Icons.Default.LocalShipping,
                label = "Delivered Jobs",
                value = "${metrics.deliveredJobs}",
                subtitle = "Successfully Dispatched",
                valueColor = Color(0xFF2E7D32),
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                icon = Icons.Default.Cancel,
                label = "Cancelled Jobs",
                value = "${metrics.cancelledJobs}",
                subtitle = "Terminated Early",
                valueColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    subtitle: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = valueColor
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
