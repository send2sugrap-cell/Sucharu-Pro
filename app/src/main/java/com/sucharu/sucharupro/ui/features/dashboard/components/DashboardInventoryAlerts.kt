package com.sucharu.sucharupro.ui.features.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.dashboard.DashboardInventoryAlert
import com.sucharu.sucharupro.domain.model.inventory.StockStatusType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.SectionHeader
import com.sucharu.sucharupro.ui.components.StockStatusBadge
import com.sucharu.sucharupro.ui.theme.spacing
import com.sucharu.sucharupro.ui.theme.statusColors

/**
 * Finished Product Stock Alerts Section for Sucharu Pro Dashboard.
 *
 * Displays LOW_STOCK and OUT_OF_STOCK alerts exclusively for finished/saleable
 * inventory items (Quran Sharif, Noorani Qaida, Ampara, Calendars, Diaries, Gift Sets).
 *
 * ⚠️ Raw materials (paper, ink, plates, chemicals, lamination film) are NOT tracked
 * or displayed here.
 */
@Composable
fun DashboardInventoryAlerts(
    alerts: List<DashboardInventoryAlert>,
    onViewInventoryClick: () -> Unit,
    modifier: Modifier = Modifier,
    userRole: UserRole? = null
) {
    // Show only to roles that manage warehouse/operations/management
    val showStock = userRole == null || userRole in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.WAREHOUSE, UserRole.STAFF)
    if (!showStock || alerts.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Finished Product Stock Alerts",
            subtitle = "${alerts.size} finished product SKUs below minimum stock threshold",
            actionText = "Inventory",
            onActionClick = onViewInventoryClick
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isTabletOrDesktop = maxWidth >= 600.dp

            if (isTabletOrDesktop) {
                // 2-column layout on tablet/desktop
                val chunkedAlerts = alerts.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                    chunkedAlerts.forEach { rowAlerts ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                        ) {
                            rowAlerts.forEach { alert ->
                                FinishedProductAlertCard(
                                    alert = alert,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowAlerts.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                // Single column on mobile
                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                ) {
                    alerts.forEach { alert ->
                        FinishedProductAlertCard(alert = alert)
                    }
                }
            }
        }
    }
}

@Composable
private fun FinishedProductAlertCard(
    alert: DashboardInventoryAlert,
    modifier: Modifier = Modifier
) {
    val statusColors = MaterialTheme.statusColors
    val isOutOfStock = alert.stockStatus == StockStatusType.OUT_OF_STOCK

    val cardBorder = if (isOutOfStock) {
        androidx.compose.foundation.BorderStroke(1.dp, statusColors.stockOut.border)
    } else {
        null
    }

    AppCard(
        modifier = modifier.fillMaxWidth(),
        border = cardBorder,
        contentPadding = PaddingValues(MaterialTheme.spacing.medium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.itemName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${alert.category} • In Stock: ${alert.currentStock.toInt()} ${alert.unit} (Min: ${alert.minThreshold.toInt()} ${alert.unit})",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOutOfStock) statusColors.stockOut.content else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isOutOfStock) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            StockStatusBadge(status = alert.stockStatus)
        }
    }
}
