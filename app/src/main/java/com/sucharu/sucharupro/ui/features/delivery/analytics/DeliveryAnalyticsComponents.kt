package com.sucharu.sucharupro.ui.features.delivery.analytics

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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlert
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlertCategory
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlertSeverity
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlertStatus
import com.sucharu.sucharupro.ui.theme.spacing

@Composable
fun DeliveryAnalyticsMetricCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(contentColor.copy(alpha = 0.12f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun DeliveryGovernanceAlertSeverityBadge(
    severity: DeliveryGovernanceAlertSeverity,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (severity) {
        DeliveryGovernanceAlertSeverity.CRITICAL -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        DeliveryGovernanceAlertSeverity.WARNING -> Color(0xFFFFF3E0) to Color(0xFFEF6C00)
        DeliveryGovernanceAlertSeverity.INFO -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Text(
            text = severity.label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun DeliveryGovernanceAlertStatusBadge(
    status: DeliveryGovernanceAlertStatus,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (status) {
        DeliveryGovernanceAlertStatus.OPEN -> Color(0xFFFFEBEE) to Color(0xFFD32F2F)
        DeliveryGovernanceAlertStatus.ACKNOWLEDGED -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
        DeliveryGovernanceAlertStatus.RESOLVED -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        DeliveryGovernanceAlertStatus.DISMISSED -> Color(0xFFECEFF1) to Color(0xFF546E7A)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Text(
            text = status.label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun DeliveryGovernanceAlertCard(
    alert: DeliveryGovernanceAlert,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DeliveryGovernanceAlertSeverityBadge(severity = alert.severity)
                    Spacer(modifier = Modifier.width(8.dp))
                    DeliveryGovernanceAlertStatusBadge(status = alert.status)
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "View Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = alert.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = alert.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Reference: ${alert.referenceType} #${alert.referenceId}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
