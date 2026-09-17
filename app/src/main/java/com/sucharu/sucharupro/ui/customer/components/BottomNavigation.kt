package com.sucharu.sucharupro.ui.customer.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.R
import com.sucharu.sucharupro.data.api.model.UserRole

/**
 * Mobile Bottom Navigation tab items (AI Dock Only).
 */
enum class CustomerBottomTab(
    val title: String,
    val icon: ImageVector,
    val route: String
) {
    AI_ASSISTANT("সুচারু এআই", Icons.Default.AutoAwesome, "ai-assistant")
}

/**
 * Pure & Clean Custom Bottom Navigation Bar.
 * Renders user's exact asset R.drawable.ic_bottom_ai with overlaid glowing AI button in the center arch.
 */
@Composable
fun CustomerBottomNavigation(
    onTabSelect: (tab: CustomerBottomTab) -> Unit,
    modifier: Modifier = Modifier,
    selectedTab: CustomerBottomTab = CustomerBottomTab.AI_ASSISTANT,
    userRole: UserRole? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable { onTabSelect(CustomerBottomTab.AI_ASSISTANT) },
        contentAlignment = Alignment.BottomCenter
    ) {
        // User's Exact Custom Bottom Bar Image (R.drawable.ic_bottom_ai)
        Image(
            painter = painterResource(id = R.drawable.ic_bottom_ai),
            contentDescription = "সুচারু এআই বটম বার",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // Center Arched Glowing AI Sparkles Icon & Label
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00B4D8).copy(alpha = 0.9f))
                    .border(1.5.dp, Color(0xFF9ECAFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "সুচারু এআই",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "সুচারু এআই",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF38BDF8)
            )
        }
    }
}
