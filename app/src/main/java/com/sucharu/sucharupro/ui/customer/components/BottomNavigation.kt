package com.sucharu.sucharupro.ui.customer.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.UserRole

/**
 * Mobile Bottom Navigation tab items.
 */
enum class CustomerBottomTab(
    val title: String,
    val icon: ImageVector,
    val route: String
) {
    HOME("হোম", Icons.Default.Home, "public/home"),
    AI_ASSISTANT("সুচারু এআই", Icons.Default.AutoAwesome, "ai-assistant")
}

/**
 * Premium Glowing Bottom Navigation Bar matching reference design image (AI Center Dock with arched glowing top border).
 */
@Composable
fun CustomerBottomNavigation(
    selectedTab: CustomerBottomTab = CustomerBottomTab.HOME,
    onTabSelect: (tab: CustomerBottomTab) -> Unit,
    modifier: Modifier = Modifier,
    userRole: UserRole? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        color = Color(0xFF0B132B),
        border = BorderStroke(1.5.dp, Color(0xFF00B4D8).copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Item: Home Anchor
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onTabSelect(CustomerBottomTab.HOME) }
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "হোম",
                    tint = if (selectedTab == CustomerBottomTab.HOME) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "হোম",
                    fontSize = 10.sp,
                    fontWeight = if (selectedTab == CustomerBottomTab.HOME) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedTab == CustomerBottomTab.HOME) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                )
            }

            // Center Item: AI Center Dock (Matching Reference Design Image)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onTabSelect(CustomerBottomTab.AI_ASSISTANT) }
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00B4D8))
                        .border(1.5.dp, Color(0xFF9ECAFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "সুচারু এআই",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "সুচারু এআই",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
            }
        }
    }
}
