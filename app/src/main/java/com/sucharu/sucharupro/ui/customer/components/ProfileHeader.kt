package com.sucharu.sucharupro.ui.customer.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
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
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme

/**
 * Mobile Profile Header for Customer & Affiliate experiences.
 */
@Composable
fun ProfileHeader(
    principal: AuthenticatedPrincipal?,
    modifier: Modifier = Modifier,
    notificationCount: Int = 0,
    onNotificationsClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = CustomerTheme.colors.surface,
        border = BorderStroke(1.dp, CustomerTheme.colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CustomerTheme.spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CustomerTheme.colors.accentContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile Avatar",
                        tint = CustomerTheme.colors.accentPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(CustomerTheme.spacing.md))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Hello, ${principal?.username ?: "Guest"}",
                            style = CustomerTheme.typography.title,
                            color = CustomerTheme.colors.primaryText
                        )
                        Spacer(modifier = Modifier.width(CustomerTheme.spacing.xs))
                        val roleTag = if (principal?.role == UserRole.AFFILIATE) "AFFILIATE" else "CUSTOMER"
                        StatusChip(label = roleTag, dotColor = CustomerTheme.colors.accentPrimary)
                    }
                    Text(
                        text = "Account: ${principal?.effectiveCustomerId ?: principal?.effectiveAffiliateId ?: "Personal Workspace"}",
                        style = CustomerTheme.typography.caption,
                        color = CustomerTheme.colors.secondaryText
                    )
                }
            }

            if (onNotificationsClick != null) {
                Box {
                    IconButton(onClick = onNotificationsClick) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = CustomerTheme.colors.secondaryText
                        )
                    }
                    if (notificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 6.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CustomerTheme.colors.error)
                        )
                    }
                }
            }
        }
    }
}
