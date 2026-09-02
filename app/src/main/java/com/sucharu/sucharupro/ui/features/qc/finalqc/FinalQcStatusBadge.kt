package com.sucharu.sucharupro.ui.features.qc.finalqc

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
import com.sucharu.sucharupro.domain.model.qc.FinalQcStatus

/**
 * Visual badge for [FinalQcStatus] in Sucharu Pro ERP (Module 06 Step 07).
 */
@Composable
fun FinalQcStatusBadge(
    status: FinalQcStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        FinalQcStatus.DRAFT -> Pair(Color(0xFFF1F5F9), Color(0xFF475569))
        FinalQcStatus.PENDING -> Pair(Color(0xFFDBEAFE), Color(0xFF1E40AF))
        FinalQcStatus.ASSIGNED -> Pair(Color(0xFFCFFAFE), Color(0xFF155E75))
        FinalQcStatus.IN_INSPECTION -> Pair(Color(0xFFFEF3C7), Color(0xFF92400E))
        FinalQcStatus.PASSED -> Pair(Color(0xFFDCFCE7), Color(0xFF166534))
        FinalQcStatus.FAILED -> Pair(Color(0xFFFEE2E2), Color(0xFF991B1B))
        FinalQcStatus.BLOCKED -> Pair(Color(0xFFFFEDD5), Color(0xFF9A3412))
        FinalQcStatus.CANCELLED -> Pair(Color(0xFFF3F4F6), Color(0xFF6B7280))
        FinalQcStatus.RELEASED -> Pair(Color(0xFFE0E7FF), Color(0xFF3730A3))
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
