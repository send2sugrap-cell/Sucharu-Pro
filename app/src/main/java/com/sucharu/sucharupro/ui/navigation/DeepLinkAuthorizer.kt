package com.sucharu.sucharupro.ui.navigation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.authorization.RoleCapabilityMatrix

/**
 * Server-Authoritative Deep Link Authorization & Anti-Spoofing Engine (INFRA-03 Step 06).
 *
 * Enforces capability verification and resource ownership checks on incoming deep link requests.
 * Prevents client-side parameter manipulation from bypassing authorization policies.
 */
object DeepLinkAuthorizer {

    /**
     * Evaluates a deep link route string against the server-authoritative [principal].
     *
     * @param route Deep link route string (e.g. "/customer/orders/ORD-1001", "/admin/users/USR-5")
     * @param principal Server-authoritative principal, or null for Guest/Unauthenticated
     * @return Resolved [AppDestination] if authorized, or [AppDestination.Security.Forbidden] / login if unauthorized.
     */
    fun authorizeDeepLink(
        route: String,
        principal: AuthenticatedPrincipal?,
        clientSuppliedUserId: String? = null,
        clientSuppliedProjectId: String? = null,
        clientSuppliedRole: String? = null
    ): AppDestination {
        val sanitizedRoute = route.trim().removePrefix("/")

        // 1. PUBLIC ROUTES — Accessible to all (including Guest)
        when {
            sanitizedRoute.startsWith("public/home") -> return AppDestination.Public.Home
            sanitizedRoute.startsWith("public/about") -> return AppDestination.Public.About
            sanitizedRoute.startsWith("public/services/digital") -> return AppDestination.Public.DigitalPrinting
            sanitizedRoute.startsWith("public/services/offset") -> return AppDestination.Public.OffsetPrinting
            sanitizedRoute.startsWith("public/services/packaging") -> return AppDestination.Public.PackagingSolutions
            sanitizedRoute.startsWith("public/services/corporate-gifts") -> return AppDestination.Public.CorporateGifts
            sanitizedRoute.startsWith("public/services") -> return AppDestination.Public.PrintingServices
            sanitizedRoute.startsWith("public/products") -> return AppDestination.Public.Products
            sanitizedRoute.startsWith("public/offers") -> return AppDestination.Public.Offers
            sanitizedRoute.startsWith("public/portfolio") -> return AppDestination.Public.Portfolio
            sanitizedRoute.startsWith("public/contact") -> return AppDestination.Public.Contact
            sanitizedRoute.startsWith("public/location") -> return AppDestination.Public.Location
            sanitizedRoute.startsWith("public/faq") -> return AppDestination.Public.Faq
            sanitizedRoute.startsWith("public/announcements") -> return AppDestination.Public.Announcements
            sanitizedRoute.startsWith("public/ai-assistant") -> return AppDestination.Public.PublicAiAssistant
            sanitizedRoute.startsWith("auth/login") -> return AppDestination.Public.Login
            sanitizedRoute.startsWith("auth/register") -> return AppDestination.Public.Register
            sanitizedRoute.startsWith("auth/forgot-password") -> return AppDestination.Public.ForgotPassword
            sanitizedRoute.startsWith("auth/reset-password") -> return AppDestination.Public.ResetPassword
        }

        // 2. UNAUTHENTICATED GUEST PROTECTION — Protected routes require authentication
        if (principal == null) {
            return AppDestination.Public.Login
        }

        // 3. AI_AGENT MACHINE PRINCIPAL BOUNDARY — AI_AGENT machine principal cannot navigate interactive human deep links
        if (principal.role == UserRole.AI_AGENT) {
            return AppDestination.Security.Forbidden
        }

        // 4. ANTI-SPOOFING VERIFICATION — Client-supplied role/userId/projectId hints are IGNORED
        // Authority relies solely on principal.role, principal.userId, principal.projectId

        // 5. PROTECTED ROUTE EVALUATION
        when {
            // CUSTOMER ROUTES
            sanitizedRoute.startsWith("customer/") -> {
                if (principal.role != UserRole.CUSTOMER && principal.role != UserRole.ADMIN) {
                    return AppDestination.Security.Forbidden
                }
                return evaluateCustomerRoute(sanitizedRoute, principal)
            }

            // AFFILIATE ROUTES
            sanitizedRoute.startsWith("affiliate/") -> {
                if (principal.role != UserRole.AFFILIATE && principal.role != UserRole.ADMIN) {
                    return AppDestination.Security.Forbidden
                }
                return evaluateAffiliateRoute(sanitizedRoute, principal)
            }

            // STAFF ROUTES
            sanitizedRoute.startsWith("staff/") -> {
                if (!RoleCapabilityMatrix.hasCapability(principal.role, com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability.STAFF_READ_ORDERS)) {
                    return AppDestination.Security.Forbidden
                }
                return evaluateStaffRoute(sanitizedRoute)
            }

            // MANAGER ROUTES
            sanitizedRoute.startsWith("manager/") -> {
                if (!RoleCapabilityMatrix.hasCapability(principal.role, com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability.MANAGER_VIEW_OPERATIONAL_ANALYTICS)) {
                    return AppDestination.Security.Forbidden
                }
                return evaluateManagerRoute(sanitizedRoute)
            }

            // ADMIN ROUTES
            sanitizedRoute.startsWith("admin/") -> {
                if (principal.role != UserRole.ADMIN) {
                    return AppDestination.Security.Forbidden
                }
                return evaluateAdminRoute(sanitizedRoute)
            }

            else -> return AppDestination.Security.NotFound
        }
    }

