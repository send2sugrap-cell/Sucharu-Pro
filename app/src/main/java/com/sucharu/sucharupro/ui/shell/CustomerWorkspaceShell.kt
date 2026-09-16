package com.sucharu.sucharupro.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.composition.AppRuntimeComposition
import com.sucharu.sucharupro.data.datasource.DemoOrderFixtures
import com.sucharu.sucharupro.data.datasource.FakeOrderDataSource
import com.sucharu.sucharupro.data.repository.OrderRepositoryImpl
import com.sucharu.sucharupro.ui.customer.myarea.CustomerMyAreaScreen
import com.sucharu.sucharupro.ui.customer.myarea.CustomerMyAreaViewModel
import com.sucharu.sucharupro.ui.customer.wall.SucharuWallScreen
import com.sucharu.sucharupro.ui.customer.wall.SucharuWallViewModel
import com.sucharu.sucharupro.ui.features.orders.order.OrderListScreen
import com.sucharu.sucharupro.ui.features.orders.order.OrderListViewModel
import com.sucharu.sucharupro.ui.features.orders.order.details.OrderDetailsScreen
import com.sucharu.sucharupro.ui.features.orders.order.details.OrderDetailsViewModel
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Mobile-First Customer Workspace Navigation Shell.
 */
@Composable
fun CustomerWorkspaceShell(
    principal: AuthenticatedPrincipal,
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    composition: AppRuntimeComposition? = null,
    modifier: Modifier = Modifier
) {
    val orderRepo = remember(composition) {
        composition?.orderRepository ?: OrderRepositoryImpl(FakeOrderDataSource(DemoOrderFixtures.demoOrders()))
    }

    // Direct Full-Screen Content for Home Wall and My Profile Area
    when (currentDestination) {
        is AppDestination.Customer.Home -> {
            SucharuWallScreen(
                viewModel = viewModel { SucharuWallViewModel() },
                principal = principal,
                onNavigateToDestination = onNavigate,
                modifier = modifier
            )
        }

        is AppDestination.Customer.Profile -> {
            CustomerMyAreaScreen(
                viewModel = viewModel { CustomerMyAreaViewModel() },
                principal = principal,
                onNavigateToDestination = onNavigate,
                onNavigateBack = { onNavigate(AppDestination.Customer.Home) },
                modifier = modifier
            )
        }

        else -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color(0xFF0B132B))
                    .padding(16.dp)
            ) {
                // Customer Header Card
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
                            Text("CUSTOMER PORTAL", fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), fontSize = 16.sp)
                            Text("Welcome back, ${principal.username}", color = Color.White, fontSize = 12.sp)
                            Text("ID: ${principal.userId}", color = Color(0xFFB7C8D8), fontSize = 10.sp)
                        }
                        Surface(color = Color(0xFF00497D), shape = RoundedCornerShape(8.dp)) {
                            Text("Active Account", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation Tab Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentDestination == AppDestination.Customer.Home,
                        onClick = { onNavigate(AppDestination.Customer.Home) },
                        label = { Text("Overview") }
                    )
                    FilterChip(
                        selected = currentDestination == AppDestination.Customer.Orders || currentDestination is AppDestination.Customer.OrderDetails,
                        onClick = { onNavigate(AppDestination.Customer.Orders) },
                        label = { Text("My Orders") }
                    )
                    FilterChip(
                        selected = currentDestination == AppDestination.Customer.Invoices,
                        onClick = { onNavigate(AppDestination.Customer.Invoices) },
                        label = { Text("Invoices") }
                    )
                    FilterChip(
                        selected = currentDestination == AppDestination.Customer.DeliveryTracking,
                        onClick = { onNavigate(AppDestination.Customer.DeliveryTracking) },
                        label = { Text("Delivery") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dynamic Active Destination Body
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
                            .padding(12.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        when (currentDestination) {
                            is AppDestination.Customer.Orders -> {
                                OrderListScreen(
                                    viewModel = viewModel { OrderListViewModel(orderRepo) },
                                    onOrderClick = { orderId -> onNavigate(AppDestination.Customer.OrderDetails(orderId)) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            is AppDestination.Customer.OrderDetails -> {
                                OrderDetailsScreen(
                                    viewModel = viewModel(key = currentDestination.orderId) {
                                        OrderDetailsViewModel(
                                            orderId = currentDestination.orderId,
                                            repository = orderRepo
                                        )
                                    },
                                    onBackClick = { onNavigate(AppDestination.Customer.Orders) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            is AppDestination.Customer.Invoices -> {
                                DemoInfoScreen("MY INVOICES", "You have 1 pending invoice: INV-2026-001 (৳ 12,500.00)")
                            }
                            is AppDestination.Customer.DeliveryTracking -> {
                                DemoInfoScreen("DELIVERY TRACKING", "Your order ORD-DEMO-001 is ready for pickup.")
                            }
                            else -> {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                                    Text("DASHBOARD OVERVIEW", fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    DashboardMiniStat("Active Orders", "1")
                                    DashboardMiniStat("Pending Invoices", "৳ 12,500")
                                    DashboardMiniStat("Loyalty Points", "450")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardMiniStat(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B132B).copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color(0xFFB7C8D8), fontSize = 14.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun DemoInfoScreen(title: String, info: String) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(info, color = Color.White, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
