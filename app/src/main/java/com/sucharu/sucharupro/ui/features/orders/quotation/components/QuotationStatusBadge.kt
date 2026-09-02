package com.sucharu.sucharupro.ui.features.orders.quotation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sucharu.sucharupro.domain.model.order.QuotationStatusType
import com.sucharu.sucharupro.ui.components.StatusBadge
import com.sucharu.sucharupro.ui.theme.StatusColor

/**
 * Semantic status badge for Commercial Quotation lifecycle states.
 */
@Composable
fun QuotationStatusBadge(
    status: QuotationStatusType,
    modifier: Modifier = Modifier,
    customLabel: String? = null,
    showDot: Boolean = true
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val color = when (status) {
        QuotationStatusType.DRAFT -> if (isDark) {
            StatusColor(container = Color(0xFF1E293B), content = Color(0xFF94A3B8), border = Color(0xFF334155))
        } else {
            StatusColor(container = Color(0xFFF1F5F9), content = Color(0xFF475569), border = Color(0xFFCBD5E1))
        }

        QuotationStatusType.SENT -> if (isDark) {
            StatusColor(container = Color(0xFF0C4A6E), content = Color(0xFF7DD3FC), border = Color(0xFF0369A1))
        } else {
            StatusColor(container = Color(0xFFE0F2FE), content = Color(0xFF0284C7), border = Color(0xFFBAE6FD))
        }

        QuotationStatusType.NEGOTIATION -> if (isDark) {
            StatusColor(container = Color(0xFF451A03), content = Color(0xFFFCD34D), border = Color(0xFF78350F))
        } else {
            StatusColor(container = Color(0xFFFEF3C7), content = Color(0xFFB45309), border = Color(0xFFFDE68A))
        }

        QuotationStatusType.APPROVED -> if (isDark) {
            StatusColor(container = Color(0xFF064E3B), content = Color(0xFF6EE7B7), border = Color(0xFF047857))
        } else {
            StatusColor(container = Color(0xFFD1FAE5), content = Color(0xFF047857), border = Color(0xFFA7F3D0))
        }

        QuotationStatusType.REJECTED -> if (isDark) {
            StatusColor(container = Color(0xFF450A0A), content = Color(0xFFFCA5A5), border = Color(0xFF7F1D1D))
        } else {
            StatusColor(container = Color(0xFFFEE2E2), content = Color(0xFFB91C1C), border = Color(0xFFFECACA))
        }

        QuotationStatusType.EXPIRED -> if (isDark) {
            StatusColor(container = Color(0xFF334155), content = Color(0xFFCBD5E1), border = Color(0xFF475569))
        } else {
            StatusColor(container = Color(0xFFE2E8F0), content = Color(0xFF64748B), border = Color(0xFFCBD5E1))
        }

        QuotationStatusType.CANCELLED -> if (isDark) {
            StatusColor(container = Color(0xFF450A0A), content = Color(0xFFFCA5A5), border = Color(0xFF7F1D1D))
        } else {
            StatusColor(container = Color(0xFFFEE2E2), content = Color(0xFFB91C1C), border = Color(0xFFFECACA))
        }
    }

    StatusBadge(
        label = customLabel ?: status.defaultLabel,
        statusColor = color,
        modifier = modifier,
        showDot = showDot
    )
}
