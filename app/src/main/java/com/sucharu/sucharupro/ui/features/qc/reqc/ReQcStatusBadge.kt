package com.sucharu.sucharupro.ui.features.qc.reqc

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
import com.sucharu.sucharupro.domain.model.qc.ReQcStatus

/**
 * Visual badge for [ReQcStatus] (Module 06 Step 06).
 */
@Composable
fun ReQcStatusBadge(
    status: ReQcStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        ReQcStatus.DRAFT -> Pair(Color(0xFFF1F5F9), Color(0xFF475569))
        ReQcStatus.PENDING -> Pair(Color(0xFFDBEAFE), Color(0xFF1E40AF))
        ReQcStatus.ASSIGNED -> Pair(Color(0xFFCFFAFE), Color(0xFF155E75))
        ReQcStatus.IN_INSPECTION -> Pair(Color(0xFFFEF3C7), Color(0xFF92400E))
        ReQcStatus.PASSED -> Pair(Color(0xFFDCFCE7), Color(0xFF166534))
        ReQcStatus.FAILED -> Pair(Color(0xFFFEE2E2), Color(0xFF991B1B))
        ReQcStatus.RETURNED_TO_REWORK -> Pair(Color(0xFFFFEDD5), Color(0xFF9A3412))
        ReQcStatus.CANCELLED -> Pair(Color(0xFFF3F4F6), Color(0xFF6B7280))
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
