package com.sucharu.sucharupro.ui.features.cost

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.domain.model.businesscost.BusinessCostTrackingSummary
import java.text.SimpleDateFormat
import java.util.*

// Design System Palette (Dark Navy / Near-Black Cyber-ERP)
private val DeepNavyBg = Color(0xFF0B111E)
private val CardSurface = Color(0xFF141E33)
private val CardBorder = Color(0xFF223254)
private val AccentCyan = Color(0xFF00E5FF)
private val AccentEmerald = Color(0xFF00E676)
private val AccentAmber = Color(0xFFFFB300)
private val AccentRose = Color(0xFFFF1744)
private val AccentPurple = Color(0xFFD500F9)
private val TextPrimary = Color(0xFFF0F4FC)
private val TextSecondary = Color(0xFF90A4AE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessCostManagementScreen(
    costCenters: List<BusinessCostCenterResponse> = emptyList(),
    costCategories: List<BusinessCostCategoryResponse> = emptyList(),
    trackingRecords: List<BusinessCostTrackingResponse> = emptyList(),
    trackingSummary: BusinessCostTrackingSummary? = null,
    userRole: String = "ADMIN",
    onCreateCostCenter: (CreateBusinessCostCenterRequest) -> Unit = {},
    onCreateCostCategory: (CreateBusinessCostCategoryRequest) -> Unit = {},
    onTrackCost: (TrackOperationalCostRequest) -> Unit = {},
    onClassifyCost: (String, ClassifyCostRequest) -> Unit = { _, _ -> },
    onReclassifyCost: (String, ReclassifyCostRequest) -> Unit = { _, _ -> },
    onActivateCenter: (String) -> Unit = {},
    onDeactivateCenter: (String) -> Unit = {},
    onActivateCategory: (String) -> Unit = {},
    onDeactivateCategory: (String) -> Unit = {},
    onViewAudit: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Cost Tracking, 1: Cost Centers, 2: Cost Categories, 3: KPI Analytics
    var searchQuery by remember { mutableStateOf("") }
    var selectedAllocationFilter by remember { mutableStateOf("ALL") }
    var selectedCenterFilter by remember { mutableStateOf("ALL") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }

    // Modals
    var showTrackCostModal by remember { mutableStateOf(false) }
    var showCreateCenterModal by remember { mutableStateOf(false) }
    var showCreateCategoryModal by remember { mutableStateOf(false) }
    var selectedTrackingForClassify by remember { mutableStateOf<BusinessCostTrackingResponse?>(null) }
    var selectedTrackingForReclassify by remember { mutableStateOf<BusinessCostTrackingResponse?>(null) }
    var selectedTrackingForDetail by remember { mutableStateOf<BusinessCostTrackingResponse?>(null) }

    val filteredTracking = remember(trackingRecords, searchQuery, selectedAllocationFilter, selectedCenterFilter, selectedCategoryFilter) {
        trackingRecords.filter { t ->
            val matchesAlloc = when (selectedAllocationFilter) {
                "ALL" -> true
                else -> t.allocationStatus.equals(selectedAllocationFilter, ignoreCase = true)
            }
            val matchesCenter = when (selectedCenterFilter) {
                "ALL" -> true
                else -> t.costCenterId == selectedCenterFilter
            }
            val matchesCat = when (selectedCategoryFilter) {
                "ALL" -> true
                else -> t.costCategoryId == selectedCategoryFilter
            }
            val matchesSearch = searchQuery.isBlank() ||
                    t.id.contains(searchQuery, ignoreCase = true) ||
                    t.sourceId.contains(searchQuery, ignoreCase = true) ||
                    (t.jobId?.contains(searchQuery, ignoreCase = true) == true) ||
                    (t.notes?.contains(searchQuery, ignoreCase = true) == true)

            matchesAlloc && matchesCenter && matchesCat && matchesSearch
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavyBg)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Business Cost Management & Tracking",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Cost Center Governance, Categorization & Canonical Job Cost Association (Module 15 Step 04)",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (userRole in setOf("ADMIN", "MANAGER")) {
                        Button(
                            onClick = { showCreateCenterModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CardSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Business, contentDescription = null, tint = AccentCyan)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Cost Center", color = AccentCyan, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showCreateCategoryModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CardSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentPurple),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Category, contentDescription = null, tint = AccentPurple)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Category", color = AccentPurple, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { showTrackCostModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Track Cost", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Top KPI Dashboard Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CostKpiCard(
                    title = "Total Tracked Cost",
                    amount = "${trackingSummary?.totalTrackedCost ?: "0.0000"} BDT",
                    subtitle = "Canonical Source Value",
                    accentColor = AccentCyan,
                    icon = Icons.Default.MonetizationOn,
                    modifier = Modifier.weight(1f)
                )
                CostKpiCard(
                    title = "Allocated Cost",
                    amount = "${trackingSummary?.totalAllocatedCost ?: "0.0000"} BDT",
                    subtitle = "Assigned to Production Jobs",
                    accentColor = AccentEmerald,
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
                CostKpiCard(
                    title = "Unallocated Cost",
                    amount = "${trackingSummary?.totalUnallocatedCost ?: "0.0000"} BDT",
                    subtitle = "Overhead / Pending Assignment",
                    accentColor = AccentAmber,
                    icon = Icons.Default.Pending,
                    modifier = Modifier.weight(1f)
                )
                CostKpiCard(
                    title = "Cost Centers / Categories",
                    amount = "${trackingSummary?.totalCostCenters ?: costCenters.size} / ${trackingSummary?.totalActiveCategories ?: costCategories.size}",
                    subtitle = "Active Organizational Units",
                    accentColor = AccentPurple,
                    icon = Icons.Default.AccountTree,
                    modifier = Modifier.weight(1f)
                )
            }

            // Tab Navigation Bar
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CardSurface,
                contentColor = AccentCyan,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Cost Tracking (${trackingRecords.size})", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.TrackChanges, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Cost Centers (${costCenters.size})", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Business, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Cost Categories (${costCategories.size})", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Category, contentDescription = null) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Tab Content
            when (selectedTab) {
                0 -> CostTrackingView(
                    records = filteredTracking,
                    costCenters = costCenters,
                    costCategories = costCategories,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    selectedAllocationFilter = selectedAllocationFilter,
                    onAllocationFilterChange = { selectedAllocationFilter = it },
                    userRole = userRole,
                    onClassifyClick = { selectedTrackingForClassify = it },
                    onReclassifyClick = { selectedTrackingForReclassify = it },
                    onDetailClick = { selectedTrackingForDetail = it }
                )
                1 -> CostCentersView(
                    costCenters = costCenters,
                    userRole = userRole,
                    onActivate = onActivateCenter,
                    onDeactivate = onDeactivateCenter
                )
                2 -> CostCategoriesView(
                    categories = costCategories,
                    userRole = userRole,
                    onActivate = onActivateCategory,
                    onDeactivate = onDeactivateCategory
                )
            }
        }

        // --- Dialogs ---

        if (showCreateCenterModal) {
            CreateCostCenterDialog(
                costCenters = costCenters,
                onDismiss = { showCreateCenterModal = false },
                onConfirm = {
                    onCreateCostCenter(it)
                    showCreateCenterModal = false
                }
            )
        }

        if (showCreateCategoryModal) {
            CreateCostCategoryDialog(
                categories = costCategories,
                onDismiss = { showCreateCategoryModal = false },
                onConfirm = {
                    onCreateCostCategory(it)
                    showCreateCategoryModal = false
                }
            )
        }

        if (showTrackCostModal) {
            TrackOperationalCostDialog(
                costCenters = costCenters.filter { it.isActive },
                costCategories = costCategories.filter { it.isActive },
                onDismiss = { showTrackCostModal = false },
                onConfirm = {
                    onTrackCost(it)
                    showTrackCostModal = false
                }
            )
        }

        selectedTrackingForClassify?.let { tracking ->
            ClassifyCostDialog(
                tracking = tracking,
                costCenters = costCenters.filter { it.isActive },
                costCategories = costCategories.filter { it.isActive },
                onDismiss = { selectedTrackingForClassify = null },
                onConfirm = { req ->
                    onClassifyCost(tracking.id, req)
                    selectedTrackingForClassify = null
                }
            )
        }

        selectedTrackingForReclassify?.let { tracking ->
            ReclassifyCostDialog(
                tracking = tracking,
                costCenters = costCenters.filter { it.isActive },
                costCategories = costCategories.filter { it.isActive },
                onDismiss = { selectedTrackingForReclassify = null },
                onConfirm = { req ->
                    onReclassifyCost(tracking.id, req)
                    selectedTrackingForReclassify = null
                }
            )
        }

        selectedTrackingForDetail?.let { tracking ->
            CostTrackingDetailDialog(
                tracking = tracking,
                costCenters = costCenters,
                costCategories = costCategories,
                onDismiss = { selectedTrackingForDetail = null },
                onViewAudit = { onViewAudit(tracking.id) }
            )
        }
    }
}

// =============================================================================
// SUB-VIEWS
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CostTrackingView(
    records: List<BusinessCostTrackingResponse>,
    costCenters: List<BusinessCostCenterResponse>,
    costCategories: List<BusinessCostCategoryResponse>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedAllocationFilter: String,
    onAllocationFilterChange: (String) -> Unit,
    userRole: String,
    onClassifyClick: (BusinessCostTrackingResponse) -> Unit,
    onReclassifyClick: (BusinessCostTrackingResponse) -> Unit,
    onDetailClick: (BusinessCostTrackingResponse) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Filter & Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search by ID, Source, Job, or Notes...", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // Status Filter Chips
            listOf("ALL", "UNALLOCATED", "FULLY_ALLOCATED", "RECLASSIFIED", "RECLASSIFICATION_PENDING").forEach { status ->
                FilterChip(
                    selected = selectedAllocationFilter == status,
                    onClick = { onAllocationFilterChange(status) },
                    label = { Text(status.replace("_", " "), fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentCyan.copy(alpha = 0.2f),
                        selectedLabelColor = AccentCyan,
                        containerColor = CardSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedAllocationFilter == status,
                        borderColor = CardBorder,
                        selectedBorderColor = AccentCyan
                    )
                )
            }
        }

        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardSurface, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SOURCE / ID", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
            Text("COST CENTER", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
            Text("CATEGORY", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
            Text("JOB REF", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.0f))
            Text("AMOUNT (BDT)", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
            Text("STATUS", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
            Text("ACTIONS", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
        }

        // Records List
        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(CardSurface, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No operational cost tracking records match the current criteria.", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(CardSurface, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
            ) {
                items(records) { tracking ->
                    val center = costCenters.find { it.id == tracking.costCenterId }
                    val category = costCategories.find { it.id == tracking.costCategoryId }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDetailClick(tracking) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(tracking.sourceType, color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(tracking.sourceId, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }

                        Text(center?.name ?: tracking.costCenterId, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1.5f))
                        Text(category?.name ?: tracking.costCategoryId, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1.5f))

                        Text(
                            text = tracking.jobId ?: "—",
                            color = if (tracking.jobId != null) AccentEmerald else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1.0f)
                        )

                        Text(
                            text = "${tracking.amount} ${tracking.currency}",
                            color = AccentAmber,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.2f)
                        )

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when (tracking.allocationStatus) {
                                        "FULLY_ALLOCATED" -> AccentEmerald.copy(alpha = 0.15f)
                                        "RECLASSIFIED" -> AccentPurple.copy(alpha = 0.15f)
                                        "RECLASSIFICATION_PENDING" -> AccentRose.copy(alpha = 0.15f)
                                        else -> AccentAmber.copy(alpha = 0.15f)
                                    }
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tracking.allocationStatus.replace("_", " "),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (tracking.allocationStatus) {
                                    "FULLY_ALLOCATED" -> AccentEmerald
                                    "RECLASSIFIED" -> AccentPurple
                                    "RECLASSIFICATION_PENDING" -> AccentRose
                                    else -> AccentAmber
                                }
                            )
                        }

                        // Action Buttons
                        Row(
                            modifier = Modifier.weight(1.2f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (userRole in setOf("ADMIN", "MANAGER")) {
                                IconButton(
                                    onClick = { onReclassifyClick(tracking) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = "Reclassify", tint = AccentPurple, modifier = Modifier.size(16.dp))
                                }
                            }
                            IconButton(
                                onClick = { onClassifyClick(tracking) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Classify", tint = AccentCyan, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { onDetailClick(tracking) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = "Details", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Divider(color = CardBorder, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun CostCentersView(
    costCenters: List<BusinessCostCenterResponse>,
    userRole: String,
    onActivate: (String) -> Unit,
    onDeactivate: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CardSurface, RoundedCornerShape(8.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val rootCenters = costCenters.filter { it.parentCostCenterId == null }
        items(rootCenters) { root ->
            val children = costCenters.filter { it.parentCostCenterId == root.id }
            CostCenterTreeItem(
                center = root,
                children = children,
                userRole = userRole,
                onActivate = onActivate,
                onDeactivate = onDeactivate
            )
        }
    }
}

@Composable
private fun CostCenterTreeItem(
    center: BusinessCostCenterResponse,
    children: List<BusinessCostCenterResponse>,
    userRole: String,
    onActivate: (String) -> Unit,
    onDeactivate: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DeepNavyBg.copy(alpha = 0.5f))
            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Business, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(center.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("[${center.code}]", color = AccentCyan, fontSize = 12.sp)
                    }
                    center.description?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (center.isActive) AccentEmerald.copy(alpha = 0.15f) else AccentRose.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (center.isActive) "ACTIVE" else "INACTIVE",
                        color = if (center.isActive) AccentEmerald else AccentRose,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (userRole in setOf("ADMIN", "MANAGER")) {
                    Switch(
                        checked = center.isActive,
                        onCheckedChange = { if (it) onActivate(center.id) else onDeactivate(center.id) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentEmerald,
                            checkedTrackColor = AccentEmerald.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = CardBorder
                        )
                    )
                }
            }
        }

        // Render Child Cost Centers
        if (children.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                children.forEach { child ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(CardSurface)
                            .border(1.dp, CardBorder.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.SubdirectoryArrowRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            Text(child.name, color = TextPrimary, fontSize = 13.sp)
                            Text("[${child.code}]", color = AccentPurple, fontSize = 11.sp)
                        }

                        if (userRole in setOf("ADMIN", "MANAGER")) {
                            Switch(
                                checked = child.isActive,
                                onCheckedChange = { if (it) onActivate(child.id) else onDeactivate(child.id) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AccentEmerald,
                                    checkedTrackColor = AccentEmerald.copy(alpha = 0.3f),
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = CardBorder
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CostCategoriesView(
    categories: List<BusinessCostCategoryResponse>,
    userRole: String,
    onActivate: (String) -> Unit,
    onDeactivate: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CardSurface, RoundedCornerShape(8.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val rootCats = categories.filter { it.parentCategoryId == null }
        items(rootCats) { root ->
            val children = categories.filter { it.parentCategoryId == root.id }
            CostCategoryTreeItem(
                category = root,
                children = children,
                userRole = userRole,
                onActivate = onActivate,
                onDeactivate = onDeactivate
            )
        }
    }
}

@Composable
private fun CostCategoryTreeItem(
    category: BusinessCostCategoryResponse,
    children: List<BusinessCostCategoryResponse>,
    userRole: String,
    onActivate: (String) -> Unit,
    onDeactivate: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DeepNavyBg.copy(alpha = 0.5f))
            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Category, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(category.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("[${category.code}]", color = AccentPurple, fontSize = 12.sp)
                        if (category.isSystemDefined) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(AccentAmber.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("SYSTEM", color = AccentAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    category.description?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (category.isActive) AccentEmerald.copy(alpha = 0.15f) else AccentRose.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (category.isActive) "ACTIVE" else "INACTIVE",
                        color = if (category.isActive) AccentEmerald else AccentRose,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (userRole in setOf("ADMIN", "MANAGER")) {
                    Switch(
                        checked = category.isActive,
                        onCheckedChange = { if (it) onActivate(category.id) else onDeactivate(category.id) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentEmerald,
                            checkedTrackColor = AccentEmerald.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = CardBorder
                        )
                    )
                }
            }
        }

        if (children.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                children.forEach { child ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(CardSurface)
                            .border(1.dp, CardBorder.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.SubdirectoryArrowRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            Text(child.name, color = TextPrimary, fontSize = 13.sp)
                            Text("[${child.code}]", color = AccentPurple, fontSize = 11.sp)
                        }

                        if (userRole in setOf("ADMIN", "MANAGER")) {
                            Switch(
                                checked = child.isActive,
                                onCheckedChange = { if (it) onActivate(child.id) else onDeactivate(child.id) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AccentEmerald,
                                    checkedTrackColor = AccentEmerald.copy(alpha = 0.3f),
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = CardBorder
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// MODALS & DIALOGS
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateCostCenterDialog(
    costCenters: List<BusinessCostCenterResponse>,
    onDismiss: () -> Unit,
    onConfirm: (CreateBusinessCostCenterRequest) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var parentId by remember { mutableStateOf<String?>(null) }
    var isParentMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Business Cost Center", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Cost Center Code (e.g. CC-PRINT)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Cost Center Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Parent Selection Dropdown
                Box {
                    OutlinedTextField(
                        value = costCenters.find { it.id == parentId }?.name ?: "None (Root Cost Center)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Parent Cost Center") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { isParentMenuExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = isParentMenuExpanded,
                        onDismissRequest = { isParentMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (Root Cost Center)") },
                            onClick = {
                                parentId = null
                                isParentMenuExpanded = false
                            }
                        )
                        costCenters.filter { it.isActive }.forEach { cc ->
                            DropdownMenuItem(
                                text = { Text("${cc.name} [${cc.code}]") },
                                onClick = {
                                    parentId = cc.id
                                    isParentMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isNotBlank() && name.isNotBlank()) {
                        onConfirm(
                            CreateBusinessCostCenterRequest(
                                code = code.trim(),
                                name = name.trim(),
                                description = description.ifBlank { null },
                                parentCostCenterId = parentId
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
            ) {
                Text("Create", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = CardSurface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateCostCategoryDialog(
    categories: List<BusinessCostCategoryResponse>,
    onDismiss: () -> Unit,
    onConfirm: (CreateBusinessCostCategoryRequest) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var parentId by remember { mutableStateOf<String?>(null) }
    var isParentMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Business Cost Category", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Category Code (e.g. CAT-PAPER)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Parent Selection Dropdown
                Box {
                    OutlinedTextField(
                        value = categories.find { it.id == parentId }?.name ?: "None (Root Category)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Parent Category") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { isParentMenuExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = isParentMenuExpanded,
                        onDismissRequest = { isParentMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (Root Category)") },
                            onClick = {
                                parentId = null
                                isParentMenuExpanded = false
                            }
                        )
                        categories.filter { it.isActive }.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.name} [${cat.code}]") },
                                onClick = {
                                    parentId = cat.id
                                    isParentMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isNotBlank() && name.isNotBlank()) {
                        onConfirm(
                            CreateBusinessCostCategoryRequest(
                                code = code.trim(),
                                name = name.trim(),
                                description = description.ifBlank { null },
                                parentCategoryId = parentId
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) {
                Text("Create", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = CardSurface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackOperationalCostDialog(
    costCenters: List<BusinessCostCenterResponse>,
    costCategories: List<BusinessCostCategoryResponse>,
    onDismiss: () -> Unit,
    onConfirm: (TrackOperationalCostRequest) -> Unit
) {
    var sourceType by remember { mutableStateOf("BUSINESS_EXPENSE") }
    var sourceId by remember { mutableStateOf("") }
    var selectedCenterId by remember { mutableStateOf(costCenters.firstOrNull()?.id ?: "") }
    var selectedCategoryId by remember { mutableStateOf(costCategories.firstOrNull()?.id ?: "") }
    var jobId by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Track Operational Cost Record", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Link canonical financial items to operational cost centers & jobs.", color = TextSecondary, fontSize = 12.sp)

                OutlinedTextField(
                    value = sourceId,
                    onValueChange = { sourceId = it },
                    label = { Text("Source ID (e.g. EXP-1001, PAY-2001)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = jobId,
                    onValueChange = { jobId = it },
                    label = { Text("Job ID (Optional, e.g. JOB-1025)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (BDT, optional if from expense/payable)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Operational Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (sourceId.isNotBlank() && selectedCenterId.isNotBlank() && selectedCategoryId.isNotBlank()) {
                        onConfirm(
                            TrackOperationalCostRequest(
                                sourceType = sourceType,
                                sourceId = sourceId.trim(),
                                costCenterId = selectedCenterId,
                                costCategoryId = selectedCategoryId,
                                jobId = jobId.ifBlank { null },
                                amount = amount.ifBlank { null },
                                notes = notes.ifBlank { null }
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
            ) {
                Text("Track Cost", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = CardSurface
    )
}

@Composable
private fun ClassifyCostDialog(
    tracking: BusinessCostTrackingResponse,
    costCenters: List<BusinessCostCenterResponse>,
    costCategories: List<BusinessCostCategoryResponse>,
    onDismiss: () -> Unit,
    onConfirm: (ClassifyCostRequest) -> Unit
) {
    var selectedCenterId by remember { mutableStateOf(tracking.costCenterId) }
    var selectedCategoryId by remember { mutableStateOf(tracking.costCategoryId) }
    var jobId by remember { mutableStateOf(tracking.jobId ?: "") }
    var notes by remember { mutableStateOf(tracking.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Classify Operational Cost", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tracked Record: ${tracking.id} (${tracking.amount} ${tracking.currency})", color = AccentCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = jobId,
                    onValueChange = { jobId = it },
                    label = { Text("Job ID") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Classification Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        ClassifyCostRequest(
                            costCenterId = selectedCenterId,
                            costCategoryId = selectedCategoryId,
                            jobId = jobId.ifBlank { null },
                            notes = notes.ifBlank { null }
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
            ) {
                Text("Save Classification", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = CardSurface
    )
}

@Composable
private fun ReclassifyCostDialog(
    tracking: BusinessCostTrackingResponse,
    costCenters: List<BusinessCostCenterResponse>,
    costCategories: List<BusinessCostCategoryResponse>,
    onDismiss: () -> Unit,
    onConfirm: (ReclassifyCostRequest) -> Unit
) {
    var selectedCenterId by remember { mutableStateOf(tracking.costCenterId) }
    var selectedCategoryId by remember { mutableStateOf(tracking.costCategoryId) }
    var jobId by remember { mutableStateOf(tracking.jobId ?: "") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reclassify Operational Cost", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentAmber.copy(alpha = 0.1f))
                        .border(1.dp, AccentAmber.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        "Accounting record remains immutable; this action changes operational classification/allocation through the canonical cost-management workflow.",
                        color = AccentAmber,
                        fontSize = 12.sp
                    )
                }

                Text("Record: ${tracking.id} | Amount: ${tracking.amount} ${tracking.currency}", color = TextPrimary, fontWeight = FontWeight.SemiBold)

                OutlinedTextField(
                    value = jobId,
                    onValueChange = { jobId = it },
                    label = { Text("New Target Job ID") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Mandatory Reclassification Reason (Min 3 chars)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.trim().length >= 3) {
                        onConfirm(
                            ReclassifyCostRequest(
                                newCostCenterId = selectedCenterId,
                                newCostCategoryId = selectedCategoryId,
                                newJobId = jobId.ifBlank { null },
                                reason = reason.trim()
                            )
                        )
                    }
                },
                enabled = reason.trim().length >= 3,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) {
                Text("Confirm Reclassification", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = CardSurface
    )
}

@Composable
private fun CostTrackingDetailDialog(
    tracking: BusinessCostTrackingResponse,
    costCenters: List<BusinessCostCenterResponse>,
    costCategories: List<BusinessCostCategoryResponse>,
    onDismiss: () -> Unit,
    onViewAudit: () -> Unit
) {
    val center = costCenters.find { it.id == tracking.costCenterId }
    val category = costCategories.find { it.id == tracking.costCategoryId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Operational Cost Tracking Detail", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("Tracking ID", tracking.id)
                DetailRow("Source Type", tracking.sourceType)
                DetailRow("Source ID", tracking.sourceId)
                DetailRow("Amount", "${tracking.amount} ${tracking.currency}")
                DetailRow("Cost Center", "${center?.name ?: tracking.costCenterId} [${center?.code ?: ""}]")
                DetailRow("Category", "${category?.name ?: tracking.costCategoryId} [${category?.code ?: ""}]")
                DetailRow("Job Reference", tracking.jobId ?: "Unallocated")
                DetailRow("Allocation Status", tracking.allocationStatus)
                DetailRow("Classification Status", tracking.classificationStatus)
                tracking.notes?.takeIf { it.isNotBlank() }?.let {
                    DetailRow("Notes", it)
                }
                DetailRow("Created By", tracking.createdBy)
            }
        },
        confirmButton = {
            Button(
                onClick = onViewAudit,
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
            ) {
                Text("View Audit Trail", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = TextSecondary) }
        },
        containerColor = CardSurface
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CostKpiCard(
    title: String,
    amount: String,
    subtitle: String,
    accentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(amount, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
        }
    }
}
