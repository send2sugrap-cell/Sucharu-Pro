package com.sucharu.sucharupro.ui.features.orders.inquiry.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sucharu.sucharupro.domain.model.order.InquiryStatusType
import com.sucharu.sucharupro.ui.components.StatusBadge
import com.sucharu.sucharupro.ui.theme.StatusColor

/**
 * Semantic status badge for Customer Inquiry lifecycle states.
 */
@Composable
fun InquiryStatusBadge(
    status: InquiryStatusType,
    modifier: Modifier = Modifier,
    customLabel: String? = null,
    showDot: Boolean = true
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val color = when (status) {
        InquiryStatusType.NEW -> if (isDark) {
            StatusColor(container = Color(0xFF1E293B), content = Color(0xFF94A3B8), border = Color(0xFF334155))
        } else {
            StatusColor(container = Color(0xFFF1F5F9), content = Color(0xFF475569), border = Color(0xFFCBD5E1))
        }

        InquiryStatusType.IN_PROGRESS -> if (isDark) {
            StatusColor(container = Color(0xFF172554), content = Color(0xFF93C5FD), border = Color(0xFF1E40AF))
        } else {
            StatusColor(container = Color(0xFFDBEAFE), content = Color(0xFF1D4ED8), border = Color(0xFFBFDBFE))
        }

        InquiryStatusType.QUOTED -> if (isDark) {
            StatusColor(container = Color(0xFF3B1864), content = Color(0xFFC4B5FD), border = Color(0xFF5B21B6))
        } else {
            StatusColor(container = Color(0xFFF3E8FF), content = Color(0xFF7C3AED), border = Color(0xFFDDD6FE))
        }

        InquiryStatusType.CONVERTED -> if (isDark) {
            StatusColor(container = Color(0xFF064E3B), content = Color(0xFF6EE7B7), border = Color(0xFF047857))
        } else {
            StatusColor(container = Color(0xFFD1FAE5), content = Color(0xFF047857), border = Color(0xFFA7F3D0))
        }

        InquiryStatusType.CLOSED -> if (isDark) {
            StatusColor(container = Color(0xFF334155), content = Color(0xFFCBD5E1), border = Color(0xFF475569))
        } else {
            StatusColor(container = Color(0xFFE2E8F0), content = Color(0xFF64748B), border = Color(0xFFCBD5E1))
        }

        InquiryStatusType.CANCELLED -> if (isDark) {
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