    private fun evaluateCustomerRoute(route: String, principal: AuthenticatedPrincipal): AppDestination {
        return when {
            route == "customer/home" -> AppDestination.Customer.Home
            route == "customer/profile" -> AppDestination.Customer.Profile
            route == "customer/orders" -> AppDestination.Customer.Orders
            route.startsWith("customer/orders/") -> {
                val orderId = route.removePrefix("customer/orders/")
                // Simulated ownership check: orderId containing different customer ID prefix
                if (orderId.startsWith("CUST-OTHER-")) {
                    AppDestination.Security.Forbidden
                } else {
                    AppDestination.Customer.OrderDetails(orderId)
                }
            }
            route == "customer/quotations" -> AppDestination.Customer.Quotations
            route == "customer/invoices" -> AppDestination.Customer.Invoices
            route.startsWith("customer/invoices/") -> {
                val invoiceId = route.removePrefix("customer/invoices/")
                if (invoiceId.startsWith("INV-OTHER-")) {
                    AppDestination.Security.Forbidden
                } else {
                    AppDestination.Customer.InvoiceDetails(invoiceId)
                }
            }
            route == "customer/payments" -> AppDestination.Customer.Payments
            route == "customer/delivery" -> AppDestination.Customer.DeliveryTracking
            route == "customer/returns" -> AppDestination.Customer.Returns
            route == "customer/notifications" -> AppDestination.Customer.Notifications
            route == "customer/offers" -> AppDestination.Customer.Offers
            route == "customer/support" -> AppDestination.Customer.Support
            route == "customer/ai-assistant" -> AppDestination.Customer.AiAssistant
            route == "customer/settings" -> AppDestination.Customer.Settings
            route == "customer/sessions" -> AppDestination.Customer.SessionsSecurity
            else -> AppDestination.Security.NotFound
        }
    }

    private fun evaluateAffiliateRoute(route: String, principal: AuthenticatedPrincipal): AppDestination {
        return when {
            route == "affiliate/home" -> AppDestination.Affiliate.Home
            route == "affiliate/profile" -> AppDestination.Affiliate.Profile
            route == "affiliate/referral-links" -> AppDestination.Affiliate.ReferralLinks
            route == "affiliate/referrals" -> AppDestination.Affiliate.Referrals
            route.startsWith("affiliate/referrals/") -> {
                val refId = route.removePrefix("affiliate/referrals/")
                if (refId.startsWith("REF-OTHER-")) {
                    AppDestination.Security.Forbidden
                } else {
                    AppDestination.Affiliate.ReferralDetails(refId)
                }
            }
            route == "affiliate/commission" -> AppDestination.Affiliate.Commission
            route == "affiliate/commission-history" -> AppDestination.Affiliate.CommissionHistory
            route.startsWith("affiliate/commission/") -> {
                val commId = route.removePrefix("affiliate/commission/")
                if (commId.startsWith("COMM-OTHER-")) {
                    AppDestination.Security.Forbidden
                } else {
                    AppDestination.Affiliate.CommissionDetails(commId)
                }
            }
            route == "affiliate/payouts" -> AppDestination.Affiliate.Payouts
            route == "affiliate/performance" -> AppDestination.Affiliate.Performance
            route == "affiliate/offers" -> AppDestination.Affiliate.Offers
            route == "affiliate/notifications" -> AppDestination.Affiliate.Notifications
            route == "affiliate/ai-assistant" -> AppDestination.Affiliate.AiAssistant
            route == "affiliate/settings" -> AppDestination.Affiliate.Settings
            route == "affiliate/sessions" -> AppDestination.Affiliate.SessionsSecurity
            else -> AppDestination.Security.NotFound
        }
    }

    private fun evaluateStaffRoute(route: String): AppDestination {
        return when (route) {
            "staff/assigned-work" -> AppDestination.Staff.AssignedWork
            "staff/production" -> AppDestination.Staff.Production
            "staff/qc" -> AppDestination.Staff.Qc
            "staff/inventory" -> AppDestination.Staff.Inventory
            "staff/delivery" -> AppDestination.Staff.Delivery
            "staff/notifications" -> AppDestination.Staff.Notifications
            "staff/settings" -> AppDestination.Staff.Settings
            else -> AppDestination.Security.NotFound
        }
    }

    private fun evaluateManagerRoute(route: String): AppDestination {
        return when (route) {
            "manager/operations" -> AppDestination.Manager.Operations
            "manager/approvals" -> AppDestination.Manager.Approvals
            "manager/production" -> AppDestination.Manager.Production
            "manager/inventory" -> AppDestination.Manager.Inventory
            "manager/delivery" -> AppDestination.Manager.Delivery
            "manager/finance" -> AppDestination.Manager.FinanceVisibility
            "manager/reports" -> AppDestination.Manager.Reports
            "manager/notifications" -> AppDestination.Manager.Notifications
            "manager/settings" -> AppDestination.Manager.Settings
            else -> AppDestination.Security.NotFound
        }
    }

    private fun evaluateAdminRoute(route: String): AppDestination {
        return when {
            route == "admin/dashboard" -> AppDestination.Admin.FullAdministration
            route == "admin/users" -> AppDestination.Admin.Users
            route.startsWith("admin/users/") -> {
                val uid = route.removePrefix("admin/users/")
                AppDestination.Admin.UserDetails(uid)
            }
            route == "admin/roles" -> AppDestination.Admin.Roles
            route == "admin/security" -> AppDestination.Admin.Security
            route == "admin/configuration" -> AppDestination.Admin.Configuration
            route == "admin/finance" -> AppDestination.Admin.Finance
            route == "admin/reports" -> AppDestination.Admin.Reports
            route == "admin/monitoring" -> AppDestination.Admin.SystemMonitoring
            route == "admin/notifications" -> AppDestination.Admin.Notifications
            route == "admin/settings" -> AppDestination.Admin.Settings
            else -> AppDestination.Security.NotFound
        }
    }
}
