package com.sucharu.sucharupro.ui.features.qc.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.qc.analytics.QcJobAnalytics
import com.sucharu.sucharupro.ui.components.AppCard

@Composable
fun QcJobAnalyticsScreen(
    jobs: List<QcJobAnalytics>
) {
    if (jobs.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No job analytical records found.", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(jobs, key = { it.productionJobId }) { job ->
            JobAnalyticsCard(job = job)
        }
    }
}

@Composable
fun JobAnalyticsCard(job: QcJobAnalytics) {
    val scoreColor = when {
        job.efficiencyScore >= 80.0 -> Color(0xFF2E7D32)
        job.efficiencyScore >= 50.0 -> Color(0xFFF57F17)
        else -> Color(0xFFC62828)
    }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Job: ${job.productionJobId}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Project: ${job.projectId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format("%.0f", job.efficiencyScore)}/100",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                    Text(
                        text = "Efficiency Score",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Cost: ${String.format("%.2f", job.totalQcCost)} BDT", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Variance: ${if (job.costVariance > 0) "+" else ""}${String.format("%.2f", job.costVariance)} BDT",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (job.costVariance > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Time: ${job.totalQcTimeMinutes} mins", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Variance: ${if (job.timeVarianceMinutes > 0) "+" else ""}${job.timeVarianceMinutes} mins",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (job.timeVarianceMinutes > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "Defects: ${job.defectCount}", style = MaterialTheme.typography.bodySmall)
                Text(text = "Reworks: ${job.reworkCount}", style = MaterialTheme.typography.bodySmall)
                Text(text = "Re-QC: ${job.reQcCycleCount}", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = if (job.firstPassQc) "1st Pass: YES" else "1st Pass: NO",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (job.firstPassQc) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
