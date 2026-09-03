package com.sucharu.sucharupro.ui.navigation

import com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability

/**
 * Type-safe Canonical Navigation Destinations (INFRA-03 Step 06).
 * Serves as the single destination model for Guest, Customer, Affiliate, Staff, Manager, Admin.
 */
sealed class AppDestination(
    val route: String,
    val title: String,
    val requiredCapability: AuthorizationCapability? = null,
    val isPublic: Boolean = false
) {
    // PUBLIC DESTINATIONS (Accessible to GUEST without login)
    sealed class Public(route: String, title: String) : AppDestination(route, title, isPublic = true) {
        object Home : Public("public/home", "Home")
        object About : Public("public/about", "About Us")
        object PrintingServices : Public("public/services", "Services")
        object DigitalPrinting : Public("public/services/digital", "Digital Printing")
        object OffsetPrinting : Public("public/services/offset", "Offset Printing")
        object PackagingSolutions : Public("public/services/packaging", "Packaging Solutions")
        object CorporateGifts : Public("public/services/corporate-gifts", "Corporate Gifts")
        object Products : Public("public/products", "Products")
        object Offers : Public("public/offers", "Offers")
        object Portfolio : Public("public/portfolio", "Portfolio")
        object Contact : Public("public/contact", "Contact")
        object Location : Public("public/location", "Location")
        object Faq : Public("public/faq", "FAQ")
        object Announcements : Public("public/announcements", "Announcements")
        object PublicAiAssistant : Public("public/ai-assistant", "AI Assistant")
        object Login : Public("auth/login", "Sign In")
        object Register : Public("auth/register", "Sign Up")
        object ForgotPassword : Public("auth/forgot-password", "Forgot Password")
        object ResetPassword : Public("auth/reset-password", "Reset Password")
    }

    // CUSTOMER WORKSPACE DESTINATIONS
    sealed class Customer(
        route: String,
        title: String,
        requiredCapability: AuthorizationCapability = AuthorizationCapability.READ_OWN_IDENTITY
    ) : AppDestination(route, title, requiredCapability = requiredCapability, isPublic = false) {
        object Home : Customer("customer/home", "Dashboard", AuthorizationCapability.READ_OWN_IDENTITY)
        object Profile : Customer("customer/profile", "My Profile", AuthorizationCapability.READ_OWN_PROFILE)
        object Orders : Customer("customer/orders", "My Orders", AuthorizationCapability.READ_OWN_ORDERS)
        data class OrderDetails(val orderId: String) : Customer("customer/orders/$orderId", "Order Details", AuthorizationCapability.READ_OWN_ORDERS)
        object Quotations : Customer("customer/quotations", "Quotations", AuthorizationCapability.READ_OWN_ORDERS)
        object Invoices : Customer("customer/invoices", "Invoices", AuthorizationCapability.READ_OWN_INVOICES)
        data class InvoiceDetails(val invoiceId: String) : Customer("customer/invoices/$invoiceId", "Invoice Details", AuthorizationCapability.READ_OWN_INVOICES)
        object Payments : Customer("customer/payments", "Payments", AuthorizationCapability.READ_OWN_PAYMENTS)
        object DeliveryTracking : Customer("customer/delivery", "Delivery Tracking", AuthorizationCapability.READ_OWN_DELIVERIES)
        object Returns : Customer("customer/returns", "Returns & Replacement", AuthorizationCapability.READ_OWN_RETURNS)
        object Notifications : Customer("customer/notifications", "Notifications", AuthorizationCapability.READ_OWN_IDENTITY)
        object Offers : Customer("customer/offers", "Special Offers", AuthorizationCapability.PUBLIC_READ_OFFERS)
        object Support : Customer("customer/support", "Customer Support", AuthorizationCapability.READ_OWN_IDENTITY)
        object AiAssistant : Customer("customer/ai-assistant", "Printing Assistant", AuthorizationCapability.PUBLIC_USE_AI_ASSISTANT)
        object Settings : Customer("customer/settings", "Settings", AuthorizationCapability.READ_OWN_PROFILE)
        object SessionsSecurity : Customer("customer/sessions", "Session Security", AuthorizationCapability.READ_OWN_SESSIONS)
    }

    // AFFILIATE WORKSPACE DESTINATIONS
    sealed class Affiliate(
        route: String,
        title: String,
        requiredCapability: AuthorizationCapability = AuthorizationCapability.READ_OWN_AFFILIATE_PROFILE
    ) : AppDestination(route, title, requiredCapability = requiredCapability, isPublic = false) {
        object Home : Affiliate("affiliate/home", "Affiliate Dashboard", AuthorizationCapability.READ_OWN_AFFILIATE_PROFILE)
        object Profile : Affiliate("affiliate/profile", "Affiliate Profile", AuthorizationCapability.READ_OWN_AFFILIATE_PROFILE)
        object ReferralLinks : Affiliate("affiliate/referral-links", "Referral Links", AuthorizationCapability.READ_OWN_REFERRALS)
        object Referrals : Affiliate("affiliate/referrals", "Referral History", AuthorizationCapability.READ_OWN_REFERRALS)
        data class ReferralDetails(val referralId: String) : Affiliate("affiliate/referrals/$referralId", "Referral Details", AuthorizationCapability.READ_OWN_REFERRALS)
        object Commission : Affiliate("affiliate/commission", "Commissions", AuthorizationCapability.READ_OWN_COMMISSIONS)
        object CommissionHistory : Affiliate("affiliate/commission-history", "Commission History", AuthorizationCapability.READ_OWN_COMMISSIONS)
        data class CommissionDetails(val commissionId: String) : Affiliate("affiliate/commission/$commissionId", "Commission Details", AuthorizationCapability.READ_OWN_COMMISSIONS)
        object Payouts : Affiliate("affiliate/payouts", "Payouts", AuthorizationCapability.READ_OWN_COMMISSIONS)
        object Performance : Affiliate("affiliate/performance", "Performance Analytics", AuthorizationCapability.READ_OWN_AFFILIATE_PROFILE)
        object Offers : Affiliate("affiliate/offers", "Affiliate Offers", AuthorizationCapability.PUBLIC_READ_OFFERS)
        object Notifications : Affiliate("affiliate/notifications", "Notifications", AuthorizationCapability.READ_OWN_IDENTITY)
        object AiAssistant : Affiliate("affiliate/ai-assistant", "Affiliate AI Assistant", AuthorizationCapability.PUBLIC_USE_AI_ASSISTANT)
        object Settings : Affiliate("affiliate/settings", "Settings", AuthorizationCapability.READ_OWN_PROFILE)
        object SessionsSecurity : Affiliate("affiliate/sessions", "Session Security", AuthorizationCapability.READ_OWN_SESSIONS)
    }

    // STAFF WORKSPACE DESTINATIONS
    sealed class Staff(
        route: String,
        title: String,
        requiredCapability: AuthorizationCapability
    ) : AppDestination(route, title, requiredCapability = requiredCapability, isPublic = false) {
        object AssignedWork : Staff("staff/assigned-work", "Assigned Work", AuthorizationCapability.STAFF_READ_ORDERS)
        object Production : Staff("staff/production", "Production Operations", AuthorizationCapability.STAFF_READ_ORDERS)
        object ProductionScheduling : Staff("staff/production-scheduling", "Production Scheduling & Queue", AuthorizationCapability.STAFF_READ_ORDERS)
        object ShopFloorTracking : Staff("staff/shop-floor-tracking", "Shop-Floor Live Tracking & Telemetry", AuthorizationCapability.STAFF_READ_ORDERS)
        object FinalQcPackaging : Staff("staff/final-qc-packaging", "Final Quality Control & Packaging", AuthorizationCapability.STAFF_READ_QC)
        object ProductionJobCosting : Staff("staff/job-costing", "Actual Job Costing & Variance", AuthorizationCapability.STAFF_READ_QC)
        object ProductionJobClosure : Staff("staff/job-closure", "Production Job Closure & Governance", AuthorizationCapability.STAFF_READ_QC)
        object SubstrateReservation : Staff("staff/substrate-reservation", "Substrate Stock Auto-Reservation", AuthorizationCapability.STAFF_READ_INVENTORY)
        object SubstrateReplenishment : Staff("staff/substrate-replenishment", "Substrate Auto-Replenishment & Alerts", AuthorizationCapability.STAFF_READ_INVENTORY)
        object SubstrateReleaseGovernance : Staff("staff/substrate-release-governance", "Substrate Release Governance", AuthorizationCapability.STAFF_READ_INVENTORY)
        object Imposition : Staff("staff/imposition", "Dynamic Imposition & Layout", AuthorizationCapability.STAFF_READ_ORDERS)
        object GangRun : Staff("staff/gang-run", "Multi-Job Gang-Run Optimizer", AuthorizationCapability.STAFF_READ_ORDERS)
        object DynamicNesting : Staff("staff/dynamic-nesting", "Dynamic 2D Nesting & Wastage", AuthorizationCapability.STAFF_READ_ORDERS)
        object SignatureImposition : Staff("staff/signature-imposition", "Signature Layout & Work-and-Turn", AuthorizationCapability.STAFF_READ_ORDERS)
        object CtpOutput : Staff("staff/ctp-output", "Prepress CTP Output & Plate Packages", AuthorizationCapability.STAFF_READ_ORDERS)
        object Qc : Staff("staff/qc", "Quality Control", AuthorizationCapability.STAFF_READ_QC)
        object Inventory : Staff("staff/inventory", "Stock & Inventory", AuthorizationCapability.STAFF_READ_INVENTORY)
        object Delivery : Staff("staff/delivery", "Dispatch & Delivery", AuthorizationCapability.STAFF_READ_DELIVERY)
        object Workflows : Staff("staff/workflows", "Workflow Monitor", AuthorizationCapability.WORKFLOW_VIEW)
        object Notifications : Staff("staff/notifications", "Staff Alerts", AuthorizationCapability.READ_OWN_IDENTITY)
        object Settings : Staff("staff/settings", "Settings", AuthorizationCapability.READ_OWN_PROFILE)
    }

    // MANAGER WORKSPACE DESTINATIONS
    sealed class Manager(
        route: String,
        title: String,
        requiredCapability: AuthorizationCapability
    ) : AppDestination(route, title, requiredCapability = requiredCapability, isPublic = false) {
        object Operations : Manager("manager/operations", "Operations Overview", AuthorizationCapability.MANAGER_VIEW_OPERATIONAL_ANALYTICS)
        object Approvals : Manager("manager/approvals", "Pending Approvals", AuthorizationCapability.MANAGER_APPROVE_ORDER)
        object Workflows : Manager("manager/workflows", "Workflow Control", AuthorizationCapability.WORKFLOW_VIEW)
        object Production : Manager("manager/production", "Production Oversight", AuthorizationCapability.STAFF_READ_ORDERS)
        object ProductionScheduling : Manager("manager/production-scheduling", "Production Scheduling & Capacity", AuthorizationCapability.STAFF_READ_ORDERS)
        object ShopFloorTracking : Manager("manager/shop-floor-tracking", "Shop-Floor Live Tracking & Telemetry", AuthorizationCapability.STAFF_READ_ORDERS)
        object FinalQcPackaging : Manager("manager/final-qc-packaging", "Final Quality Control & Packaging Release", AuthorizationCapability.STAFF_READ_QC)
        object ProductionJobCosting : Manager("manager/job-costing", "Actual Job Costing & Manufacturing Variance", AuthorizationCapability.MANAGER_VIEW_FINANCIAL_SUMMARY)
        object ProductionJobClosure : Manager("manager/job-closure", "Production Job Closure & Master Seal", AuthorizationCapability.MANAGER_VIEW_FINANCIAL_SUMMARY)
        object SubstrateReservation : Manager("manager/substrate-reservation", "Substrate Stock Auto-Reservation", AuthorizationCapability.STAFF_READ_INVENTORY)
        object SubstrateReplenishment : Manager("manager/substrate-replenishment", "Substrate Auto-Replenishment & Alerts", AuthorizationCapability.STAFF_READ_INVENTORY)
        object SubstrateReleaseGovernance : Manager("manager/substrate-release-governance", "Substrate Release Governance", AuthorizationCapability.STAFF_READ_INVENTORY)
        object Imposition : Manager("manager/imposition", "Dynamic Imposition & Layout", AuthorizationCapability.STAFF_READ_ORDERS)
        object GangRun : Manager("manager/gang-run", "Multi-Job Gang-Run Optimizer", AuthorizationCapability.STAFF_READ_ORDERS)
        object DynamicNesting : Manager("manager/dynamic-nesting", "Dynamic 2D Nesting & Wastage", AuthorizationCapability.STAFF_READ_ORDERS)
        object SignatureImposition : Manager("manager/signature-imposition", "Signature Layout & Work-and-Turn", AuthorizationCapability.STAFF_READ_ORDERS)
        object CtpOutput : Manager("manager/ctp-output", "Prepress CTP Output & Plate Packages", AuthorizationCapability.STAFF_READ_ORDERS)
        object Inventory : Manager("manager/inventory", "Inventory Management", AuthorizationCapability.STAFF_READ_INVENTORY)
        object Delivery : Manager("manager/delivery", "Delivery Logistics", AuthorizationCapability.STAFF_READ_DELIVERY)
        object FinanceVisibility : Manager("manager/finance", "Financial Summary", AuthorizationCapability.MANAGER_VIEW_FINANCIAL_SUMMARY)
        object Profitability : Manager("manager/profitability", "Profit & Cost Analysis", AuthorizationCapability.MANAGER_VIEW_FINANCIAL_SUMMARY)
        object Reports : Manager("manager/reports", "Operational Reports", AuthorizationCapability.MANAGER_VIEW_OPERATIONAL_ANALYTICS)
        object Notifications : Manager("manager/notifications", "Manager Alerts", AuthorizationCapability.READ_OWN_IDENTITY)
        object Settings : Manager("manager/settings", "Settings", AuthorizationCapability.READ_OWN_PROFILE)
    }

    // ADMIN WORKSPACE DESTINATIONS
    sealed class Admin(
        route: String,
        title: String,
        requiredCapability: AuthorizationCapability
    ) : AppDestination(route, title, requiredCapability = requiredCapability, isPublic = false) {
        object FullAdministration : Admin("admin/dashboard", "System Control Center", AuthorizationCapability.ADMIN_ALL)
        object Workflows : Admin("admin/workflows", "Workflow Control & Governance", AuthorizationCapability.WORKFLOW_VIEW)
        object WorkflowMetrics : Admin("admin/workflow-metrics", "Workflow Metrics", AuthorizationCapability.WORKFLOW_METRICS_VIEW)
        object WorkflowAudit : Admin("admin/workflow-audit", "Workflow Audit Trail", AuthorizationCapability.WORKFLOW_AUDIT_VIEW)
        object SubstrateReservation : Admin("admin/substrate-reservation", "Substrate Stock Auto-Reservation", AuthorizationCapability.ADMIN_ALL)
        object SubstrateBatchSelection : Admin("admin/substrate-batch-selection", "Substrate Batch & Grain Optimizer", AuthorizationCapability.ADMIN_ALL)
        object SubstrateReplenishment : Admin("admin/substrate-replenishment", "Substrate Auto-Replenishment & Alerts", AuthorizationCapability.ADMIN_ALL)
        object SubstrateReleaseGovernance : Admin("admin/substrate-release-governance", "Substrate Release Governance", AuthorizationCapability.ADMIN_ALL)
        object Imposition : Admin("admin/imposition", "Dynamic Imposition & Layout", AuthorizationCapability.ADMIN_ALL)
        object GangRun : Admin("admin/gang-run", "Multi-Job Gang-Run Optimizer", AuthorizationCapability.ADMIN_ALL)
        object DynamicNesting : Admin("admin/dynamic-nesting", "Dynamic 2D Nesting & Wastage", AuthorizationCapability.ADMIN_ALL)
        object SignatureImposition : Admin("admin/signature-imposition", "Signature Layout & Work-and-Turn", AuthorizationCapability.ADMIN_ALL)
        object CtpOutput : Admin("admin/ctp-output", "Prepress CTP Output & Plate Packages", AuthorizationCapability.ADMIN_ALL)
        object PrepressOrchestration : Admin("admin/prepress-orchestration", "Prepress Master Orchestration & AI Governance", AuthorizationCapability.ADMIN_ALL)


        object Users : Admin("admin/users", "User Management", AuthorizationCapability.ADMIN_MANAGE_USERS)
        data class UserDetails(val targetUserId: String) : Admin("admin/users/$targetUserId", "User Details", AuthorizationCapability.ADMIN_MANAGE_USERS)
        object Roles : Admin("admin/roles", "Role & Capability Matrix", AuthorizationCapability.ADMIN_MANAGE_ROLES)
        object Security : Admin("admin/security", "Security Audit Logs", AuthorizationCapability.ADMIN_VIEW_AUDIT)
        object Configuration : Admin("admin/configuration", "System Configuration", AuthorizationCapability.ADMIN_MANAGE_SYSTEM_CONFIGURATION)
        object ProductionScheduling : Admin("admin/production-scheduling", "Scheduling & Dispatch Engine", AuthorizationCapability.ADMIN_ALL)
        object ShopFloorTracking : Admin("admin/shop-floor-tracking", "Shop-Floor Live Tracking & Telemetry", AuthorizationCapability.ADMIN_ALL)
        object FinalQcPackaging : Admin("admin/final-qc-packaging", "Final Quality Control & Packaging Release", AuthorizationCapability.ADMIN_ALL)
        object ProductionJobCosting : Admin("admin/job-costing", "Actual Job Costing & Variance Engine", AuthorizationCapability.ADMIN_ALL)
        object ProductionJobClosure : Admin("admin/job-closure", "Production Job Closure & Governance Engine", AuthorizationCapability.ADMIN_ALL)
        object Finance : Admin("admin/finance", "ERP Financial Ledger", AuthorizationCapability.ADMIN_ALL)



        object Profitability : Admin("admin/profitability", "Profit & Cost Intelligence", AuthorizationCapability.ADMIN_ALL)
        object Reports : Admin("admin/reports", "Executive Analytics", AuthorizationCapability.ADMIN_ALL)
        object SystemMonitoring : Admin("admin/monitoring", "Infrastructure Health", AuthorizationCapability.ADMIN_ALL)
        object Notifications : Admin("admin/notifications", "System Alerts", AuthorizationCapability.READ_OWN_IDENTITY)
        object Settings : Admin("admin/settings", "Settings", AuthorizationCapability.READ_OWN_PROFILE)
    }


    // SECURITY & EXCEPTION DESTINATIONS
    sealed class Security(route: String, title: String) : AppDestination(route, title, isPublic = true) {
        object VerificationRequired : Security("security/verification-required", "Verification Required")
        object AccountUnavailable : Security("security/account-unavailable", "Account Status Alert")
        object SecurityReview : Security("security/security-review", "Security Review")
        object SessionExpired : Security("security/session-expired", "Session Expired")
        object Forbidden : Security("security/forbidden", "Access Restricted")
        object NotFound : Security("security/not-found", "Page Not Found")
    }
}
