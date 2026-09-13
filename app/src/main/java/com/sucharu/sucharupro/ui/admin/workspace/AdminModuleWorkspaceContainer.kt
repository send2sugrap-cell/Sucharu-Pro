package com.sucharu.sucharupro.ui.admin.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.ui.admin.components.AdminBadge
import com.sucharu.sucharupro.ui.admin.components.AdminButton
import com.sucharu.sucharupro.ui.admin.components.AdminButtonStyle
import com.sucharu.sucharupro.ui.admin.components.AdminCard
import com.sucharu.sucharupro.ui.admin.components.AdminCardSkeleton
import com.sucharu.sucharupro.ui.admin.components.AdminEmptyState
import com.sucharu.sucharupro.ui.admin.components.AdminIconContainer
import com.sucharu.sucharupro.ui.admin.components.AdminKpiGridSkeleton
import com.sucharu.sucharupro.ui.admin.theme.AdminTheme
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Reusable Unified Admin Module Workspace Container.
 *
 * Wraps canonical Module 00–24 screens with a consistent page header, breadcrumb path,
 * capability badge, status chips, loading/empty/error state slots, and Phase 01 design tokens.
 *
 * @param title Module page title.
 * @param subtitle Module canonical authority description (e.g. "Module 04 Production Execution & 13 Stages").
 * @param icon Section icon.
 * @param canonicalModule Code string (e.g. "Module 04").
 * @param requiredCapability Security capability string.
 * @param principal Currently authenticated principal context.
 * @param modifier Optional modifier.
 * @param isLoading Displays skeleton loader when true.
 * @param errorMessage Renders error card with retry handler when non-null.
 * @param onRetry Optional retry handler.
 * @param onNavigateTo Destination click handler.
 * @param actionSlot Optional trailing header action control.
 * @param content Screen body content.
 */
@Composable
fun AdminModuleWorkspaceContainer(
    title: String,
    subtitle: String,
    icon: ImageVector,
    canonicalModule: String,
    requiredCapability: String,
    principal: AuthenticatedPrincipal?,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    onNavigateTo: ((destination: AppDestination) -> Unit)? = null,
    actionSlot: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    AdminTheme {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Page Header Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AdminIconContainer(
                        icon = icon,
                        iconTint = AdminTheme.colors.accentPrimary,
                        containerColor = AdminTheme.colors.accentContainer,
                        boxSize = 48.dp,
                        iconSize = 24.dp
                    )
                    Spacer(modifier = Modifier.width(AdminTheme.spacing.md))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                style = AdminTheme.typography.pageTitle,
                                color = AdminTheme.colors.primaryText
                            )
                            Spacer(modifier = Modifier.width(AdminTheme.spacing.sm))
                            AdminBadge(
                                text = canonicalModule,
                                containerColor = AdminTheme.colors.accentContainer,
                                contentColor = AdminTheme.colors.accentPrimary,
                                borderColor = AdminTheme.colors.accentPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$subtitle • Required Capability: $requiredCapability",
                            style = AdminTheme.typography.caption,
                            color = AdminTheme.colors.secondaryText
                        )
                    }
                }

                if (actionSlot != null) {
                    actionSlot()
                }
            }

            Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))

            // 2. State-Driven Content Rendering
            when {
                isLoading -> {
                    AdminKpiGridSkeleton(count = 2)
                    Spacer(modifier = Modifier.height(AdminTheme.spacing.md))
                    AdminCardSkeleton(cardHeight = 180.dp)
                }

                errorMessage != null -> {
                    AdminCard(
                        accentBarColor = AdminTheme.colors.error,
                        contentPadding = AdminTheme.spacing.xl
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AdminIconContainer(
                                icon = icon,
                                iconTint = AdminTheme.colors.error,
                                containerColor = AdminTheme.colors.errorContainer,
                                borderColor = AdminTheme.colors.error
                            )
                            Spacer(modifier = Modifier.height(AdminTheme.spacing.md))
                            Text(
                                text = "Unable to Load Module Data",
                                style = AdminTheme.typography.cardTitle,
                                color = AdminTheme.colors.primaryText
                            )
                            Spacer(modifier = Modifier.height(AdminTheme.spacing.xs))
                            Text(
                                text = errorMessage,
                                style = AdminTheme.typography.body,
                                color = AdminTheme.colors.secondaryText
                            )
                            if (onRetry != null) {
                                Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))
                                AdminButton(
                                    text = "Retry Action",
                                    onClick = onRetry,
                                    style = AdminButtonStyle.SECONDARY
                                )
                            }
                        }
                    }
                }

                else -> {
                    content()
                }
            }

            Spacer(modifier = Modifier.height(AdminTheme.spacing.xxl))
        }
    }
}
