package com.sucharu.sucharupro.ui.navigation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.authorization.RoleCapabilityMatrix

/**
 * Capability-Aware Navigation & Menu Filtering Infrastructure (INFRA-03 Step 06).
 *
 * Implements two-layer security:
 * Layer 1: UI Visibility (hides menu items if the user's role lacks the required capability).
 * Layer 2: Server-Authoritative Route Verification (evaluates capability when opening destinations).
 */
object CapabilityAwareNavigation {

    /**
     * Layer 1 — Filters a list of [AppDestination] for UI menu display based on user role capabilities.
     */
    fun filterDestinationsForRole(
        destinations: List<AppDestination>,
        role: UserRole
    ): List<AppDestination> {
        if (role == UserRole.AI_AGENT) return emptyList() // Machine principal gets zero human menu items

        return destinations.filter { dest ->
            when {
                dest.isPublic -> true
                dest.requiredCapability == null -> true
                else -> RoleCapabilityMatrix.hasCapability(role, dest.requiredCapability)
            }
        }
    }

    /**
     * Layer 2 — Server-authoritative capability verification for route entry.
     */
    fun isRouteAuthorized(
        principal: AuthenticatedPrincipal?,
        destination: AppDestination
    ): Boolean {
        // Public destinations are accessible to all
        if (destination.isPublic) return true

        // Protected destinations require an authenticated human principal
        if (principal == null) return false
        if (principal.role == UserRole.AI_AGENT) return false // Machine principal blocked

        val reqCap = destination.requiredCapability ?: return true
        return RoleCapabilityMatrix.hasCapability(principal.role, reqCap)
    }
}
