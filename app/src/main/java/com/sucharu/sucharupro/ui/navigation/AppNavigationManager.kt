package com.sucharu.sucharupro.ui.navigation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.auth.session.AuthenticationSessionManager
import com.sucharu.sucharupro.ui.features.auth.PostLoginRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single-Source-of-Truth Navigation Controller & Back-Stack Security Manager (INFRA-03 Step 06).
 */
class AppNavigationManager(
    private val sessionManager: AuthenticationSessionManager
) {
    private val _currentDestination = MutableStateFlow<AppDestination>(AppDestination.Public.Home)
    val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

    private val backStack = mutableListOf<AppDestination>(AppDestination.Public.Home)

    /**
     * Navigates to a target [destination] with server-authoritative capability verification.
     */
    fun navigateTo(destination: AppDestination, principal: AuthenticatedPrincipal?): Boolean {
        // Prevent infinite self-redirection
        if (_currentDestination.value == destination) return true

        val authorized = CapabilityAwareNavigation.isRouteAuthorized(principal, destination)
        val target = if (authorized) destination else AppDestination.Security.Forbidden

        _currentDestination.value = target
        backStack.add(target)
        return authorized
    }

    /**
     * Process an incoming deep link route with anti-spoofing protection.
     */
    fun processDeepLink(
        route: String,
        principal: AuthenticatedPrincipal?,
        clientSuppliedUserId: String? = null,
        clientSuppliedProjectId: String? = null,
        clientSuppliedRole: String? = null
    ): AppDestination {
        val target = DeepLinkAuthorizer.authorizeDeepLink(
            route = route,
            principal = principal,
            clientSuppliedUserId = clientSuppliedUserId,
            clientSuppliedProjectId = clientSuppliedProjectId,
            clientSuppliedRole = clientSuppliedRole
        )
        _currentDestination.value = target
        backStack.add(target)
        return target
    }

    /**
     * Post-login automated navigation resolution.
     */
    fun syncWithPostLoginRouter(principal: AuthenticatedPrincipal) {
        val target = PostLoginRouter.resolveAppDestination(principal)
        _currentDestination.value = target
        backStack.clear()
        backStack.add(AppDestination.Public.Home)
        backStack.add(target)
    }

    /**
     * Secure logout: Clears session, resets navigation stack, prevents back-navigation to protected screens.
     */
    suspend fun performSecureLogout() {
        sessionManager.logout()
        backStack.clear()
        backStack.add(AppDestination.Public.Home)
        _currentDestination.value = AppDestination.Public.Home
    }

    /**
     * Session expiration handler: Clears protected stack, sets SessionExpired state.
     */
    fun handleSessionExpiration() {
        backStack.clear()
        backStack.add(AppDestination.Public.Home)
        backStack.add(AppDestination.Security.SessionExpired)
        _currentDestination.value = AppDestination.Security.SessionExpired
    }

    /**
     * Back navigation handler.
     */
    fun navigateBack(): Boolean {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
            _currentDestination.value = backStack.last()
            return true
        }
        return false
    }

    fun getBackStackDepth(): Int = backStack.size
}
