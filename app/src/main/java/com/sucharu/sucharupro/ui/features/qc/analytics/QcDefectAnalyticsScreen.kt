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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.qc.analytics.QcDefectAnalytics
import com.sucharu.sucharupro.ui.components.AppCard

@Composable
fun QcDefectAnalyticsScreen(
    defects: List<QcDefectAnalytics>
) {
    if (defects.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No defect analytics available.", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(defects, key = { it.defectCategory.name }) { item ->
            DefectCategoryCard(item = item)
        }
    }
}

@Composable
fun DefectCategoryCard(item: QcDefectAnalytics) {
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
                    text = item.defectCategory.defaultLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${item.defectCount} defects (${String.format("%.1f", item.percentageOfTotalDefects)}%)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Affected Qty: ${item.affectedQuantity} units", style = MaterialTheme.typography.bodySmall)
                Text(text = "Cost: ${String.format("%.2f", item.totalQcCost)} BDT", style = MaterialTheme.typography.bodySmall)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Reworks: ${item.reworkCount}", style = MaterialTheme.typography.bodySmall)
                Text(text = "Re-QC Cycles: ${item.reQcCycleCount}", style = MaterialTheme.typography.bodySmall)
                Text(text = "Time: ${item.totalQcTimeMinutes}m", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
