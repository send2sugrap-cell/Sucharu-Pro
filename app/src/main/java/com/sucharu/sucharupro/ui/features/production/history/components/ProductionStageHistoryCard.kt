package com.sucharu.sucharupro.ui.features.production.history.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.job.ProductionStageHistoryItem
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus

/**
 * Card displaying individual stage execution details in historical view.
 */
@Composable
fun ProductionStageHistoryCard(
    stageHistory: ProductionStageHistoryItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sequence circle indicator
            Surface(
                shape = CircleShape,
                color = when (stageHistory.status) {
                    ProductionStageStatus.COMPLETED -> Color(0xFF2E7D32)
                    ProductionStageStatus.SKIPPED -> Color(0xFF757575)
                    ProductionStageStatus.IN_PROGRESS -> Color(0xFFE65100)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = when (stageHistory.status) {
                        ProductionStageStatus.COMPLETED -> Icons.Default.Check
                        ProductionStageStatus.SKIPPED -> Icons.Default.Close
                        ProductionStageStatus.IN_PROGRESS -> Icons.Default.PlayArrow
                        else -> Icons.Default.Schedule
                    },
                    contentDescription = null,
                    tint = if (stageHistory.status == ProductionStageStatus.PENDING) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        Color.White
                    },
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Stage Info & Metadata
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${stageHistory.sequence}. ${stageHistory.stageType.defaultLabel}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (stageHistory.status) {
                            ProductionStageStatus.COMPLETED -> Color(0xFF2E7D32).copy(alpha = 0.12f)
                            ProductionStageStatus.SKIPPED -> Color(0xFF757575).copy(alpha = 0.12f)
                            ProductionStageStatus.IN_PROGRESS -> Color(0xFFE65100).copy(alpha = 0.12f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = stageHistory.status.defaultLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = when (stageHistory.status) {
                                ProductionStageStatus.COMPLETED -> Color(0xFF2E7D32)
                                ProductionStageStatus.SKIPPED -> Color(0xFF757575)
                                ProductionStageStatus.IN_PROGRESS -> Color(0xFFE65100)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Operator & Duration Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stageHistory.operatorName ?: "Unassigned",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (stageHistory.durationSeconds > 0L) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stageHistory.formattedDuration ?: "${stageHistory.durationSeconds / 60}m",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Output Quantity if recorded
                if (stageHistory.recordedOutputQuantity > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Output: ${stageHistory.recordedOutputQuantity} produced",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Medium
                    )
                }

                // Remarks if present
                if (!stageHistory.remarks.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Note: ${stageHistory.remarks}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
