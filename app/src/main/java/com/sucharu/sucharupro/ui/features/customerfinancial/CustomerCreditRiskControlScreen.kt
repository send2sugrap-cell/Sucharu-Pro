package com.sucharu.sucharupro.ui.features.customerfinancial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.data.api.model.AgingBucketSummaryDto
import com.sucharu.sucharupro.data.api.model.CustomerCreditProfileDto
import com.sucharu.sucharupro.data.api.model.CustomerReceivableAgingReportDto
import com.sucharu.sucharupro.data.api.model.CustomerReceivableRiskSummaryDto
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.theme.spacing
import java.math.BigDecimal

/**
 * Customer Credit Limit, Payment Terms & Receivable Risk Control UI (Module 14 Step 07).
 */
@Composable
fun CustomerCreditRiskControlScreen(
    profile: CustomerCreditProfileDto?,
    riskSummary: CustomerReceivableRiskSummaryDto?,
    agingReport: CustomerReceivableAgingReportDto?,
    onEditProfileClick: () -> Unit = {},
    onPlaceHoldClick: () -> Unit = {},
    onReleaseHoldClick: () -> Unit = {},
    onCreditCheckClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        item {
            HeaderSection(
                profile = profile,
                riskSummary = riskSummary,
                onEditProfileClick = onEditProfileClick,
                onPlaceHoldClick = onPlaceHoldClick,
                onReleaseHoldClick = onReleaseHoldClick,
                onCreditCheckClick = onCreditCheckClick
            )
        }

        if (riskSummary != null) {
            item {
                ExposureAndLimitKpiSection(riskSummary)
            }
        }

        if (agingReport != null) {
            item {
                Text(
                    text = "Receivable Aging Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(agingReport.buckets) { bucket ->
                AgingBucketCard(bucket)
            }
        }
    }
}

@Composable
private fun HeaderSection(
    profile: CustomerCreditProfileDto?,
    riskSummary: CustomerReceivableRiskSummaryDto?,
    onEditProfileClick: () -> Unit,
    onPlaceHoldClick: () -> Unit,
    onReleaseHoldClick: () -> Unit,
    onCreditCheckClick: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Credit & Risk Management",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Customer: ${profile?.customerId ?: "-"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val riskStatus = riskSummary?.riskStatus ?: "NORMAL"
                val (statusColor, textColor) = when (riskStatus) {
                    "FINANCIAL_HOLD" -> Color(0xFFB71C1C) to Color.White
                    "OVERDUE", "OVER_LIMIT" -> Color(0xFFC62828) to Color.White
                    "LIMIT_REACHED", "WATCH" -> Color(0xFFF57F17) to Color.Black
                    "ADVANCE_REQUIRED" -> Color(0xFF1565C0) to Color.White
                    else -> Color(0xFF2E7D32) to Color.White
                }

                Box(
                    modifier = Modifier
                        .background(statusColor, shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = riskStatus,
                        color = textColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEditProfileClick) {
                    Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Edit Terms")
                }

                if (profile?.financialHold == true) {
                    Button(onClick = onReleaseHoldClick) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Release Hold")
                    }
                } else {
                    OutlinedButton(onClick = onPlaceHoldClick) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Place Hold")
                    }
                }

                Button(onClick = onCreditCheckClick) {
                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Credit Check")
                }
            }
        }
    }
}

@Composable
private fun ExposureAndLimitKpiSection(risk: CustomerReceivableRiskSummaryDto) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KpiCard(
                title = "Approved Limit",
                amount = risk.creditLimit,
                icon = Icons.Default.CreditCard,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Available Credit",
                amount = risk.availableCreditLimit,
                icon = Icons.Default.Security,
                color = Color(0xFF2E7D32),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KpiCard(
                title = "Net Exposure",
                amount = risk.netReceivableExposure,
                icon = Icons.Default.Warning,
                color = Color(0xFFE65100),
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Overdue Amount",
                amount = risk.overdueAmount,
                icon = Icons.Default.Timer,
                color = Color(0xFFC62828),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    amount: BigDecimal,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.size(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "৳ ${amount.toPlainString()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun AgingBucketCard(bucket: AgingBucketSummaryDto) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = bucket.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${bucket.invoiceCount} invoices",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "৳ ${bucket.outstandingAmount.toPlainString()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (bucket.bucket == "CURRENT") Color(0xFF2E7D32) else Color(0xFFC62828)
            )
        }
    }
}
