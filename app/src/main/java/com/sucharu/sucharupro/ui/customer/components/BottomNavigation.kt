package com.sucharu.sucharupro.ui.customer.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Surface
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
 * Premium Glowing Bottom Navigation Bar with Centered Glowing AI Button Dock.
 */
@Composable
fun CustomerBottomNavigation(
    onTabSelect: (tab: CustomerBottomTab) -> Unit,
    modifier: Modifier = Modifier,
    selectedTab: CustomerBottomTab = CustomerBottomTab.AI_ASSISTANT,
    userRole: UserRole? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        color = Color(0xFF0B132B),
        border = BorderStroke(1.5.dp, Color(0xFF00B4D8).copy(alpha = 0.6f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Background User Banner Image
            Image(
                painter = painterResource(id = R.drawable.ic_bottom_ai),
                contentDescription = "বটম বার ব্যাকগ্রাউন্ড",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Prominent Glowing Center AI Button Overlay
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clickable { onTabSelect(CustomerBottomTab.AI_ASSISTANT) }
                    .padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00B4D8))
                        .border(2.dp, Color(0xFF9ECAFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "সুচারু এআই",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "সুচারু এআই",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF38BDF8)
                )
            }
        }
    }
}
