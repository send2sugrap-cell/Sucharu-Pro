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
    var showCreateAffiliateDialog by remember { mutableStateOf(false) }
    var showCreateProgramDialog by remember { mutableStateOf(false) }
    var showEnrollDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showRequestVerificationDialog by remember { mutableStateOf(false) }
    var showAddDocumentDialog by remember { mutableStateOf(false) }

    var affiliateActionDialogTarget by remember { mutableStateOf<Pair<String, String>?>(null) } // action to affiliateId
    var programActionDialogTarget by remember { mutableStateOf<Pair<String, String>?>(null) } // action to programId
    var enrollmentActionDialogTarget by remember { mutableStateOf<Pair<String, String>?>(null) } // action to enrollmentId
    var verificationActionDialogTarget by remember { mutableStateOf<Pair<String, String>?>(null) } // action to verificationId
    var documentActionDialogTarget by remember { mutableStateOf<Pair<String, String>?>(null) } // action to documentId
    var actionReason by remember { mutableStateOf("") }
    var actionNotes by remember { mutableStateOf("") }

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
                                "AFFILIATE & GOVERNANCE COMMAND CENTER",
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
                                    "MODULE 20 STEP 01-03",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "Affiliate Identity, Programs, Profiles, Verifications & Governance",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                actions = {
                    if (!uiState.isPersonalView) {
                        Button(
                            onClick = {
                                when (uiState.selectedTab) {
                                    AffiliateCommandTab.PROGRAMS -> showCreateProgramDialog = true
                                    AffiliateCommandTab.ENROLLMENTS -> showEnrollDialog = true
                                    AffiliateCommandTab.PROFILE_ELIGIBILITY -> showEditProfileDialog = true
                                    else -> showCreateAffiliateDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            val (icon, label) = when (uiState.selectedTab) {
                                AffiliateCommandTab.PROGRAMS -> Icons.Default.AddBusiness to "New Program"
                                AffiliateCommandTab.ENROLLMENTS -> Icons.Default.HowToReg to "Enroll Affiliate"
                                AffiliateCommandTab.PROFILE_ELIGIBILITY -> Icons.Default.Edit to "Edit Profile"
                                else -> Icons.Default.PersonAdd to "New Affiliate"
                            }
                            Icon(icon, contentDescription = null, tint = Color(0xFF0A0E17), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(label, color = Color(0xFF0A0E17), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = { showEditProfileDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF0A0E17), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Profile", color = Color(0xFF0A0E17), fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
            // Notification Messages
            AnimatedVisibility(visible = uiState.errorMessage != null) {
                uiState.errorMessage?.let { err ->
                    Surface(
                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFEF4444))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(err, color = Color(0xFFEF4444), fontSize = 12.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.clearMessages() }) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFFEF4444))
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = uiState.successMessage != null) {
                uiState.successMessage?.let { msg ->
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(msg, color = Color(0xFF10B981), fontSize = 12.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.clearMessages() }) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF10B981))
                            }
                        }
                    }
                }
            }

            // Command Tabs
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = Color(0xFF0F172A),
                contentColor = Color(0xFF00E5FF),
                edgePadding = 16.dp
            ) {
                AffiliateCommandTab.values().forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    tab.title,
                                    fontWeight = if (uiState.selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    color = if (uiState.selectedTab == tab) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                                    fontSize = 13.sp
                                )
                                if (tab == AffiliateCommandTab.PENDING_APPROVAL && uiState.pendingCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color(0xFFF59E0B),
                                        shape = CircleShape,
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                "${uiState.pendingCount}",
                                                color = Color.Black,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF00E5FF)
                    )
                } else {
                    when (uiState.selectedTab) {
                        AffiliateCommandTab.OVERVIEW -> AffiliateOverviewView(uiState)
                        AffiliateCommandTab.DIRECTORY -> AffiliateDirectoryView(uiState, viewModel)
                        AffiliateCommandTab.PROGRAMS -> AffiliateProgramsView(uiState, viewModel, onAction = { action, progId ->
                            programActionDialogTarget = action to progId
                        })
                        AffiliateCommandTab.ENROLLMENTS -> AffiliateEnrollmentsView(uiState, viewModel, onAction = { action, enrId ->
                            enrollmentActionDialogTarget = action to enrId
                        })
                        AffiliateCommandTab.VERIFICATIONS -> AffiliateVerificationsView(uiState, viewModel, onAction = { action, verId ->
                            verificationActionDialogTarget = action to verId
                        })
                        AffiliateCommandTab.PENDING_APPROVAL -> AffiliatePendingApprovalView(uiState, onAction = { action, affId ->
                            affiliateActionDialogTarget = action to affId
                        })
                        AffiliateCommandTab.ACTIVE_SUSPENDED -> AffiliateActiveSuspendedView(uiState, onAction = { action, affId ->
                            affiliateActionDialogTarget = action to affId
                        })
                        AffiliateCommandTab.PROFILE_ELIGIBILITY -> AffiliateProfileAndEligibilityView(
                            uiState = uiState,
                            viewModel = viewModel,
                            onEditProfile = { showEditProfileDialog = true },
                            onRequestVerification = { showRequestVerificationDialog = true },
                            onAddDocument = { showAddDocumentDialog = true },
                            onVerificationAction = { action, verId -> verificationActionDialogTarget = action to verId },
                            onDocumentAction = { action, docId -> documentActionDialogTarget = action to docId }
                        )
                        AffiliateCommandTab.COMMUNICATION_CENTER -> AffiliateCommunicationCenterView(uiState, viewModel)
                        AffiliateCommandTab.GOVERNANCE_INTEGRITY -> AffiliateGovernanceIntegrityView(uiState, viewModel)
                        AffiliateCommandTab.AI_HANDOFF -> AffiliateAiHandoffView(uiState)
                    }
                }
            }
        }
    }

    // Modal Dialogs
    if (showCreateAffiliateDialog) {
        CreateAffiliateModal(
            onDismiss = { showCreateAffiliateDialog = false },
            onSubmit = { uid, name, code, type, phone, email, taxId, agrRef ->
                viewModel.createAffiliate(uid, name, code, type, phone, email, taxId, agrRef)
                showCreateAffiliateDialog = false
            }
        )
    }

    if (showCreateProgramDialog) {
        CreateProgramModal(
            onDismiss = { showCreateProgramDialog = false },
            onSubmit = { code, name, desc, start, end, policy, termsRef, termsVer, maxP ->
                viewModel.createProgram(code, name, desc, start, end, policy, termsRef, termsVer, maxP)
                showCreateProgramDialog = false
            }
        )
    }

    if (showEnrollDialog) {
        EnrollAffiliateModal(
            programs = uiState.programsList,
            affiliates = uiState.affiliatesList,
            onDismiss = { showEnrollDialog = false },
            onSubmit = { progId, affId, reason, meta ->
                viewModel.enrollAffiliate(progId, affId, reason, null, null, meta)
                showEnrollDialog = false
            }
        )
    }

    if (showEditProfileDialog) {
        val currentProfile = uiState.selectedOperationalProfile
        val affId = uiState.selectedAffiliate?.affiliateId ?: ""
        EditOperationalProfileModal(
            affiliateId = affId,
            initialProfile = currentProfile,
            onDismiss = { showEditProfileDialog = false },
            onSubmit = { dName, lName, bType, desc, email, phone, web, a1, a2, city, reg, ctry, post, tax, taxRef ->
                viewModel.upsertOperationalProfile(
                    affiliateId = affId,
                    displayName = dName,
                    legalName = lName,
                    businessType = bType,
                    businessDescription = desc,
                    contactEmail = email,
                    contactPhone = phone,
                    website = web,
                    addressLine1 = a1,
                    addressLine2 = a2,
                    city = city,
                    region = reg,
                    country = ctry,
                    postalCode = post,
                    taxIdOrGst = tax,
                    taxInformationReference = taxRef
                )
                showEditProfileDialog = false
            }
        )
    }

    if (showRequestVerificationDialog) {
        val affId = uiState.selectedAffiliate?.affiliateId ?: ""
        RequestVerificationModal(
            affiliateId = affId,
            onDismiss = { showRequestVerificationDialog = false },
            onSubmit = { vType, reason, metaRef ->
                viewModel.requestVerification(affId, vType, reason, metaRef)
                showRequestVerificationDialog = false
            }
        )
    }

    if (showAddDocumentDialog) {
        val affId = uiState.selectedAffiliate?.affiliateId ?: ""
        AddDocumentModal(
            affiliateId = affId,
            verifications = uiState.selectedVerifications,
            onDismiss = { showAddDocumentDialog = false },
            onSubmit = { docType, storageRef, fName, verId, fSize, mime ->
                viewModel.addDocumentReference(affId, docType, storageRef, fName, verId, fSize, mime)
                showAddDocumentDialog = false
            }
        )
    }

    // Action Confirmation Dialogs
    affiliateActionDialogTarget?.let { (action, affId) ->
        AlertDialog(
            onDismissRequest = { affiliateActionDialogTarget = null },
            title = { Text("$action Affiliate", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter operational reason for '$action' action on affiliate:", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = actionReason,
                        onValueChange = { actionReason = it },
                        label = { Text("Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = actionReason.ifBlank { "Operational transition $action" }
                        when (action) {
                            "ACTIVATE" -> viewModel.activateAffiliate(affId, reason)
                            "SUSPEND" -> viewModel.suspendAffiliate(affId, reason)
                            "REACTIVATE" -> viewModel.reactivateAffiliate(affId, reason)
                            "REJECT" -> viewModel.rejectAffiliate(affId, reason)
                        }
                        affiliateActionDialogTarget = null
                        actionReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Confirm", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { affiliateActionDialogTarget = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    programActionDialogTarget?.let { (action, progId) ->
        AlertDialog(
            onDismissRequest = { programActionDialogTarget = null },
            title = { Text("$action Program", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter operational reason for '$action' action on program:", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = actionReason,
                        onValueChange = { actionReason = it },
                        label = { Text("Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = actionReason.ifBlank { "Operational program transition $action" }
                        when (action) {
                            "ACTIVATE" -> viewModel.activateProgram(progId, reason)
                            "PAUSE" -> viewModel.pauseProgram(progId, reason)
                            "CLOSE" -> viewModel.closeProgram(progId, reason)
                        }
                        programActionDialogTarget = null
                        actionReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Confirm", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { programActionDialogTarget = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    enrollmentActionDialogTarget?.let { (action, enrId) ->
        AlertDialog(
            onDismissRequest = { enrollmentActionDialogTarget = null },
            title = { Text("$action Enrollment", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter operational reason for '$action' action on enrollment:", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = actionReason,
                        onValueChange = { actionReason = it },
                        label = { Text("Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = actionReason.ifBlank { "Operational enrollment transition $action" }
                        when (action) {
                            "APPROVE" -> viewModel.approveEnrollment(enrId, reason)
                            "REJECT" -> viewModel.rejectEnrollment(enrId, reason)
                            "ACTIVATE" -> viewModel.activateEnrollment(enrId, reason)
                            "SUSPEND" -> viewModel.suspendEnrollment(enrId, reason)
                            "RESUME" -> viewModel.resumeEnrollment(enrId, reason)
                            "TERMINATE" -> viewModel.terminateEnrollment(enrId, reason)
                        }
                        enrollmentActionDialogTarget = null
                        actionReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Confirm", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { enrollmentActionDialogTarget = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    verificationActionDialogTarget?.let { (action, verId) ->
        AlertDialog(
            onDismissRequest = { verificationActionDialogTarget = null },
            title = { Text("$action Verification Check", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter decision reason:", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = actionReason,
                        onValueChange = { actionReason = it },
                        label = { Text("Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (action == "REQUEST_CHANGES") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = actionNotes,
                            onValueChange = { actionNotes = it },
                            label = { Text("Required Change Notes") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = actionReason.ifBlank { "Verification review decision $action" }
                        when (action) {
                            "APPROVE" -> viewModel.approveVerification(verId, reason)
                            "REJECT" -> viewModel.rejectVerification(verId, reason)
                            "REQUEST_CHANGES" -> viewModel.requestVerificationChanges(verId, reason, actionNotes.ifBlank { "Please update details and resubmit" })
                        }
                        verificationActionDialogTarget = null
                        actionReason = ""
                        actionNotes = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Confirm Decision", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { verificationActionDialogTarget = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    documentActionDialogTarget?.let { (action, docId) ->
        AlertDialog(
            onDismissRequest = { documentActionDialogTarget = null },
            title = { Text("$action Document Reference", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Confirm decision on supporting document reference:", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                    if (action == "REJECT") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = actionReason,
                            onValueChange = { actionReason = it },
                            label = { Text("Rejection Reason") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = actionReason.ifBlank { "Document rejected by reviewer" }
                        when (action) {
                            "VERIFY" -> viewModel.verifyDocumentReference(docId)
                            "REJECT" -> viewModel.rejectDocumentReference(docId, reason)
                        }
                        documentActionDialogTarget = null
                        actionReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Confirm", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { documentActionDialogTarget = null }) {
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
    val progSummary = uiState.programSummary
    val profSummary = uiState.profileSummary

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
                KpiCard("TOTAL PROGRAMS", "${progSummary?.totalPrograms ?: uiState.programsList.size}", Color(0xFF38BDF8), modifier = Modifier.weight(1f))
                KpiCard("ACTIVE PROGRAMS", "${progSummary?.activePrograms ?: uiState.activeProgramsCount}", Color(0xFF10B981), modifier = Modifier.weight(1f))
                KpiCard("ACTIVE ENROLLMENTS", "${progSummary?.activeEnrollments ?: uiState.activeEnrollmentsCount}", Color(0xFF8B5CF6), modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCard("VERIFIED PROFILES", "${profSummary?.verifiedProfiles ?: 0}", Color(0xFF10B981), modifier = Modifier.weight(1f))
                KpiCard("UNDER REVIEW", "${profSummary?.pendingReviewProfiles ?: 0}", Color(0xFFF59E0B), modifier = Modifier.weight(1f))
                KpiCard("TOTAL DOCUMENTS", "${profSummary?.totalDocuments ?: 0}", Color(0xFF00E5FF), modifier = Modifier.weight(1f))
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
fun AffiliateProgramsView(
    uiState: AffiliateManagementUiState,
    viewModel: AffiliateManagementViewModel,
    onAction: (String, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.filteredPrograms) { prog ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (prog.programId == uiState.selectedProgram?.programId) Color(0xFF00E5FF) else Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.selectProgram(prog.programId) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(prog.programName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                Text("Code: ${prog.programCode} | Policy: ${prog.eligibilityPolicy}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                            StatusBadge(prog.status)
                        }
                        val desc = prog.description
                        if (desc != null) {
                            Text(desc, color = Color(0xFFCBD5E1), fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
                        }
                        Text("Active Range: ${formatDate(prog.startDate)} - ${prog.endDate?.let { formatDate(it) } ?: "Ongoing"}", color = Color(0xFF64748B), fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (prog.status) {
                                "DRAFT" -> {
                                    Button(onClick = { onAction("ACTIVATE", prog.programId) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)), shape = RoundedCornerShape(6.dp)) {
                                        Text("Activate", color = Color.Black, fontSize = 11.sp)
                                    }
                                }
                                "ACTIVE" -> {
                                    Button(onClick = { onAction("PAUSE", prog.programId) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)), shape = RoundedCornerShape(6.dp)) {
                                        Text("Pause", color = Color.Black, fontSize = 11.sp)
                                    }
                                    OutlinedButton(onClick = { onAction("CLOSE", prog.programId) }, shape = RoundedCornerShape(6.dp)) {
                                        Text("Close", color = Color(0xFFEF4444), fontSize = 11.sp)
                                    }
                                }
                                "PAUSED" -> {
                                    Button(onClick = { onAction("ACTIVATE", prog.programId) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)), shape = RoundedCornerShape(6.dp)) {
                                        Text("Resume", color = Color.Black, fontSize = 11.sp)
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

@Composable
fun AffiliateEnrollmentsView(
    uiState: AffiliateManagementUiState,
    viewModel: AffiliateManagementViewModel,
    onAction: (String, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.filteredEnrollments) { enr ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (enr.enrollmentId == uiState.selectedEnrollment?.enrollmentId) Color(0xFF00E5FF) else Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.selectEnrollment(enr.enrollmentId) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Enrollment: ${enr.enrollmentId.take(8)}...", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text("Affiliate: ${enr.affiliateId} | Program: ${enr.programId}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                            StatusBadge(enr.enrollmentStatus)
                        }
                        if (enr.enrollmentReason != null) {
                            Text("Reason: ${enr.enrollmentReason}", color = Color(0xFF00E5FF), fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
                        }
                        Text("Requested: ${formatDate(enr.requestedAt)}", color = Color(0xFF64748B), fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (enr.enrollmentStatus) {
                                "PENDING" -> {
                                    Button(onClick = { onAction("APPROVE", enr.enrollmentId) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)), shape = RoundedCornerShape(6.dp)) {
                                        Text("Approve", color = Color.Black, fontSize = 11.sp)
                                    }
                                    OutlinedButton(onClick = { onAction("REJECT", enr.enrollmentId) }, shape = RoundedCornerShape(6.dp)) {
                                        Text("Reject", color = Color(0xFFEF4444), fontSize = 11.sp)
                                    }
                                }
                                "APPROVED" -> {
                                    Button(onClick = { onAction("ACTIVATE", enr.enrollmentId) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)), shape = RoundedCornerShape(6.dp)) {
                                        Text("Activate", color = Color.Black, fontSize = 11.sp)
                                    }
                                }
                                "ACTIVE" -> {
                                    Button(onClick = { onAction("SUSPEND", enr.enrollmentId) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)), shape = RoundedCornerShape(6.dp)) {
                                        Text("Suspend", color = Color.Black, fontSize = 11.sp)
                                    }
                                    OutlinedButton(onClick = { onAction("TERMINATE", enr.enrollmentId) }, shape = RoundedCornerShape(6.dp)) {
                                        Text("Terminate", color = Color(0xFFEF4444), fontSize = 11.sp)
                                    }
                                }
                                "SUSPENDED" -> {
                                    Button(onClick = { onAction("RESUME", enr.enrollmentId) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)), shape = RoundedCornerShape(6.dp)) {
                                        Text("Resume", color = Color.Black, fontSize = 11.sp)
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

@Composable
fun AffiliateVerificationsView(
    uiState: AffiliateManagementUiState,
    viewModel: AffiliateManagementViewModel,
    onAction: (String, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (uiState.selectedVerifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No verification records for the selected affiliate.", color = Color(0xFF94A3B8), fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.selectedVerifications) { ver ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Verification: ${ver.verificationType}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    Text("ID: ${ver.verificationId.take(12)}... | Submitted: ${formatDate(ver.submittedAt ?: ver.createdAt)}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                                StatusBadge(ver.status)
                            }
                            if (ver.reason != null) {
                                Text("Reason: ${ver.reason}", color = Color(0xFFEF4444), fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
                            }
                            if (ver.changeRequestNotes != null) {
                                Text("Notes: ${ver.changeRequestNotes}", color = Color(0xFFF59E0B), fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (ver.status in setOf("SUBMITTED", "UNDER_REVIEW")) {
                                    Button(onClick = { onAction("APPROVE", ver.verificationId) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)), shape = RoundedCornerShape(6.dp)) {
                                        Text("Approve", color = Color.Black, fontSize = 11.sp)
                                    }
                                    Button(onClick = { onAction("REQUEST_CHANGES", ver.verificationId) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)), shape = RoundedCornerShape(6.dp)) {
                                        Text("Request Changes", color = Color.Black, fontSize = 11.sp)
                                    }
                                    OutlinedButton(onClick = { onAction("REJECT", ver.verificationId) }, shape = RoundedCornerShape(6.dp)) {
                                        Text("Reject", color = Color(0xFFEF4444), fontSize = 11.sp)
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

@Composable
fun AffiliateDirectoryView(
    uiState: AffiliateManagementUiState,
    viewModel: AffiliateManagementViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search by name, code, or user ID...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF00E5FF)) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00E5FF),
                unfocusedBorderColor = Color(0xFF334155),
                focusedContainerColor = Color(0xFF0F172A),
                unfocusedContainerColor = Color(0xFF0F172A)
            ),
            shape = RoundedCornerShape(8.dp)
        )

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
                            Text(aff.displayName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            Text("Code: ${aff.affiliateCode} | Type: ${aff.affiliateType}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                        StatusBadge(aff.status)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (aff.status == "ACTIVE") {
                            Button(
                                onClick = { onAction("SUSPEND", aff.affiliateId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Suspend", color = Color.Black, fontSize = 11.sp)
                            }
                        } else {
                            Button(
                                onClick = { onAction("REACTIVATE", aff.affiliateId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Reactivate", color = Color.Black, fontSize = 11.sp)
                            }
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
    viewModel: AffiliateManagementViewModel,
    onEditProfile: () -> Unit = {},
    onRequestVerification: () -> Unit = {},
    onAddDocument: () -> Unit = {},
    onVerificationAction: (String, String) -> Unit = { _, _ -> },
    onDocumentAction: (String, String) -> Unit = { _, _ -> }
) {
    val aff = uiState.selectedAffiliate
    val el = uiState.selectedEligibility
    val opProfile = uiState.selectedOperationalProfile
    val completeness = uiState.selectedCompleteness
    val verifs = uiState.selectedVerifications
    val docs = uiState.selectedDocuments
    val audits = uiState.selectedProfileAudits

    if (aff == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select an affiliate from the Directory to view details.", color = Color(0xFF94A3B8))
        }
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. Operational Profile Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(opProfile?.displayName ?: aff.displayName, fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp)
                            Text("Affiliate Code: ${aff.affiliateCode}", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        StatusBadge(opProfile?.profileStatus ?: aff.status)
                    }
                    Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 12.dp))
                    DetailRow("Legal Name", opProfile?.legalName ?: "N/A")
                    DetailRow("Business Type", opProfile?.businessType ?: aff.affiliateType)
                    DetailRow("Contact Phone", opProfile?.contactPhone ?: aff.contactPhone ?: "N/A")
                    DetailRow("Contact Email", opProfile?.contactEmail ?: aff.contactEmail ?: "N/A")
                    DetailRow("Website", opProfile?.website ?: "N/A")
                    DetailRow("Address", "${opProfile?.addressLine1 ?: "N/A"}, ${opProfile?.city ?: ""}, ${opProfile?.country ?: ""}")
                    DetailRow("Tax / GST ID", opProfile?.taxIdOrGst ?: aff.taxIdOrGst ?: "N/A")

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onEditProfile,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Profile", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        if (opProfile?.profileStatus == "INCOMPLETE" || opProfile?.profileStatus == "CHANGES_REQUIRED") {
                            Button(
                                onClick = { viewModel.submitOperationalProfile(aff.affiliateId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Submit Profile", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 2. Profile Completeness Progress
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (completeness?.isComplete == true) Color(0xFF10B981) else Color(0xFFF59E0B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("PROFILE COMPLETENESS ASSESSMENT", fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 13.sp)
                        Text("${completeness?.score ?: 0}%", color = Color(0xFF00E5FF), fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = (completeness?.score ?: 0) / 100f,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF00E5FF),
                        trackColor = Color(0xFF334155)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Readiness: ${if (completeness?.isComplete == true) "Complete Profile" else "Incomplete Profile"}", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    if (completeness?.missingFields?.isNotEmpty() == true) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Missing: ${completeness.missingFields.joinToString(", ")}", color = Color(0xFFEF4444), fontSize = 11.sp)
                    }
                }
            }
        }

        // 3. Verifications & Documents Action Header
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("VERIFICATION CHECKS & EVIDENCE", fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onRequestVerification, shape = RoundedCornerShape(6.dp)) {
                        Text("+ Request Verification", color = Color(0xFF00E5FF), fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = onAddDocument, shape = RoundedCornerShape(6.dp)) {
                        Text("+ Add Document", color = Color(0xFF00E5FF), fontSize = 11.sp)
                    }
                }
            }
        }

        // 4. Verifications Records
        items(verifs) { ver ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Check: ${ver.verificationType}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                        StatusBadge(ver.status)
                    }
                    Text("Submitted: ${formatDate(ver.submittedAt ?: ver.createdAt)}", color = Color(0xFF64748B), fontSize = 10.sp)
                    if (ver.reason != null) Text("Note: ${ver.reason}", color = Color(0xFFEF4444), fontSize = 11.sp)
                    if (ver.status in setOf("SUBMITTED", "UNDER_REVIEW")) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { onVerificationAction("APPROVE", ver.verificationId) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)), shape = RoundedCornerShape(4.dp)) {
                                Text("Approve", color = Color.Black, fontSize = 10.sp)
                            }
                            Button(onClick = { onVerificationAction("REQUEST_CHANGES", ver.verificationId) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)), shape = RoundedCornerShape(4.dp)) {
                                Text("Changes", color = Color.Black, fontSize = 10.sp)
                            }
                            OutlinedButton(onClick = { onVerificationAction("REJECT", ver.verificationId) }, shape = RoundedCornerShape(4.dp)) {
                                Text("Reject", color = Color(0xFFEF4444), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // 5. Supporting Documents
        item {
            Text("SUPPORTING DOCUMENT METADATA", fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 13.sp)
        }

        items(docs) { doc ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(doc.fileName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            Text("${doc.documentType} | ${doc.fileSizeBytes?.let { "${it / 1024} KB" } ?: "N/A"}", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        }
                        StatusBadge(doc.status)
                    }
                    if (doc.status == "UPLOADED") {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { onDocumentAction("VERIFY", doc.documentId) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)), shape = RoundedCornerShape(4.dp)) {
                                Text("Verify Document", color = Color.Black, fontSize = 10.sp)
                            }
                            OutlinedButton(onClick = { onDocumentAction("REJECT", doc.documentId) }, shape = RoundedCornerShape(4.dp)) {
                                Text("Reject", color = Color(0xFFEF4444), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // 6. Multi-dimensional Eligibility Assessment
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
                }
            }
        }

        // 7. Profile Append-Only Audit Ledger
        item {
            Text("PROFILE-LEVEL AUDIT LOG", fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 13.sp)
        }

        items(audits) { audit ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(audit.action, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                        Text(formatDate(audit.timestamp), color = Color(0xFF64748B), fontSize = 10.sp)
                    }
                    Text("Reason: ${audit.reason ?: "N/A"}", color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp))
                    Text("Actor: ${audit.actorUserId} (${audit.actorRole})", color = Color(0xFF64748B), fontSize = 10.sp)
                    Text("Hash: ${audit.recordHash.take(16)}... | Chain: ${audit.chainHash.take(16)}...", fontFamily = FontFamily.Monospace, color = Color(0xFF00E5FF).copy(alpha = 0.7f), fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
fun AffiliateAiHandoffView(uiState: AffiliateManagementUiState) {
    val handoff = uiState.selectedHandoffContract
    val progHandoff = uiState.selectedProgramHandoffContract
    val profHandoff = uiState.selectedProfileHandoffContract

    if (handoff == null && progHandoff == null && profHandoff == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No affiliate, enrollment, or profile selected for AI Handoff Contract inspection.", color = Color(0xFF94A3B8))
        }
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (profHandoff != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("MODULE 20 STEP 03 PROFILE HANDOFF CONTRACT", fontWeight = FontWeight.Black, color = Color(0xFF00E5FF), fontSize = 13.sp)
                            Surface(color = Color(0xFF00E5FF).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text(profHandoff.contractVersion, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(4.dp))
                            }
                        }
                        Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 8.dp))
                        DetailRow("Read-Only Enforced", "${profHandoff.isReadOnly}")
                        DetailRow("Affiliate ID", profHandoff.affiliateId)
                        DetailRow("Verification Status", profHandoff.profileStatus.name)
                        DetailRow("Completeness Score", "${profHandoff.completenessScore}%")
                        DetailRow("Verified State", "${profHandoff.isVerified}")
                        DetailRow("Profile Complete", "${profHandoff.isProfileComplete}")
                        DetailRow("Verification Checks", "${profHandoff.verificationSummary.size} checks")
                        DetailRow("Supporting Documents", "${profHandoff.documentCount} documents")
                        DetailRow("Integrity Seal", profHandoff.integritySealHash.take(24) + "...")
                    }
                }
            }
        }

        if (progHandoff != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.7f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("MODULE 20 STEP 02 PROGRAM HANDOFF CONTRACT", fontWeight = FontWeight.Black, color = Color(0xFF00E5FF), fontSize = 13.sp)
                            Surface(color = Color(0xFF00E5FF).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text(progHandoff.contractVersion, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(4.dp))
                            }
                        }
                        Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 8.dp))
                        DetailRow("Enrollment ID", progHandoff.enrollmentId)
                        DetailRow("Program Code", progHandoff.programCode)
                        DetailRow("Affiliate Code", progHandoff.affiliateCode)
                        DetailRow("Commission Eligible", "${progHandoff.isEligibleForCommission}")
                        DetailRow("Attribution Eligible", "${progHandoff.isEligibleForAttribution}")
                    }
                }
            }
        }

        if (handoff != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("MODULE 20 STEP 01 AFFILIATE IDENTITY CONTRACT", fontWeight = FontWeight.Black, color = Color(0xFF00E5FF), fontSize = 13.sp)
                            Surface(color = Color(0xFF00E5FF).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text(handoff.contractVersion, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(4.dp))
                            }
                        }
                        Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 8.dp))
                        DetailRow("Affiliate ID", handoff.affiliateId)
                        DetailRow("Affiliate Code", handoff.affiliateCode)
                        DetailRow("Attribution Eligible", "${handoff.isEligibleForAttribution}")
                        DetailRow("Commission Eligible", "${handoff.isEligibleForCommission}")
                    }
                }
            }
        }
    }
}

// Subcomponents & Modals

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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(affiliate.displayName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Text("Code: ${affiliate.affiliateCode} | Type: ${affiliate.affiliateType}", color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
            StatusBadge(affiliate.status)
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bg, fg) = when (status) {
        "ACTIVE", "VERIFIED", "APPROVED" -> Color(0xFF10B981).copy(alpha = 0.2f) to Color(0xFF10B981)
        "PENDING", "SUBMITTED", "UNDER_REVIEW" -> Color(0xFFF59E0B).copy(alpha = 0.2f) to Color(0xFFF59E0B)
        "CHANGES_REQUIRED" -> Color(0xFF8B5CF6).copy(alpha = 0.2f) to Color(0xFF8B5CF6)
        "SUSPENDED", "REJECTED", "TERMINATED", "INCOMPLETE" -> Color(0xFFEF4444).copy(alpha = 0.2f) to Color(0xFFEF4444)
        else -> Color(0xFF64748B).copy(alpha = 0.2f) to Color(0xFF64748B)
    }
    Surface(color = bg, shape = RoundedCornerShape(4.dp)) {
        Text(status, color = fg, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF94A3B8), fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun EligibilityCheckItem(label: String, passed: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (passed) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (passed) Color(0xFF10B981) else Color(0xFFEF4444),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = if (passed) Color.White else Color(0xFF94A3B8), fontSize = 12.sp)
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// Modal implementations

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
    var taxIdOrGst by remember { mutableStateOf("") }
    var agreementRef by remember { mutableStateOf("AFF-AGR-2026-v1.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Affiliate Identity", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = userId, onValueChange = { userId = it }, label = { Text("User ID (Required)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("Display Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = affiliateCode, onValueChange = { affiliateCode = it }, label = { Text("Affiliate Code (Optional)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = contactEmail, onValueChange = { contactEmail = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = contactPhone, onValueChange = { contactPhone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = taxIdOrGst, onValueChange = { taxIdOrGst = it }, label = { Text("Tax ID / GST") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = agreementRef, onValueChange = { agreementRef = it }, label = { Text("Agreement Reference") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(userId, displayName, affiliateCode.ifBlank { null }, affiliateType, contactPhone.ifBlank { null }, contactEmail.ifBlank { null }, taxIdOrGst.ifBlank { null }, agreementRef.ifBlank { null }) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("Create Affiliate", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF94A3B8)) }
        },
        containerColor = Color(0xFF1E293B)
    )
}

@Composable
fun CreateProgramModal(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String?, Long, Long?, String, String?, String?, Int?) -> Unit
) {
    var programCode by remember { mutableStateOf("") }
    var programName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var eligibilityPolicy by remember { mutableStateOf("STANDARD") }
    var termsRef by remember { mutableStateOf("PROG-TERMS-v1.0") }
    var termsVer by remember { mutableStateOf("v1.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Affiliate Program", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = programCode, onValueChange = { programCode = it }, label = { Text("Program Code (e.g. VIP-2026)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = programName, onValueChange = { programName = it }, label = { Text("Program Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = eligibilityPolicy, onValueChange = { eligibilityPolicy = it }, label = { Text("Eligibility Policy") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = termsRef, onValueChange = { termsRef = it }, label = { Text("Terms Reference") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(programCode, programName, description.ifBlank { null }, System.currentTimeMillis(), null, eligibilityPolicy, termsRef.ifBlank { null }, termsVer, null) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("Create Program", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF94A3B8)) }
        },
        containerColor = Color(0xFF1E293B)
    )
}

@Composable
fun EnrollAffiliateModal(
    programs: List<AffiliateProgramDto>,
    affiliates: List<AffiliateProfileDto>,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String?, String?) -> Unit
) {
    var selectedProgramId by remember { mutableStateOf(programs.firstOrNull()?.programId ?: "") }
    var selectedAffiliateId by remember { mutableStateOf(affiliates.firstOrNull()?.affiliateId ?: "") }
    var reason by remember { mutableStateOf("Direct admin enrollment") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enroll Affiliate in Program", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = selectedProgramId, onValueChange = { selectedProgramId = it }, label = { Text("Program ID") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = selectedAffiliateId, onValueChange = { selectedAffiliateId = it }, label = { Text("Affiliate ID") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Enrollment Reason") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedProgramId, selectedAffiliateId, reason.ifBlank { null }, null) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("Enroll Affiliate", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF94A3B8)) }
        },
        containerColor = Color(0xFF1E293B)
    )
}

@Composable
fun EditOperationalProfileModal(
    affiliateId: String,
    initialProfile: AffiliateOperationalProfileResponseDto?,
    onDismiss: () -> Unit,
    onSubmit: (String, String?, String, String?, String?, String?, String?, String?, String?, String?, String?, String?, String?, String?, String?) -> Unit
) {
    var displayName by remember { mutableStateOf(initialProfile?.displayName ?: "") }
    var legalName by remember { mutableStateOf(initialProfile?.legalName ?: "") }
    var businessType by remember { mutableStateOf(initialProfile?.businessType ?: "INDIVIDUAL") }
    var businessDescription by remember { mutableStateOf(initialProfile?.businessDescription ?: "") }
    var contactEmail by remember { mutableStateOf(initialProfile?.contactEmail ?: "") }
    var contactPhone by remember { mutableStateOf(initialProfile?.contactPhone ?: "") }
    var website by remember { mutableStateOf(initialProfile?.website ?: "") }
    var addressLine1 by remember { mutableStateOf(initialProfile?.addressLine1 ?: "") }
    var city by remember { mutableStateOf(initialProfile?.city ?: "") }
    var country by remember { mutableStateOf(initialProfile?.country ?: "") }
    var taxIdOrGst by remember { mutableStateOf(initialProfile?.taxIdOrGst ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Operational Profile", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("Display Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = legalName, onValueChange = { legalName = it }, label = { Text("Legal Entity Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = businessType, onValueChange = { businessType = it }, label = { Text("Business Type (INDIVIDUAL, BUSINESS, etc.)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = contactEmail, onValueChange = { contactEmail = it }, label = { Text("Contact Email") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = contactPhone, onValueChange = { contactPhone = it }, label = { Text("Contact Phone") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = website, onValueChange = { website = it }, label = { Text("Website") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = addressLine1, onValueChange = { addressLine1 = it }, label = { Text("Address Line 1") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = country, onValueChange = { country = it }, label = { Text("Country") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = taxIdOrGst, onValueChange = { taxIdOrGst = it }, label = { Text("Tax ID / GST Number") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        displayName,
                        legalName.ifBlank { null },
                        businessType,
                        businessDescription.ifBlank { null },
                        contactEmail.ifBlank { null },
                        contactPhone.ifBlank { null },
                        website.ifBlank { null },
                        addressLine1.ifBlank { null },
                        null,
                        city.ifBlank { null },
                        null,
                        country.ifBlank { null },
                        null,
                        taxIdOrGst.ifBlank { null },
                        null
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("Save Profile", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF94A3B8)) }
        },
        containerColor = Color(0xFF1E293B)
    )
}

@Composable
fun RequestVerificationModal(
    affiliateId: String,
    onDismiss: () -> Unit,
    onSubmit: (String, String?, String?) -> Unit
) {
    var verificationType by remember { mutableStateOf("IDENTITY") }
    var reason by remember { mutableStateOf("Initial KYC Identity Verification Check") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request Verification Check", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = verificationType, onValueChange = { verificationType = it }, label = { Text("Type (IDENTITY, BUSINESS, TAX, etc.)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Request Reason / Scope") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(verificationType, reason.ifBlank { null }, null) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("Submit Request", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF94A3B8)) }
        },
        containerColor = Color(0xFF1E293B)
    )
}

@Composable
fun AddDocumentModal(
    affiliateId: String,
    verifications: List<AffiliateVerificationResponseDto>,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String?, Long?, String?) -> Unit
) {
    var documentType by remember { mutableStateOf("IDENTITY_PROOF") }
    var fileName by remember { mutableStateOf("national_id_scan.pdf") }
    var storageRef by remember { mutableStateOf("gs://sucharu-pro-affiliate-docs/doc_${UUID.randomUUID()}.pdf") }
    var selectedVerificationId by remember { mutableStateOf(verifications.firstOrNull()?.verificationId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Supporting Document Reference", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = documentType, onValueChange = { documentType = it }, label = { Text("Doc Type (IDENTITY_PROOF, BUSINESS_REG, etc.)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = fileName, onValueChange = { fileName = it }, label = { Text("File Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = storageRef, onValueChange = { storageRef = it }, label = { Text("Storage URI Reference") }, modifier = Modifier.fillMaxWidth())
                if (verifications.isNotEmpty()) {
                    OutlinedTextField(value = selectedVerificationId ?: "", onValueChange = { selectedVerificationId = it.ifBlank { null } }, label = { Text("Linked Verification ID (Optional)") }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(documentType, storageRef, fileName, selectedVerificationId, 1048576L, "application/pdf") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("Upload Document", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF94A3B8)) }
        },
        containerColor = Color(0xFF1E293B)
    )
}

@Composable
fun AffiliateCommunicationCenterView(
    uiState: AffiliateManagementUiState,
    viewModel: AffiliateManagementViewModel
) {
    val summary = uiState.communicationSummary
    val comms = uiState.communicationsList
    val prefs = uiState.notificationPreferences
    val unreadCount = uiState.unreadCommunicationCount

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Affiliate Communication Governance", color = Color(0xFF00E5FF), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Surface(
                            color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Unread: $unreadCount", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            Text("${summary?.totalCommunications ?: comms.size}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Delivered", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            Text("${summary?.deliveredCount ?: comms.count { it.status == "DELIVERED" }}", color = Color(0xFF34D399), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Read", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            Text("${summary?.readCount ?: comms.count { it.isRead }}", color = Color(0xFF38BDF8), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Failed", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            Text("${summary?.failedCount ?: 0}", color = Color(0xFFF87171), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Notification Preferences Section
        item {
            Text("Notification Channel Preferences", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        if (prefs.isEmpty()) {
            item {
                Text("Default preferences active (In-App & Push enabled).", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
        } else {
            items(prefs, key = { it.preferenceId }) { pref ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(pref.communicationType, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                if (pref.isMandatory) "Mandatory Delivery (In-App Required)" else "Custom Channels",
                                color = if (pref.isMandatory) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (pref.inAppEnabled) Surface(color = Color(0xFF38BDF8).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) { Text("IN_APP", color = Color(0xFF38BDF8), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) }
                            if (pref.pushEnabled) Surface(color = Color(0xFFC084FC).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) { Text("PUSH", color = Color(0xFFC084FC), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) }
                            if (pref.emailEnabled) Surface(color = Color(0xFF34D399).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) { Text("EMAIL", color = Color(0xFF34D399), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) }
                        }
                    }
                }
            }
        }

        // Communications Log Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Communication History & Alerts", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        if (comms.isEmpty()) {
            item {
                Text("No communication records available for this affiliate.", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
        } else {
            items(comms, key = { it.communicationId }) { item ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.isRead) Color(0xFF1E293B) else Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.title, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Surface(
                                color = if (item.isRead) Color(0xFF64748B).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    if (item.isRead) "READ" else "UNREAD",
                                    color = if (item.isRead) Color(0xFF94A3B8) else Color(0xFFF59E0B),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(item.message, color = Color(0xFFCBD5E1), fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Type: ${item.communicationType}", color = Color(0xFF64748B), fontSize = 10.sp)
                            if (!item.isRead) {
                                TextButton(
                                    onClick = { viewModel.markCommunicationRead(item.communicationId) },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Mark as Read", color = Color(0xFF38BDF8), fontSize = 11.sp)
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
fun AffiliateGovernanceIntegrityView(
    uiState: AffiliateManagementUiState,
    viewModel: AffiliateManagementViewModel
) {
    val readiness = uiState.selectedIntegrationReadiness
    val integrity = uiState.lifecycleIntegrityResult
    val finalHandoff = uiState.selectedFinalHandoffContract
    val chainVerification = uiState.auditChainVerificationResult

    if (uiState.isGovernanceIntegrityLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF00E5FF))
        }
        return
    }

    if (readiness == null && integrity == null && finalHandoff == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select an affiliate from the Directory or Pending tab to inspect Governance Integrity.", color = Color(0xFF94A3B8))
        }
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (finalHandoff != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("MODULE 20 STEP 06 FINAL HANDOFF CONTRACT", fontWeight = FontWeight.Black, color = Color(0xFF00E5FF), fontSize = 13.sp)
                            Surface(color = Color(0xFF00E5FF).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text(finalHandoff.contractVersion, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(4.dp))
                            }
                        }
                        Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 8.dp))
                        DetailRow("Read-Only Enforced", "${finalHandoff.isReadOnly}")
                        DetailRow("Affiliate ID", finalHandoff.affiliateId)
                        DetailRow("Affiliate Code", finalHandoff.affiliateCode)
                        DetailRow("Status", finalHandoff.currentStatus.name)
                        DetailRow("Ready for Attribution (Module 21)", "${finalHandoff.isReadyForAttribution}")
                        DetailRow("Ready for Commission (Module 22)", "${finalHandoff.isReadyForCommission}")
                        DetailRow("Ready for Payout (Module 23)", "${finalHandoff.isReadyForPayout}")
                        DetailRow("Ready for Analytics (Module 24)", "${finalHandoff.isReadyForAnalytics}")
                        DetailRow("Integrity Seal", finalHandoff.integritySealHash.take(24) + "...")
                    }
                }
            }
        }

        if (readiness != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("CROSS-MODULE INTEGRATION READINESS", fontWeight = FontWeight.Black, color = Color(0xFF10B981), fontSize = 13.sp)
                        Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 8.dp))
                        DetailRow("Composite Score", "${readiness.readinessScore} / 100")
                        DetailRow("Identity Verified", "${readiness.isIdentityVerified}")
                        DetailRow("Agreement Accepted", "${readiness.isAgreementAccepted}")
                        DetailRow("Tax Compliant", "${readiness.isTaxCompliant}")
                        DetailRow("Account Active", "${readiness.isAccountActive}")
                        DetailRow("Fully Eligible", "${readiness.isFullyEligible}")
                        DetailRow("Clear Governance Queue", "${readiness.hasClearGovernanceQueue}")
                    }
                }
            }
        }

        if (integrity != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (integrity.isIntegrityValid) Color(0xFF10B981) else Color(0xFFEF4444))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("LIFECYCLE INTEGRITY ASSESSMENT", fontWeight = FontWeight.Black, color = if (integrity.isIntegrityValid) Color(0xFF10B981) else Color(0xFFEF4444), fontSize = 13.sp)
                        Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 8.dp))
                        DetailRow("Integrity Valid", "${integrity.isIntegrityValid}")
                        DetailRow("Critical Violations", "${integrity.criticalCount}")
                        DetailRow("High Violations", "${integrity.highCount}")
                        DetailRow("Medium Violations", "${integrity.mediumCount}")
                        DetailRow("Summary", integrity.summary)
                    }
                }
            }
        }

        if (chainVerification != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("SHA-256 AUDIT CHAIN VERIFICATION", fontWeight = FontWeight.Black, color = Color(0xFFF59E0B), fontSize = 13.sp)
                        Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 8.dp))
                        DetailRow("Chain Intact", "${chainVerification.isChainIntact}")
                        DetailRow("Records Checked", "${chainVerification.totalRecordsChecked}")
                        DetailRow("Verification Summary", chainVerification.summary)
                    }
                }
            }
        }
    }
}

