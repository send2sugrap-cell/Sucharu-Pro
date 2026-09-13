package com.sucharu.sucharupro.ui.admin.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.ui.admin.components.AdminBadge
import com.sucharu.sucharupro.ui.admin.theme.AdminTheme

/**
 * Reusable Admin Top Application Bar providing page context, search, notifications, tenant badge, and profile status.
 *
 * @param title Current screen or section page title.
 * @param modifier Optional modifier.
 * @param subtitle Optional page subtitle or breadcrumb context.
 * @param principal Currently authenticated principal context.
 * @param notificationCount Count of unread system alerts.
 * @param onToggleNavigation Click handler to toggle navigation sidebar/drawer.
 * @param onSearchClick Click handler for global search.
 * @param onNotificationsClick Click handler for notification center.
 */
@Composable
fun AdminTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    principal: AuthenticatedPrincipal? = null,
    notificationCount: Int = 3,
    onToggleNavigation: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
    onNotificationsClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        color = AdminTheme.colors.surface,
        border = BorderStroke(1.dp, AdminTheme.colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AdminTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Area: Toggle Navigation & Title Context
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onToggleNavigation != null) {
                    IconButton(onClick = onToggleNavigation) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Toggle Navigation",
                            tint = AdminTheme.colors.primaryText
                        )
                    }
                    Spacer(modifier = Modifier.width(AdminTheme.spacing.xs))
                }

                Column {
                    Text(
                        text = title,
                        style = AdminTheme.typography.sectionTitle,
                        color = AdminTheme.colors.primaryText
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = AdminTheme.typography.caption,
                            color = AdminTheme.colors.secondaryText
                        )
                    }
                }
            }

            // Right Area: Search, Notifications, Tenant Badge & User Profile
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AdminTheme.spacing.xs)
            ) {
                if (onSearchClick != null) {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = AdminTheme.colors.secondaryText
                        )
                    }
                }

                if (onNotificationsClick != null) {
                    Box {
                        IconButton(onClick = onNotificationsClick) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = AdminTheme.colors.secondaryText
                            )
                        }
                        if (notificationCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 6.dp, end = 6.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AdminTheme.colors.error)
                            )
                        }
                    }
                }

                if (principal != null) {
                    AdminBadge(
                        text = principal.projectId,
                        containerColor = AdminTheme.colors.elevatedSurface,
                        contentColor = AdminTheme.colors.accentPrimary,
                        borderColor = AdminTheme.colors.border
                    )

                    Spacer(modifier = Modifier.width(AdminTheme.spacing.xs))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = AdminTheme.colors.elevatedSurface,
                        border = BorderStroke(1.dp, AdminTheme.colors.border)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(AdminTheme.colors.accentContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = AdminTheme.colors.accentPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = principal.username,
                                style = AdminTheme.typography.caption,
                                color = AdminTheme.colors.primaryText
                            )
                        }
                    }
                }
            }
        }
    }
}
