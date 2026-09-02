package com.sucharu.sucharupro.ui.features.design.proof

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
import com.sucharu.sucharupro.domain.model.design.ProofStatus

/**
 * Standard Status Badge for Proof Lifecycle (Module 05 Step 03).
 */
@Composable
fun ProofStatusBadge(
    status: ProofStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        ProofStatus.DRAFT -> Pair(Color(0xFFE2E8F0), Color(0xFF334155))
        ProofStatus.READY_FOR_REVIEW -> Pair(Color(0xFFDBEAFE), Color(0xFF1E40AF))
        ProofStatus.REVISION_REQUESTED -> Pair(Color(0xFFFEF3C7), Color(0xFF92400E))
        ProofStatus.REVISING -> Pair(Color(0xFFEDE9FE), Color(0xFF5B21B6))
        ProofStatus.RESUBMITTED -> Pair(Color(0xFFD1FAE5), Color(0xFF065F46))
        ProofStatus.ARCHIVED -> Pair(Color(0xFFF1F5F9), Color(0xFF64748B))
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
