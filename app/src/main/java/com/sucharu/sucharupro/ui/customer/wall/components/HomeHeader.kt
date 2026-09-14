package com.sucharu.sucharupro.ui.customer.wall.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme

/**
 * Top Header for Redesigned Front-Facing Home / Sucharu Wall.
 *
 * LEFT: Sucharu Graphics / Sucharu Pro brand logo and title
 * RIGHT: Profile avatar action and Notification bell action
 */
@Composable
fun HomeHeader(
    principal: AuthenticatedPrincipal?,
    modifier: Modifier = Modifier,
    notificationCount: Int = 0,
    onProfileClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = CustomerTheme.colors.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CustomerTheme.spacing.lg, vertical = CustomerTheme.spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT: Sucharu Graphics Branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onProfileClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CustomerTheme.colors.accentContainer)
                        .border(1.dp, CustomerTheme.colors.accentPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "Sucharu Graphics Logo",
                        tint = CustomerTheme.colors.accentPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(CustomerTheme.spacing.md))

                Column {
                    Text(
                        text = "SUCHARU GRAPHICS",
                        style = CustomerTheme.typography.title.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = CustomerTheme.colors.primaryText
                    )
                    Text(
                        text = "Commercial Printing ERP",
                        style = CustomerTheme.typography.caption,
                        color = CustomerTheme.colors.secondaryText
                    )
                }
            }

            // RIGHT: Actions (Notification & Profile)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    IconButton(
                        onClick = onNotificationClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = CustomerTheme.colors.primaryText
                        )
                    }
                    if (notificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 4.dp, end = 4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CustomerTheme.colors.error)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(CustomerTheme.spacing.xs))

                IconButton(
                    onClick = onProfileClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CustomerTheme.colors.elevatedSurface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User Profile",
                        tint = CustomerTheme.colors.accentPrimary
                    )
                }
            }
        }
    }
}
