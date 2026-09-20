package com.sucharu.sucharupro.ui.admin.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.ui.admin.theme.AdminTheme
import com.sucharu.sucharupro.ui.navigation.AppDestination
import com.sucharu.sucharupro.ui.navigation.CapabilityAwareNavigation
import kotlinx.coroutines.launch

/**
 * Responsive Sucharu Pro Admin Panel Application Shell.
 *
 * Wraps all Admin Panel screens with:
 * 1. Responsive Sidebar / Navigation Rail (Desktop, Tablet, Mobile Drawer)
 * 2. Top Application Bar (Contextual Title, Notifications, Tenant Badge, User Profile)
 * 3. Breadcrumb & Page Context Header
 * 4. Server-Authoritative Capability Authorization Guard (Renders [AdminAccessDeniedContent] on 403 Forbidden)
 *
 * @param currentDestination Currently active AppDestination.
 * @param principal Currently authenticated principal context.
 * @param modifier Optional modifier.
 * @param breadcrumbItems Hierarchy list for page context (e.g. ["Admin", "Production", "Job Card #101"]).
 * @param onNavigateTo Destination click handler.
 * @param onNavigateBack Back navigation handler.
 * @param content Slot for screen content.
 */
@Composable
fun AdminShell(
    currentDestination: AppDestination,
    principal: AuthenticatedPrincipal?,
    modifier: Modifier = Modifier,
    breadcrumbItems: List<String> = listOf("Admin Control Center", currentDestination.title),
    onNavigateTo: (destination: AppDestination) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    val isTablet = screenWidthDp in 600..839
    val isMobile = screenWidthDp < 600

    var isCompactSidebar by remember { mutableStateOf(isTablet) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val isAuthorized = remember(currentDestination, principal) {
        CapabilityAwareNavigation.isRouteAuthorized(principal, currentDestination)
    }

    AdminTheme {
        if (isMobile) {
            // Mobile Layout: Modal Navigation Drawer + TopBar + Content
            ModalNavigationDrawer(
                drawerState = drawerState,
                scrimColor = Color.Black.copy(alpha = 0.65f),
                modifier = modifier.fillMaxSize(),
                drawerContent = {
                    AdminSidebar(
                        currentDestination = currentDestination,
                        principal = principal,
                        isCompact = false,
                        onDestinationSelect = { dest ->
                            coroutineScope.launch { drawerState.close() }
                            onNavigateTo(dest)
                        }
                    )
                }
            ) {
                AdminShellScaffold(
                    currentDestination = currentDestination,
                    principal = principal,
                    breadcrumbItems = breadcrumbItems,
                    isAuthorized = isAuthorized,
                    onToggleNavigation = { coroutineScope.launch { drawerState.open() } },
                    onNavigateTo = onNavigateTo,
                    content = content
                )
            }
        } else {
            // Desktop / Tablet Layout: Side-by-side Sidebar + Content
            Row(
                modifier = modifier.fillMaxSize()
            ) {
                AdminSidebar(
                    currentDestination = currentDestination,
                    principal = principal,
                    isCompact = isCompactSidebar,
                    onDestinationSelect = onNavigateTo,
                    onToggleCompact = { isCompactSidebar = !isCompactSidebar }
                )

                AdminShellScaffold(
                    currentDestination = currentDestination,
                    principal = principal,
                    breadcrumbItems = breadcrumbItems,
                    isAuthorized = isAuthorized,
                    onToggleNavigation = if (isTablet) { { isCompactSidebar = !isCompactSidebar } } else null,
                    onNavigateTo = onNavigateTo,
                    content = content
                )
            }
        }
    }
}

@Composable
private fun AdminShellScaffold(
    currentDestination: AppDestination,
    principal: AuthenticatedPrincipal?,
    breadcrumbItems: List<String>,
    isAuthorized: Boolean,
    onToggleNavigation: (() -> Unit)?,
    onNavigateTo: (destination: AppDestination) -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AdminTheme.colors.background,
        topBar = {
            AdminTopBar(
                title = currentDestination.title,
                subtitle = "Subsystem: ${currentDestination.route}",
                principal = principal,
                notificationCount = 0,
                onToggleNavigation = onToggleNavigation,
                onNotificationsClick = { onNavigateTo(AppDestination.Admin.Notifications) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = AdminTheme.spacing.screenPadding)
        ) {
            Spacer(modifier = Modifier.height(AdminTheme.spacing.md))

            // Breadcrumb Header
            AdminBreadcrumb(
                items = breadcrumbItems,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AdminTheme.spacing.md))

            // Main Content Area (Authorization Guarded)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (isAuthorized) {
                    content()
                } else {
                    AdminAccessDeniedContent(
                        destination = currentDestination,
                        principal = principal,
                        onReturnHome = {
                            onNavigateTo(AppDestination.Admin.FullAdministration)
                        }
                    )
                }
            }
        }
    }
}
