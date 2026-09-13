package com.sucharu.sucharupro.ui.admin.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.ui.admin.components.AdminButton
import com.sucharu.sucharupro.ui.admin.components.AdminButtonStyle
import com.sucharu.sucharupro.ui.admin.components.AdminCard
import com.sucharu.sucharupro.ui.admin.components.AdminIconContainer
import com.sucharu.sucharupro.ui.admin.theme.AdminTheme
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Access Denied (403 Forbidden) presentation card rendered when a destination is unauthorized for the principal's role.
 *
 * @param destination Target destination attempted.
 * @param principal Currently authenticated principal.
 * @param modifier Optional modifier.
 * @param onReturnHome Action handler to return to an authorized home destination.
 */
@Composable
fun AdminAccessDeniedContent(
    destination: AppDestination,
    principal: AuthenticatedPrincipal?,
    modifier: Modifier = Modifier,
    onReturnHome: (() -> Unit)? = null
) {
    AdminCard(
        modifier = modifier.fillMaxWidth(),
        accentBarColor = AdminTheme.colors.error,
        contentPadding = AdminTheme.spacing.xxl
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AdminIconContainer(
                icon = Icons.Default.Lock,
                iconTint = AdminTheme.colors.error,
                containerColor = AdminTheme.colors.errorContainer,
                borderColor = AdminTheme.colors.error
            )

            Spacer(modifier = Modifier.height(AdminTheme.spacing.md))

            Text(
                text = "403 — Access Denied",
                style = AdminTheme.typography.cardTitle,
                color = AdminTheme.colors.primaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(AdminTheme.spacing.xs))

            val roleName = principal?.role?.name ?: "UNAUTHENTICATED"
            Text(
                text = "Your active account role ('$roleName') lacks required capability '${destination.requiredCapability?.name ?: "ADMIN_ALL"}' to access '${destination.title}'.",
                style = AdminTheme.typography.body,
                color = AdminTheme.colors.secondaryText,
                textAlign = TextAlign.Center
            )

            if (onReturnHome != null) {
                Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))
                AdminButton(
                    text = "Return to Authorized Workspace",
                    onClick = onReturnHome,
                    style = AdminButtonStyle.SECONDARY
                )
            }
        }
    }
}
