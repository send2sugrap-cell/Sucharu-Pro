package com.sucharu.sucharupro.ui.features.communication.vendor.document

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.sucharu.sucharupro.domain.model.communication.vendor.document.*
import java.text.SimpleDateFormat
import java.util.*

private val BgColor = Color(0xFF0F172A)
private val SurfaceColor = Color(0xFF1E293B)
private val AccentColor = Color(0xFF38BDF8)
private val AccentGreen = Color(0xFF22D3EE)
private val AccentAmber = Color(0xFFFBBF24)
private val AccentRed = Color(0xFFF87171)
private val TextPrimary = Color(0xFFF1F5F9)
private val TextSecondary = Color(0xFF94A3B8)

private fun Long.toDateStr() = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(this))

// =========================================================================
// Compliance Dashboard Screen
// =========================================================================
@Composable
fun VendorComplianceDashboardScreen(
    viewModel: VendorComplianceViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {
        VendorDocTopBar(title = "Compliance Dashboard", onBack = onNavigateBack)

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentColor)
            }
            return@Column
        }

        if (state.summaries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(56.dp))
                    Text("No compliance data available", color = TextSecondary, fontSize = 14.sp)
                }
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val compliant = state.summaries.count { it.overallStatus == VendorComplianceStatus.COMPLIANT }
                    val nonCompliant = state.summaries.count { it.overallStatus == VendorComplianceStatus.NON_COMPLIANT }
                    VendorDocMetricCard("Compliant", "$compliant", Icons.Default.CheckCircle, AccentGreen, Modifier.weight(1f))
                    VendorDocMetricCard("Non-Compliant", "$nonCompliant", Icons.Default.Cancel, AccentRed, Modifier.weight(1f))
                }
            }

            items(state.summaries) { summary ->
                ComplianceSummaryCard(summary = summary)
            }
        }
    }
}

@Composable
private fun ComplianceSummaryCard(summary: VendorComplianceSummary) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(summary.vendorId, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                VendorComplianceStatusBadge(status = summary.overallStatus)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ComplianceItem("Total", "${summary.totalRequiredDocuments}", AccentColor)
                ComplianceItem("Approved", "${summary.approvedDocuments}", AccentGreen)
                ComplianceItem("Expired", "${summary.expiredDocuments}", AccentRed)
                ComplianceItem("Missing", "${summary.missingRequiredDocumentTypes.size}", AccentAmber)
            }
            val score = summary.compliancePercentage
            LinearProgressIndicator(
                progress = { score.toFloat() / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = when {
                    score >= 80 -> AccentGreen
                    score >= 50 -> AccentAmber
                    else -> AccentRed
                },
                trackColor = Color(0xFF334155)
            )
            Text("Score: ${score.toInt()}%", color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ComplianceItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}

// =========================================================================
// Expiry Tracker Screen
// =========================================================================
@Composable
fun VendorDocumentExpiryScreen(
    viewModel: VendorDocumentExpiryViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {
        VendorDocTopBar(title = "Expiry Tracker", onBack = onNavigateBack)

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentColor)
            }
            return@Column
        }

        val displayed = if (state.filter != null) {
            state.expiryInfoList.filter { it.expiryStatus == state.filter }
        } else state.expiryInfoList

        if (displayed.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(56.dp))
                    Text("No expiry data found", color = TextSecondary, fontSize = 14.sp)
                }
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(displayed) { info ->
                ExpiryInfoCard(info = info)
            }
        }
    }
}

@Composable
private fun ExpiryInfoCard(info: VendorDocumentExpiryInfo) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(info.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(info.documentType.defaultLabel, color = TextSecondary, fontSize = 12.sp)
                info.expiryDate?.let {
                    Text("Expires: ${it.toDateStr()}", color = TextSecondary, fontSize = 11.sp)
                }
                info.daysUntilExpiry?.let {
                    val color = when {
                        it < 0 -> AccentRed
                        it <= 30 -> AccentAmber
                        else -> AccentGreen
                    }
                    Text(
                        text = if (it < 0) "Expired ${-it}d ago" else "Expires in ${it}d",
                        color = color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            VendorExpiryStatusBadge(status = info.expiryStatus)
        }
    }
}

// =========================================================================
// Version History Screen
// =========================================================================
@Composable
fun VendorDocumentVersionHistoryScreen(
    documentId: String,
    viewModel: VendorDocumentVersionHistoryViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(documentId) { viewModel.load(documentId) }

    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {
        VendorDocTopBar(title = "Version History", onBack = onNavigateBack)

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentColor)
            }
            return@Column
        }

        if (state.versions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No versions found", color = TextSecondary)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.versions) { version ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("v${version.versionNumber}", color = AccentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            VendorDocStatusBadge(status = version.status)
                        }
                        Text(version.fileName, color = TextSecondary, fontSize = 12.sp)
                        Text(
                            "Submitted: ${version.submittedAt.toDateStr()}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        if (version.notes.isNotBlank()) {
                            Text(version.notes, color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// Activity Log Screen
// =========================================================================
@Composable
fun VendorDocumentActivityScreen(
    viewModel: VendorDocumentActivityViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {
        VendorDocTopBar(title = "Activity Log", onBack = onNavigateBack)

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentColor)
            }
            return@Column
        }

        if (state.events.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Timeline, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(56.dp))
                    Text("No activity recorded", color = TextSecondary, fontSize = 14.sp)
                }
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.events) { event ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .padding(top = 4.dp)
                                .background(AccentColor, shape = RoundedCornerShape(4.dp))
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(event.eventType.name.replace('_', ' '), color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("Actor: ${event.actorId} (${event.actorRole})", color = TextSecondary, fontSize = 11.sp)
                            Text(event.timestamp.toDateStr(), color = TextSecondary, fontSize = 11.sp)
                            if (!event.details.isNullOrBlank()) {
                                Text(event.details, color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
