package com.sucharu.sucharupro.ui.features.ledger

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.*
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
fun BusinessLedgerScreen(
    postings: List<BusinessLedgerPostingDto> = emptyList(),
    allocations: List<BusinessCostAllocationDto> = emptyList(),
    balanceSummary: BusinessLedgerBalanceSummaryDto? = null,
    userRole: String = "ADMIN",
    onReversePosting: (String, ReversePostingRequest) -> Unit = { _, _ -> },
    onAllocateCost: (AllocateBusinessCostRequest) -> Unit = {},
    onReverseAllocation: (String, ReverseBusinessCostAllocationRequest) -> Unit = { _, _ -> },
    onViewAudit: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedTab by remember { mutableStateOf(0) } // 0: Ledger Postings, 1: Cost Allocations, 2: Balance & Insights
    var selectedPostingForDetail by remember { mutableStateOf<BusinessLedgerPostingDto?>(null) }
    var selectedAllocationForDetail by remember { mutableStateOf<BusinessCostAllocationDto?>(null) }

    var showAllocateModal by remember { mutableStateOf(false) }
    var showReversePostingModal by remember { mutableStateOf<BusinessLedgerPostingDto?>(null) }
    var showReverseAllocationModal by remember { mutableStateOf<BusinessCostAllocationDto?>(null) }
    var reversalReasonInput by remember { mutableStateOf("") }

    val filteredPostings = remember(postings, searchQuery, selectedFilter) {
        postings.filter { p ->
            val matchesFilter = when (selectedFilter) {
                "ALL" -> true
                "REVERSED" -> p.isReversed
                "ACTIVE" -> !p.isReversed
                else -> p.postingType.equals(selectedFilter, ignoreCase = true) || p.sourceType.equals(selectedFilter, ignoreCase = true)
            }
            val matchesSearch = searchQuery.isBlank() ||
                    p.postingNumber.contains(searchQuery, ignoreCase = true) ||
                    p.sourceId.contains(searchQuery, ignoreCase = true) ||
                    p.description.contains(searchQuery, ignoreCase = true) ||
                    (p.jobId?.contains(searchQuery, ignoreCase = true) == true) ||
                    (p.vendorId?.contains(searchQuery, ignoreCase = true) == true)
            matchesFilter && matchesSearch
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
                        text = "Business Financial Ledger",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Normalized Multi-Source Ledger & Job Cost Allocations (Module 15 Step 03)",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showAllocateModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Allocate Cost", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Top KPI Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LedgerKpiCard(
                    title = "Total Debits",
                    amount = "${balanceSummary?.totalDebit ?: "0.0000"} BDT",
                    subtitle = "Expenses & Liabilities",
                    accentColor = AccentAmber,
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
                LedgerKpiCard(
                    title = "Total Credits",
                    amount = "${balanceSummary?.totalCredit ?: "0.0000"} BDT",
                    subtitle = "Payments & Outflows",
                    accentColor = AccentEmerald,
                    icon = Icons.Default.Payment,
                    modifier = Modifier.weight(1f)
                )
                LedgerKpiCard(
                    title = "Net Movement",
                    amount = "${balanceSummary?.netMovement ?: "0.0000"} BDT",
                    subtitle = "Debits - Credits",
                    accentColor = AccentCyan,
                    icon = Icons.Default.AccountBalance,
                    modifier = Modifier.weight(1f)
                )
                LedgerKpiCard(
                    title = "Closing Balance",
                    amount = "${balanceSummary?.closingBalance ?: "0.0000"} BDT",
                    subtitle = "Ledger Position",
                    accentColor = AccentPurple,
                    icon = Icons.Default.AccountBalanceWallet,
                    modifier = Modifier.weight(1f)
                )
            }

            // Tab Navigation
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CardSurface,
                contentColor = AccentCyan,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .padding(bottom = 12.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("General Postings (${postings.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Job Cost Allocations (${allocations.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Ledger Balances & Insights", fontWeight = FontWeight.SemiBold) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // Search & Filters for Postings
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by posting #, source ID, description, Job or Vendor...", color = TextSecondary) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AccentCyan) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = CardSurface,
                                unfocusedContainerColor = CardSurface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }

                    // Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ALL", "ACTIVE", "EXPENSE_RECOGNITION", "VENDOR_LIABILITY_RECOGNITION", "VENDOR_PAYMENT", "REVERSED").forEach { chip ->
                            FilterChip(
                                selected = selectedFilter == chip,
                                onClick = { selectedFilter = chip },
                                label = { Text(chip, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = AccentCyan,
                                    containerColor = CardSurface,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedFilter == chip,
                                    borderColor = if (selectedFilter == chip) AccentCyan else CardBorder
                                )
                            )
                        }
                    }

                    // Postings List
                    if (filteredPostings.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CardSurface)
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No ledger postings match current filter.", color = TextSecondary)
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredPostings, key = { it.id }) { p ->
                                LedgerPostingCard(
                                    posting = p,
                                    onViewDetail = { selectedPostingForDetail = p },
                                    onReverse = { showReversePostingModal = p },
                                    canReverse = userRole in setOf("ADMIN", "MANAGER") && !p.isReversed && p.postingType != "REVERSAL"
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Cost Allocations View
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(allocations, key = { it.id }) { a ->
                            CostAllocationCard(
                                allocation = a,
                                onViewDetail = { selectedAllocationForDetail = a },
                                onReverse = { showReverseAllocationModal = a },
                                canReverse = userRole in setOf("ADMIN", "MANAGER") && !a.isReversed
                            )
                        }
                    }
                }
                2 -> {
                    // Balances & Insights
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardSurface)
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Financial Postings Summary Formula", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                "Opening Balance (0.0000) + Total Debits Recognized (${balanceSummary?.totalDebit ?: "0.0000"}) - Total Credits Settled (${balanceSummary?.totalCredit ?: "0.0000"}) = Closing Balance (${balanceSummary?.closingBalance ?: "0.0000"} BDT)",
                                color = AccentCyan,
                                fontSize = 14.sp
                            )
                            Divider(color = CardBorder)
                            Text("Accounting Invariant Guarantees", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("• Customer financial ledger remains completely isolated and untouched.", color = TextSecondary, fontSize = 13.sp)
                            Text("• All financial ledger postings are strictly immutable with cryptographically verified checksums.", color = TextSecondary, fontSize = 13.sp)
                            Text("• Compensating reversals preserve complete history without mutating historical entries.", color = TextSecondary, fontSize = 13.sp)
                            Text("• Cost allocations to commercial printing jobs are strictly analytical and bounded by source total.", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Posting Detail Modal
        selectedPostingForDetail?.let { p ->
            AlertDialog(
                onDismissRequest = { selectedPostingForDetail = null },
                containerColor = CardSurface,
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(p.postingNumber, fontWeight = FontWeight.Bold, color = TextPrimary)
                        if (p.isReversed) {
                            Surface(
                                color = AccentRose.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("REVERSED", color = AccentRose, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Type: ${p.postingType}", color = AccentCyan, fontWeight = FontWeight.SemiBold)
                        Text("Source: ${p.sourceType} (${p.sourceId})", color = TextPrimary)
                        Text("Account: ${p.accountCategory}", color = TextSecondary)
                        Text("Debit: ${p.debitAmount} ${p.currency}", color = AccentAmber)
                        Text("Credit: ${p.creditAmount} ${p.currency}", color = AccentEmerald)
                        p.jobId?.let { Text("Job ID: $it", color = AccentPurple) }
                        p.vendorId?.let { Text("Vendor ID: $it", color = TextSecondary) }
                        Text("Description: ${p.description}", color = TextSecondary)
                        Text("Checksum: ${p.checksum ?: "N/A"}", fontSize = 10.sp, color = TextSecondary)
                        if (p.isReversed) {
                            Divider(color = CardBorder, modifier = Modifier.padding(vertical = 4.dp))
                            Text("Reversal Reason: ${p.reversalReason}", color = AccentRose)
                            Text("Reversed By: ${p.reversedBy}", color = TextSecondary)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { selectedPostingForDetail = null },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                    ) {
                        Text("Close", color = Color.Black)
                    }
                }
            )
        }

        // Reversal Confirmation Modal
        showReversePostingModal?.let { p ->
            AlertDialog(
                onDismissRequest = {
                    showReversePostingModal = null
                    reversalReasonInput = ""
                },
                containerColor = CardSurface,
                title = { Text("Confirm Reversal of ${p.postingNumber}", color = AccentRose, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "This action will create an immutable compensating reversal entry in the business ledger. A mandatory reason is required.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = reversalReasonInput,
                            onValueChange = { reversalReasonInput = it },
                            label = { Text("Reversal Reason (Mandatory)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentRose,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (reversalReasonInput.isNotBlank()) {
                                onReversePosting(p.id, ReversePostingRequest(reason = reversalReasonInput))
                                showReversePostingModal = null
                                reversalReasonInput = ""
                            }
                        },
                        enabled = reversalReasonInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRose)
                    ) {
                        Text("Execute Reversal", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showReversePostingModal = null
                        reversalReasonInput = ""
                    }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // Cost Allocation Modal
        if (showAllocateModal) {
            var allocSourceType by remember { mutableStateOf("BUSINESS_EXPENSE") }
            var allocSourceId by remember { mutableStateOf("") }
            var allocJobId by remember { mutableStateOf("") }
            var allocAmount by remember { mutableStateOf("") }
            var allocCategory by remember { mutableStateOf("PRODUCTION_COST") }
            var allocReason by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAllocateModal = false },
                containerColor = CardSurface,
                title = { Text("Allocate Cost to Job", color = AccentCyan, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = allocSourceId,
                            onValueChange = { allocSourceId = it },
                            label = { Text("Source ID (Expense or Payable ID)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = allocJobId,
                            onValueChange = { allocJobId = it },
                            label = { Text("Job ID (e.g. JOB-1025)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = allocAmount,
                            onValueChange = { allocAmount = it },
                            label = { Text("Amount to Allocate") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = allocReason,
                            onValueChange = { allocReason = it },
                            label = { Text("Reason / Job Stage Notes") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (allocSourceId.isNotBlank() && allocJobId.isNotBlank() && allocAmount.isNotBlank()) {
                                onAllocateCost(
                                    AllocateBusinessCostRequest(
                                        sourceType = allocSourceType,
                                        sourceId = allocSourceId,
                                        jobId = allocJobId,
                                        allocatedAmount = allocAmount,
                                        costCategory = allocCategory,
                                        reason = allocReason.ifBlank { null }
                                    )
                                )
                                showAllocateModal = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                    ) {
                        Text("Save Allocation", color = Color.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAllocateModal = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun LedgerKpiCard(
    title: String,
    amount: String,
    subtitle: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 12.sp, color = TextSecondary)
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = amount, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accentColor)
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun LedgerPostingCard(
    posting: BusinessLedgerPostingDto,
    onViewDetail: () -> Unit,
    onReverse: () -> Unit,
    canReverse: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, if (posting.isReversed) AccentRose.copy(alpha = 0.4f) else CardBorder, RoundedCornerShape(8.dp))
            .clickable { onViewDetail() },
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(posting.postingNumber, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                    Surface(
                        color = when (posting.postingType) {
                            "EXPENSE_RECOGNITION" -> AccentAmber.copy(alpha = 0.2f)
                            "VENDOR_LIABILITY_RECOGNITION" -> AccentPurple.copy(alpha = 0.2f)
                            "VENDOR_PAYMENT" -> AccentEmerald.copy(alpha = 0.2f)
                            "REVERSAL" -> AccentRose.copy(alpha = 0.2f)
                            else -> AccentCyan.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            posting.postingType,
                            color = when (posting.postingType) {
                                "EXPENSE_RECOGNITION" -> AccentAmber
                                "VENDOR_LIABILITY_RECOGNITION" -> AccentPurple
                                "VENDOR_PAYMENT" -> AccentEmerald
                                "REVERSAL" -> AccentRose
                                else -> AccentCyan
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (posting.isReversed) {
                        Surface(color = AccentRose.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text("REVERSED", color = AccentRose, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${posting.sourceType} • ${posting.sourceId} • ${posting.accountCategory}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = posting.description,
                    fontSize = 12.sp,
                    color = TextPrimary.copy(alpha = 0.9f),
                    maxLines = 1
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                if (posting.debitAmount != "0.0000") {
                    Text("DR: ${posting.debitAmount} ${posting.currency}", fontWeight = FontWeight.Bold, color = AccentAmber, fontSize = 13.sp)
                }
                if (posting.creditAmount != "0.0000") {
                    Text("CR: ${posting.creditAmount} ${posting.currency}", fontWeight = FontWeight.Bold, color = AccentEmerald, fontSize = 13.sp)
                }

                if (canReverse) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = onReverse,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRose),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("Reverse", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CostAllocationCard(
    allocation: BusinessCostAllocationDto,
    onViewDetail: () -> Unit,
    onReverse: () -> Unit,
    canReverse: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, if (allocation.isReversed) AccentRose.copy(alpha = 0.4f) else CardBorder, RoundedCornerShape(8.dp))
            .clickable { onViewDetail() },
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(allocation.allocationNumber, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                    Surface(color = AccentPurple.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                        Text("JOB: ${allocation.jobId}", color = AccentPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text("Source: ${allocation.sourceType} (${allocation.sourceId}) • Category: ${allocation.costCategory}", fontSize = 12.sp, color = TextSecondary)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("${allocation.allocatedAmount} ${allocation.currency}", fontWeight = FontWeight.Bold, color = AccentCyan, fontSize = 14.sp)
                if (canReverse) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = onReverse,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRose),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("Reverse", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
