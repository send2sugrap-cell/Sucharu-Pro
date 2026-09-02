package com.sucharu.sucharupro.ui.features.finance.reporting

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialReportLine
import com.sucharu.sucharupro.domain.model.finance.FinancialReportLineType
import com.sucharu.sucharupro.domain.model.finance.FinancialReportStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialReportType

@Composable
fun ReportStatusBadge(
    status: FinancialReportStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        FinancialReportStatus.READY -> Color(0xFF064E3B) to Color(0xFF6EE7B7)
        FinancialReportStatus.CONTROL_EXCEPTION -> Color(0xFF7F1D1D) to Color(0xFFFCA5A5)
        FinancialReportStatus.GENERATING -> Color(0xFF78350F) to Color(0xFFFDE68A)
        FinancialReportStatus.SUPERSEDED -> Color(0xFF334155) to Color(0xFF94A3B8)
        FinancialReportStatus.FAILED -> Color(0xFF7F1D1D) to Color(0xFFFCA5A5)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = status.defaultLabel,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun FinancialKpiCard(
    title: String,
    amount: Money,
    subtitle: String? = null,
    percentage: Double? = null,
    icon: ImageVector,
    iconBgColor: Color = Color(0xFF1E3A8A),
    iconTint: Color = Color(0xFF60A5FA),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF334155).copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(iconBgColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${amount.formatted()} BDT",
                color = Color(0xFFF8FAFC),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            if (subtitle != null || percentage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (percentage != null) {
                        val isPositive = percentage >= 0
                        Surface(
                            color = if (isPositive) Color(0xFF064E3B) else Color(0xFF7F1D1D),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${if (isPositive) "+" else ""}${String.format("%.1f", percentage)}%",
                                color = if (isPositive) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReportLineRow(
    line: FinancialReportLine,
    modifier: Modifier = Modifier
) {
    val isHeader = line.lineType == FinancialReportLineType.SECTION_HEADER
    val isTotal = line.lineType == FinancialReportLineType.TOTAL || line.lineType == FinancialReportLineType.GRAND_TOTAL
    val isSubtotal = line.lineType == FinancialReportLineType.SUBTOTAL

    val textColor = when {
        isTotal -> Color(0xFF38BDF8)
        isSubtotal -> Color(0xFFF8FAFC)
        isHeader -> Color(0xFF94A3B8)
        else -> Color(0xFFCBD5E1)
    }

    val fontWeight = when {
        isTotal || isHeader -> FontWeight.Bold
        isSubtotal -> FontWeight.SemiBold
        else -> FontWeight.Normal
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = (line.indentLevel * 16).dp,
                top = if (isHeader || isTotal) 10.dp else 6.dp,
                bottom = if (isTotal) 10.dp else 6.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = line.label,
            color = textColor,
            fontSize = if (isTotal) 14.sp else 13.sp,
            fontWeight = fontWeight,
            modifier = Modifier.weight(1f)
        )
        if (!isHeader) {
            Text(
                text = "${line.amount.formatted()} BDT",
                color = textColor,
                fontSize = if (isTotal) 14.sp else 13.sp,
                fontWeight = fontWeight
            )
        }
    }
}

@Composable
fun ControlExceptionBanner(
    exceptions: List<String>,
    modifier: Modifier = Modifier
) {
    if (exceptions.isEmpty()) return
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFDC2626), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFF87171),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Financial Control Exception Detected",
                    color = Color(0xFFF87171),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                exceptions.forEach { err ->
                    Text(
                        text = "• $err",
                        color = Color(0xFFFCA5A5),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
