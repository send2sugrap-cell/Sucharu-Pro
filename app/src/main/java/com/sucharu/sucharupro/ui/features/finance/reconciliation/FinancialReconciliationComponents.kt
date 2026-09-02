package com.sucharu.sucharupro.ui.features.finance.reconciliation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.finance.FinancialDiscrepancySeverity
import com.sucharu.sucharupro.domain.model.finance.FinancialDiscrepancyStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliation
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationDiscrepancy
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FinancialReconciliationStatusBadge(
    status: FinancialReconciliationStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        FinancialReconciliationStatus.DRAFT -> Color(0xFF334155) to Color(0xFFCBD5E1)
        FinancialReconciliationStatus.IN_PROGRESS -> Color(0xFF1E3A8A) to Color(0xFF93C5FD)
        FinancialReconciliationStatus.MATCHED -> Color(0xFF064E3B) to Color(0xFF6EE7B7)
        FinancialReconciliationStatus.PARTIALLY_MATCHED -> Color(0xFF78350F) to Color(0xFFFDE68A)
        FinancialReconciliationStatus.MISMATCHED -> Color(0xFF7F1D1D) to Color(0xFFFCA5A5)
        FinancialReconciliationStatus.APPROVED -> Color(0xFF14532D) to Color(0xFF86EFAC)
        FinancialReconciliationStatus.CLOSED -> Color(0xFF0F172A) to Color(0xFF94A3B8)
        FinancialReconciliationStatus.CANCELLED -> Color(0xFF334155) to Color(0xFF64748B)
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Text(
            text = status.defaultLabel,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun DiscrepancySeverityBadge(
    severity: FinancialDiscrepancySeverity,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (severity) {
        FinancialDiscrepancySeverity.LOW -> Color(0xFF1E293B) to Color(0xFF94A3B8)
        FinancialDiscrepancySeverity.MEDIUM -> Color(0xFF78350F) to Color(0xFFFCD34D)
        FinancialDiscrepancySeverity.HIGH -> Color(0xFF9A3412) to Color(0xFFFDBA74)
        FinancialDiscrepancySeverity.CRITICAL -> Color(0xFF7F1D1D) to Color(0xFFFCA5A5)
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Text(
            text = severity.defaultLabel,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun DiscrepancyStatusBadge(
    status: FinancialDiscrepancyStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        FinancialDiscrepancyStatus.OPEN -> Color(0xFF7F1D1D) to Color(0xFFFCA5A5)
        FinancialDiscrepancyStatus.UNDER_REVIEW -> Color(0xFF1E3A8A) to Color(0xFF93C5FD)
        FinancialDiscrepancyStatus.RESOLVED -> Color(0xFF064E3B) to Color(0xFF6EE7B7)
        FinancialDiscrepancyStatus.WAIVED -> Color(0xFF581C87) to Color(0xFFD8B4FE)
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Text(
            text = status.defaultLabel,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun FinancialControlStatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    badgeText: String? = null,
    badgeColor: Color = Color(0xFF10B981),
    isWarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        )
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
                if (badgeText != null) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = if (isWarning) Color(0xFFEF4444) else Color(0xFFF8FAFC),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun ReconciliationItemCard(
    reconciliation: FinancialReconciliation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        )
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
                Column {
                    Text(
                        text = reconciliation.reconciliationNo,
                        color = Color(0xFFF8FAFC),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = reconciliation.reconciliationType.defaultLabel,
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
                FinancialReconciliationStatusBadge(status = reconciliation.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Expected", color = Color(0xFF64748B), fontSize = 11.sp)
                    Text(reconciliation.expectedAmount.formatted(), color = Color(0xFFCBD5E1), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Actual", color = Color(0xFF64748B), fontSize = 11.sp)
                    Text(reconciliation.actualAmount.formatted(), color = Color(0xFFCBD5E1), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Variance", color = Color(0xFF64748B), fontSize = 11.sp)
                    Text(
                        text = reconciliation.differenceAmount.formatted(),
                        color = if (reconciliation.differenceAmount.isZero()) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = dateFormat.format(Date(reconciliation.createdAt)),
                color = Color(0xFF475569),
                fontSize = 10.sp
            )
        }
    }
}
