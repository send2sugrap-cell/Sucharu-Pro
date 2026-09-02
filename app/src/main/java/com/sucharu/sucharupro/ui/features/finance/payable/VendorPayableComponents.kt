package com.sucharu.sucharupro.ui.features.finance.payable

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
import com.sucharu.sucharupro.domain.model.finance.VendorPayable
import com.sucharu.sucharupro.domain.model.finance.VendorPayableActivityEvent
import com.sucharu.sucharupro.domain.model.finance.VendorPayableAgingBucket
import com.sucharu.sucharupro.domain.model.finance.VendorPayableStatus
import com.sucharu.sucharupro.domain.model.finance.VendorPayableSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VendorPayableStatusBadge(
    status: VendorPayableStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        VendorPayableStatus.DRAFT -> Color(0xFFE2E8F0) to Color(0xFF475569)
        VendorPayableStatus.PENDING -> Color(0xFFFEF3C7) to Color(0xFFD97706)
        VendorPayableStatus.APPROVED -> Color(0xFFDBEAFE) to Color(0xFF1D4ED8)
        VendorPayableStatus.PARTIALLY_SETTLED -> Color(0xFFE0E7FF) to Color(0xFF4338CA)
        VendorPayableStatus.SETTLED -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        VendorPayableStatus.OVERDUE -> Color(0xFFFEE2E2) to Color(0xFFB91C1C)
        VendorPayableStatus.CANCELLED -> Color(0xFFF1F5F9) to Color(0xFF64748B)
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
fun VendorPayableAgingBadge(
    bucket: VendorPayableAgingBucket,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (bucket) {
        VendorPayableAgingBucket.CURRENT -> Color(0xFFECFDF5) to Color(0xFF047857)
        VendorPayableAgingBucket.DAYS_1_TO_30 -> Color(0xFFFEF9C3) to Color(0xFFA16207)
        VendorPayableAgingBucket.DAYS_31_TO_60 -> Color(0xFFFFEDD5) to Color(0xFFC2410C)
        VendorPayableAgingBucket.DAYS_61_TO_90 -> Color(0xFFFEE2E2) to Color(0xFFDC2626)
        VendorPayableAgingBucket.DAYS_OVER_90 -> Color(0xFF7F1D1D) to Color(0xFFFEF2F2)
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = bucket.defaultLabel,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun VendorPayableCard(
    payable: VendorPayable,
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
                    text = "#${payable.payableNo}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                VendorPayableStatusBadge(status = payable.status)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Vendor: ${payable.vendorId}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!payable.supplierInvoiceNo.isNullOrBlank()) {
                Text(
                    text = "Invoice: ${payable.supplierInvoiceNo}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Obligation",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${payable.originalAmount.formatted()} ${payable.currency}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Outstanding Due",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${payable.outstandingAmount.formatted()} ${payable.currency}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (payable.outstandingAmount.isPositive()) Color(0xFFDC2626) else Color(0xFF16A34A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Due: ${dateFormat.format(Date(payable.dueDate))}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                VendorPayableAgingBadge(bucket = payable.agingBucket)
            }
        }
    }
}

@Composable
fun VendorPayableSummaryCard(
    summary: VendorPayableSummary,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Vendor Payable Overview",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Total Liability", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = summary.totalOriginalAmount.formatted(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Column {
                    Text(text = "Settled Paid", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = summary.totalSettledAmount.formatted(), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF16A34A))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Outstanding Due", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = summary.totalOutstandingPayable.formatted(), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFDC2626))
                }
            }

            if (summary.totalOverdueAmount.isPositive()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Overdue (${summary.overduePayablesCount} bills)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFDC2626)
                    )
                    Text(
                        text = summary.totalOverdueAmount.formatted(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFFDC2626)
                    )
                }
            }
        }
    }
}

@Composable
fun VendorPayableActivityTimelineItem(
    event: VendorPayableActivityEvent,
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
