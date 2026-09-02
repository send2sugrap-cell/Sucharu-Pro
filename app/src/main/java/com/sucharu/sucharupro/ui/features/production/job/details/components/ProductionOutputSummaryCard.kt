package com.sucharu.sucharupro.ui.features.production.job.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.job.ProductionItemOutputReconciliation
import com.sucharu.sucharupro.domain.model.job.ProductionOutputReconciliation
import com.sucharu.sucharupro.ui.features.orders.components.DetailSectionCard
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Card displaying comprehensive production output quantity reconciliation for a job card.
 */
@Composable
fun ProductionOutputSummaryCard(
    reconciliation: ProductionOutputReconciliation,
    modifier: Modifier = Modifier
) {
    DetailSectionCard(
        title = "উৎপাদন আউটপুট ও পরিমাণ সামঞ্জস্য (Output Reconciliation)",
        icon = Icons.Default.Assessment,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            // Overall Reconciliation Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                MetricBox(
                    label = "পরিকল্পিত (Planned)",
                    value = "${reconciliation.plannedQuantity} ${reconciliation.unit}",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    label = "রেকর্ডকৃত (Recorded)",
                    value = "${reconciliation.recordedQuantity} ${reconciliation.unit}",
                    containerColor = if (reconciliation.isOverProduced) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    contentColor = if (reconciliation.isOverProduced) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                if (reconciliation.isOverProduced) {
                    MetricBox(
                        label = "অতিরিক্ত (Over)",
                        value = "+${reconciliation.overProductionQuantity} ${reconciliation.unit}",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    MetricBox(
                        label = "অবশিষ্ট (Remaining)",
                        value = "${reconciliation.remainingQuantity} ${reconciliation.unit}",
                        containerColor = if (reconciliation.remainingQuantity == 0) {
                            Color(0xFFE8F5E9)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (reconciliation.remainingQuantity == 0) {
                            Color(0xFF2E7D32)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                MetricBox(
                    label = "সম্পন্ন (Completion)",
                    value = reconciliation.formattedCompletionPercentage,
                    containerColor = if (reconciliation.isFullyProduced) {
                        Color(0xFFE8F5E9)
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    contentColor = if (reconciliation.isFullyProduced) {
                        Color(0xFF2E7D32)
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Progress Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { (reconciliation.completionPercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = if (reconciliation.isOverProduced) {
                        MaterialTheme.colorScheme.tertiary
                    } else if (reconciliation.isFullyProduced) {
                        Color(0xFF2E7D32)
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Multi-Item Breakdown (if applicable)
            if (reconciliation.itemReconciliations.size > 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = "আইটেম ভিত্তিক অগ্রগতি (Item-Level Breakdown)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                ) {
                    reconciliation.itemReconciliations.forEach { item ->
                        ItemReconciliationRow(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.small),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun ItemReconciliationRow(
    item: ProductionItemOutputReconciliation,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "পরিকল্পিত: ${item.plannedQuantity} ${item.unit} | রেকর্ডকৃত: ${item.recordedQuantity} ${item.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = String.format(java.util.Locale.US, "%.1f%%", item.completionPercentage),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (item.isFullyProduced) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
            )
        }
    }
}
