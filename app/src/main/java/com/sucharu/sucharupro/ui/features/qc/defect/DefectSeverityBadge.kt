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
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity

/**
 * Visual badge for [DefectSeverity] (Module 06 Step 04).
 */
@Composable
fun DefectSeverityBadge(
    severity: DefectSeverity,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (severity) {
        DefectSeverity.MINOR -> Pair(Color(0xFFE0F2FE), Color(0xFF0369A1))
        DefectSeverity.MAJOR -> Pair(Color(0xFFFEF3C7), Color(0xFFB45309))
        DefectSeverity.CRITICAL -> Pair(Color(0xFFFEE2E2), Color(0xFFB91C1C))
    }

    Box(
        modifier = modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = severity.defaultLabel,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
