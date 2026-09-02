package com.sucharu.sucharupro.ui.features.finance.periodclose

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.finance.AccountingPeriod
import com.sucharu.sucharupro.domain.model.finance.AccountingPeriodStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialClosingChecklistItem
import com.sucharu.sucharupro.domain.model.finance.FinancialClosingReadinessStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialPeriodReopenStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AccountingPeriodStatusBadge(
    status: AccountingPeriodStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        AccountingPeriodStatus.OPEN -> Color(0xFF064E3B) to Color(0xFF6EE7B7)
        AccountingPeriodStatus.CLOSING -> Color(0xFF78350F) to Color(0xFFFDE68A)
        AccountingPeriodStatus.CLOSED -> Color(0xFF1E293B) to Color(0xFF94A3B8)
        AccountingPeriodStatus.REOPENED -> Color(0xFF581C87) to Color(0xFFD8B4FE)
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
fun PeriodReopenStatusBadge(
    status: FinancialPeriodReopenStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        FinancialPeriodReopenStatus.PENDING -> Color(0xFF78350F) to Color(0xFFFDE68A)
        FinancialPeriodReopenStatus.APPROVED -> Color(0xFF064E3B) to Color(0xFF6EE7B7)
        FinancialPeriodReopenStatus.REJECTED -> Color(0xFF7F1D1D) to Color(0xFFFCA5A5)
        FinancialPeriodReopenStatus.CANCELLED -> Color(0xFF1E293B) to Color(0xFF94A3B8)
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
fun ChecklistItemCard(
    item: FinancialClosingChecklistItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = if (item.isPassed) Color(0xFF064E3B) else Color(0xFF7F1D1D),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isPassed) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (item.isPassed) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = Color(0xFFF8FAFC),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (item.details.isNotBlank()) {
                    Text(
                        text = item.details,
                        color = if (item.isPassed) Color(0xFF94A3B8) else Color(0xFFFCA5A5),
                        fontSize = 11.sp
                    )
                }
            }

            Surface(
                color = if (item.isPassed) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFF7F1D1D).copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (item.isPassed) "PASSED" else "BLOCKING",
                    color = if (item.isPassed) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
