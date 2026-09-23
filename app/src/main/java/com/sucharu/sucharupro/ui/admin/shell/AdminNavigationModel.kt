package com.sucharu.sucharupro.ui.admin.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SafetyCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.ui.graphics.vector.ImageVector
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.auth.authorization.RoleCapabilityMatrix
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Single Navigation Item definition linking UI presentation, route, and canonical module authority.
 */
data class AdminNavItem(
    val destination: AppDestination,
    val canonicalModule: String,
    val icon: ImageVector,
    val badgeText: String? = null
)

/**
 * Grouped Navigation Section mapping canonical Modules 00–24 for usability.
 */
data class AdminNavGroup(
    val groupId: String,
    val title: String,
    val items: List<AdminNavItem>
)

/**
 * Canonical Navigation Registry mapping all Sucharu Pro ERP modules (Modules 00–24).
 */
object AdminNavigationRegistry {

    val foundationGroup = AdminNavGroup(
        groupId = "FOUNDATION",
        title = "FOUNDATION & SECURITY",
        items = listOf(
            AdminNavItem(AppDestination.Admin.FullAdministration, "Module 00 / 01 Architecture & Security", Icons.Default.Dashboard, "CORE"),
            AdminNavItem(AppDestination.Admin.Users, "Module 01 User Management", Icons.Default.Group),
            AdminNavItem(AppDestination.Admin.Roles, "Module 01 Role & Capability Matrix", Icons.Default.VerifiedUser),
            AdminNavItem(AppDestination.Admin.Security, "Module 01 Security Audit Logs", Icons.Default.Security)
        )
    )

    val commercialGroup = AdminNavGroup(
        groupId = "COMMERCIAL",
        title = "CUSTOMER & COMMERCIAL",
        items = listOf(
            AdminNavItem(AppDestination.Customer.Orders, "Module 02 Customer Management", Icons.Default.Store),
            AdminNavItem(AppDestination.Customer.Quotations, "Module 03 Quotation & Sales Orders", Icons.Default.ShoppingCart)
        )
    )

    val productionGroup = AdminNavGroup(
        groupId = "PRODUCTION",
        title = "PRODUCTION & PREPRESS",
        items = listOf(
            AdminNavItem(AppDestination.Staff.Production, "Module 04 Production Execution & 13 Stages", Icons.Default.Engineering),
            AdminNavItem(AppDestination.Admin.PrepressOrchestration, "Module 05 Design, Proofing & Approval", Icons.Default.Palette),
            AdminNavItem(AppDestination.Admin.CtpOutput, "Module 06 Prepress CTP & QC Verification", Icons.AutoMirrored.Filled.FactCheck)
        )
    )

    val inventoryGroup = AdminNavGroup(
        groupId = "INVENTORY",
        title = "INVENTORY & LOGISTICS",
        items = listOf(
            AdminNavItem(AppDestination.Staff.Inventory, "Module 07 Finished Goods Inventory", Icons.Default.Inventory),
            AdminNavItem(AppDestination.Staff.Delivery, "Module 08 / 11 Delivery, Challan & Dispatch", Icons.Default.LocalShipping),
            AdminNavItem(AppDestination.Admin.SubstrateReservation, "Module 19 Substrate Stock Reservation", Icons.Default.Category)
        )
    )

    val financeGroup = AdminNavGroup(
        groupId = "FINANCE",
        title = "COMMERCIAL & FINANCE",
        items = listOf(
            AdminNavItem(AppDestination.Admin.Finance, "Module 09 / 14 / 15 Finance & Invoicing", Icons.Default.AccountBalance),
            AdminNavItem(AppDestination.Admin.ProductionJobCosting, "Module 15 / 18 Job Costing & Rate Cards", Icons.Default.MonetizationOn)
        )
    )

    val relationshipGroup = AdminNavGroup(
        groupId = "RELATIONSHIP",
        title = "AFFILIATE & NETWORK",
        items = listOf(
            AdminNavItem(AppDestination.Admin.AffiliateManagement, "Module 20 Affiliate Governance", Icons.Default.Campaign),
            AdminNavItem(AppDestination.Affiliate.Payouts, "Module 23 Wallet & Payout Accounting", Icons.Default.Wallet)
        )
    )

    val intelligenceGroup = AdminNavGroup(
        groupId = "INTELLIGENCE",
        title = "INTELLIGENCE & IOT",
        items = listOf(
            AdminNavItem(AppDestination.Admin.ShopFloorTracking, "Module 21 Machine Telemetry & OEE", Icons.Default.PrecisionManufacturing),
            AdminNavItem(AppDestination.Admin.Reports, "Module 24 Reports, Analytics & Audit", Icons.Default.Analytics, "15 CAT")
        )
    )

    val systemGroup = AdminNavGroup(
        groupId = "SYSTEM",
        title = "SYSTEM GOVERNANCE & CMS",
        items = listOf(
            AdminNavItem(AppDestination.Admin.Configuration, "Module 00 System Configuration", Icons.Default.Tune),
            AdminNavItem(AppDestination.Admin.Notifications, "Module 10 System Alerts & Notifications", Icons.Default.Notifications),
            AdminNavItem(AppDestination.Admin.FullAdministration, "Module 25 CMS Wall & Banner Management", Icons.Default.Campaign, "CMS"),
            AdminNavItem(AppDestination.Admin.Settings, "Module 01 Settings & Profile", Icons.Default.Settings)
        )
    )

    val allGroups: List<AdminNavGroup> = listOf(
        foundationGroup,
        commercialGroup,
        productionGroup,
        inventoryGroup,
        financeGroup,
        relationshipGroup,
        intelligenceGroup,
        systemGroup
    )

    /**
     * Evaluates RoleCapabilityMatrix and returns only capability-authorized navigation groups for the principal.
     */
    fun getAuthorizedGroups(principal: AuthenticatedPrincipal?): List<AdminNavGroup> {
        if (principal == null) return emptyList()
        val role = principal.role

        return allGroups.mapNotNull { group ->
            val authorizedItems = group.items.filter { item ->
                val capability = item.destination.requiredCapability
                capability == null || RoleCapabilityMatrix.hasCapability(role, capability)
            }
            if (authorizedItems.isNotEmpty()) {
                group.copy(items = authorizedItems)
            } else null
        }
    }
}
