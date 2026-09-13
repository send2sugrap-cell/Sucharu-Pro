package com.sucharu.sucharupro.ui.admin.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.ui.admin.components.AdminBadge
import com.sucharu.sucharupro.ui.admin.theme.AdminTheme
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Reusable Admin Navigation Sidebar providing grouped navigation, active destination highlighting, and collapsible groups.
 *
 * @param currentDestination Active destination route.
 * @param principal Currently authenticated principal for capability filtering.
 * @param modifier Optional modifier.
 * @param isCompact Compact icon-only rail mode when true.
 * @param onDestinationSelect Selection handler when a navigation item is clicked.
 * @param onToggleCompact Click handler to toggle compact mode.
 */
@Composable
fun AdminSidebar(
    currentDestination: AppDestination,
    principal: AuthenticatedPrincipal?,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    onDestinationSelect: (destination: AppDestination) -> Unit,
    onToggleCompact: (() -> Unit)? = null
) {
    val authorizedGroups = remember(principal) {
        AdminNavigationRegistry.getAuthorizedGroups(principal)
    }

    val expandedGroups = remember {
        mutableStateMapOf<String, Boolean>().apply {
            authorizedGroups.forEach { put(it.groupId, true) }
        }
    }

    val sidebarWidth = if (isCompact) 72.dp else 260.dp

    Surface(
        modifier = modifier
            .width(sidebarWidth)
            .fillMaxHeight(),
        color = AdminTheme.colors.surface,
        border = BorderStroke(1.dp, AdminTheme.colors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = AdminTheme.spacing.md)
        ) {
            // Sidebar Header / Brand Logo Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AdminTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isCompact) Arrangement.Center else Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AdminTheme.colors.accentContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Sucharu Pro Logo",
                            tint = AdminTheme.colors.accentPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (!isCompact) {
                        Spacer(modifier = Modifier.width(AdminTheme.spacing.sm))
                        Column {
                            Text(
                                text = "SUCHARU PRO",
                                style = AdminTheme.typography.cardTitle,
                                color = AdminTheme.colors.primaryText,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Printing ERP Admin",
                                style = AdminTheme.typography.caption,
                                color = AdminTheme.colors.secondaryText
                            )
                        }
                    }
                }

                if (!isCompact && onToggleCompact != null) {
                    IconButton(onClick = onToggleCompact) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Compact Sidebar",
                            tint = AdminTheme.colors.secondaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))

            // Navigation Groups List
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                authorizedGroups.forEach { group ->
                    val isGroupExpanded = expandedGroups[group.groupId] ?: true

                    if (!isCompact) {
                        // Group Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedGroups[group.groupId] = !isGroupExpanded }
                                .padding(
                                    horizontal = AdminTheme.spacing.md,
                                    vertical = AdminTheme.spacing.xs
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = group.title,
                                style = AdminTheme.typography.caption,
                                color = AdminTheme.colors.mutedText,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = if (isGroupExpanded) Icons.Default.ExpandMore else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = AdminTheme.colors.mutedText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = isGroupExpanded || isCompact) {
                        Column {
                            group.items.forEach { item ->
                                val isSelected = currentDestination.route == item.destination.route
                                SidebarNavItemRow(
                                    item = item,
                                    isSelected = isSelected,
                                    isCompact = isCompact,
                                    onClick = { onDestinationSelect(item.destination) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(AdminTheme.spacing.sm))
                }
            }
        }
    }
}

@Composable
private fun SidebarNavItemRow(
    item: AdminNavItem,
    isSelected: Boolean,
    isCompact: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) AdminTheme.colors.accentContainer else Color.Transparent
    val contentColor = if (isSelected) AdminTheme.colors.accentPrimary else AdminTheme.colors.secondaryText

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isCompact) AdminTheme.spacing.xs else AdminTheme.spacing.sm,
                vertical = 2.dp
            )
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AdminTheme.spacing.md,
                vertical = AdminTheme.spacing.sm
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isCompact) Arrangement.Center else Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.destination.title,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )

                if (!isCompact) {
                    Spacer(modifier = Modifier.width(AdminTheme.spacing.md))
                    Text(
                        text = item.destination.title,
                        style = AdminTheme.typography.body,
                        color = contentColor,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }

            if (!isCompact && item.badgeText != null) {
                AdminBadge(
                    text = item.badgeText,
                    containerColor = if (isSelected) AdminTheme.colors.accentPrimary.copy(alpha = 0.2f) else AdminTheme.colors.elevatedSurface,
                    contentColor = if (isSelected) AdminTheme.colors.accentPrimary else AdminTheme.colors.secondaryText,
                    borderColor = null
                )
            }
        }
    }
}
