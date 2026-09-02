package com.sucharu.sucharupro.ui.features.design.artwork

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.design.ArtworkStatus

/**
 * Status badge for [ArtworkStatus].
 */
@Composable
fun ArtworkStatusBadge(
    status: ArtworkStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        ArtworkStatus.DRAFT -> Color(0xFFF1F5F9) to Color(0xFF475569)
        ArtworkStatus.ACTIVE -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        ArtworkStatus.ARCHIVED -> Color(0xFFFEE2E2) to Color(0xFF991B1B)
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
