package com.sucharu.sucharupro.ui.features.orders.order.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.ui.components.StatusBadge
import com.sucharu.sucharupro.ui.theme.StatusColor

/**
 * Semantic priority badge for Customer Orders (NORMAL, HIGH, URGENT).
 */
@Composable
fun OrderPriorityBadge(
    priority: OrderPriority,
    modifier: Modifier = Modifier,
    customLabel: String? = null,
    showDot: Boolean = true
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val color = when (priority) {
        OrderPriority.NORMAL -> if (isDark) {
            StatusColor(container = Color(0xFF1E293B), content = Color(0xFF94A3B8), border = Color(0xFF334155))
        } else {
            StatusColor(container = Color(0xFFF1F5F9), content = Color(0xFF475569), border = Color(0xFFCBD5E1))
        }

        OrderPriority.HIGH -> if (isDark) {
            StatusColor(container = Color(0xFF451A03), content = Color(0xFFFCD34D), border = Color(0xFF78350F))
        } else {
            StatusColor(container = Color(0xFFFEF3C7), content = Color(0xFFB45309), border = Color(0xFFFDE68A))
        }

        OrderPriority.URGENT -> if (isDark) {
            StatusColor(container = Color(0xFF450A0A), content = Color(0xFFFCA5A5), border = Color(0xFF7F1D1D))
        } else {
            StatusColor(container = Color(0xFFFEE2E2), content = Color(0xFFB91C1C), border = Color(0xFFFECACA))
        }
    }

    StatusBadge(
        label = customLabel ?: priority.defaultLabel,
        statusColor = color,
        modifier = modifier,
        showDot = showDot
    )
}
