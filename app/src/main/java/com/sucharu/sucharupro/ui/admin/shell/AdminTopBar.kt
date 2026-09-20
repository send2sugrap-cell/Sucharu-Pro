package com.sucharu.sucharupro.ui.admin.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal

/**
 * Reusable Admin Top Application Bar matching reference design image with tight typography and zero awkward text wrapping.
 */
@Composable
fun AdminTopBar(
    title: String = "ADMIN OPERATIONS CENTER",
    modifier: Modifier = Modifier,
    subtitle: String? = "নিয়ন্ত্রণ • সমন্বয় • উৎপাদন • উন্নত ভবিষ্যৎ",
    principal: AuthenticatedPrincipal? = null,
    notificationCount: Int = 3,
    onToggleNavigation: (() -> Unit)? = null,
    onNotificationsClick: (() -> Unit)? = null
) {
    val configuration = LocalConfiguration.current
    val isCompactScreen = configuration.screenWidthDp < 600

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        color = Color(0xFF070E1E),
        border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Area: Toggle Navigation & Operations Title Context
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                if (onToggleNavigation != null) {
                    IconButton(onClick = onToggleNavigation, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Toggle Navigation",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        text = subtitle ?: "নিয়ন্ত্রণ • সমন্বয় • উৎপাদন • উন্নত ভবিষ্যৎ",
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right Area: Date Range Selector, Notifications & Super Admin Profile
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Date Range Filter Box (Pill view on desktop, icon only on compact screens)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Date Range",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(13.dp)
                        )
                        if (!isCompactScreen) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "২২ এপ্রিল ২০২৬ - ২২ এপ্রিল ২০২৬",
                                fontSize = 9.5.sp,
                                lineHeight = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Notification Bell with Badge Count (3)
                Box(contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = { onNotificationsClick?.invoke() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (notificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 1.dp, end = 1.dp)
                                .size(13.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = notificationCount.toString(),
                                fontSize = 7.5.sp,
                                lineHeight = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Super Admin Profile Avatar
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00B4D8).copy(alpha = 0.3f))
                                .border(1.dp, Color(0xFF38BDF8), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        if (!isCompactScreen) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = principal?.username ?: "Admin",
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Super Admin",
                                    fontSize = 7.5.sp,
                                    lineHeight = 8.sp,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
