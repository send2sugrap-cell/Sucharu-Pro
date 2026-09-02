package com.sucharu.sucharupro.ui.features.finance.supplierpayment

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.sucharu.sucharupro.domain.model.finance.SupplierPayment
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentActivityEvent
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentMethod
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentSettlement
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SupplierPaymentStatusBadge(
    status: SupplierPaymentStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        SupplierPaymentStatus.DRAFT -> Color(0xFFE2E8F0) to Color(0xFF475569)
        SupplierPaymentStatus.PENDING -> Color(0xFFFEF3C7) to Color(0xFFD97706)
        SupplierPaymentStatus.APPROVED -> Color(0xFFDBEAFE) to Color(0xFF1D4ED8)
        SupplierPaymentStatus.POSTED -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        SupplierPaymentStatus.REJECTED -> Color(0xFFFEE2E2) to Color(0xFFB91C1C)
        SupplierPaymentStatus.CANCELLED -> Color(0xFFF1F5F9) to Color(0xFF64748B)
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
fun SupplierPaymentMethodBadge(
    method: SupplierPaymentMethod,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = method.defaultLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun SupplierPaymentCard(
    payment: SupplierPayment,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                    text = "#${payment.paymentNo}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                SupplierPaymentStatusBadge(status = payment.status)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Vendor: ${payment.vendorId}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            if (!payment.paymentReference.isNullOrBlank()) {
                Text(
                    text = "Ref: ${payment.paymentReference}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Disbursed Amount",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${payment.amount.formatted()} ${payment.currency}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF16A34A)
                    )
                }
                SupplierPaymentMethodBadge(method = payment.paymentMethod)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Date: ${dateFormat.format(Date(payment.paymentDate))}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun SupplierPaymentSettlementCard(
    settlement: SupplierPaymentSettlement,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Settlement Record",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = dateFormat.format(Date(settlement.settlementDate)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Previous Outstanding", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = settlement.previousOutstanding.formatted(), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Column {
                    Text(text = "Settled Amount", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = settlement.settledAmount.formatted(), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF16A34A))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Remaining Outstanding", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = settlement.newOutstanding.formatted(), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFDC2626))
                }
            }
        }
    }
}

@Composable
fun SupplierPaymentActivityTimelineItem(
    event: SupplierPaymentActivityEvent,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = event.activityType.defaultLabel,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    text = dateFormat.format(Date(event.timestamp)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = event.details,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "By: ${event.actorId}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
