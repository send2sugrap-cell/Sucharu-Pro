package com.sucharu.sucharupro.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.model.VerificationType
import com.sucharu.sucharupro.data.auth.session.AppEntryState
import com.sucharu.sucharupro.data.auth.session.AuthenticationSessionManager
import com.sucharu.sucharupro.data.composition.DemoRole
import com.sucharu.sucharupro.ui.features.auth.ForgotPasswordScreen
import com.sucharu.sucharupro.ui.features.auth.LoginScreen
import com.sucharu.sucharupro.ui.features.auth.RegisterScreen
import com.sucharu.sucharupro.ui.features.auth.ResetPasswordScreen
import com.sucharu.sucharupro.ui.features.auth.VerificationScreen
import com.sucharu.sucharupro.ui.features.demo.DemoRoleSelectorScreen
import com.sucharu.sucharupro.ui.navigation.AppDestination
import com.sucharu.sucharupro.ui.navigation.AppNavigationManager
import kotlinx.coroutines.launch

/**
 * Top-Level Architecture Shell with Server-Authoritative Workspace Routing and Isolated Demo Showcase (INFRA-03 Step 06 & INFRA-06).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SucharuGraphicsAppShell(
    sessionManager: AuthenticationSessionManager,
    navigationManager: AppNavigationManager = remember(sessionManager) { AppNavigationManager(sessionManager) },
    isDemoMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val entryState by sessionManager.entryState.collectAsState()
    val currentDestination by navigationManager.currentDestination.collectAsState()

    var activeAuthScreenOverride by remember { mutableStateOf<String?>(null) }
    var pendingVerificationIdentifier by remember { mutableStateOf<String?>(null) }
    var pendingVerificationType by remember { mutableStateOf(VerificationType.PHONE) }
    var selectedDemoRole by remember { mutableStateOf(DemoRole.CUSTOMER) }
    var showDemoRoleMenu by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Intercept Android hardware / gesture Back actions safely across auth & demo flows
    BackHandler(enabled = activeAuthScreenOverride != null) {
        when (activeAuthScreenOverride) {
            "demo_verification" -> {
                errorMessage = null
                successMessage = null
                activeAuthScreenOverride = "demo_role_selector"
            }
            "demo_role_selector" -> {
                errorMessage = null
                successMessage = null
                activeAuthScreenOverride = null
            }
            "verification" -> {
                errorMessage = null
                successMessage = null
                activeAuthScreenOverride = "register"
            }
            "register" -> {
                errorMessage = null
                successMessage = null
                activeAuthScreenOverride = "login"
            }
            "forgot_password" -> {
                errorMessage = null
                successMessage = null
                activeAuthScreenOverride = "login"
            }
            "reset_password" -> {
                errorMessage = null
                successMessage = null
                activeAuthScreenOverride = "login"
            }
            "login" -> {
                errorMessage = null
                successMessage = null
                activeAuthScreenOverride = null
            }
            else -> {
                errorMessage = null
                successMessage = null
                activeAuthScreenOverride = null
            }
        }
    }

    LaunchedEffect(Unit) {
        sessionManager.restoreSession()
    }

    LaunchedEffect(entryState) {
        when (val state = entryState) {
            is AppEntryState.Authenticated -> {
                navigationManager.syncWithPostLoginRouter(state.principal)
            }
            is AppEntryState.Public, is AppEntryState.SessionExpired -> {
                if (currentDestination !is AppDestination.Public) {
                    navigationManager.navigateTo(AppDestination.Public.Home, null)
                }
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.clickable { 
                        navigationManager.navigateTo(AppDestination.Public.Home, (entryState as? AppEntryState.Authenticated)?.principal)
                    }) {
                        Text("SUCHARU GRAPHICS", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF9ECAFF))
                        Text("Commercial Printing ERP", fontSize = 10.sp, color = Color(0xFFB7C8D8))
                    }
                },
                actions = {
                    when (val state = entryState) {
                        is AppEntryState.Authenticated -> {
                            if (isDemoMode) {
                                Box {
                                    Surface(
                                        color = Color(0xFF00384D),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .padding(end = 4.dp)
                                            .clickable { showDemoRoleMenu = true }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "DEMO: ${state.principal.role.name}",
                                                color = Color(0xFF00B4D8),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("▼", color = Color(0xFF9ECAFF), fontSize = 8.sp)
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showDemoRoleMenu,
                                        onDismissRequest = { showDemoRoleMenu = false },
                                        modifier = Modifier.background(Color(0xFF1C2541))
                                    ) {
                                        DemoRole.entries.forEach { role ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(
                                                            text = "${role.iconDescription} ${role.displayName}",
                                                            color = if (role.userRole == state.principal.role) Color(0xFF00B4D8) else Color.White,
                                                            fontWeight = if (role.userRole == state.principal.role) FontWeight.Bold else FontWeight.Normal,
                                                            fontSize = 13.sp
                                                        )
                                                        Text(
                                                            text = "Role: ${role.userRole.name}",
                                                            color = Color(0xFF8692A6),
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    showDemoRoleMenu = false
                                                    selectedDemoRole = role
                                                    scope.launch {
                                                        isLoading = true
                                                        sessionManager.login(
                                                            com.sucharu.sucharupro.data.auth.model.LoginRequestDto(
                                                                identifier = role.demoUsername,
                                                                password = "demoPassword123!"
                                                            )
                                                        )
                                                        isLoading = false
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    color = Color(0xFF00497D),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(state.principal.username, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(state.principal.role.name, color = Color(0xFF9ECAFF), fontSize = 9.sp)
                                    }
                                }
                            }

                            TextButton(onClick = {
                                scope.launch {
                                    navigationManager.performSecureLogout()
                                    activeAuthScreenOverride = null
                                }
                            }) {
                                Text("Sign Out", color = Color(0xFFFFB4AB), fontSize = 11.sp)
                            }
                        }

                        else -> {
                            TextButton(onClick = { activeAuthScreenOverride = "login" }) {
                                Text("Sign In", color = Color(0xFF9ECAFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B132B))
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0B132B))
        ) {
            when {
                // Auth Screen Overrides (Login, Register, Recovery)
                activeAuthScreenOverride == "login" -> {
                    LoginScreen(
                        onLoginSubmit = { req ->
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                val res = sessionManager.login(req)
                                isLoading = false
                                if (res is com.sucharu.sucharupro.data.api.model.ApiResult.Success) {
                                    activeAuthScreenOverride = null
                                    successMessage = null
                                } else if (res is com.sucharu.sucharupro.data.api.model.ApiResult.Error) {
                                    errorMessage = res.errorResponse.message
                                }
                            }
                        },
                        onNavigateToRegister = {
                            errorMessage = null
                            successMessage = null
                            activeAuthScreenOverride = "register"
                        },
                        onNavigateToForgotPassword = {
                            errorMessage = null
                            successMessage = null
                            activeAuthScreenOverride = "forgot_password"
                        },
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        successMessage = successMessage
                    )
                }

                activeAuthScreenOverride == "register" -> {
                    RegisterScreen(
                        onRegisterSubmit = { req ->
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                val res = sessionManager.register(req)
                                isLoading = false
                                if (res is com.sucharu.sucharupro.data.api.model.ApiResult.Success) {
                                    pendingVerificationIdentifier = res.data.userId
                                    pendingVerificationType = VerificationType.PHONE
                                    successMessage = res.data.message
                                    activeAuthScreenOverride = "verification"
                                } else if (res is com.sucharu.sucharupro.data.api.model.ApiResult.Error) {
                                    errorMessage = res.errorResponse.message
                                }
                            }
                        },
                        onNavigateToLogin = {
                            errorMessage = null
                            successMessage = null
                            activeAuthScreenOverride = "login"
                        },
                        isLoading = isLoading,
                        errorMessage = errorMessage
                    )
                }

                activeAuthScreenOverride == "verification" -> {
                    VerificationScreen(
                        verificationType = pendingVerificationType,
                        recipient = pendingVerificationIdentifier ?: "Registered contact",
                        onBackClick = {
                            errorMessage = null
                            successMessage = null
                            activeAuthScreenOverride = "register"
                        },
                        onConfirmToken = { token ->
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                val res = sessionManager.confirmVerification(token, pendingVerificationType)
                                isLoading = false
                                if (res is com.sucharu.sucharupro.data.api.model.ApiResult.Success) {
                                    successMessage = "Account verified successfully! Please sign in."
                                    activeAuthScreenOverride = "login"
                                } else if (res is com.sucharu.sucharupro.data.api.model.ApiResult.Error) {
                                    errorMessage = res.errorResponse.message
                                }
                            }
                        },
                        onRequestResendToken = {
                            scope.launch {
                                val id = pendingVerificationIdentifier ?: return@launch
                                sessionManager.resendVerification(id)
                            }
                        },
                        onNavigateToHome = {
                            errorMessage = null
                            successMessage = null
                            activeAuthScreenOverride = "login"
                        },
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        successMessage = successMessage
                    )
                }

                activeAuthScreenOverride == "forgot_password" -> {
                    ForgotPasswordScreen(
                        onRequestRecovery = { req ->
                            scope.launch {
                                isLoading = true
                                val res = sessionManager.requestPasswordRecovery(req)
                                isLoading = false
                                if (res is com.sucharu.sucharupro.data.api.model.ApiResult.Success) {
                                    successMessage = res.data.message
                                }
                            }
                        },
                        onNavigateToResetConfirm = {
                            errorMessage = null
                            activeAuthScreenOverride = "reset_password"
                        },
                        onNavigateToLogin = {
                            errorMessage = null
                            successMessage = null
                            activeAuthScreenOverride = "login"
                        },
                        isLoading = isLoading,
                        confirmationMessage = successMessage
                    )
                }

                activeAuthScreenOverride == "demo_role_selector" -> {
                    DemoRoleSelectorScreen(
                        onSelectRole = { role ->
                            selectedDemoRole = role
                            errorMessage = null
                            successMessage = null
                            activeAuthScreenOverride = "demo_verification"
                        },
                        onBackToPublic = {
                            errorMessage = null
                            successMessage = null
                            activeAuthScreenOverride = null
                        }
                    )
                }

                activeAuthScreenOverride == "demo_verification" -> {
                    VerificationScreen(
                        verificationType = VerificationType.PHONE,
                        recipient = "${selectedDemoRole.displayName} (Demo OTP: 123456)",
                        onBackClick = {
                            errorMessage = null
                            successMessage = null
                            activeAuthScreenOverride = "demo_role_selector"
                        },
                        onConfirmToken = { token ->
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                val res = sessionManager.confirmVerification(token, VerificationType.PHONE)
                                if (res is com.sucharu.sucharupro.data.api.model.ApiResult.Success) {
                                    val loginRes = sessionManager.login(
                                        com.sucharu.sucharupro.data.auth.model.LoginRequestDto(
                                            identifier = selectedDemoRole.demoUsername,
                                            password = "demoPassword123!"
                                        )
                                    )
                                    isLoading = false
                                    if (loginRes is com.sucharu.sucharupro.data.api.model.ApiResult.Success) {
                                        activeAuthScreenOverride = null
                                        successMessage = null
                                    } else if (loginRes is com.sucharu.sucharupro.data.api.model.ApiResult.Error) {
                                        errorMessage = loginRes.errorResponse.message
                                    }
                                } else if (res is com.sucharu.sucharupro.data.api.model.ApiResult.Error) {
                                    isLoading = false
                                    errorMessage = res.errorResponse.message
                                }
                            }
                        },
                        onRequestResendToken = {
                            successMessage = "Demo OTP is: 123456"
                            errorMessage = null
                        },
                        onNavigateToHome = {
                            errorMessage = null
                            successMessage = null
                            activeAuthScreenOverride = null
                        },
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        successMessage = successMessage
                    )
                }

                activeAuthScreenOverride == "reset_password" -> {
                    ResetPasswordScreen(
                        onResetSubmit = { req ->
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                val res = sessionManager.confirmPasswordReset(req)
                                isLoading = false
                                if (res is com.sucharu.sucharupro.data.api.model.ApiResult.Success) {
                                    successMessage = "Password successfully reset! Please sign in with your new password."
                                    activeAuthScreenOverride = "login"
                                } else if (res is com.sucharu.sucharupro.data.api.model.ApiResult.Error) {
                                    errorMessage = res.errorResponse.message
                                }
                            }
                        },
                        onNavigateToLogin = {
                            errorMessage = null
                            successMessage = null
                            activeAuthScreenOverride = "login"
                        },
                        isLoading = isLoading,
                        errorMessage = errorMessage
                    )
                }

                // Default Navigation & Workspace Routing
                else -> {
                    if (currentDestination.isPublic) {
                        PublicWorkspaceShell(
                            currentDestination = currentDestination,
                            onNavigate = { dest -> navigationManager.navigateTo(dest, (entryState as? AppEntryState.Authenticated)?.principal) },
                            isDemoMode = isDemoMode,
                            onTryDemo = if (isDemoMode) { { activeAuthScreenOverride = "demo_role_selector" } } else null
                        )
                    } else {
                        when (val state = entryState) {
                            is AppEntryState.Initializing -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Color(0xFF9ECAFF))
                                }
                            }

                            is AppEntryState.Public, is AppEntryState.SessionExpired -> {
                                // Fallback for public routes if not explicitly handled by PublicWorkspaceShell
                                PublicWorkspaceShell(
                                    currentDestination = currentDestination,
                                    onNavigate = { dest -> navigationManager.navigateTo(dest, null) },
                                    isDemoMode = isDemoMode,
                                    onTryDemo = if (isDemoMode) { { activeAuthScreenOverride = "demo_role_selector" } } else null
                                )
                            }

                            is AppEntryState.Authenticated -> {
                                val principal = state.principal
                                when (principal.role) {
                                    UserRole.CUSTOMER -> {
                                        CustomerWorkspaceShell(
                                            principal = principal,
                                            currentDestination = currentDestination,
                                            onNavigate = { dest -> navigationManager.navigateTo(dest, principal) }
                                        )
                                    }
                                    UserRole.AFFILIATE -> {
                                        AffiliateWorkspaceShell(
                                            principal = principal,
                                            currentDestination = currentDestination,
                                            onNavigate = { dest -> navigationManager.navigateTo(dest, principal) }
                                        )
                                    }
                                    UserRole.STAFF, UserRole.MANAGER, UserRole.ADMIN -> {
                                        InternalWorkspaceShell(
                                            principal = principal,
                                            currentDestination = currentDestination,
                                            onNavigate = { dest -> navigationManager.navigateTo(dest, principal) }
                                        )
                                    }
                                    else -> {
                                        PublicWorkspaceShell(
                                            currentDestination = currentDestination,
                                            onNavigate = { dest -> navigationManager.navigateTo(dest, principal) },
                                            isDemoMode = isDemoMode,
                                            onTryDemo = if (isDemoMode) { { activeAuthScreenOverride = "demo_role_selector" } } else null
                                        )
                                    }
                                }
                            }

                            is AppEntryState.VerificationRequired -> {
                                SecurityStateView(
                                    destination = AppDestination.Security.VerificationRequired,
                                    onNavigateHome = { scope.launch { sessionManager.logout() } }
                                )
                            }

                            is AppEntryState.AccountUnavailable -> {
                                SecurityStateView(
                                    destination = AppDestination.Security.AccountUnavailable,
                                    onNavigateHome = { scope.launch { sessionManager.logout() } }
                                )
                            }

                            else -> {
                                navigationManager.navigateTo(AppDestination.Public.Home, null)
                            }
                        }
                    }
                }
            }
        }
    }
}
