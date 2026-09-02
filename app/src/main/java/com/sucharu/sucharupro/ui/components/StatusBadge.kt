package com.sucharu.sucharupro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.inventory.StockStatusType
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.payment.PaymentStatusType
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.ui.theme.BadgeShape
import com.sucharu.sucharupro.ui.theme.StatusColor
import com.sucharu.sucharupro.ui.theme.spacing
import com.sucharu.sucharupro.ui.theme.statusColors

// ============================================================================
// Core Status Badge Composable
// ============================================================================

/**
 * Base StatusBadge pill component with background container, text, border, and optional indicator dot or icon.
 */
@Composable
fun StatusBadge(
    label: String,
    statusColor: StatusColor,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    showDot: Boolean = true,
    shape: Shape = BadgeShape
) {
    val borderModifier = if (statusColor.border != Color.Transparent) {
        Modifier.border(width = 1.dp, color = statusColor.border, shape = shape)
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .clip(shape)
            .background(statusColor.container)
            .then(borderModifier)
            .padding(
                horizontal = MaterialTheme.spacing.small,
                vertical = MaterialTheme.spacing.extraSmall
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
        } else if (showDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(statusColor.content)
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = statusColor.content
        )
    }
}

// ============================================================================
// Specialized Status Badges
// ============================================================================

/**
 * Commercial order lifecycle badge.
 * Maps [OrderStatusType] values: PENDING, CONFIRMED, IN_PRODUCTION, READY, DELIVERED, ON_HOLD, CANCELLED.
 */
@Composable
fun OrderStatusBadge(
    status: OrderStatusType,
    modifier: Modifier = Modifier,
    customLabel: String? = null,
    showDot: Boolean = true
) {
    val statusColors = MaterialTheme.statusColors
    val color = when (status) {
        OrderStatusType.PENDING       -> statusColors.orderPending
        OrderStatusType.CONFIRMED     -> statusColors.orderConfirmed
        OrderStatusType.IN_PRODUCTION -> statusColors.orderInProduction
        OrderStatusType.READY         -> statusColors.orderReady
        OrderStatusType.DELIVERED     -> statusColors.orderDelivered
        OrderStatusType.ON_HOLD       -> statusColors.orderOnHold
        OrderStatusType.CANCELLED     -> statusColors.orderCancelled
    }

    StatusBadge(
        label = customLabel ?: status.defaultLabel,
        statusColor = color,
        modifier = modifier,
        showDot = showDot
    )
}

/**
 * Production stage badge for the canonical 13-stage workflow.
 * Maps all [ProductionStageType] values to distinct stage colors.
 */
@Composable
fun ProductionStageBadge(
    stage: ProductionStageType,
    modifier: Modifier = Modifier,
    customLabel: String? = null,
    showDot: Boolean = true
) {
    val statusColors = MaterialTheme.statusColors
    val color = when (stage) {
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

    StatusBadge(
        label = customLabel ?: stage.defaultLabel,
        statusColor = color,
        modifier = modifier,
        showDot = showDot
    )
}

/**
 * Payment status badge.
 */
@Composable
fun PaymentStatusBadge(
    status: PaymentStatusType,
    modifier: Modifier = Modifier,
    customLabel: String? = null,
    showDot: Boolean = true
) {
    val statusColors = MaterialTheme.statusColors
    val color = when (status) {
        PaymentStatusType.PAID    -> statusColors.paymentPaid
        PaymentStatusType.PARTIAL -> statusColors.paymentPartial
        PaymentStatusType.UNPAID  -> statusColors.paymentUnpaid
        PaymentStatusType.OVERDUE -> statusColors.paymentOverdue
    }

    StatusBadge(
        label = customLabel ?: status.defaultLabel,
        statusColor = color,
        modifier = modifier,
        showDot = showDot
    )
}

/**
 * Finished product inventory stock alert badge.
 */
@Composable
fun StockStatusBadge(
    status: StockStatusType,
    modifier: Modifier = Modifier,
    customLabel: String? = null,
    showDot: Boolean = true
) {
    val statusColors = MaterialTheme.statusColors
    val color = when (status) {
        StockStatusType.IN_STOCK     -> statusColors.stockInStock
        StockStatusType.LOW_STOCK    -> statusColors.stockLow
        StockStatusType.OUT_OF_STOCK -> statusColors.stockOut
    }

    StatusBadge(
        label = customLabel ?: status.defaultLabel,
        statusColor = color,
        modifier = modifier,
        showDot = showDot
    )
}
