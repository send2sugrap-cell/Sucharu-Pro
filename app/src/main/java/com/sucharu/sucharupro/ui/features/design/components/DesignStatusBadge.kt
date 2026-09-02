package com.sucharu.sucharupro.ui.features.design.components

import androidx.compose.foundation.background
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
import com.sucharu.sucharupro.domain.model.design.DesignStatus

/**
 * Minimal badge component displaying the strongly typed [DesignStatus].
 */
@Composable
fun DesignStatusBadge(
    status: DesignStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        DesignStatus.NOT_STARTED -> Color(0xFFF1F5F9) to Color(0xFF475569)
        DesignStatus.ASSIGNED -> Color(0xFFE0F2FE) to Color(0xFF0369A1)
        DesignStatus.IN_DESIGN -> Color(0xFFFEF3C7) to Color(0xFFB45309)
        DesignStatus.PROOF_PENDING -> Color(0xFFEDE9FE) to Color(0xFF6D28D9)
        DesignStatus.CUSTOMER_REVIEW -> Color(0xFFFCE7F3) to Color(0xFFBE185D)
        DesignStatus.REVISION_REQUIRED -> Color(0xFFFFEDD5) to Color(0xFFC2410C)
        DesignStatus.APPROVAL_PENDING -> Color(0xFFE0E7FF) to Color(0xFF4338CA)
        DesignStatus.APPROVED -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        DesignStatus.FINALIZED -> Color(0xFFCCFBF1) to Color(0xFF0F766E)
        DesignStatus.HANDED_OFF_TO_PRODUCTION -> Color(0xFFD1FAE5) to Color(0xFF065F46)
        DesignStatus.CANCELLED -> Color(0xFFFEE2E2) to Color(0xFFB91C1C)
    }

    Text(
        text = status.defaultLabel,
        color = textColor,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
