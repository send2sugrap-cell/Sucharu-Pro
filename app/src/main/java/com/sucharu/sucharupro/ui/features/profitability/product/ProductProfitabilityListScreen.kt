package com.sucharu.sucharupro.ui.features.profitability.product

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.profitability.ProductProfitabilitySnapshotDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductProfitabilityListScreen(
    snapshots: List<ProductProfitabilitySnapshotDto>,
    onProductClick: (String) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val darkNavyBg = Color(0xFF0B132B)
    val cardBg = Color(0xFF1C2541)
    val accentCyan = Color(0xFF9ECAFF)
    val successGreen = Color(0xFF4EBA6F)
    val warningOrange = Color(0xFFFFB74D)
    val errorRed = Color(0xFFFF6B6B)

    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(snapshots, searchQuery) {
        if (searchQuery.isBlank()) snapshots
        else snapshots.filter {
            (it.productName ?: "").contains(searchQuery, ignoreCase = true) ||
            it.productId.contains(searchQuery, ignoreCase = true) ||
            (it.sku ?: "").contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Product Profitability List", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Text("${filtered.size} Products Analyzed", fontSize = 12.sp, color = Color.LightGray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkNavyBg)
            )
        },
        containerColor = darkNavyBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search products by name, ID or SKU", color = Color.LightGray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = accentCyan,
                    unfocusedBorderColor = Color.DarkGray
                ),
                shape = RoundedCornerShape(8.dp)
            )

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No Products Found", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered) { snap ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onProductClick(snap.productId) },
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(snap.productName ?: snap.productId, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    val badgeColor = when (snap.profitabilityClassification) {
                                        "HIGHLY_PROFITABLE", "PROFITABLE" -> successGreen
                                        "LOW_MARGIN", "BREAK_EVEN" -> warningOrange
                                        else -> errorRed
                                    }
                                    Surface(color = badgeColor.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                        Text(
                                            text = snap.profitabilityClassification,
                                            color = badgeColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Qty: ${snap.totalQuantity} • Rev: ৳${snap.recognizedRevenue}", color = Color.LightGray, fontSize = 12.sp)
                                    val gpColor = if (snap.grossProfit.toDouble() >= 0) successGreen else errorRed
                                    Text("GP: ৳${snap.grossProfit} (${snap.grossMarginPercentage ?: 0}%)", color = gpColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Unit Rev: ৳${snap.unitEconomics.unitRevenue ?: "N/A"}", color = Color.Gray, fontSize = 11.sp)
                                    Text("Unit Cost: ৳${snap.unitEconomics.unitActualCost ?: "N/A"}", color = Color.Gray, fontSize = 11.sp)
                                    Text("Unit GP: ৳${snap.unitEconomics.unitGrossProfit ?: "N/A"}", color = accentCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
