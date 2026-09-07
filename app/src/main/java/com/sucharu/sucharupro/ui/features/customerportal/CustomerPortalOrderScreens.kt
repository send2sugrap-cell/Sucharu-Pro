package com.sucharu.sucharupro.ui.features.customerportal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.CustomerOrderDetailDto
import com.sucharu.sucharupro.data.api.model.CustomerOrderSummaryDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPortalOrderListScreen(
    viewModel: CustomerPortalOrdersViewModel,
    onNavigateBack: () -> Unit,
    onNavigateOrderDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.ordersState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadOrders()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Orders", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1B2A))
            )
        },
        containerColor = Color(0xFF0A0F1D)
    ) { padding ->
        Box(modifier = modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                CustomerPortalUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF00E5FF))
                    }
                }
                is CustomerPortalUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(current.message, color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadOrders() }) {
                            Text("Retry")
                        }
                    }
                }
                CustomerPortalUiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("You have no active or historical orders.", color = Color.LightGray)
                    }
                }
                is CustomerPortalUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(current.data) { order ->
                            CustomerOrderItemCard(order = order, onClick = { onNavigateOrderDetail(order.orderId) })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPortalOrderDetailsScreen(
    orderId: String,
    viewModel: CustomerPortalOrdersViewModel,
    onNavigateBack: () -> Unit,
    onNavigateTrackProduction: (String) -> Unit,
    onNavigateTrackDelivery: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.selectedOrderDetailState.collectAsState()

    LaunchedEffect(orderId) {
        viewModel.loadOrderDetail(orderId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Details", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1B2A))
            )
        },
        containerColor = Color(0xFF0A0F1D)
    ) { padding ->
        Box(modifier = modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                CustomerPortalUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF00E5FF))
                    }
                }
                is CustomerPortalUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(current.message, color = Color.White)
                    }
                }
                CustomerPortalUiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Order details unavailable.", color = Color.LightGray)
                    }
                }
                is CustomerPortalUiState.Success -> {
                    val order = current.data
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF162032))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(order.orderNumber, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Status: ${order.status}", color = Color(0xFF00E5FF), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Total Amount: ৳${order.totalAmount}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { onNavigateTrackProduction(order.orderId) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                ) {
                                    Icon(Icons.Default.Build, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Track Production")
                                }
                                Button(
                                    onClick = { onNavigateTrackDelivery(order.orderId) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                ) {
                                    Icon(Icons.Default.LocalShipping, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Track Delivery")
                                }
                            }
                        }

                        item {
                            Text("Items Breakdown", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        }

                        items(order.items) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF162032))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(item.description, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("Quantity: ${item.quantity} PCS", color = Color.LightGray, fontSize = 12.sp)
                                    }
                                    Text("৳${item.totalPrice}", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
