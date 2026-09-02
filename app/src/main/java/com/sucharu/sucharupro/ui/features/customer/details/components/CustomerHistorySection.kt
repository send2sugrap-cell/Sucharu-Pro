package com.sucharu.sucharupro.ui.features.customer.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.SectionHeader
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Foundation history component presenting lifecycle and activity records derived from the customer domain.
 *
 * NOTE: This is a foundation-level activity timeline. Future business transactions (orders, jobs, invoices)
 * will integrate with this timeline as respective modules are completed.
 */
@Composable
fun CustomerHistorySection(
    customer: Customer,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(MaterialTheme.spacing.large)
    ) {
        SectionHeader(
            title = "Activity & History",
            subtitle = "Lifecycle events & operational audit records"
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
            // 1. Initial Creation Event
            HistoryTimelineItem(
                icon = Icons.Default.CheckCircle,
                title = "Customer Profile Created",
                timestamp = customer.createdAt,
                description = "Account registered as ${customer.customerType.defaultLabel} customer (${customer.status.defaultLabel}).",
                isLast = customer.updatedAt == customer.createdAt
            )

            // 2. Profile Update Event (if updatedAt differs from createdAt)
            if (customer.updatedAt.isNotBlank() && customer.updatedAt != customer.createdAt) {
                HistoryTimelineItem(
                    icon = Icons.Default.Edit,
                    title = "Profile Information Updated",
                    timestamp = customer.updatedAt,
                    description = "Customer records, contacts, or addresses were modified.",
                    isLast = true
                )
            }

            // 3. Informational Empty-Activity Note
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(MaterialTheme.spacing.medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                Text(
                    text = "No transactional activity recorded yet. Orders, jobs, and invoices will appear in this timeline as business is conducted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HistoryTimelineItem(
    icon: ImageVector,
    title: String,
    timestamp: String,
    description: String,
    isLast: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(14.dp)
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = timestamp.take(10), // Safe date substring
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
