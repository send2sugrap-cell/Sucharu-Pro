package com.sucharu.sucharupro.ui.features.orders.inquiry.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.order.InquiryRequirement
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Renders the list of specification requirements within an inquiry.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InquiryRequirementsSection(
    items: List<InquiryRequirement>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No specification requirements recorded for this inquiry.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        items.forEachIndexed { index, item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(MaterialTheme.spacing.medium)
            ) {
                // Item title + Qty header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}. ${item.productName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${item.quantity} ${item.unit}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Description
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                // Specification Pills / Attributes FlowRow
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
                ) {
                    if (!item.size.isNullOrBlank()) {
                        SpecificationPill(label = "Size: ${item.size}")
                    }
                    if (!item.paperMaterial.isNullOrBlank()) {
                        SpecificationPill(label = "Material: ${item.paperMaterial}")
                    }
                    if (item.gsm != null) {
                        SpecificationPill(label = "GSM: ${item.gsm}")
                    }
                    if (!item.colorSpecification.isNullOrBlank()) {
                        SpecificationPill(label = "Color: ${item.colorSpecification}")
                    }
                    if (!item.printingMethod.isNullOrBlank()) {
                        SpecificationPill(label = "Method: ${item.printingMethod}")
                    }
                    if (!item.finishing.isNullOrBlank()) {
                        SpecificationPill(label = "Finishing: ${item.finishing}")
                    }
                    if (item.isDesignRequired) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Design Service Required", style = MaterialTheme.typography.labelSmall) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Brush,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                    }
                }

                // Item Notes if present
                if (!item.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                    Text(
                        text = "Notes: ${item.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (index < items.size - 1) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }
        }
    }
}

@Composable
private fun SpecificationPill(
    label: String,
    modifier: Modifier = Modifier
) {
    SuggestionChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
    )
}
