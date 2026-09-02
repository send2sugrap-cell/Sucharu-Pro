package com.sucharu.sucharupro.ui.features.communication.vendor.document

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BgColor = Color(0xFF0F172A)
private val AccentColor = Color(0xFF38BDF8)
private val TextPrimary = Color(0xFFF1F5F9)
private val TextSecondary = Color(0xFF94A3B8)
private val AccentGreen = Color(0xFF22D3EE)
private val AccentRed = Color(0xFFF87171)
private val AccentAmber = Color(0xFFFBBF24)

@Composable
fun VendorDocumentDashboardScreen(
    viewModel: VendorDocumentDashboardViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRequests: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToCompliance: () -> Unit,
    onNavigateToExpiry: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        VendorDocTopBar(title = "Vendor Documents", onBack = onNavigateBack)

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentColor)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Document Overview",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VendorDocMetricCard(
                        title = "Total Documents",
                        value = "${state.totalDocuments}",
                        icon = Icons.Default.Description,
                        modifier = Modifier.weight(1f)
                    )
                    VendorDocMetricCard(
                        title = "Pending Review",
                        value = "${state.pendingReview}",
                        icon = Icons.Default.HourglassEmpty,
                        iconTint = AccentAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VendorDocMetricCard(
                        title = "Approved",
                        value = "${state.approved}",
                        icon = Icons.Default.CheckCircle,
                        iconTint = AccentGreen,
                        modifier = Modifier.weight(1f)
                    )
                    VendorDocMetricCard(
                        title = "Rejected",
                        value = "${state.rejected}",
                        icon = Icons.Default.Cancel,
                        iconTint = AccentRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VendorDocMetricCard(
                        title = "Expired",
                        value = "${state.expired}",
                        icon = Icons.Default.Warning,
                        iconTint = AccentRed,
                        modifier = Modifier.weight(1f)
                    )
                    VendorDocMetricCard(
                        title = "Expiring Soon",
                        value = "${state.expiringSoon}",
                        icon = Icons.Default.Schedule,
                        iconTint = AccentAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text(
                    text = "Quick Actions",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionButton("Document Requests", Icons.Default.Assignment, onNavigateToRequests)
                    QuickActionButton("All Documents", Icons.Default.FolderOpen, onNavigateToDocuments)
                    QuickActionButton("Compliance Dashboard", Icons.Default.VerifiedUser, onNavigateToCompliance)
                    QuickActionButton("Expiry Tracker", Icons.Default.DateRange, onNavigateToExpiry)
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = AccentColor, modifier = Modifier.size(20.dp))
            Text(text = label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}
