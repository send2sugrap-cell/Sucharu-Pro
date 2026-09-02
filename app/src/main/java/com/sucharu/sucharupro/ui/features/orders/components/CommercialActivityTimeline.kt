package com.sucharu.sucharupro.ui.features.orders.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
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
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.activity.CommercialActivityEvent
import com.sucharu.sucharupro.domain.model.activity.CommercialActivityType
import com.sucharu.sucharupro.domain.model.activity.CommercialEntityType
import com.sucharu.sucharupro.domain.repository.CommercialActivityRepository
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.LoadingIndicator
import com.sucharu.sucharupro.ui.theme.spacing
import kotlinx.coroutines.flow.map

/**
 * Commercial Activity Timeline composable.
 *
 * - Renders audit events newest-first.
 * - Shows activity label, actor, timestamp, status/value changes, reason/note.
 * - Handles Loading, Empty, and populated states.
 * - Responsive: fills available width with no horizontal clipping.
 * - Bangla/Unicode-safe: uses Text without hardcoded ASCII assumptions.
 * - Does NOT implement complex filter controls.
 *
 * @param activityRepository  The audit trail repository.
 * @param entityType          Commercial entity type to filter events.
 * @param entityId            The entity's primary key.
 * @param modifier            External modifier.
 */
@Composable
fun CommercialActivityTimeline(
    activityRepository: CommercialActivityRepository,
    entityType: CommercialEntityType,
    entityId: String,
    modifier: Modifier = Modifier
) {
    val events by activityRepository
        .observeActivitiesForEntity(entityType, entityId)
        .collectAsState(initial = null)

    Column(modifier = modifier.fillMaxWidth()) {
        when {
            events == null -> {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(message = "Loading activity history…")
                }
            }

            events!!.isEmpty() -> {
                // Empty state
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No activity history recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                // Success state — render events newest-first
                val eventList = events!!
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        eventList.forEachIndexed { index, event ->
                            CommercialActivityTimelineItem(
                                event = event,
                                isLast = index == eventList.lastIndex
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private: Individual timeline row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CommercialActivityTimelineItem(
    event: CommercialActivityEvent,
    isLast: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        // ── Timeline track (icon + vertical line) ──
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = activityTypeColor(event.activityType).copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = activityTypeIcon(event.activityType),
                    contentDescription = event.activityType.defaultLabel,
                    tint = activityTypeColor(event.activityType),
                    modifier = Modifier.size(16.dp)
                )
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(MaterialTheme.spacing.large)
                        .background(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // ── Event content ──
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (!isLast) MaterialTheme.spacing.medium else 0.dp)
        ) {
            // Activity type label
            Text(
                text = event.activityType.defaultLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = activityTypeColor(event.activityType)
            )

            // Actor and timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = event.resolvedActorName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatAuditTimestamp(event.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Status transition: Previous → New
            val prevStatus = event.previousStatus
            val nextStatus = event.newStatus
            if (!prevStatus.isNullOrBlank() || !nextStatus.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!prevStatus.isNullOrBlank()) {
                        StatusChip(
                            text = prevStatus,
                            faded = true
                        )
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "changed to",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!nextStatus.isNullOrBlank()) {
                        StatusChip(text = nextStatus, faded = false)
                    }
                }
            }

            // Value transitions: previousValue → newValue
            val prevVal = event.previousValue
            val nextVal = event.newValue
            if (!prevVal.isNullOrBlank() || !nextVal.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                val valueLine = buildString {
                    if (!prevVal.isNullOrBlank()) append("$prevVal → ")
                    if (!nextVal.isNullOrBlank()) append(nextVal)
                }.trim().trimEnd('→').trim()
                Text(
                    text = valueLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Reason
            if (!event.reason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Reason: ${event.reason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                    fontStyle = FontStyle.Italic
                )
            }

            // Note
            val note = event.note
            if (!note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, faded: Boolean) {
    Box(
        modifier = Modifier
            .background(
                color = if (faded)
                    MaterialTheme.colorScheme.surfaceVariant
                else
                    MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (faded)
                MaterialTheme.colorScheme.onSurfaceVariant
            else
                MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers: Icon and color mapping per activity type
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun activityTypeColor(type: CommercialActivityType) = when (type) {
    CommercialActivityType.CREATED -> MaterialTheme.colorScheme.primary
    CommercialActivityType.VIEWED -> MaterialTheme.colorScheme.outline
    CommercialActivityType.STATUS_CHANGED -> MaterialTheme.colorScheme.secondary
    CommercialActivityType.REVISED -> MaterialTheme.colorScheme.tertiary
    CommercialActivityType.APPROVED -> MaterialTheme.colorScheme.primary
    CommercialActivityType.REJECTED -> MaterialTheme.colorScheme.error
    CommercialActivityType.CANCELLED -> MaterialTheme.colorScheme.error
    CommercialActivityType.ORDER_CONVERTED -> MaterialTheme.colorScheme.primary
    CommercialActivityType.PRIORITY_CHANGED -> MaterialTheme.colorScheme.tertiary
    CommercialActivityType.HANDOFF_READY -> MaterialTheme.colorScheme.primary
    CommercialActivityType.NOTES_UPDATED -> MaterialTheme.colorScheme.secondary
}

private fun activityTypeIcon(type: CommercialActivityType): ImageVector = when (type) {
    CommercialActivityType.CREATED -> Icons.Default.Create
    CommercialActivityType.VIEWED -> Icons.Default.Visibility
    CommercialActivityType.STATUS_CHANGED -> Icons.Default.SwapHoriz
    CommercialActivityType.REVISED -> Icons.Default.Edit
    CommercialActivityType.APPROVED -> Icons.Default.CheckCircle
    CommercialActivityType.REJECTED -> Icons.Default.Cancel
    CommercialActivityType.CANCELLED -> Icons.Default.Cancel
    CommercialActivityType.ORDER_CONVERTED -> Icons.Default.ShoppingCart
    CommercialActivityType.PRIORITY_CHANGED -> Icons.Default.PriorityHigh
    CommercialActivityType.HANDOFF_READY -> Icons.Default.AssignmentTurnedIn
    CommercialActivityType.NOTES_UPDATED -> Icons.Default.Notes
}

/**
 * Formats an ISO-8601 timestamp for display in the timeline.
 * Handles null/blank safely, always producing a displayable string.
 */
private fun formatAuditTimestamp(timestamp: String): String {
    if (timestamp.isBlank()) return "—"
    return try {
        // Parse ISO-8601 and format as "dd MMM yyyy, HH:mm"
        val instant = java.time.Instant.parse(timestamp)
        val zdt = instant.atZone(java.time.ZoneId.systemDefault())
        val formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
        zdt.format(formatter)
    } catch (_: Exception) {
        // Fallback: show raw timestamp (handles any custom format)
        timestamp.take(16).replace("T", " ")
    }
}
