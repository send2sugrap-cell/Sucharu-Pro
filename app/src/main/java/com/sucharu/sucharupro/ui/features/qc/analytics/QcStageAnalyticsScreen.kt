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
import com.sucharu.sucharupro.domain.model.qc.analytics.QcStageAnalytics
import com.sucharu.sucharupro.ui.components.AppCard

@Composable
fun QcStageAnalyticsScreen(
    stages: List<QcStageAnalytics>
) {
    if (stages.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No stage analytics available.", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(stages, key = { it.productionStage.name }) { stage ->
            StageAnalyticsCard(stage = stage)
        }
    }
}

@Composable
fun StageAnalyticsCard(stage: QcStageAnalytics) {
    val defectRateColor = if (stage.defectRate > 20.0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)

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
                Text(
                    text = "${stage.productionStage.displayOrder}. ${stage.productionStage.defaultLabel}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Defect Rate: ${String.format("%.1f", stage.defectRate)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = defectRateColor
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Defects: ${stage.defectCount}", style = MaterialTheme.typography.bodySmall)
                Text(text = "Reworks: ${stage.reworkCount}", style = MaterialTheme.typography.bodySmall)
                Text(text = "Re-QC: ${stage.reQcCount}", style = MaterialTheme.typography.bodySmall)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Cost: ${String.format("%.2f", stage.totalQcCost)} BDT", style = MaterialTheme.typography.bodySmall)
                Text(text = "Time: ${stage.totalQcTimeMinutes}m", style = MaterialTheme.typography.bodySmall)
                Text(text = "Rework Rate: ${String.format("%.1f", stage.reworkRate)}%", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
