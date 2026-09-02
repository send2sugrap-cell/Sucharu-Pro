package com.sucharu.sucharupro.ui.features.orders.quotation.details.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.order.Quotation
import com.sucharu.sucharupro.domain.model.order.QuotationRevision
import com.sucharu.sucharupro.ui.features.orders.components.DetailSectionCard
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Renders the revision history of a commercial quotation, highlighting current, approved, and historical revisions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuotationRevisionHistorySection(
    quotation: Quotation,
    revisions: List<QuotationRevision>,
    selectedRevisionId: String?,
    onSelectRevision: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    DetailSectionCard(
        title = "Revision History (${revisions.size})",
        icon = Icons.Default.History,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            revisions.sortedByDescending { it.revisionNumber }.forEach { revision ->
                val isCurrent = revision.revisionNumber == quotation.currentRevisionNumber
                val isApproved = quotation.isApproved && revision.revisionId == quotation.approvedRevisionId
                val isSelected = if (selectedRevisionId != null) {
                    revision.revisionId == selectedRevisionId
                } else {
                    isCurrent
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectRevision(if (isCurrent && selectedRevisionId == null) null else revision.revisionId) },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = if (isSelected) {
                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.medium)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Revision #${revision.revisionNumber}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = revision.totalAmount.formatted(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Badges for CURRENT / APPROVED / VIEWING
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
                        ) {
                            if (isCurrent) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("CURRENT", style = MaterialTheme.typography.labelSmall) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                )
                            }
                            if (isApproved) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("APPROVED", style = MaterialTheme.typography.labelSmall) },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                            if (isSelected) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("Viewing Snapshot", style = MaterialTheme.typography.labelSmall) },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                        // Created by & Date
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "By: ${revision.createdBy}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = revision.createdAt.take(10),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Revision Reason if present
                        if (!revision.revisionReason.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Reason: ${revision.revisionReason}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
