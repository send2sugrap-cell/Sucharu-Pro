package com.sucharu.sucharupro.ui.admin.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.admin.theme.AdminTheme

/**
 * Reusable Empty State component for Admin Panel screens when no items or records match filters.
 *
 * @param message Primary empty message title.
 * @param modifier Optional modifier.
 * @param subtitle Optional secondary explanatory subtitle.
 * @param icon Icon vector for empty state.
 * @param actionLabel Optional action button text.
 * @param onActionClick Optional action button click handler.
 */
@Composable
fun AdminEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector = Icons.Default.Inbox,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    AdminCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = AdminTheme.spacing.xxl
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AdminIconContainer(
                icon = icon,
                iconTint = AdminTheme.colors.secondaryText,
                containerColor = AdminTheme.colors.elevatedSurface,
                borderColor = AdminTheme.colors.border,
                boxSize = 56.dp,
                iconSize = 28.dp
            )

            Spacer(modifier = Modifier.height(AdminTheme.spacing.md))

            Text(
                text = message,
                style = AdminTheme.typography.cardTitle,
                color = AdminTheme.colors.primaryText,
                textAlign = TextAlign.Center
            )

            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(AdminTheme.spacing.xs))
                Text(
                    text = subtitle,
                    style = AdminTheme.typography.body,
                    color = AdminTheme.colors.secondaryText,
                    textAlign = TextAlign.Center
                )
            }

            if (!actionLabel.isNullOrBlank() && onActionClick != null) {
                Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))
                AdminButton(
                    text = actionLabel,
                    onClick = onActionClick,
                    style = AdminButtonStyle.SECONDARY
                )
            }
        }
    }
}
