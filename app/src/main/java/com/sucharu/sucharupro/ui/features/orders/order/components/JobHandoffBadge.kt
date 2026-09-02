package com.sucharu.sucharupro.ui.features.orders.order.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sucharu.sucharupro.domain.model.order.JobHandoffStatus
import com.sucharu.sucharupro.ui.components.StatusBadge
import com.sucharu.sucharupro.ui.theme.StatusColor

/**
 * Minimal badge indicating commercial order readiness for Module 04 (Job Card & Production).
 */
@Composable
fun JobHandoffBadge(
    status: JobHandoffStatus,
    modifier: Modifier = Modifier,
    customLabel: String? = null,
    showDot: Boolean = true
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val color = when (status) {
        JobHandoffStatus.NOT_READY -> if (isDark) {
            StatusColor(container = Color(0xFF1E293B), content = Color(0xFF94A3B8), border = Color(0xFF334155))
        } else {
            StatusColor(container = Color(0xFFF1F5F9), content = Color(0xFF64748B), border = Color(0xFFCBD5E1))
        }

        JobHandoffStatus.READY_FOR_JOB -> if (isDark) {
            StatusColor(container = Color(0xFF064E3B), content = Color(0xFF6EE7B7), border = Color(0xFF047857))
        } else {
            StatusColor(container = Color(0xFFD1FAE5), content = Color(0xFF047857), border = Color(0xFFA7F3D0))
        }
    }

    StatusBadge(
        label = customLabel ?: status.defaultLabel,
        statusColor = color,
        modifier = modifier,
        showDot = showDot
    )
}
