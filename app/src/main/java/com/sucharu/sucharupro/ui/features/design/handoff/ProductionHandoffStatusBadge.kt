package com.sucharu.sucharupro.ui.features.design.handoff

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

/**
 * Minimal status badge indicating Production Handoff readiness (Module 05 Step 05).
 */
@Composable
fun ProductionHandoffStatusBadge(
    isAuthorized: Boolean,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, label) = if (isAuthorized) {
        Triple(Color(0xFF059669), Color.White, "✓ PROD HANDOFF: AUTHORIZED")
    } else {
        Triple(Color(0xFFE2E8F0), Color(0xFF475569), "PROD HANDOFF: BLOCKED")
    }

    Box(
        modifier = modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
