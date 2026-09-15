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
 * Top Header Bar for Front-Facing Home / Sucharu Wall.
 *
 * LEFT: Sucharu Graphics logo icon and stacked title/subtitle
 * LINE 1: "সুচারু গ্রাফিক্স" (Bold)
 * LINE 2: "এন্ড প্রিন্টিং" (Subtle subtitle)
 * RIGHT: Notification Bell and Profile Avatar
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
            // LEFT: Company Logo & Stacked Title Layout
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onProfileClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CustomerTheme.colors.accentContainer)
                        .border(1.dp, CustomerTheme.colors.accentPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "Sucharu Graphics Logo",
                        tint = CustomerTheme.colors.accentPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(CustomerTheme.spacing.md))

                Column {
                    Text(
                        text = "সুচারু গ্রাফিক্স",
                        style = CustomerTheme.typography.title.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 0.2.sp
                        ),
                        color = CustomerTheme.colors.primaryText
                    )
                    Text(
                        text = "এন্ড প্রিন্টিং",
                        style = CustomerTheme.typography.caption.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
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
