package com.sucharu.sucharupro.domain.model.user

/**
 * User role definitions for Sucharu Pro.
 *
 * Defines all system actors and their primary domain context.
 * Used as the foundation for future role-based access control (RBAC).
 *
 * Role boundaries:
 * - Internal staff roles: ADMIN, MANAGER, STAFF, DESIGNER, QC_INSPECTOR, ACCOUNTS, WAREHOUSE
 * - External actors: VENDOR, CUSTOMER, AFFILIATE
 *
 * Future implementation will restrict UI features, navigation routes, and
 * data operations based on the authenticated user's role.
 */
enum class UserRole(
    val defaultLabel: String,
    /** Whether this is an internal staff role (vs external actor). */
    val isInternal: Boolean,
    /** Whether this role has access to financial/accounting data. */
    val hasFinancialAccess: Boolean = false,
    /** Whether this role can approve production stages. */
    val canApproveProduction: Boolean = false
) {
    /**
     * Full system access. Can manage all modules, users, settings and data.
     */
    ADMIN(
        defaultLabel = "Admin",
        isInternal = true,
        hasFinancialAccess = true,
        canApproveProduction = true
    ),

    /**
     * Operational manager. Can oversee orders, production, staff and reports.
     * Cannot modify system settings.
     */
    MANAGER(
        defaultLabel = "Manager",
        isInternal = true,
        hasFinancialAccess = true,
        canApproveProduction = true
    ),

    /**
     * General operations staff. Can update job status and log activities.
     * Limited financial visibility.
     */
    STAFF(
        defaultLabel = "Staff",
        isInternal = true
    ),

    /**
     * Creative designer. Handles design stages, proofs and customer artwork.
     */
    DESIGNER(
        defaultLabel = "Designer",
        isInternal = true,
        canApproveProduction = false
    ),

    /**
     * Quality control inspector. Performs QC checks and raises rework requests.
     */
    QC_INSPECTOR(
        defaultLabel = "QC Inspector",
        isInternal = true,
        canApproveProduction = true
    ),

    /**
     * Accounts/finance staff. Manages invoices, payments, and financial reporting.
     */
    ACCOUNTS(
        defaultLabel = "Accounts",
        isInternal = true,
        hasFinancialAccess = true
    ),

    /**
     * Warehouse/store manager. Manages finished product inventory, challans, and transfers.
     */
    WAREHOUSE(
        defaultLabel = "Warehouse",
        isInternal = true
    ),

    /**
     * External vendor/supplier. Can view vendor-specific jobs and bills.
     */
    VENDOR(
        defaultLabel = "Vendor",
        isInternal = false
    ),

    /**
     * End customer. Can view their orders, invoices, and delivery status.
     */
    CUSTOMER(
        defaultLabel = "Customer",
        isInternal = false
    ),

    /**
     * Affiliate/referral partner. Can view referred customers, commissions, and wallet balance.
     */
    AFFILIATE(
        defaultLabel = "Affiliate",
        isInternal = false,
        hasFinancialAccess = true // limited to own commission/wallet
    );

    companion object {
        /** All internal staff roles. */
        val internalRoles: List<UserRole> = entries.filter { it.isInternal }

        /** All external actor roles. */
        val externalRoles: List<UserRole> = entries.filter { !it.isInternal }
    }
}
