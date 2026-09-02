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

/**
 * Responsive Dark Navy Foundation Workspace Shell for Internal ERP Roles: STAFF, MANAGER, ADMIN (INFRA-03 Step 06).
 */
@Composable
fun InternalWorkspaceShell(
    principal: AuthenticatedPrincipal,
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
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
                    FilterChip(selected = currentDestination == AppDestination.Manager.FinanceVisibility, onClick = { onNavigate(AppDestination.Manager.FinanceVisibility) }, label = { Text("Finance Summary") })
                    FilterChip(selected = currentDestination == AppDestination.Manager.Reports, onClick = { onNavigate(AppDestination.Manager.Reports) }, label = { Text("Reports") })
                }
                UserRole.ADMIN -> {
                    FilterChip(selected = currentDestination == AppDestination.Admin.FullAdministration, onClick = { onNavigate(AppDestination.Admin.FullAdministration) }, label = { Text("System Dashboard") })
                    FilterChip(selected = currentDestination == AppDestination.Admin.Workflows, onClick = { onNavigate(AppDestination.Admin.Workflows) }, label = { Text("Workflow Control") })
                    FilterChip(selected = currentDestination == AppDestination.Admin.ProductionScheduling, onClick = { onNavigate(AppDestination.Admin.ProductionScheduling) }, label = { Text("Scheduling Engine") })
                    FilterChip(selected = currentDestination == AppDestination.Admin.ShopFloorTracking, onClick = { onNavigate(AppDestination.Admin.ShopFloorTracking) }, label = { Text("Live Tracking") })
                    FilterChip(selected = currentDestination == AppDestination.Admin.FinalQcPackaging, onClick = { onNavigate(AppDestination.Admin.FinalQcPackaging) }, label = { Text("Final QC & Pack") })
                    FilterChip(selected = currentDestination == AppDestination.Admin.ProductionJobCosting, onClick = { onNavigate(AppDestination.Admin.ProductionJobCosting) }, label = { Text("Job Cost & Variance") })
                    FilterChip(selected = currentDestination == AppDestination.Admin.ProductionJobClosure, onClick = { onNavigate(AppDestination.Admin.ProductionJobClosure) }, label = { Text("Job Closure & Seal") })
                    FilterChip(selected = currentDestination == AppDestination.Admin.Imposition, onClick = { onNavigate(AppDestination.Admin.Imposition) }, label = { Text("Dynamic Imposition") })
                    FilterChip(selected = currentDestination == AppDestination.Admin.GangRun, onClick = { onNavigate(AppDestination.Admin.GangRun) }, label = { Text("Gang-Run Optimizer") })
                    FilterChip(selected = currentDestination == AppDestination.Admin.DynamicNesting, onClick = { onNavigate(AppDestination.Admin.DynamicNesting) }, label = { Text("2D Dynamic Nesting") })
                    FilterChip(selected = currentDestination == AppDestination.Admin.SignatureImposition, onClick = { onNavigate(AppDestination.Admin.SignatureImposition) }, label = { Text("Signature Imposition") })
                    FilterChip(selected = currentDestination == AppDestination.Admin.CtpOutput, onClick = { onNavigate(AppDestination.Admin.CtpOutput) }, label = { Text("CTP Plates & Marks") })
                    FilterChip(selected = currentDestination == AppDestination.Admin.Users, onClick = { onNavigate(AppDestination.Admin.Users) }, label = { Text("Users & Roles") })
                    FilterChip(selected = currentDestination == AppDestination.Admin.Security, onClick = { onNavigate(AppDestination.Admin.Security) }, label = { Text("Audit Security") })
                    FilterChip(selected = currentDestination == AppDestination.Admin.Finance, onClick = { onNavigate(AppDestination.Admin.Finance) }, label = { Text("ERP Ledger") })
                    FilterChip(selected = currentDestination == AppDestination.Admin.Configuration, onClick = { onNavigate(AppDestination.Admin.Configuration) }, label = { Text("Config") })
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
            DashboardScreen(
                viewModel = viewModel(),
                onNavigateToNewOrder = {},
                onNavigateToOrders = {},
                onNavigateToOrderDetail = {},
                onNavigateToProductionStage = {},
                onNavigateToPrintingCalculator = {},
                onNavigateToCustomers = {},
                onNavigateToInvoices = {},
                onNavigateToInventory = {},
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
            com.sucharu.sucharupro.ui.features.workflow.WorkflowDashboardScreen(
                principal = principal,
                modifier = Modifier.weight(1f)
            )
        } else if (currentDestination == AppDestination.Staff.ProductionScheduling ||
            currentDestination == AppDestination.Manager.ProductionScheduling ||
            currentDestination == AppDestination.Admin.ProductionScheduling
        ) {
            com.sucharu.sucharupro.ui.features.production.scheduling.ProductionSchedulingCommandCenterScreen(
                schedule = null,
                isLoading = false,
                onNavigateBack = {}
            )
        } else if (currentDestination == AppDestination.Staff.ShopFloorTracking ||
            currentDestination == AppDestination.Manager.ShopFloorTracking ||
            currentDestination == AppDestination.Admin.ShopFloorTracking
        ) {
            com.sucharu.sucharupro.ui.features.production.tracking.ShopFloorTrackingCommandCenterScreen(
                jobId = "JOB-LIVE-001",
                isLoading = false,
                onNavigateBack = {}
            )
        } else if (currentDestination == AppDestination.Staff.FinalQcPackaging ||
            currentDestination == AppDestination.Manager.FinalQcPackaging ||
            currentDestination == AppDestination.Admin.FinalQcPackaging
        ) {
            com.sucharu.sucharupro.ui.features.production.finalqc.FinalQcPackagingCommandCenterScreen(
                jobId = "JOB-FINAL-001",
                isLoading = false,
                onNavigateBack = {}
            )
        } else if (currentDestination == AppDestination.Staff.ProductionJobCosting ||
            currentDestination == AppDestination.Manager.ProductionJobCosting ||
            currentDestination == AppDestination.Admin.ProductionJobCosting
        ) {
            com.sucharu.sucharupro.ui.features.production.jobcosting.ProductionJobCostingCommandCenterScreen(
                jobId = "JOB-COST-001",
                isLoading = false,
                onNavigateBack = {}
            )
        } else if (currentDestination == AppDestination.Staff.ProductionJobClosure ||
            currentDestination == AppDestination.Manager.ProductionJobClosure ||
            currentDestination == AppDestination.Admin.ProductionJobClosure
        ) {
            com.sucharu.sucharupro.ui.features.production.jobclosure.ProductionJobClosureCommandCenterScreen(
                jobId = "JOB-CLOSE-001",
                isLoading = false,
                onNavigateBack = {}
            )
        } else if (currentDestination == AppDestination.Staff.SubstrateReservation ||
            currentDestination == AppDestination.Manager.SubstrateReservation ||
            currentDestination == AppDestination.Admin.SubstrateReservation
        ) {
            com.sucharu.sucharupro.ui.features.inventory.substratereservation.SubstrateReservationCommandCenterScreen(
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                onNavigateBack = {}
            )
        } else if (currentDestination == AppDestination.Staff.Imposition ||
            currentDestination == AppDestination.Manager.Imposition ||
            currentDestination == AppDestination.Admin.Imposition
        ) {
            com.sucharu.sucharupro.ui.features.imposition.ImpositionCommandCenterScreen(
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                onNavigateBack = {}
            )
        } else if (currentDestination == AppDestination.Staff.GangRun ||
            currentDestination == AppDestination.Manager.GangRun ||
            currentDestination == AppDestination.Admin.GangRun
        ) {
            com.sucharu.sucharupro.ui.features.imposition.GangRunCommandCenterScreen(
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                onNavigateBack = {}
            )
        } else if (currentDestination == AppDestination.Staff.DynamicNesting ||
            currentDestination == AppDestination.Manager.DynamicNesting ||
            currentDestination == AppDestination.Admin.DynamicNesting
        ) {
            com.sucharu.sucharupro.ui.features.imposition.DynamicNestingCommandCenterScreen(
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                onNavigateBack = {}
            )
        } else if (currentDestination == AppDestination.Staff.SignatureImposition ||
            currentDestination == AppDestination.Manager.SignatureImposition ||
            currentDestination == AppDestination.Admin.SignatureImposition
        ) {
            com.sucharu.sucharupro.ui.features.imposition.SignatureImpositionCommandCenterScreen(
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                onNavigateBack = {}
            )
        } else if (currentDestination == AppDestination.Staff.CtpOutput ||
            currentDestination == AppDestination.Manager.CtpOutput ||
            currentDestination == AppDestination.Admin.CtpOutput
        ) {
            com.sucharu.sucharupro.ui.features.imposition.CtpOutputCommandCenterScreen(
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                onNavigateBack = {}
            )
        } else if (currentDestination == AppDestination.Admin.PrepressOrchestration
        ) {
            com.sucharu.sucharupro.ui.features.imposition.PrepressOrchestrationCommandCenterScreen(
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                onNavigateBack = {}
            )
        } else if (currentDestination == AppDestination.Admin.SubstrateBatchSelection
        ) {
            com.sucharu.sucharupro.ui.features.substratereservation.SubstrateBatchSelectionCommandCenterScreen(
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            )
        } else if (currentDestination == AppDestination.Staff.SubstrateReplenishment ||
            currentDestination == AppDestination.Manager.SubstrateReplenishment ||
            currentDestination == AppDestination.Admin.SubstrateReplenishment
        ) {
            com.sucharu.sucharupro.ui.features.substratereservation.SubstrateReplenishmentCommandCenterScreen(
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                onNavigateBack = {}
            )
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
