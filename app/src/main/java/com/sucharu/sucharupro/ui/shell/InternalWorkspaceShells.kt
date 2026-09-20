package com.sucharu.sucharupro.ui.shell

import androidx.compose.foundation.background
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.ui.features.dashboard.DashboardScreen
import com.sucharu.sucharupro.ui.navigation.AppDestination
import com.sucharu.sucharupro.data.composition.AppRuntimeComposition

/**
 * Command Center Workspace Shell for ADMIN Role (INFRA-05 Step 03 & Admin Operations Center).
 */
@Composable
fun AdminWorkspaceShell(
    principal: AuthenticatedPrincipal,
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    composition: AppRuntimeComposition? = null,
    modifier: Modifier = Modifier
) {
    com.sucharu.sucharupro.ui.admin.shell.AdminShell(
        currentDestination = currentDestination,
        principal = principal,
        modifier = modifier,
        onNavigateTo = onNavigate
    ) {
        val orderRepo = remember(composition) {
            composition?.orderRepository ?: com.sucharu.sucharupro.data.repository.OrderRepositoryImpl(
                dataSource = com.sucharu.sucharupro.data.datasource.FakeOrderDataSource()
            )
        }
        val dashboardRepo = remember(composition) {
            composition?.dashboardRepository ?: com.sucharu.sucharupro.data.repository.FakeDashboardRepository()
        }

        when (currentDestination) {
            AppDestination.Admin.FullAdministration -> {
                val dashboardViewModel: com.sucharu.sucharupro.ui.features.dashboard.DashboardViewModel = viewModel {
                    com.sucharu.sucharupro.ui.features.dashboard.DashboardViewModel(repository = dashboardRepo)
                }
                com.sucharu.sucharupro.ui.admin.screens.UnifiedAdminDashboardScreen(
                    viewModel = dashboardViewModel,
                    principal = principal,
                    onNavigateToDestination = onNavigate
                )
            }
            AppDestination.Customer.Quotations -> {
                val wizardViewModel: com.sucharu.sucharupro.ui.features.orders.order.wizard.OrderPlacementWizardViewModel = viewModel {
                    com.sucharu.sucharupro.ui.features.orders.order.wizard.OrderPlacementWizardViewModel(
                        orderRepository = orderRepo
                    )
                }
                com.sucharu.sucharupro.ui.features.orders.order.wizard.OrderPlacementWizardScreen(
                    viewModel = wizardViewModel,
                    onBackClick = { onNavigate(AppDestination.Admin.FullAdministration) },
                    onOrderCreated = { onNavigate(AppDestination.Customer.Orders) }
                )
            }
            AppDestination.Admin.PrintingCalculator -> {
                val calcService = remember(composition) {
                    composition?.printingCalculatorService ?: com.sucharu.sucharupro.domain.service.printingcalculator.PrintingCalculatorServiceImpl(
                        repository = com.sucharu.sucharupro.data.repository.printingcalculator.PrintingCalculatorRepositoryImpl(
                            dataSource = com.sucharu.sucharupro.data.datasource.printingcalculator.FakePrintingCalculatorDataSource()
                        )
                    )
                }
                val calcViewModel: com.sucharu.sucharupro.ui.features.printing.calculator.PrintingCalculatorViewModel = viewModel {
                    com.sucharu.sucharupro.ui.features.printing.calculator.PrintingCalculatorViewModel(
                        calculatorService = calcService
                    )
                }
                val calcUiState by calcViewModel.uiState.collectAsState()
                com.sucharu.sucharupro.ui.features.printing.calculator.PrintingCalculatorScreen(
                    onCalculate = { calcViewModel.calculate(it) },
                    calculationResult = calcUiState.calculationResult,
                    validationResult = calcUiState.validationResult,
                    isLoading = calcUiState.isLoading,
                    errorMessage = calcUiState.errorMessage
                )
            }
            AppDestination.Customer.Orders -> {
                val orderListViewModel: com.sucharu.sucharupro.ui.features.orders.order.OrderListViewModel = viewModel {
                    com.sucharu.sucharupro.ui.features.orders.order.OrderListViewModel(repository = orderRepo)
                }
                com.sucharu.sucharupro.ui.features.orders.order.OrderListScreen(
                    viewModel = orderListViewModel,
                    onOrderClick = { orderId -> onNavigate(AppDestination.Customer.OrderDetails(orderId)) }
                )
            }
            AppDestination.Staff.Production, AppDestination.Manager.Production -> {
                val prodRepo: com.sucharu.sucharupro.domain.repository.ProductionJobRepository = remember {
                    com.sucharu.sucharupro.data.repository.ProductionJobRepositoryImpl(
                        dataSource = com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource()
                    )
                }
                val prodViewModel: com.sucharu.sucharupro.ui.features.production.job.list.ProductionJobListViewModel = viewModel {
                    com.sucharu.sucharupro.ui.features.production.job.list.ProductionJobListViewModel(repository = prodRepo)
                }
                com.sucharu.sucharupro.ui.features.production.job.list.ProductionJobListScreen(
                    viewModel = prodViewModel,
                    onJobClick = {}
                )
            }
            AppDestination.Staff.Delivery, AppDestination.Manager.Delivery -> {
                val delRepo: com.sucharu.sucharupro.domain.repository.DeliveryOrderRepository = remember {
                    com.sucharu.sucharupro.data.repository.DeliveryOrderRepositoryImpl(
                        dataSource = com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource()
                    )
                }
                val delViewModel: com.sucharu.sucharupro.ui.features.delivery.DeliveryOrderListViewModel = viewModel {
                    com.sucharu.sucharupro.ui.features.delivery.DeliveryOrderListViewModel(repository = delRepo)
                }
                com.sucharu.sucharupro.ui.features.delivery.DeliveryOrderListScreen(
                    projectId = principal.projectId,
                    viewModel = delViewModel,
                    onCreateDeliveryOrder = {},
                    onDeliveryOrderClick = {},
                    onViewDispatchRequests = {}
                )
            }
            AppDestination.Admin.Users -> {
                com.sucharu.sucharupro.ui.admin.screens.AdminCustomerManagementScreen(
                    principal = principal,
                    onNavigateToDestination = onNavigate
                )
            }
            AppDestination.Admin.PrepressOrchestration -> {
                com.sucharu.sucharupro.ui.admin.screens.AdminProductionOperationsScreen(
                    principal = principal,
                    onNavigateToDestination = onNavigate
                )
            }
            AppDestination.Admin.Finance -> {
                com.sucharu.sucharupro.ui.admin.screens.AdminFinanceOperationsScreen(
                    principal = principal,
                    onNavigateToDestination = onNavigate
                )
            }
            AppDestination.Admin.Reports -> {
                com.sucharu.sucharupro.ui.admin.screens.AdminReportsAnalyticsScreen(
                    principal = principal,
                    onNavigateToDestination = onNavigate
                )
            }
            AppDestination.Admin.AffiliateManagement -> {
                com.sucharu.sucharupro.ui.admin.screens.AdminAffiliateGovernanceScreen(
                    principal = principal,
                    onNavigateToDestination = onNavigate
                )
            }
            AppDestination.Admin.SubstrateReservation -> {
                com.sucharu.sucharupro.ui.admin.screens.AdminInventoryLogisticsScreen(
                    principal = principal,
                    onNavigateToDestination = onNavigate
                )
            }
            AppDestination.Admin.ShopFloorTracking -> {
                com.sucharu.sucharupro.ui.admin.screens.AdminMachineOeeScreen(
                    principal = principal,
                    onNavigateToDestination = onNavigate
                )
            }
            else -> {
                com.sucharu.sucharupro.ui.admin.screens.AdminPanelFoundationScreen(
                    onNavigateBack = { onNavigate(AppDestination.Admin.FullAdministration) }
                )
            }
        }
    }
}

