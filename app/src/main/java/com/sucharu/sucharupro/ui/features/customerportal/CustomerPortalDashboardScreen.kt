package com.sucharu.sucharupro.ui.features.customerportal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.CustomerOrderSummaryDto
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPortalDashboardScreen(
    viewModel: CustomerPortalDashboardViewModel,
    onNavigateOrders: () -> Unit,
    onNavigateOrderDetail: (String) -> Unit,
    onNavigateInvoices: () -> Unit,
    onNavigatePayments: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateSupport: () -> Unit,
    onNavigateReturns: () -> Unit,
    onNavigateNotifications: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sucharu Pro", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Customer Personal Area", fontSize = 12.sp, color = Color.LightGray)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateNotifications) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White)
                    }
                    IconButton(onClick = onNavigateProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "My Profile", tint = Color.White)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out", tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1B2A))
            )
        },
        containerColor = Color(0xFF0A0F1D)
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val current = state) {
                CustomerPortalUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF00E5FF))
                    }
                }
                is CustomerPortalUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(current.message, color = Color.White, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadDashboard() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))) {
                            Text("Retry", color = Color.Black)
                        }
                    }
                }
                CustomerPortalUiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No customer dashboard data available.", color = Color.LightGray)
                    }
                }
                is CustomerPortalUiState.Success -> {
                    val data = current.data
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Welcome Banner
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFF1E3A8A), Color(0xFF0284C7))
                                            )
                                        )
                                        .padding(20.dp)
                                ) {
                                    Column {
                                        Text("Welcome back,", color = Color.LightGray, fontSize = 14.sp)
                                        Text(data.profile.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Customer Code: ${data.profile.customerCode}", color = Color(0xFFE0F2FE), fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        // Summary Cards Grid
                        item {
                            Text("Overview", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SummaryKpiCard(
                                    title = "Active Orders",
                                    value = "${data.activeOrderCount}",
                                    icon = Icons.Default.ShoppingCart,
                                    accentColor = Color(0xFF00E5FF),
                                    modifier = Modifier.weight(1f),
                                    onClick = onNavigateOrders
                                )
                                SummaryKpiCard(
                                    title = "In Production",
                                    value = "${data.inProductionCount}",
                                    icon = Icons.Default.Build,
                                    accentColor = Color(0xFFF59E0B),
                                    modifier = Modifier.weight(1f),
                                    onClick = onNavigateOrders
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SummaryKpiCard(
                                    title = "Ready for Delivery",
                                    value = "${data.readyForDeliveryCount}",
                                    icon = Icons.Default.LocalShipping,
                                    accentColor = Color(0xFF10B981),
                                    modifier = Modifier.weight(1f),
                                    onClick = onNavigateOrders
                                )
                                SummaryKpiCard(
                                    title = "Pending Payment",
                                    value = "৳${data.totalOutstandingBalance}",
                                    icon = Icons.Default.AccountBalanceWallet,
                                    accentColor = Color(0xFFEF4444),
                                    modifier = Modifier.weight(1f),
                                    onClick = onNavigateInvoices
                                )
                            }
                        }

                        // Quick Shortcuts
                        item {
                            Text("Quick Actions", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                QuickActionButton("Invoices", Icons.Default.Receipt, onNavigateInvoices)
                                QuickActionButton("Payments", Icons.Default.Payment, onNavigatePayments)
                                QuickActionButton("Returns", Icons.Default.AssignmentReturn, onNavigateReturns)
                                QuickActionButton("Support", Icons.Default.HeadsetMic, onNavigateSupport)
                            }
                        }

                        // Recent Orders
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Recent Orders", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                TextTextButton(text = "View All", onClick = onNavigateOrders)
                            }
                        }

                        if (data.recentOrders.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF162032))
                                ) {
                                    Text("No orders placed yet.", modifier = Modifier.padding(16.dp), color = Color.Gray)
                                }
                            }
                        } else {
                            items(data.recentOrders) { order ->
                                CustomerOrderItemCard(order = order, onClick = { onNavigateOrderDetail(order.orderId) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryKpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF162032))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(title, color = Color.LightGray, fontSize = 12.sp)
        }
    }
}

@Composable
fun QuickActionButton(title: String, icon: ImageVector, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(Color(0xFF1E293B), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(title, color = Color.LightGray, fontSize = 12.sp)
    }
}

@Composable
fun TextTextButton(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = Color(0xFF00E5FF),
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
fun CustomerOrderItemCard(order: CustomerOrderSummaryDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF162032))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(order.orderNumber, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Total: ৳${order.totalAmount}", color = Color(0xFF00E5FF), fontSize = 14.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (order.status) {
                        "READY", "DELIVERED" -> Color(0xFF059669)
                        "IN_PRODUCTION", "PRINTING" -> Color(0xFFD97706)
                        else -> Color(0xFF2563EB)
                    }
                ) {
                    Text(
                        order.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "View", tint = Color.Gray)
            }
        }
    }
}
