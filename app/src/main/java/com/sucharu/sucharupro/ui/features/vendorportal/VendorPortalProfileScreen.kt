package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.data.api.model.VendorPortalProfileSummaryDto

/**
 * Production Jetpack Compose Vendor Portal Profile Screen (Module 13 Step 02).
 */
@Composable
fun VendorPortalProfileScreen(
    profile: VendorPortalProfileSummaryDto?,
    isLoading: Boolean,
    errorMessage: String? = null,
    onRefresh: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A)
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(errorMessage, color = Color(0xFFF87171))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRefresh, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))) {
                        Text("Retry", color = Color.Black)
                    }
                }
            }
        } else if (profile != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF0284C7).copy(alpha = 0.2f), Color(0xFF0F172A))
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column {
                                Text(
                                    profile.vendorName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                profile.legalName?.let {
                                    Text("Legal: $it", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(color = Color(0xFF0284C7).copy(alpha = 0.25f), shape = RoundedCornerShape(4.dp)) {
                                        Text(profile.vendorCode, color = Color(0xFF38BDF8), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                                    }
                                    Surface(color = Color(0xFF10B981).copy(alpha = 0.25f), shape = RoundedCornerShape(4.dp)) {
                                        Text(profile.status, color = Color(0xFF34D399), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }

                // General Business Info
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Business Information", color = Color.White, fontWeight = FontWeight.Bold)
                            ProfileItemRow("Category", profile.category)
                            ProfileItemRow("Vendor Type", profile.vendorType)
                            ProfileItemRow("Address", profile.address ?: "Not provided")
                            ProfileItemRow("Project Scope", profile.projectScope)
                        }
                    }
                }

                // Contact Details
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Primary Contact", color = Color.White, fontWeight = FontWeight.Bold)
                            ProfileItemRow("Name", profile.primaryContactName ?: "N/A")
                            ProfileItemRow("Email", profile.primaryContactEmail ?: "N/A")
                            ProfileItemRow("Phone", profile.primaryContactPhone ?: "N/A")
                        }
                    }
                }

                // Portal & Capability Metrics
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Portal & Capability Metrics", color = Color.White, fontWeight = FontWeight.Bold)
                            ProfileItemRow("Portal Role", profile.portalRole)
                            ProfileItemRow("Portal Status", profile.portalAccountStatus)
                            ProfileItemRow("Registered Services", "${profile.serviceCount}")
                            ProfileItemRow("Active Capabilities", "${profile.capabilityCount}")
                            ProfileItemRow("Active Service Rates", "${profile.activeRatesCount}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodyMedium)
        Text(value, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
