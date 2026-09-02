package com.sucharu.sucharupro.ui.features.finance.reporting

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.finance.FinancialReportPeriod

@Composable
fun ReportPeriodSelector(
    selectedPeriod: FinancialReportPeriod,
    onPeriodSelected: (FinancialReportPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val periods = listOf(
        FinancialReportPeriod.Today,
        FinancialReportPeriod.Yesterday,
        FinancialReportPeriod.CurrentWeek,
        FinancialReportPeriod.PreviousWeek,
        FinancialReportPeriod.CurrentMonth,
        FinancialReportPeriod.PreviousMonth,
        FinancialReportPeriod.CurrentQuarter,
        FinancialReportPeriod.CurrentFinancialYear
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        periods.forEach { period ->
            val isSelected = selectedPeriod == period
            FilterChip(
                selected = isSelected,
                onClick = { onPeriodSelected(period) },
                label = {
                    Text(
                        text = period.defaultLabel,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFFF8FAFC) else Color(0xFF94A3B8)
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF2563EB),
                    containerColor = Color(0xFF1E293B)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) Color(0xFF60A5FA) else Color(0xFF334155),
                    selectedBorderColor = Color(0xFF60A5FA)
                )
            )
        }
    }
}