/**
 * Responsive Dark Navy Foundation Workspace Shell for Internal ERP Roles: STAFF & MANAGER (INFRA-03 Step 06 & INFRA-05 Step 03).
 */
@Composable
fun InternalWorkspaceShell(
    principal: AuthenticatedPrincipal,
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    composition: AppRuntimeComposition? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B132B))
            .padding(16.dp)
    ) {
        // Internal ERP Header
        Surface(
            color = Color(0xFF1C2541),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = when (principal.role) {
                            UserRole.STAFF -> "STAFF PRODUCTION WORKSPACE"
                            UserRole.MANAGER -> "MANAGER OPERATIONS & APPROVAL CENTER"
                            UserRole.ADMIN -> "ADMIN SYSTEM CONTROL & SECURITY CENTER"
                            else -> "INTERNAL ERP WORKSPACE"
                        },
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9ECAFF),
                        fontSize = 16.sp
                    )
                    Text("User: ${principal.username} (${principal.role.name})", color = Color.White, fontSize = 12.sp)
                    Text("Project: ${principal.projectId}", color = Color(0xFFB7C8D8), fontSize = 10.sp)
                }
                Surface(
                    color = when (principal.role) {
                        UserRole.ADMIN -> Color(0xFF8C1D40)
                        UserRole.MANAGER -> Color(0xFF00497D)
                        else -> Color(0xFF005A36)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = principal.role.name,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Role-Specific Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (principal.role) {
                UserRole.STAFF -> {
                    FilterChip(selected = currentDestination == AppDestination.Staff.AssignedWork, onClick = { onNavigate(AppDestination.Staff.AssignedWork) }, label = { Text("Assigned Work") })
                    FilterChip(selected = currentDestination == AppDestination.Staff.Production, onClick = { onNavigate(AppDestination.Staff.Production) }, label = { Text("Production") })
                    FilterChip(selected = currentDestination == AppDestination.Staff.ProductionScheduling, onClick = { onNavigate(AppDestination.Staff.ProductionScheduling) }, label = { Text("Scheduling & Queue") })
                    FilterChip(selected = currentDestination == AppDestination.Staff.ShopFloorTracking, onClick = { onNavigate(AppDestination.Staff.ShopFloorTracking) }, label = { Text("Live Tracking") })
                    FilterChip(selected = currentDestination == AppDestination.Staff.FinalQcPackaging, onClick = { onNavigate(AppDestination.Staff.FinalQcPackaging) }, label = { Text("Final QC & Pack") })
                    FilterChip(selected = currentDestination == AppDestination.Staff.ProductionJobCosting, onClick = { onNavigate(AppDestination.Staff.ProductionJobCosting) }, label = { Text("Job Cost & Variance") })
                    FilterChip(selected = currentDestination == AppDestination.Staff.ProductionJobClosure, onClick = { onNavigate(AppDestination.Staff.ProductionJobClosure) }, label = { Text("Job Closure & Seal") })
                    FilterChip(selected = currentDestination == AppDestination.Staff.Imposition, onClick = { onNavigate(AppDestination.Staff.Imposition) }, label = { Text("Dynamic Imposition") })
                    FilterChip(selected = currentDestination == AppDestination.Staff.GangRun, onClick = { onNavigate(AppDestination.Staff.GangRun) }, label = { Text("Gang-Run Optimizer") })
                    FilterChip(selected = currentDestination == AppDestination.Staff.DynamicNesting, onClick = { onNavigate(AppDestination.Staff.DynamicNesting) }, label = { Text("2D Dynamic Nesting") })
                    FilterChip(selected = currentDestination == AppDestination.Staff.SignatureImposition, onClick = { onNavigate(AppDestination.Staff.SignatureImposition) }, label = { Text("Signature Imposition") })
                    FilterChip(selected = currentDestination == AppDestination.Staff.CtpOutput, onClick = { onNavigate(AppDestination.Staff.CtpOutput) }, label = { Text("CTP Plates & Marks") })
                    FilterChip(selected = currentDestination == AppDestination.Staff.Workflows, onClick = { onNavigate(AppDestination.Staff.Workflows) }, label = { Text("Workflows") })
                    FilterChip(selected = currentDestination == AppDestination.Staff.Qc, onClick = { onNavigate(AppDestination.Staff.Qc) }, label = { Text("QC") })
                    FilterChip(selected = currentDestination == AppDestination.Staff.Inventory, onClick = { onNavigate(AppDestination.Staff.Inventory) }, label = { Text("Inventory") })
                    FilterChip(selected = currentDestination == AppDestination.Staff.Delivery, onClick = { onNavigate(AppDestination.Staff.Delivery) }, label = { Text("Delivery") })
                }
                UserRole.MANAGER -> {
                    FilterChip(selected = currentDestination == AppDestination.Manager.Operations, onClick = { onNavigate(AppDestination.Manager.Operations) }, label = { Text("Operations") })
                    FilterChip(selected = currentDestination == AppDestination.Manager.Approvals, onClick = { onNavigate(AppDestination.Manager.Approvals) }, label = { Text("Approvals") })
                    FilterChip(selected = currentDestination == AppDestination.Manager.Workflows, onClick = { onNavigate(AppDestination.Manager.Workflows) }, label = { Text("Workflows") })
                    FilterChip(selected = currentDestination == AppDestination.Manager.Production, onClick = { onNavigate(AppDestination.Manager.Production) }, label = { Text("Production") })
                    FilterChip(selected = currentDestination == AppDestination.Manager.ProductionScheduling, onClick = { onNavigate(AppDestination.Manager.ProductionScheduling) }, label = { Text("Scheduling & Capacity") })
                    FilterChip(selected = currentDestination == AppDestination.Manager.ShopFloorTracking, onClick = { onNavigate(AppDestination.Manager.ShopFloorTracking) }, label = { Text("Live Tracking") })
                    FilterChip(selected = currentDestination == AppDestination.Manager.FinalQcPackaging, onClick = { onNavigate(AppDestination.Manager.FinalQcPackaging) }, label = { Text("Final QC & Pack") })
                    FilterChip(selected = currentDestination == AppDestination.Manager.ProductionJobCosting, onClick = { onNavigate(AppDestination.Manager.ProductionJobCosting) }, label = { Text("Job Cost & Variance") })
                    FilterChip(selected = currentDestination == AppDestination.Manager.ProductionJobClosure, onClick = { onNavigate(AppDestination.Manager.ProductionJobClosure) }, label = { Text("Job Closure & Seal") })
                    FilterChip(selected = currentDestination == AppDestination.Manager.Imposition, onClick = { onNavigate(AppDestination.Manager.Imposition) }, label = { Text("Dynamic Imposition") })
                    FilterChip(selected = currentDestination == AppDestination.Manager.GangRun, onClick = { onNavigate(AppDestination.Manager.GangRun) }, label = { Text("Gang-Run Optimizer") })
                    FilterChip(selected = currentDestination == AppDestination.Manager.DynamicNesting, onClick = { onNavigate(AppDestination.Manager.DynamicNesting) }, label = { Text("2D Dynamic Nesting") })
                    FilterChip(selected = currentDestination == AppDestination.Manager.SignatureImposition, onClick = { onNavigate(AppDestination.Manager.SignatureImposition) }, label = { Text("Signature Imposition") })
                    FilterChip(selected = currentDestination == AppDestination.Manager.CtpOutput, onClick = { onNavigate(AppDestination.Manager.CtpOutput) }, label = { Text("CTP Plates & Marks") })
                    FilterChip(selected = currentDestination == AppDestination.Manager.FinalQcPackaging, onClick = { onNavigate(AppDestination.Manager.FinalQcPackaging) }, label = { Text("Final QC") })
                    FilterChip(selected = currentDestination == AppDestination.Manager.Inventory, onClick = { onNavigate(AppDestination.Manager.Inventory) }, label = { Text("Inventory") })
                    FilterChip(selected = currentDestination == AppDestination.Manager.Delivery, onClick = { onNavigate(AppDestination.Manager.Delivery) }, label = { Text("Delivery") })
                }
                else -> {}
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Destination Panel
        if (currentDestination == AppDestination.Admin.FullAdministration ||
            currentDestination == AppDestination.Manager.Operations ||
            currentDestination == AppDestination.Staff.AssignedWork
        ) {
            val dashRepo = remember(composition) {
                composition?.dashboardRepository ?: com.sucharu.sucharupro.data.repository.FakeDashboardRepository()
            }
            val defaultHome = when (principal.role) {
                UserRole.STAFF -> AppDestination.Staff.AssignedWork
                UserRole.MANAGER -> AppDestination.Manager.Operations
                UserRole.ADMIN -> AppDestination.Admin.FullAdministration
                else -> AppDestination.Public.Home
            }
            DashboardScreen(
                viewModel = viewModel { com.sucharu.sucharupro.ui.features.dashboard.DashboardViewModel(repository = dashRepo) },
                onNavigateToNewOrder = { onNavigate(AppDestination.Staff.Production) },
                onNavigateToOrders = { onNavigate(AppDestination.Staff.AssignedWork) },
                onNavigateToOrderDetail = { orderId -> onNavigate(AppDestination.Customer.OrderDetails(orderId)) },
                onNavigateToProductionStage = { onNavigate(AppDestination.Staff.Production) },
                onNavigateToPrintingCalculator = { onNavigate(AppDestination.Public.Home) },
                onNavigateToCustomers = { onNavigate(AppDestination.Admin.Users) },
                onNavigateToInvoices = { onNavigate(AppDestination.Admin.Finance) },
                onNavigateToInventory = { onNavigate(AppDestination.Staff.SubstrateReservation) },
                userRole = try {
                    com.sucharu.sucharupro.domain.model.user.UserRole.valueOf(principal.role.name)
                } catch (_: Exception) {
                    com.sucharu.sucharupro.domain.model.user.UserRole.STAFF
                },
                modifier = Modifier.weight(1f)
            )
        } else if (currentDestination is AppDestination.Staff.Workflows ||
            currentDestination is AppDestination.Manager.Workflows ||
            currentDestination is AppDestination.Admin.Workflows ||
            currentDestination is AppDestination.Admin.WorkflowMetrics ||
            currentDestination is AppDestination.Admin.WorkflowAudit
        ) {
            val defaultHome = when (principal.role) {
                UserRole.STAFF -> AppDestination.Staff.AssignedWork
                UserRole.MANAGER -> AppDestination.Manager.Operations
                UserRole.ADMIN -> AppDestination.Admin.FullAdministration
                else -> AppDestination.Public.Home
            }
            val prodRepo: com.sucharu.sucharupro.domain.repository.ProductionJobRepository = remember {
                com.sucharu.sucharupro.data.repository.ProductionJobRepositoryImpl(
                    dataSource = com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource()
                )
            }
            com.sucharu.sucharupro.ui.features.production.monitoring.ProductionMonitoringDashboardScreen(
                viewModel = viewModel {
                    com.sucharu.sucharupro.ui.features.production.monitoring.ProductionMonitoringDashboardViewModel(
                        repository = prodRepo
                    )
                },
                onOpenJobDetails = {},
                onOpenOperatorQueue = {},
                onNavigateBack = { onNavigate(defaultHome) }
            )
        } else if (currentDestination == AppDestination.Staff.Production ||
            currentDestination == AppDestination.Manager.Production
        ) {
            val defaultHome = when (principal.role) {
                UserRole.STAFF -> AppDestination.Staff.AssignedWork
                UserRole.MANAGER -> AppDestination.Manager.Operations
                UserRole.ADMIN -> AppDestination.Admin.FullAdministration
                else -> AppDestination.Public.Home
            }
            val prodRepo: com.sucharu.sucharupro.domain.repository.ProductionJobRepository = remember {
                com.sucharu.sucharupro.data.repository.ProductionJobRepositoryImpl(
                    dataSource = com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource()
                )
            }
            com.sucharu.sucharupro.ui.features.production.job.list.ProductionJobListScreen(
                viewModel = viewModel {
                    com.sucharu.sucharupro.ui.features.production.job.list.ProductionJobListViewModel(
                        repository = prodRepo
                    )
                },
                onJobClick = {},
                modifier = Modifier.weight(1f)
            )
        } else if (currentDestination == AppDestination.Staff.ProductionScheduling ||
            currentDestination == AppDestination.Manager.ProductionScheduling ||
            currentDestination == AppDestination.Admin.ProductionScheduling
        ) {
            val defaultHome = when (principal.role) {
                UserRole.STAFF -> AppDestination.Staff.AssignedWork
                UserRole.MANAGER -> AppDestination.Manager.Operations
                UserRole.ADMIN -> AppDestination.Admin.FullAdministration
                else -> AppDestination.Public.Home
            }
            com.sucharu.sucharupro.ui.features.production.scheduling.ProductionSchedulingCommandCenterScreen(
                schedule = null,
                isLoading = false,
                onNavigateBack = { onNavigate(defaultHome) }
            )
        } else if (currentDestination == AppDestination.Staff.ShopFloorTracking ||
            currentDestination == AppDestination.Manager.ShopFloorTracking ||
            currentDestination == AppDestination.Admin.ShopFloorTracking
        ) {
            val defaultHome = when (principal.role) {
                UserRole.STAFF -> AppDestination.Staff.AssignedWork
                UserRole.MANAGER -> AppDestination.Manager.Operations
                UserRole.ADMIN -> AppDestination.Admin.FullAdministration
                else -> AppDestination.Public.Home
            }
            com.sucharu.sucharupro.ui.features.production.tracking.ShopFloorTrackingCommandCenterScreen(
                jobId = "JOB-LIVE-001",
                isLoading = false,
                onNavigateBack = { onNavigate(defaultHome) }
            )
        } else if (currentDestination == AppDestination.Staff.FinalQcPackaging ||
            currentDestination == AppDestination.Manager.FinalQcPackaging ||
            currentDestination == AppDestination.Admin.FinalQcPackaging
        ) {
            val defaultHome = when (principal.role) {
                UserRole.STAFF -> AppDestination.Staff.AssignedWork
                UserRole.MANAGER -> AppDestination.Manager.Operations
                UserRole.ADMIN -> AppDestination.Admin.FullAdministration
                else -> AppDestination.Public.Home
            }
            com.sucharu.sucharupro.ui.features.production.finalqc.FinalQcPackagingCommandCenterScreen(
                jobId = "JOB-FINAL-001",
                isLoading = false,
                onNavigateBack = { onNavigate(defaultHome) }
            )
        } else if (currentDestination == AppDestination.Staff.ProductionJobCosting ||
            currentDestination == AppDestination.Manager.ProductionJobCosting ||
            currentDestination == AppDestination.Admin.ProductionJobCosting
        ) {
            val defaultHome = when (principal.role) {
                UserRole.STAFF -> AppDestination.Staff.AssignedWork
                UserRole.MANAGER -> AppDestination.Manager.Operations
                UserRole.ADMIN -> AppDestination.Admin.FullAdministration
                else -> AppDestination.Public.Home
            }
            com.sucharu.sucharupro.ui.features.production.jobcosting.ProductionJobCostingCommandCenterScreen(
                jobId = "JOB-COST-001",
                isLoading = false,
                onNavigateBack = { onNavigate(defaultHome) }
            )
        } else if (currentDestination == AppDestination.Staff.ProductionJobClosure ||
            currentDestination == AppDestination.Manager.ProductionJobClosure ||
            currentDestination == AppDestination.Admin.ProductionJobClosure
        ) {
            val defaultHome = when (principal.role) {
                UserRole.STAFF -> AppDestination.Staff.AssignedWork
                UserRole.MANAGER -> AppDestination.Manager.Operations
                UserRole.ADMIN -> AppDestination.Admin.FullAdministration
                else -> AppDestination.Public.Home
            }
            com.sucharu.sucharupro.ui.features.production.jobclosure.ProductionJobClosureCommandCenterScreen(
                jobId = "JOB-CLOSE-001",
                isLoading = false,
                onNavigateBack = { onNavigate(defaultHome) }
            )
        } else if (currentDestination == AppDestination.Staff.SubstrateReservation ||
            currentDestination == AppDestination.Manager.SubstrateReservation ||
            currentDestination == AppDestination.Admin.SubstrateReservation
        ) {
            val defaultHome = when (principal.role) {
                UserRole.STAFF -> AppDestination.Staff.AssignedWork
                UserRole.MANAGER -> AppDestination.Manager.Operations
                UserRole.ADMIN -> AppDestination.Admin.FullAdministration
                else -> AppDestination.Public.Home
            }
            val resService = remember {
                com.sucharu.sucharupro.domain.service.substratereservation.SubstrateReservationServiceImpl(
                    repository = com.sucharu.sucharupro.data.repository.substratereservation.SubstrateReservationRepositoryImpl(
                        dataSource = com.sucharu.sucharupro.data.datasource.substratereservation.FakeSubstrateReservationDataSource()
                    )
                )
            }
            com.sucharu.sucharupro.ui.features.inventory.substratereservation.SubstrateReservationCommandCenterScreen(
                viewModel = viewModel {
                    com.sucharu.sucharupro.ui.features.inventory.substratereservation.SubstrateReservationViewModel(
                        reservationService = resService,
                        defaultTenantId = principal.projectId
                    )
                },
                onNavigateBack = { onNavigate(defaultHome) }
            )
        } else if (currentDestination == AppDestination.Staff.Imposition ||
            currentDestination == AppDestination.Manager.Imposition ||
            currentDestination == AppDestination.Admin.Imposition
        ) {
            val defaultHome = when (principal.role) {
                UserRole.STAFF -> AppDestination.Staff.AssignedWork
                UserRole.MANAGER -> AppDestination.Manager.Operations
                UserRole.ADMIN -> AppDestination.Admin.FullAdministration
                else -> AppDestination.Public.Home
            }
            com.sucharu.sucharupro.ui.features.imposition.ImpositionCommandCenterScreen(
                viewModel = viewModel { com.sucharu.sucharupro.ui.features.imposition.ImpositionViewModel() },
                onNavigateBack = { onNavigate(defaultHome) }
            )
        } else if (currentDestination == AppDestination.Staff.GangRun ||
            currentDestination == AppDestination.Manager.GangRun ||
            currentDestination == AppDestination.Admin.GangRun
        ) {
            val defaultHome = when (principal.role) {
                UserRole.STAFF -> AppDestination.Staff.AssignedWork
                UserRole.MANAGER -> AppDestination.Manager.Operations
                UserRole.ADMIN -> AppDestination.Admin.FullAdministration
                else -> AppDestination.Public.Home
            }
            com.sucharu.sucharupro.ui.features.imposition.GangRunCommandCenterScreen(
                viewModel = viewModel { com.sucharu.sucharupro.ui.features.imposition.GangRunViewModel() },
                onNavigateBack = { onNavigate(defaultHome) }
            )
        } else if (currentDestination == AppDestination.Staff.DynamicNesting ||
            currentDestination == AppDestination.Manager.DynamicNesting ||
            currentDestination == AppDestination.Admin.DynamicNesting
        ) {
            val defaultHome = when (principal.role) {
                UserRole.STAFF -> AppDestination.Staff.AssignedWork
                UserRole.MANAGER -> AppDestination.Manager.Operations
                UserRole.ADMIN -> AppDestination.Admin.FullAdministration
                else -> AppDestination.Public.Home
            }
            com.sucharu.sucharupro.ui.features.imposition.DynamicNestingCommandCenterScreen(
                viewModel = viewModel { com.sucharu.sucharupro.ui.features.imposition.NestingViewModel() },
                onNavigateBack = { onNavigate(defaultHome) }
            )
        } else if (currentDestination == AppDestination.Staff.SignatureImposition ||
            currentDestination == AppDestination.Manager.SignatureImposition ||
            currentDestination == AppDestination.Admin.SignatureImposition
        ) {
            val defaultHome = when (principal.role) {
                UserRole.STAFF -> AppDestination.Staff.AssignedWork
                UserRole.MANAGER -> AppDestination.Manager.Operations
                UserRole.ADMIN -> AppDestination.Admin.FullAdministration
                else -> AppDestination.Public.Home
            }
            com.sucharu.sucharupro.ui.features.imposition.SignatureImpositionCommandCenterScreen(
                viewModel = viewModel { com.sucharu.sucharupro.ui.features.imposition.SignatureViewModel() },
                onNavigateBack = { onNavigate(defaultHome) }
            )
        } else if (currentDestination == AppDestination.Staff.CtpOutput ||
            currentDestination == AppDestination.Manager.CtpOutput ||
            currentDestination == AppDestination.Admin.CtpOutput
        ) {
            val defaultHome = when (principal.role) {
                UserRole.STAFF -> AppDestination.Staff.AssignedWork
                UserRole.MANAGER -> AppDestination.Manager.Operations
                UserRole.ADMIN -> AppDestination.Admin.FullAdministration
                else -> AppDestination.Public.Home
            }
            com.sucharu.sucharupro.ui.features.imposition.CtpOutputCommandCenterScreen(
                viewModel = viewModel { com.sucharu.sucharupro.ui.features.imposition.CtpViewModel() },
                onNavigateBack = { onNavigate(defaultHome) }
            )
        } else if (currentDestination == AppDestination.Admin.PrepressOrchestration
        ) {
            val defaultHome = when (principal.role) {
                UserRole.STAFF -> AppDestination.Staff.AssignedWork
                UserRole.MANAGER -> AppDestination.Manager.Operations
                UserRole.ADMIN -> AppDestination.Admin.FullAdministration
                else -> AppDestination.Public.Home
            }
            com.sucharu.sucharupro.ui.features.imposition.PrepressOrchestrationCommandCenterScreen(
                viewModel = viewModel { com.sucharu.sucharupro.ui.features.imposition.PrepressOrchestrationViewModel() },
                onNavigateBack = { onNavigate(defaultHome) }
            )
        } else if (currentDestination == AppDestination.Admin.SubstrateBatchSelection
        ) {
            com.sucharu.sucharupro.ui.features.substratereservation.SubstrateBatchSelectionCommandCenterScreen(
                viewModel = viewModel { com.sucharu.sucharupro.ui.features.substratereservation.SubstrateBatchSelectionViewModel() }
            )
        } else if (currentDestination == AppDestination.Staff.SubstrateReplenishment ||
            currentDestination == AppDestination.Manager.SubstrateReplenishment ||
            currentDestination == AppDestination.Admin.SubstrateReplenishment
        ) {
            val defaultHome = when (principal.role) {
                UserRole.STAFF -> AppDestination.Staff.AssignedWork
                UserRole.MANAGER -> AppDestination.Manager.Operations
                UserRole.ADMIN -> AppDestination.Admin.FullAdministration
                else -> AppDestination.Public.Home
            }
            com.sucharu.sucharupro.ui.features.substratereservation.SubstrateReplenishmentCommandCenterScreen(
                viewModel = viewModel { com.sucharu.sucharupro.ui.features.substratereservation.SubstrateReplenishmentViewModel() },
                onNavigateBack = { onNavigate(defaultHome) }
            )
        } else if (currentDestination == AppDestination.Staff.SubstrateReleaseGovernance ||
            currentDestination == AppDestination.Manager.SubstrateReleaseGovernance ||
            currentDestination == AppDestination.Admin.SubstrateReleaseGovernance
        ) {
            val defaultHome = when (principal.role) {
                UserRole.STAFF -> AppDestination.Staff.AssignedWork
                UserRole.MANAGER -> AppDestination.Manager.Operations
                UserRole.ADMIN -> AppDestination.Admin.FullAdministration
                else -> AppDestination.Public.Home
            }
            com.sucharu.sucharupro.ui.features.substratereservation.SubstrateReleaseGovernanceCommandCenterScreen(
                viewModel = viewModel { com.sucharu.sucharupro.ui.features.substratereservation.SubstrateReleaseGovernanceViewModel() },
                onNavigateBack = { onNavigate(defaultHome) }
            )
        } else if (currentDestination == AppDestination.Staff.SubstrateEnterpriseAudit ||
            currentDestination == AppDestination.Manager.SubstrateEnterpriseAudit ||
            currentDestination == AppDestination.Admin.SubstrateEnterpriseAudit
        ) {
            val defaultHome = when (principal.role) {
                UserRole.STAFF -> AppDestination.Staff.AssignedWork
                UserRole.MANAGER -> AppDestination.Manager.Operations
                UserRole.ADMIN -> AppDestination.Admin.FullAdministration
                else -> AppDestination.Public.Home
            }
            com.sucharu.sucharupro.ui.features.substratereservation.SubstrateEnterpriseAuditCommandCenterScreen(
                viewModel = viewModel { com.sucharu.sucharupro.ui.features.substratereservation.SubstrateEnterpriseAuditViewModel() },
                onNavigateBack = { onNavigate(defaultHome) }
            )
        } else if (currentDestination == AppDestination.Staff.AffiliateManagement ||
            currentDestination == AppDestination.Manager.AffiliateManagement ||
            currentDestination == AppDestination.Admin.AffiliateManagement
        ) {
            Surface(
                color = Color(0xFF1C2541),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Affiliate Management requires a real API boundary in production (INFRA-05). Direct database access is disabled.",
                        color = Color(0xFFB7C8D8),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            Surface(
                color = Color(0xFF1C2541),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentDestination.title.uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9ECAFF),
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Route: ${currentDestination.route}",
                            color = Color(0xFFB7C8D8),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                        Text(
                            text = "Server-Authoritative Capability: ${currentDestination.requiredCapability?.name ?: "PUBLIC"}",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
