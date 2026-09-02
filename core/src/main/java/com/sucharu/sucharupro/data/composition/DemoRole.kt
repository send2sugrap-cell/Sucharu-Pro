package com.sucharu.sucharupro.data.composition

import com.sucharu.sucharupro.data.api.model.UserRole

/**
 * Type-safe Enumeration of Supported Development Showcase Demo Roles.
 *
 * Exclusively used in development/demo builds for UI/UX showcase and capability inspection.
 * Stored in-memory only and strictly isolated from production authentication tables.
 */
enum class DemoRole(
    val userRole: UserRole,
    val displayName: String,
    val demoUserId: String,
    val demoUsername: String,
    val demoEmail: String,
    val demoPhone: String,
    val iconDescription: String,
    val roleBadge: String,
    val roleDescription: String,
    val highlightFeatures: List<String>
) {
    CUSTOMER(
        userRole = UserRole.CUSTOMER,
        displayName = "Customer Portal",
        demoUserId = "USER-DEMO-001",
        demoUsername = "demo",
        demoEmail = "customer@sucharugraphics.com",
        demoPhone = "+8801700000001",
        iconDescription = "👤",
        roleBadge = "CLIENT",
        roleDescription = "Customer ordering, production job tracking, quotation review, invoices & delivery tracking.",
        highlightFeatures = listOf("Live Order Tracking", "Production Stages", "Invoices & Receipts", "Print Quotations")
    ),
    AFFILIATE(
        userRole = UserRole.AFFILIATE,
        displayName = "Affiliate Partner Portal",
        demoUserId = "USER-DEMO-AFFILIATE-001",
        demoUsername = "demo_affiliate",
        demoEmail = "affiliate@sucharugraphics.com",
        demoPhone = "+8801700000002",
        iconDescription = "🤝",
        roleBadge = "PARTNER",
        roleDescription = "Track referral campaign links, customer conversions, earned commissions & payout history.",
        highlightFeatures = listOf("Unique Referral Links", "Conversion Stats", "Commission Balance", "Payout Requests")
    ),
    STAFF(
        userRole = UserRole.STAFF,
        displayName = "Staff & Operator Workspace",
        demoUserId = "USER-DEMO-STAFF-001",
        demoUsername = "demo_staff",
        demoEmail = "staff@sucharugraphics.com",
        demoPhone = "+8801700000003",
        iconDescription = "👨‍💼",
        roleBadge = "OPERATOR",
        roleDescription = "Manage assigned tasks, production job queues, stage operations, QC inspection & internal alerts.",
        highlightFeatures = listOf("Operator Work Queue", "Stage Output Logging", "Quality Control (QC)", "Team Notices")
    ),
    MANAGER(
        userRole = UserRole.MANAGER,
        displayName = "Operations & Manager Center",
        demoUserId = "USER-DEMO-MANAGER-001",
        demoUsername = "demo_manager",
        demoEmail = "manager@sucharugraphics.com",
        demoPhone = "+8801700000004",
        iconDescription = "📊",
        roleBadge = "OPERATIONS",
        roleDescription = "Operations oversight, order approvals, financial summary, cost control, budget & workflow metrics.",
        highlightFeatures = listOf("Operations Dashboard", "Order Approvals", "Financial Summary", "Workflow Monitor")
    ),
    ADMIN(
        userRole = UserRole.ADMIN,
        displayName = "Admin Executive Control",
        demoUserId = "USER-DEMO-ADMIN-001",
        demoUsername = "demo_admin",
        demoEmail = "admin@sucharugraphics.com",
        demoPhone = "+8801700000005",
        iconDescription = "🛡",
        roleBadge = "EXECUTIVE",
        roleDescription = "Full ERP system control center, user governance, audit trail, security policies & ERP config.",
        highlightFeatures = listOf("System Dashboard", "User Governance", "Audit Security Logs", "ERP Configuration")
    );

    companion object {
        fun fromIdentifier(identifier: String?): DemoRole {
            if (identifier.isNullOrBlank()) return CUSTOMER
            val normalized = identifier.trim().lowercase()
            return when {
                normalized == "demo" || normalized.contains("customer") -> CUSTOMER
                normalized.contains("affiliate") -> AFFILIATE
                normalized.contains("staff") -> STAFF
                normalized.contains("manager") -> MANAGER
                normalized.contains("admin") -> ADMIN
                else -> CUSTOMER
            }
        }

        fun fromUserRole(role: UserRole): DemoRole {
            return entries.find { it.userRole == role } ?: CUSTOMER
        }
    }
}
