package com.sucharu.sucharupro.ui.features.delivery.verification

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerification
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationIssueType
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationResultType
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeliveryItemVerificationStatusBadge(
    status: DeliveryItemVerificationStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        DeliveryItemVerificationStatus.DRAFT -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        DeliveryItemVerificationStatus.PENDING -> Color(0xFFFFF9C4) to Color(0xFFF57F17)
        DeliveryItemVerificationStatus.IN_PROGRESS -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        DeliveryItemVerificationStatus.VERIFIED -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        DeliveryItemVerificationStatus.CLOSED -> Color(0xFFEDE7F6) to Color(0xFF4527A0)
        DeliveryItemVerificationStatus.CANCELLED -> Color(0xFFF5F5F5) to Color(0xFF757575)
    }

    Box(
        modifier = modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.defaultLabel,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun DeliveryItemVerificationResultBadge(
    resultType: DeliveryItemVerificationResultType,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (resultType) {
        DeliveryItemVerificationResultType.VERIFIED -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        DeliveryItemVerificationResultType.SHORT -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        DeliveryItemVerificationResultType.EXCESS -> Color(0xFFE1F5FE) to Color(0xFF0288D1)
        DeliveryItemVerificationResultType.MISMATCH -> Color(0xFFFCE4EC) to Color(0xFFC2185B)
        DeliveryItemVerificationResultType.DAMAGED -> Color(0xFFFFEBEE) to Color(0xFFD32F2F)
        DeliveryItemVerificationResultType.MISSING -> Color(0xFFFFEBEE) to Color(0xFFC62828)
    }

    Box(
        modifier = modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = resultType.defaultLabel,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun DeliveryItemVerificationIssueBadge(
    issueType: DeliveryItemVerificationIssueType,
    modifier: Modifier = Modifier
) {
    if (issueType == DeliveryItemVerificationIssueType.NONE) return

    Box(
        modifier = modifier
            .background(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = issueType.defaultLabel,
            color = Color(0xFFD32F2F),
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun DeliveryItemVerificationSummaryCard(
    verification: DeliveryItemVerification,
    hasDiscrepancies: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hasDiscrepancies) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (hasDiscrepancies) Color(0xFFE65100) else Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = verification.verificationNo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                DeliveryItemVerificationStatusBadge(status = verification.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Dispatch: ${verification.dispatchExecutionId.take(10)}... • Challan: ${verification.deliveryChallanId.take(8)}...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Created: ${dateFormat.format(Date(verification.createdAt))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hasDiscrepancies) "Discrepancies Reported" else "Items Reconciled",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (hasDiscrepancies) Color(0xFFE65100) else Color(0xFF2E7D32)
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
