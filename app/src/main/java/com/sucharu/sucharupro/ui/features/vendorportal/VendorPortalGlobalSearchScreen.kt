package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalGlobalSearchScreen(
    onSearchTriggered: (String, List<String>) -> Unit = { _, _ -> },
    onQuickFilterClick: (String) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val selectedTypes = remember { mutableStateListOf<String>() }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Global Portal Search",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search POs, Invoices, ASNs, QC Cases, Settlements...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFF475569)
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (searchQuery.isNotBlank()) {
                                    onSearchTriggered(searchQuery, selectedTypes.toList())
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text(text = "Search", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Quick Filters",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val filterCategories = listOf(
                        "PURCHASE_ORDER" to "Purchase Orders",
                        "WORK_ORDER" to "Work Orders",
                        "DELIVERY_NOTICE" to "Delivery Notices & ASN",
                        "INVOICE" to "Invoices & Payments",
                        "QUALITY_CASE" to "Quality Cases & CAPA",
                        "SETTLEMENT" to "Settlements & Reconciliations",
                        "NOTIFICATION" to "Notifications"
                    )

                    filterCategories.forEach { (type, label) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectedTypes.contains(type)) {
                                        selectedTypes.remove(type)
                                    } else {
                                        selectedTypes.add(type)
                                    }
                                    onQuickFilterClick(type)
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedTypes.contains(type)) Color(0xFF1E3A8A) else Color(0xFF1E293B)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = label, color = Color.White, fontSize = 14.sp)
                                if (selectedTypes.contains(type)) {
                                    Badge(containerColor = Color(0xFF3B82F6)) {
                                        Text(text = "Active", color = Color.White, modifier = Modifier.padding(4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
