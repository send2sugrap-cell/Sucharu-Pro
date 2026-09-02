package com.sucharu.sucharupro.ui.features.delivery.pod

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
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofEvidenceType
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofRecipientType
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofStatus
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofType

@Composable
fun DeliveryProofStatusBadge(
    status: DeliveryProofStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        DeliveryProofStatus.DRAFT -> Color(0xFFF5F5F5) to Color(0xFF616161)
        DeliveryProofStatus.PENDING_REVIEW -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
        DeliveryProofStatus.SUBMITTED -> Color(0xFFE1F5FE) to Color(0xFF0277BD)
        DeliveryProofStatus.VERIFIED -> Color(0xFFE0F2F1) to Color(0xFF00695C)
        DeliveryProofStatus.ACCEPTED -> Color(0xFFE8F5E9) to Color(0xFF1B5E20)
        DeliveryProofStatus.REJECTED -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        DeliveryProofStatus.CANCELLED -> Color(0xFFECEFF1) to Color(0xFF455A64)
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = status.defaultLabel,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun DeliveryProofTypeBadge(
    proofType: DeliveryProofType,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFFEDE7F6), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = proofType.defaultLabel,
            color = Color(0xFF512DA8),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DeliveryProofEvidenceTypeBadge(
    evidenceType: DeliveryProofEvidenceType,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (evidenceType) {
        DeliveryProofEvidenceType.SIGNATURE_IMAGE -> Color(0xFFE8EAF6) to Color(0xFF283593)
        DeliveryProofEvidenceType.DELIVERY_PHOTO -> Color(0xFFE0F7FA) to Color(0xFF00838F)
        DeliveryProofEvidenceType.OTP_CONFIRMATION -> Color(0xFFF3E5F5) to Color(0xFF6A1B9A)
        DeliveryProofEvidenceType.SIGNED_DOCUMENT -> Color(0xFFEFEBE9) to Color(0xFF4E342E)
        DeliveryProofEvidenceType.RECIPIENT_ID_CARD -> Color(0xFFFBE9E7) to Color(0xFFD84315)
        DeliveryProofEvidenceType.OTHER -> Color(0xFFECEFF1) to Color(0xFF37474F)
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = evidenceType.defaultLabel,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DeliveryProofRecipientTypeBadge(
    recipientType: DeliveryProofRecipientType,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFFE3F2FD), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = recipientType.defaultLabel,
            color = Color(0xFF1565C0),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}
