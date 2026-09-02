package com.sucharu.sucharupro.ui.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.dashboard.StageCount
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.SectionHeader
import com.sucharu.sucharupro.ui.theme.spacing
import com.sucharu.sucharupro.ui.theme.statusColors

/**
 * Production Workflow Pipeline for Sucharu Pro Dashboard.
 *
 * Visualizes active job counts across the canonical 13-stage production workflow:
 * 1. DESIGN → 2. APPROVAL → 3. QC → 4. ITEM_APPROVAL → 5. CTP → 6. PRINTING →
 * 7. LAMINATION → 8. FOLDING → 9. BINDING → 10. FINAL_QC → 11. PACKAGING →
 * 12. READY → 13. DELIVERED
 *
 * Uses [ProductionStageType] — the single authoritative production workflow definition.
 */
@Composable
fun DashboardWorkflowPipeline(
    stageCounts: List<StageCount>,
    onStageClick: (ProductionStageType) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalActiveJobs = stageCounts.filter {
        it.stage != ProductionStageType.DELIVERED
    }.sumOf { it.count }

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Production Workflow Pipeline",
            subtitle = "$totalActiveJobs active jobs across ${ProductionStageType.TOTAL_STAGES} pipeline stages"
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isTabletOrDesktop = maxWidth >= 600.dp
            val columns = if (isTabletOrDesktop) 4 else 2
            val chunkedStages = stageCounts.chunked(columns)

            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                chunkedStages.forEach { rowStages ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        rowStages.forEach { stageItem ->
                            WorkflowStageTile(
                                stageCount = stageItem,
                                onClick = { onStageClick(stageItem.stage) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill empty slots in the last row if needed
                        val emptySlots = columns - rowStages.size
                        repeat(emptySlots) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkflowStageTile(
    stageCount: StageCount,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColors = MaterialTheme.statusColors
    val stageColor = when (stageCount.stage) {
        ProductionStageType.DESIGN        -> statusColors.stageDesign
        ProductionStageType.APPROVAL      -> statusColors.stageApproval
        ProductionStageType.QC            -> statusColors.stageQc
        ProductionStageType.ITEM_APPROVAL -> statusColors.stageItemApproval
        ProductionStageType.CTP           -> statusColors.stageCtp
        ProductionStageType.PRINTING      -> statusColors.stagePrinting
        ProductionStageType.LAMINATION    -> statusColors.stageLamination
        ProductionStageType.FOLDING       -> statusColors.stageFolding
        ProductionStageType.BINDING       -> statusColors.stageBinding
        ProductionStageType.FINAL_QC      -> statusColors.stageFinalQc
        ProductionStageType.PACKAGING     -> statusColors.stagePackaging
        ProductionStageType.READY         -> statusColors.stageReady
        ProductionStageType.DELIVERED     -> statusColors.stageDelivered
    }

    AppCard(
        modifier = modifier,
        onClick = onClick,
        border = androidx.compose.foundation.BorderStroke(1.dp, stageColor.border.copy(alpha = 0.7f)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(MaterialTheme.spacing.medium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(stageColor.content)
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                Column {
                    Text(
                        text = stageCount.stage.defaultLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Stage ${stageCount.stage.displayOrder} • ${stageCount.stage.shortCode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(stageColor.container)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${stageCount.count}",
                    style = MaterialTheme.typography.labelMedium,
                    color = stageColor.content,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (!stageCount.totalEstimatedValue.isZero()) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
            Text(
                text = "Est. ${stageCount.totalEstimatedValue.formatted()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
