package com.sucharu.sucharupro.ui.features.affiliate

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.affiliate.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AffiliateManagementCommandCenterScreen(
    viewModel: AffiliateManagementViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var actionDialogTarget by remember { mutableStateOf<Pair<String, String>?>(null) } // action to affiliateId
    var actionReason by remember { mutableStateOf("") }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0A0E17), Color(0xFF131B2E), Color(0xFF0A0E17))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "AFFILIATE MANAGEMENT FOUNDATION",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color(0xFF00E5FF),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    "MODULE 20 STEP 01",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "Canonical Affiliate Profiles, Multi-Tenant RLS & Governance",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                actions = {
                    if (!uiState.isPersonalView) {
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFF0A0E17), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Affiliate", color = Color(0xFF0A0E17), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF00E5FF))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0A0E17)
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(bgGradient)
        ) {
            // Notification Alerts
            if (uiState.errorMessage != null) {
                Surface(
                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(uiState.errorMessage ?: "", color = Color(0xFFEF4444), fontSize = 12.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.clearMessages() }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFEF4444))
                        }
                    }
                }
            }

            if (uiState.successMessage != null) {
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(uiState.successMessage ?: "", color = Color(0xFF10B981), fontSize = 12.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.clearMessages() }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF10B981))
                        }
                    }
                }
            }

            // Tabs Navigation Bar
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = Color(0xFF0F172A),
                contentColor = Color(0xFF00E5FF),
                edgePadding = 16.dp
            ) {
                AffiliateCommandTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    tab.title,
                                    fontWeight = if (uiState.selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                                if (tab == AffiliateCommandTab.PENDING_APPROVAL && uiState.pendingCount > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        color = Color(0xFFF59E0B),
                                        shape = CircleShape,
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                "${uiState.pendingCount}",
                                                color = Color.Black,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF00E5FF))
            }

            // Tab Content
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                when (uiState.selectedTab) {
                    AffiliateCommandTab.OVERVIEW -> AffiliateOverviewView(uiState)
                    AffiliateCommandTab.DIRECTORY -> AffiliateDirectoryView(uiState, viewModel)
                    AffiliateCommandTab.PENDING_APPROVAL -> AffiliatePendingApprovalView(uiState, onAction = { action, affId ->
                        actionDialogTarget = action to affId
                    })
                    AffiliateCommandTab.ACTIVE_SUSPENDED -> AffiliateActiveSuspendedView(uiState, onAction = { action, affId ->
                        actionDialogTarget = action to affId
                    })
                    AffiliateCommandTab.PROFILE_ELIGIBILITY -> AffiliateProfileAndEligibilityView(uiState, viewModel)
                    AffiliateCommandTab.AI_HANDOFF -> AffiliateAiHandoffView(uiState)
                }
            }
        }
    }

    // Create Affiliate Dialog
    if (showCreateDialog) {
        CreateAffiliateModal(
            onDismiss = { showCreateDialog = false },
            onSubmit = { userId, name, code, type, phone, email, tax, agreement ->
                viewModel.createAffiliate(userId, name, code, type, phone, email, tax, agreement)
                showCreateDialog = false
            }
        )
    }

    // Action Confirmation Dialog
    if (actionDialogTarget != null) {
        val (action, targetAffiliateId) = actionDialogTarget!!
        AlertDialog(
            onDismissRequest = { actionDialogTarget = null },
            title = { Text("$action Affiliate", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Provide a reason or justification for this governance action ($action):", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = actionReason,
                        onValueChange = { actionReason = it },
                        label = { Text("Reason / Audit Note") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF475569)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (action) {
                            "ACTIVATE" -> viewModel.activateAffiliate(targetAffiliateId, actionReason)
                            "SUSPEND" -> viewModel.suspendAffiliate(targetAffiliateId, actionReason)
                            "REACTIVATE" -> viewModel.reactivateAffiliate(targetAffiliateId, actionReason)
                            "REJECT" -> viewModel.rejectAffiliate(targetAffiliateId, actionReason)
                            "TERMINATE" -> viewModel.terminateAffiliate(targetAffiliateId, actionReason)
                        }
                        actionReason = ""
                        actionDialogTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Confirm", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { actionDialogTarget = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

@Composable
fun AffiliateOverviewView(uiState: AffiliateManagementUiState) {
    val summary = uiState.summary
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("TENANT AFFILIATE GOVERNANCE OVERVIEW", fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCard("TOTAL AFFILIATES", "${summary?.totalAffiliates ?: uiState.affiliatesList.size}", Color(0xFF00E5FF), modifier = Modifier.weight(1f))
                KpiCard("ACTIVE PARTNERS", "${summary?.activeAffiliates ?: uiState.activeCount}", Color(0xFF10B981), modifier = Modifier.weight(1f))
                KpiCard("PENDING APPROVAL", "${summary?.pendingAffiliates ?: uiState.pendingCount}", Color(0xFFF59E0B), modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCard("SUSPENDED", "${summary?.suspendedAffiliates ?: uiState.suspendedCount}", Color(0xFFEF4444), modifier = Modifier.weight(1f))
                KpiCard("VERIFIED COMPLIANT", "${summary?.verifiedCount ?: 0}", Color(0xFF3B82F6), modifier = Modifier.weight(1f))
                KpiCard("COMMISSION ELIGIBLE", "${summary?.eligibleCount ?: 0}", Color(0xFF8B5CF6), modifier = Modifier.weight(1f))
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("AFFILIATE CATEGORY DISTRIBUTION", fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    CategoryRow("Individual Creators", summary?.individualCount ?: 0)
                    CategoryRow("Business Entities", summary?.businessCount ?: 0)
                    CategoryRow("Strategic Partners", summary?.partnerCount ?: 0)
                    CategoryRow("Content Creators / Influencers", summary?.creatorCount ?: 0)
                    CategoryRow("Referral Partners", summary?.referralPartnerCount ?: 0)
                }
            }
        }
    }
}

@Composable
fun AffiliateDirectoryView(
    uiState: AffiliateManagementUiState,
    viewModel: AffiliateManagementViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Search & Filter Bar
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by name, code, or user ID...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF00E5FF)) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedContainerColor = Color(0xFF0F172A),
                    unfocusedContainerColor = Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.filteredAffiliates) { aff ->
                AffiliateCard(
                    affiliate = aff,
                    isSelected = aff.affiliateId == uiState.selectedAffiliate?.affiliateId,
                    onClick = {
                        viewModel.selectAffiliate(aff.affiliateId)
                        viewModel.selectTab(AffiliateCommandTab.PROFILE_ELIGIBILITY)
                    }
                )
            }
        }
    }
}

