package com.sucharu.sucharupro.ui.features.qc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.qc.QcStatus

/**
 * Standard Status Badge for Quality Control Lifecycle (Module 06 Step 01).
 */
@Composable
fun QcStatusBadge(
    status: QcStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        QcStatus.DRAFT -> Pair(Color(0xFFE2E8F0), Color(0xFF334155))
        QcStatus.PENDING_INSPECTION -> Pair(Color(0xFFDBEAFE), Color(0xFF1E40AF))
        QcStatus.IN_INSPECTION -> Pair(Color(0xFFEDE9FE), Color(0xFF5B21B6))
        QcStatus.PASSED -> Pair(Color(0xFFD1FAE5), Color(0xFF065F46))
        QcStatus.FAILED -> Pair(Color(0xFFFEE2E2), Color(0xFF991B1B))
        QcStatus.CANCELLED -> Pair(Color(0xFFF1F5F9), Color(0xFF64748B))
    }

    Box(
        modifier = modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.defaultLabel,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
