package com.sucharu.sucharupro.ui.features.customer.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.ui.components.StatusBadge
import com.sucharu.sucharupro.ui.theme.statusColors

/**
 * Customer status badge for ACTIVE, INACTIVE, BLOCKED, ARCHIVED states.
 */
@Composable
fun CustomerStatusBadge(
    status: CustomerStatusType,
    modifier: Modifier = Modifier,
    showDot: Boolean = true
) {
    val statusColors = MaterialTheme.statusColors
    val color = when (status) {
        CustomerStatusType.ACTIVE   -> statusColors.paymentPaid
        CustomerStatusType.INACTIVE -> statusColors.orderPending
        CustomerStatusType.BLOCKED  -> statusColors.orderCancelled
        CustomerStatusType.ARCHIVED -> statusColors.orderOnHold
    }

    StatusBadge(
        label = status.defaultLabel,
        statusColor = color,
        modifier = modifier,
        showDot = showDot
    )
}

/**
 * Customer entity classification badge (Individual, Business, Dealer, Institution, Organization, Other).
 */
@Composable
fun CustomerTypeBadge(
    type: CustomerType,
    modifier: Modifier = Modifier
) {
    val statusColors = MaterialTheme.statusColors
    val color = when (type) {
        CustomerType.INDIVIDUAL   -> statusColors.orderConfirmed
        CustomerType.BUSINESS     -> statusColors.stagePrinting
        CustomerType.DEALER       -> statusColors.stageCtp
        CustomerType.VIP          -> statusColors.paymentPaid
        CustomerType.GOVERNMENT   -> statusColors.stageApproval
        CustomerType.INSTITUTION  -> statusColors.stageBinding
        CustomerType.ORGANIZATION -> statusColors.stagePackaging
        CustomerType.OTHER        -> statusColors.stageDesign
    }

    StatusBadge(
        label = type.defaultLabel,
        statusColor = color,
        modifier = modifier,
        showDot = false
    )
}
