package com.sucharu.sucharupro.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Bottom navigation item definition with associated route, label, and Material icon.
 */
data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String = label
)

val bottomNavItems = listOf(
    BottomNavItem(
        screen = Screen.Dashboard,
        label = "Dashboard",
        icon = Icons.Default.Home
    ),
    BottomNavItem(
        screen = Screen.Orders,
        label = "Orders",
        icon = Icons.AutoMirrored.Filled.ReceiptLong
    ),
    BottomNavItem(
        screen = Screen.Printing,
        label = "Printing",
        icon = Icons.Default.Print
    ),
    BottomNavItem(
        screen = Screen.Customers,
        label = "Customers",
        icon = Icons.Default.People
    ),
    BottomNavItem(
        screen = Screen.Reports,
        label = "Reports",
        icon = Icons.Default.BarChart
    ),
    BottomNavItem(
        screen = Screen.Settings,
        label = "Settings",
        icon = Icons.Default.Settings
    )
)
