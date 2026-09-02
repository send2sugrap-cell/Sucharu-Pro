package com.sucharu.sucharupro.ui.features.communication.vendor.document

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.communication.vendor.document.*

private val BgColor = Color(0xFF0F172A)
private val SurfaceColor = Color(0xFF1E293B)
private val AccentColor = Color(0xFF38BDF8)
private val AccentGreen = Color(0xFF22D3EE)
private val AccentAmber = Color(0xFFFBBF24)
private val AccentRed = Color(0xFFF87171)
private val AccentPurple = Color(0xFFA78BFA)
private val TextPrimary = Color(0xFFF1F5F9)
private val TextSecondary = Color(0xFF94A3B8)

@Composable
fun VendorDocStatusBadge(status: VendorDocumentStatus, modifier: Modifier = Modifier) {
    val (color, label) = when (status) {
        VendorDocumentStatus.APPROVED -> Pair(AccentGreen, "Approved")
        VendorDocumentStatus.REJECTED -> Pair(AccentRed, "Rejected")
        VendorDocumentStatus.UNDER_REVIEW -> Pair(AccentAmber, "Under Review")
        VendorDocumentStatus.SUBMITTED -> Pair(AccentColor, "Submitted")
        VendorDocumentStatus.REQUESTED -> Pair(TextSecondary, "Requested")
        VendorDocumentStatus.EXPIRED -> Pair(AccentRed, "Expired")
        VendorDocumentStatus.RENEWAL_REQUIRED -> Pair(AccentAmber, "Renewal Required")
        VendorDocumentStatus.CANCELLED -> Pair(TextSecondary, "Cancelled")
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun VendorComplianceStatusBadge(status: VendorComplianceStatus, modifier: Modifier = Modifier) {
    val (color, label) = when (status) {
        VendorComplianceStatus.COMPLIANT -> Pair(AccentGreen, "Compliant")
        VendorComplianceStatus.PARTIALLY_COMPLIANT -> Pair(AccentAmber, "Partial")
        VendorComplianceStatus.NON_COMPLIANT -> Pair(AccentRed, "Non-Compliant")
        VendorComplianceStatus.UNDER_REVIEW -> Pair(AccentColor, "Under Review")
        VendorComplianceStatus.EXPIRING -> Pair(AccentAmber, "Expiring")
        VendorComplianceStatus.UNKNOWN -> Pair(TextSecondary, "Unknown")
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun VendorExpiryStatusBadge(status: VendorDocumentExpiryStatus, modifier: Modifier = Modifier) {
    val (color, label) = when (status) {
        VendorDocumentExpiryStatus.VALID -> Pair(AccentGreen, "Valid")
        VendorDocumentExpiryStatus.EXPIRING_SOON -> Pair(AccentAmber, "Expiring Soon")
        VendorDocumentExpiryStatus.EXPIRED -> Pair(AccentRed, "Expired")
        VendorDocumentExpiryStatus.NO_EXPIRY -> Pair(TextSecondary, "No Expiry")
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun VendorDocMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color = AccentColor,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                Text(text = title, color = TextSecondary, fontSize = 12.sp)
            }
            Text(text = value, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun VendorDocumentListItem(
    document: VendorDocument,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = document.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(text = document.documentType.defaultLabel, color = TextSecondary, fontSize = 12.sp)
                Text(text = document.documentNo, color = TextSecondary, fontSize = 11.sp)
            }
            VendorDocStatusBadge(status = document.status)
        }
    }
}

@Composable
fun VendorDocumentRequestListItem(
    request: VendorDocumentRequest,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = request.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(text = request.documentType.defaultLabel, color = TextSecondary, fontSize = 12.sp)
                Text(text = request.requestNo, color = TextSecondary, fontSize = 11.sp)
                if (request.isOverdue) {
                    Text(text = "⚠ OVERDUE", color = AccentRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = when (request.status) {
                    VendorDocumentRequestStatus.OPEN -> AccentColor.copy(alpha = 0.15f)
                    VendorDocumentRequestStatus.COMPLETED -> AccentGreen.copy(alpha = 0.15f)
                    VendorDocumentRequestStatus.CANCELLED -> TextSecondary.copy(alpha = 0.15f)
                    else -> AccentAmber.copy(alpha = 0.15f)
                }
            ) {
                Text(
                    text = request.status.defaultLabel,
                    color = when (request.status) {
                        VendorDocumentRequestStatus.OPEN -> AccentColor
                        VendorDocumentRequestStatus.COMPLETED -> AccentGreen
                        VendorDocumentRequestStatus.CANCELLED -> TextSecondary
                        else -> AccentAmber
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun VendorDocTopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(text = title, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Row { actions() }
    }
}
