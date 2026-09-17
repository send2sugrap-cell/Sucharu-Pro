package com.sucharu.sucharupro.ui.customer.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
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
 * Premium Glowing Bottom Navigation Bar (Renders full-width user asset R.drawable.ic_bottom_ai across screen width).
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
            .height(68.dp)
            .clickable { onTabSelect(CustomerBottomTab.AI_ASSISTANT) },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_bottom_ai),
            contentDescription = "সুচারু এআই বটম বার",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillWidth
        )
    }
}
