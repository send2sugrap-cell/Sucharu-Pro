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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
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
import com.sucharu.sucharupro.domain.model.customer.CustomerActivity
import com.sucharu.sucharupro.domain.model.customer.CustomerActivityType
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.SectionHeader
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Customer Operational Activity Timeline Section.
 */
@Composable
fun CustomerActivitySection(
    activities: List<CustomerActivity>,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(MaterialTheme.spacing.large)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(
                title = "Recent Activity & Timeline",
                subtitle = "Customer management event history and updates"
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            if (activities.isEmpty()) {
                ActivityEmptyView()
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                ) {
                    activities.take(8).forEach { activity ->
                        CustomerActivityItem(activity = activity)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerActivityItem(
    activity: CustomerActivity,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        val icon = activityIcon(activity.type)
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.description,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatIsoDate(activity.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!activity.actorName.isNullOrBlank()) {
                    Text(
                        text = "• by ${activity.actorName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityEmptyView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .padding(vertical = MaterialTheme.spacing.large, horizontal = MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
        Text(
            text = "No recent activity recorded",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun activityIcon(type: CustomerActivityType): ImageVector {
    return when (type) {
        CustomerActivityType.CUSTOMER_CREATED -> Icons.Default.Person
        CustomerActivityType.CUSTOMER_UPDATED -> Icons.Default.EditNote
        CustomerActivityType.CONTACT_UPDATED  -> Icons.Default.Call
        CustomerActivityType.NOTE_ADDED       -> Icons.Default.EditNote
        CustomerActivityType.NOTE_UPDATED     -> Icons.Default.EditNote
        CustomerActivityType.NOTE_DELETED     -> Icons.Default.EditNote
        CustomerActivityType.STATUS_CHANGED   -> Icons.Default.CheckCircle
        CustomerActivityType.TYPE_CHANGED     -> Icons.Default.SwapHoriz
        CustomerActivityType.FOLLOW_UP_SCHEDULED -> Icons.Default.CalendarMonth
        CustomerActivityType.FOLLOW_UP_CLEARED   -> Icons.Default.CalendarMonth
    }
}

private fun formatIsoDate(isoString: String): String {
    return isoString.replace("T", " ").replace("Z", "")
}
