package com.sucharu.sucharupro.ui.customer.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme

/**
 * Streamlined Mobile Bottom Navigation tab items.
 */
enum class CustomerBottomTab(
    val title: String,
    val icon: ImageVector,
    val route: String
) {
    HOME("হোম", Icons.Default.Home, "customer/home"),
    ACCOUNT("প্রোফাইল", Icons.Default.Person, "customer/account")
}

/**
 * Streamlined Bottom Dock for Customer & Affiliate experiences.
 */
@Composable
fun CustomerBottomNavigation(
    selectedTab: CustomerBottomTab,
    onTabSelect: (tab: CustomerBottomTab) -> Unit,
    modifier: Modifier = Modifier,
    userRole: UserRole? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        color = CustomerTheme.colors.surface,
        border = BorderStroke(1.dp, CustomerTheme.colors.border)
    ) {
        NavigationBar(
            containerColor = CustomerTheme.colors.surface,
            contentColor = CustomerTheme.colors.primaryText
        ) {
            CustomerBottomTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onTabSelect(tab) },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title
                        )
                    },
                    label = {
                        Text(
                            text = tab.title,
                            style = CustomerTheme.typography.caption
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CustomerTheme.colors.accentPrimary,
                        selectedTextColor = CustomerTheme.colors.accentPrimary,
                        indicatorColor = CustomerTheme.colors.accentContainer,
                        unselectedIconColor = CustomerTheme.colors.secondaryText,
                        unselectedTextColor = CustomerTheme.colors.secondaryText
                    )
                )
            }
        }
    }
}
