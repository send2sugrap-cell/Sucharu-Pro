package com.sucharu.sucharupro.ui.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.dashboard.PaymentBreakdown
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.SectionHeader
import com.sucharu.sucharupro.ui.theme.PillShape
import com.sucharu.sucharupro.ui.theme.spacing
import com.sucharu.sucharupro.ui.theme.statusColors

/**
 * Payment & Receivables Financial Snapshot for Sucharu Pro.
 *
 * Prepared for role-aware visibility: only displayed if [userRole] has financial access.
 */
@Composable
fun DashboardPaymentSummary(
    paymentBreakdown: PaymentBreakdown,
    onViewInvoicesClick: () -> Unit,
    modifier: Modifier = Modifier,
    userRole: UserRole? = null
) {
    if (userRole != null && !userRole.hasFinancialAccess) return

    val statusColors = MaterialTheme.statusColors

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Payment & Receivables",
            subtitle = "Daily collection ratio & outstanding balances",
            actionText = "Invoices",
            onActionClick = onViewInvoicesClick
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

        AppCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(MaterialTheme.spacing.large)
        ) {
            // Collection Rate Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Collection Efficiency",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${(paymentBreakdown.collectionRate * 100).toInt()}% Collected",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Invoiced: ${paymentBreakdown.totalInvoicedToday.formatted()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Collected: ${paymentBreakdown.totalCollectedToday.formatted()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColors.paymentPaid.content,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            // Visual Progress Bar
            LinearProgressIndicator(
                progress = { paymentBreakdown.collectionRate },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(PillShape),
                color = statusColors.paymentPaid.content,
                trackColor = statusColors.paymentUnpaid.container
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

            // 4 Status Breakdown Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PaymentStatusSummaryPill(
                    label = "Paid",
                    count = paymentBreakdown.paidCount,
                    color = statusColors.paymentPaid.content,
                    background = statusColors.paymentPaid.container
                )
                PaymentStatusSummaryPill(
                    label = "Partial",
                    count = paymentBreakdown.partialCount,
                    color = statusColors.paymentPartial.content,
                    background = statusColors.paymentPartial.container
                )
                PaymentStatusSummaryPill(
                    label = "Due",
                    count = paymentBreakdown.dueCount,
                    color = statusColors.paymentUnpaid.content,
                    background = statusColors.paymentUnpaid.container
                )
                PaymentStatusSummaryPill(
                    label = "Overdue",
                    count = paymentBreakdown.overdueCount,
                    color = statusColors.paymentOverdue.content,
                    background = statusColors.paymentOverdue.container
                )
            }
        }
    }
}

@Composable
private fun PaymentStatusSummaryPill(
    label: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color,
    background: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(PillShape)
            .background(background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$label: $count",
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
