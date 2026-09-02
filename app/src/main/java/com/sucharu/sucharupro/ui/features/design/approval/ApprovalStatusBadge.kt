package com.sucharu.sucharupro.ui.features.design.approval

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
import com.sucharu.sucharupro.domain.model.design.ApprovalStatus

/**
 * Standard Status Badge for Approval Lifecycle (Module 05 Step 04).
 */
@Composable
fun ApprovalStatusBadge(
    status: ApprovalStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        ApprovalStatus.DRAFT -> Pair(Color(0xFFE2E8F0), Color(0xFF334155))
        ApprovalStatus.PENDING_REVIEW -> Pair(Color(0xFFDBEAFE), Color(0xFF1E40AF))
        ApprovalStatus.UNDER_REVIEW -> Pair(Color(0xFFEDE9FE), Color(0xFF5B21B6))
        ApprovalStatus.REVISION_REQUIRED -> Pair(Color(0xFFFEF3C7), Color(0xFF92400E))
        ApprovalStatus.REJECTED -> Pair(Color(0xFFFEE2E2), Color(0xFF991B1B))
        ApprovalStatus.RESUBMITTED -> Pair(Color(0xFFE0E7FF), Color(0xFF3730A3))
        ApprovalStatus.APPROVED -> Pair(Color(0xFFD1FAE5), Color(0xFF065F46))
        ApprovalStatus.FINAL_LOCKED -> Pair(Color(0xFF10B981), Color.White)
    }

    Box(
        modifier = modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = if (status == ApprovalStatus.FINAL_LOCKED) "🔒 FINAL LOCKED" else status.defaultLabel,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
