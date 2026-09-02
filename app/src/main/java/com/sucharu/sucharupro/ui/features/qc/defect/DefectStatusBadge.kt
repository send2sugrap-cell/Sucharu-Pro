package com.sucharu.sucharupro.ui.features.qc.defect

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
import com.sucharu.sucharupro.domain.model.qc.DefectStatus

/**
 * Visual badge for [DefectStatus] (Module 06 Step 04).
 */
@Composable
fun DefectStatusBadge(
    status: DefectStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        DefectStatus.OPEN -> Pair(Color(0xFFDBEAFE), Color(0xFF1E40AF))
        DefectStatus.ACKNOWLEDGED -> Pair(Color(0xFFEDE9FE), Color(0xFF6D28D9))
        DefectStatus.UNDER_INVESTIGATION -> Pair(Color(0xFFE0E7FF), Color(0xFF3730A3))
        DefectStatus.CONTAINED -> Pair(Color(0xFFFEF3C7), Color(0xFF92400E))
        DefectStatus.RESOLUTION_PENDING -> Pair(Color(0xFFCFFAFE), Color(0xFF155E75))
        DefectStatus.RESOLVED -> Pair(Color(0xFFD1FAE5), Color(0xFF065F46))
        DefectStatus.CLOSED -> Pair(Color(0xFFF1F5F9), Color(0xFF475569))
        DefectStatus.CANCELLED -> Pair(Color(0xFFFEE2E2), Color(0xFF991B1B))
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