@Composable
fun AffiliatePendingApprovalView(
    uiState: AffiliateManagementUiState,
    onAction: (String, String) -> Unit
) {
    val pendingList = uiState.affiliatesList.filter { it.status == "PENDING" }
    if (pendingList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No pending affiliate applications requiring review.", color = Color(0xFF94A3B8), fontSize = 13.sp)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(pendingList) { aff ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(aff.displayName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                Text("Code: ${aff.affiliateCode} | Type: ${aff.affiliateType}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                Text("User ID: ${aff.userId} | Phone: ${aff.contactPhone ?: "N/A"}", color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                            StatusBadge(aff.status)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onAction("ACTIVATE", aff.affiliateId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Approve & Activate", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { onAction("REJECT", aff.affiliateId) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Reject", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AffiliateActiveSuspendedView(
    uiState: AffiliateManagementUiState,
    onAction: (String, String) -> Unit
) {
    val activeAndSuspended = uiState.affiliatesList.filter { it.status in setOf("ACTIVE", "SUSPENDED") }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(activeAndSuspended) { aff ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (aff.status == "ACTIVE") Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFFEF4444).copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(aff.displayName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("Code: ${aff.affiliateCode} | Type: ${aff.affiliateType}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                        StatusBadge(aff.status)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (aff.status == "ACTIVE") {
                            Button(
                                onClick = { onAction("SUSPEND", aff.affiliateId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Suspend Partner", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        } else {
                            Button(
                                onClick = { onAction("REACTIVATE", aff.affiliateId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Reactivate Partner", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                        OutlinedButton(
                            onClick = { onAction("TERMINATE", aff.affiliateId) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Terminate", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AffiliateProfileAndEligibilityView(
    uiState: AffiliateManagementUiState,
    viewModel: AffiliateManagementViewModel
) {
    val aff = uiState.selectedAffiliate
    val el = uiState.selectedEligibility
    val audits = uiState.selectedAuditRecords

    if (aff == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select an affiliate from the Directory to view details.", color = Color(0xFF94A3B8))
        }
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(aff.displayName, fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp)
                            Text("Affiliate Code: ${aff.affiliateCode}", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        StatusBadge(aff.status)
                    }
                    Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 12.dp))
                    DetailRow("Affiliate ID", aff.affiliateId)
                    DetailRow("User ID", aff.userId)
                    DetailRow("Customer Link", aff.customerId ?: "None (Pure Affiliate)")
                    DetailRow("Category Type", aff.affiliateType)
                    DetailRow("Contact Phone", aff.contactPhone ?: "N/A")
                    DetailRow("Contact Email", aff.contactEmail ?: "N/A")
                    DetailRow("Tax ID / GST", aff.taxIdOrGst ?: "N/A")
                    DetailRow("Agreement Ref", "${aff.agreementReference ?: "None"} (${aff.agreementVersion ?: "N/A"})")
                    DetailRow("Verification State", aff.verificationState)
                    DetailRow("Joined At", formatDate(aff.joinedAt))
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (el?.isEligible == true) Color(0xFF10B981) else Color(0xFFF59E0B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("MULTI-DIMENSIONAL ELIGIBILITY ASSESSMENT", fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 13.sp)
                        Surface(
                            color = if (el?.isEligible == true) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                if (el?.isEligible == true) "ELIGIBLE" else "INELIGIBLE",
                                color = if (el?.isEligible == true) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    EligibilityCheckItem("Identity Verified", el?.identityVerified == true)
                    EligibilityCheckItem("Agreement Accepted", el?.agreementAccepted == true)
                    EligibilityCheckItem("Account Active", el?.accountActive == true)
                    EligibilityCheckItem("Tax & GST Compliant", el?.taxCompliant == true)
                    EligibilityCheckItem("Business Verified", el?.businessVerified == true)

                    if (el?.rejectionReasons?.isNotEmpty() == true) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Rejection / Blockers:", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        el.rejectionReasons.forEach { reason ->
                            Text("• $reason", color = Color(0xFFEF4444), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Text("APPEND-ONLY AUDIT LEDGER", fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 13.sp)
        }

        items(audits) { audit ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(audit.eventType, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                        Text(formatDate(audit.timestamp), color = Color(0xFF64748B), fontSize = 10.sp)
                    }
                    Text("Reason: ${audit.reason}", color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp))
                    Text("Actor: ${audit.actorId} (${audit.actorRole})", color = Color(0xFF64748B), fontSize = 10.sp)
                    Text("Record Hash: ${audit.recordHash.take(16)}... | Chain Hash: ${audit.chainHash.take(16)}...", fontFamily = FontFamily.Monospace, color = Color(0xFF00E5FF).copy(alpha = 0.7f), fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
fun AffiliateAiHandoffView(uiState: AffiliateManagementUiState) {
    val handoff = uiState.selectedHandoffContract
    if (handoff == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No affiliate selected for AI Handoff Contract inspection.", color = Color(0xFF94A3B8))
        }
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF00E5FF))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("MODULE 20 STEP 01 AI HANDOFF CONTRACT", fontWeight = FontWeight.Black, color = Color(0xFF00E5FF), fontSize = 13.sp)
                        Surface(color = Color(0xFF00E5FF).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text(handoff.contractVersion, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(4.dp))
                        }
                    }
                    Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 8.dp))
                    DetailRow("Read-Only Enforced", "${handoff.isReadOnly}")
                    DetailRow("Affiliate ID", handoff.affiliateId)
                    DetailRow("Affiliate Code", handoff.affiliateCode)
                    DetailRow("Attribution Eligible", "${handoff.isEligibleForAttribution}")
                    DetailRow("Commission Eligible", "${handoff.isEligibleForCommission}")
                    DetailRow("Integrity Seal", handoff.integritySealHash.take(24) + "...")
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF10B981))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ALLOWED AI CAPABILITIES", fontWeight = FontWeight.Bold, color = Color(0xFF10B981), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    handoff.allowedAiActions.forEach { action ->
                        Text("✓ $action", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFEF4444))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("STRICTLY FORBIDDEN AI MUTATIONS", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    handoff.forbiddenAiActions.forEach { action ->
                        Text("✕ $action", color = Color(0xFFFCA5A5), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// Subcomponents & Helpers

@Composable
fun KpiCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun CategoryRow(name: String, count: Long) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, color = Color(0xFFCBD5E1), fontSize = 12.sp)
        Text("$count", fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 12.sp)
    }
}

@Composable
fun AffiliateCard(
    affiliate: AffiliateProfileDto,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A)
        ),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(affiliate.displayName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Text("Code: ${affiliate.affiliateCode} | Type: ${affiliate.affiliateType}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                Text("User ID: ${affiliate.userId}", color = Color(0xFF64748B), fontSize = 10.sp)
            }
            StatusBadge(affiliate.status)
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bg, fg) = when (status.uppercase()) {
        "ACTIVE" -> Color(0xFF10B981).copy(alpha = 0.2f) to Color(0xFF10B981)
        "PENDING" -> Color(0xFFF59E0B).copy(alpha = 0.2f) to Color(0xFFF59E0B)
        "SUSPENDED" -> Color(0xFFEF4444).copy(alpha = 0.2f) to Color(0xFFEF4444)
        "REJECTED" -> Color(0xFFDC2626).copy(alpha = 0.2f) to Color(0xFFDC2626)
        "TERMINATED" -> Color(0xFF6B7280).copy(alpha = 0.2f) to Color(0xFF9CA3AF)
        else -> Color(0xFF64748B).copy(alpha = 0.2f) to Color(0xFF94A3B8)
    }
    Surface(color = bg, shape = RoundedCornerShape(4.dp)) {
        Text(status, color = fg, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF94A3B8), fontSize = 12.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 12.sp)
    }
}

@Composable
fun EligibilityCheckItem(title: String, isPassed: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isPassed) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (isPassed) Color(0xFF10B981) else Color(0xFFEF4444),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, color = if (isPassed) Color(0xFFCBD5E1) else Color(0xFF94A3B8), fontSize = 12.sp)
    }
}

@Composable
fun CreateAffiliateModal(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String?, String, String?, String?, String?, String?) -> Unit
) {
    var userId by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var affiliateCode by remember { mutableStateOf("") }
    var affiliateType by remember { mutableStateOf("INDIVIDUAL") }
    var contactPhone by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var taxId by remember { mutableStateOf("") }
    var agreementRef by remember { mutableStateOf("AGR-TERMS-2026-V1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Affiliate Profile", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = userId, onValueChange = { userId = it }, label = { Text("User ID *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("Display Name *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = affiliateCode, onValueChange = { affiliateCode = it }, label = { Text("Custom Code (Optional)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = contactPhone, onValueChange = { contactPhone = it }, label = { Text("Contact Phone") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = contactEmail, onValueChange = { contactEmail = it }, label = { Text("Contact Email") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = taxId, onValueChange = { taxId = it }, label = { Text("Tax ID / GST (Required for Business)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = agreementRef, onValueChange = { agreementRef = it }, label = { Text("Agreement Reference") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (userId.isNotBlank() && displayName.isNotBlank()) {
                        onSubmit(
                            userId, displayName, affiliateCode.ifBlank { null },
                            affiliateType, contactPhone.ifBlank { null },
                            contactEmail.ifBlank { null }, taxId.ifBlank { null },
                            agreementRef.ifBlank { null }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("Create", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF94A3B8)) }
        },
        containerColor = Color(0xFF1E293B)
    )
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
