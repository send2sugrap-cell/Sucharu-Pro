package com.sucharu.sucharupro.ui.features.orders.quotation.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.Timeline
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
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.Quotation
import com.sucharu.sucharupro.domain.model.order.QuotationRevision
import com.sucharu.sucharupro.domain.model.order.QuotationStatusType
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.features.orders.components.DetailSectionCard
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Responsive Lifecycle Actions Card on the Quotation Details screen.
 * Displays contextual commercial actions based on the current quotation status.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuotationLifecycleActionsCard(
    quotation: Quotation,
    activeRevision: QuotationRevision?,
    linkedOrders: List<Order>,
    isActionInProgress: Boolean,
    onSendClick: () -> Unit,
    onStartNegotiationClick: () -> Unit,
    onCreateRevisionClick: () -> Unit,
    onApproveClick: () -> Unit,
    onRejectClick: () -> Unit,
    onCancelClick: () -> Unit,
    onConvertToOrderClick: () -> Unit,
    onViewOrderClick: (orderId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    DetailSectionCard(
        title = "Commercial Lifecycle & Actions",
        icon = Icons.Default.Timeline,
        modifier = modifier
    ) {
        when (quotation.status) {
            QuotationStatusType.DRAFT -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "This quotation is in DRAFT state. You can review items, adjust prices, or send it to the client for commercial review.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        AppButton(
                            text = "Send to Client",
                            onClick = onSendClick,
                            leadingIcon = {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isActionInProgress
                        )

                        AppOutlinedButton(
                            text = "Cancel Quotation",
                            onClick = onCancelClick,
                            leadingIcon = {
                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isActionInProgress
                        )
                    }
                }
            }

            QuotationStatusType.SENT -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Quotation was sent to the client and is awaiting customer feedback or negotiation.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        AppButton(
                            text = "Approve Quotation",
                            onClick = onApproveClick,
                            leadingIcon = {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isActionInProgress
                        )

                        AppOutlinedButton(
                            text = "Start Negotiation",
                            onClick = onStartNegotiationClick,
                            leadingIcon = {
                                Icon(Icons.Default.Handshake, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isActionInProgress
                        )

                        AppOutlinedButton(
                            text = "Reject",
                            onClick = onRejectClick,
                            leadingIcon = {
                                Icon(Icons.Default.ThumbDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isActionInProgress
                        )

                        AppOutlinedButton(
                            text = "Cancel",
                            onClick = onCancelClick,
                            leadingIcon = {
                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isActionInProgress
                        )
                    }
                }
            }

            QuotationStatusType.NEGOTIATION -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Active commercial negotiation in progress. You can branch a new revision, adjust pricing/specifications, or finalize approval.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        AppButton(
                            text = "Approve Revision #${activeRevision?.revisionNumber ?: quotation.currentRevisionNumber}",
                            onClick = onApproveClick,
                            leadingIcon = {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isActionInProgress
                        )

                        AppOutlinedButton(
                            text = "Create Revision #${quotation.currentRevisionNumber + 1}",
                            onClick = onCreateRevisionClick,
                            leadingIcon = {
                                Icon(Icons.Default.HistoryEdu, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isActionInProgress
                        )

                        AppOutlinedButton(
                            text = "Send Proposal",
                            onClick = onSendClick,
                            leadingIcon = {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isActionInProgress
                        )

                        AppOutlinedButton(
                            text = "Reject",
                            onClick = onRejectClick,
                            leadingIcon = {
                                Icon(Icons.Default.ThumbDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isActionInProgress
                        )

                        AppOutlinedButton(
                            text = "Cancel",
                            onClick = onCancelClick,
                            leadingIcon = {
                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isActionInProgress
                        )
                    }
                }
            }

            QuotationStatusType.APPROVED -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(MaterialTheme.spacing.medium),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                            Column {
                                Text(
                                    text = "Quotation Approved (Revision #${quotation.revisions.find { it.revisionId == quotation.approvedRevisionId }?.revisionNumber ?: quotation.currentRevisionNumber})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                if (!quotation.approvedBy.isNullOrBlank()) {
                                    Text(
                                        text = "Approved by ${quotation.approvedBy} on ${quotation.approvedAt?.take(10).orEmpty()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                    if (linkedOrders.isNotEmpty()) {
                        val firstOrder = linkedOrders.first()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Converted Order: ${firstOrder.orderNumber}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            AppButton(
                                text = "View Order",
                                onClick = { onViewOrderClick(firstOrder.orderId) },
                                leadingIcon = {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            )
                        }
                    } else {
                        AppButton(
                            text = "Convert to Confirmed Order",
                            onClick = onConvertToOrderClick,
                            leadingIcon = {
                                Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isActionInProgress
                        )
                    }
                }
            }

            QuotationStatusType.REJECTED -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "This quotation was marked as REJECTED by customer. Revision history is preserved.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                    AppOutlinedButton(
                        text = "Reopen for Negotiation",
                        onClick = onStartNegotiationClick,
                        leadingIcon = {
                            Icon(Icons.Default.Handshake, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        enabled = !isActionInProgress
                    )
                }
            }

            QuotationStatusType.EXPIRED -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Quotation validity period has elapsed. Commercial terms may require re-estimation.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                    AppOutlinedButton(
                        text = "Re-open Negotiation / Re-quote",
                        onClick = onStartNegotiationClick,
                        leadingIcon = {
                            Icon(Icons.Default.Handshake, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        enabled = !isActionInProgress
                    )
                }
            }

            QuotationStatusType.CANCELLED -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "This quotation is CANCELLED. All historical snapshots remain permanently archived as read-only.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
