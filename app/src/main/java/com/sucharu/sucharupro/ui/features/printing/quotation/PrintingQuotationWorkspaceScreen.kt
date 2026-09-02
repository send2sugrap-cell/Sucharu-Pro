package com.sucharu.sucharupro.ui.features.printing.quotation

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.printingquote.*
import com.sucharu.sucharupro.data.api.model.commercialcommitment.*
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintingQuotationWorkspaceScreen(
    onNavigateBack: () -> Unit = {},
    onCreateQuote: (CreatePrintingQuoteRequestDto) -> Unit = {},
    onCalculateQuote: (CalculatePrintingQuoteRequestDto) -> Unit = {},
    onSubmitForReview: (String) -> Unit = {},
    onReviewQuote: (QuoteReviewRequestDto) -> Unit = {},
    onReconcileQuote: (String, String) -> Unit = { _, _ -> },
    onExportHandoff: (String) -> Unit = {},
    onCheckEligibility: (String) -> Unit = {},
    onConvertToOrder: (String, ConvertQuotationToOrderRequestDto) -> Unit = { _, _ -> },
    onNavigateToOrder: (String) -> Unit = {},
    quoteData: PrintingQuoteDto? = null,
    quoteVersionData: PrintingQuoteVersionDto? = null,
    costComponents: List<PrintingCostComponentDto> = emptyList(),
    quantityTiers: List<PrintingQuantityTierDto> = emptyList(),
    auditTrail: List<QuoteAuditEventDto> = emptyList(),
    handoffContract: Module17Step02PrintingQuotationHandoffContractDto? = null,
    conversionEligibility: ConversionEligibilityDto? = null,
    commercialCommitment: CommercialCommitmentDto? = null,
    conversionResult: ConversionResultDto? = null,
    commitmentEvents: List<CommercialCommitmentEventDto> = emptyList(),
    step03HandoffContract: CommercialCommitmentHandoffDto? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Quote & Config, 1: Cost Breakdown, 2: Pricing & Tiers, 3: Review & Governance, 4: AI Handoff

    // Form inputs
    var calculationIdInput by remember { mutableStateOf("CALC-DEMO-2026-001") }
    var jobTitleInput by remember { mutableStateOf("Premium Corporate Brochure (1,000 Pcs)") }
    var customerRefInput by remember { mutableStateOf("CUST-CORP-908") }
    var customerNoteInput by remember { mutableStateOf("High priority client. Ensure spot UV finishing on front cover.") }
    var internalNoteInput by remember { mutableStateOf("Standard 4C sheetfed offset with Heidelberg Speedmaster.") }

    // Pricing & Costing parameters
    var pricingMethod by remember { mutableStateOf("COST_PLUS") } // COST_PLUS, TARGET_MARGIN, MANUAL
    var markupPctStr by remember { mutableStateOf("25.0000") }
    var targetMarginPctStr by remember { mutableStateOf("20.0000") }
    var discountType by remember { mutableStateOf("NONE") } // NONE, FIXED_AMOUNT, PERCENTAGE
    var discountValStr by remember { mutableStateOf("0.0000") }
    var taxPctStr by remember { mutableStateOf("5.0000") }
    var overheadAllocationPctStr by remember { mutableStateOf("10.0000") }
    var wastageCosted by remember { mutableStateOf(true) }
    var tierBreaksInput by remember { mutableStateOf("500, 1000, 2500, 5000") }

    // Review dialog state
    var showReviewDialog by remember { mutableStateOf(false) }
    var reviewApprovalChoice by remember { mutableStateOf(true) }
    var reviewReasonInput by remember { mutableStateOf("") }

    var showConvertDialog by remember { mutableStateOf(false) }
    var customOrderNumberInput by remember { mutableStateOf("") }
    var convertPriorityInput by remember { mutableStateOf("NORMAL") }
    var convertPaymentTermsInput by remember { mutableStateOf("NET_30") }
    var convertNotesInput by remember { mutableStateOf("") }

    val currentQuoteId = quoteData?.quoteId ?: "QUO-DEMO-001"
    val currentVersionId = quoteVersionData?.versionId ?: "VER-DEMO-001"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.RequestQuote,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Printing Quotation Workspace",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Text(
                                "Commercial Costing & Price Intelligence Engine • Module 17 Step 02",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    quoteData?.let { q ->
                        QuoteStatusBadge(status = q.status)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        containerColor = Color(0xFF0B0F19)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs Bar
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFF38BDF8),
                edgePadding = 12.dp
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Quotation Config") },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Cost Breakdown") },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Pricing & Tiers") },
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Governance & Review") },
                    icon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("Commercial Conversion") },
                    icon = { Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    text = { Text("AI Handoff Contract") },
                    icon = { Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            // Error banner if any
            errorMessage?.let { msg ->
                Surface(
                    color = Color(0xFF7F1D1D),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFFCA5A5))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(msg, color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            // Loading bar
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF38BDF8),
                    trackColor = Color(0xFF1E293B)
                )
            }

            // Tab contents
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> QuoteConfigTab(
                        calculationId = calculationIdInput,
                        onCalculationIdChange = { calculationIdInput = it },
                        jobTitle = jobTitleInput,
                        onJobTitleChange = { jobTitleInput = it },
                        customerRef = customerRefInput,
                        onCustomerRefChange = { customerRefInput = it },
                        customerNote = customerNoteInput,
                        onCustomerNoteChange = { customerNoteInput = it },
                        internalNote = internalNoteInput,
                        onInternalNoteChange = { internalNoteInput = it },
                        pricingMethod = pricingMethod,
                        onPricingMethodChange = { pricingMethod = it },
                        markupPct = markupPctStr,
                        onMarkupPctChange = { markupPctStr = it },
                        targetMarginPct = targetMarginPctStr,
                        onTargetMarginPctChange = { targetMarginPctStr = it },
                        discountType = discountType,
                        onDiscountTypeChange = { discountType = it },
                        discountVal = discountValStr,
                        onDiscountValChange = { discountValStr = it },
                        taxPct = taxPctStr,
                        onTaxPctChange = { taxPctStr = it },
                        overheadAllocationPct = overheadAllocationPctStr,
                        onOverheadAllocationPctChange = { overheadAllocationPctStr = it },
                        wastageCosted = wastageCosted,
                        onWastageCostedChange = { wastageCosted = it },
                        tierBreaks = tierBreaksInput,
                        onTierBreaksChange = { tierBreaksInput = it },
                        onCreateQuote = {
                            onCreateQuote(
                                CreatePrintingQuoteRequestDto(
                                    calculationId = calculationIdInput,
                                    jobTitle = jobTitleInput,
                                    customerRef = customerRefInput.takeIf { it.isNotBlank() },
                                    customerNote = customerNoteInput.takeIf { it.isNotBlank() },
                                    internalNote = internalNoteInput.takeIf { it.isNotBlank() }
                                )
                            )
                        },
                        onCalculateQuote = {
                            val tiers = tierBreaksInput.split(",")
                                .mapNotNull { it.trim().toLongOrNull() }
                            onCalculateQuote(
                                CalculatePrintingQuoteRequestDto(
                                    quoteId = currentQuoteId,
                                    overheadAllocationPct = overheadAllocationPctStr,
                                    wastageCosted = wastageCosted,
                                    pricingMethod = pricingMethod,
                                    markupPercentage = markupPctStr,
                                    targetMarginPercentage = targetMarginPctStr,
                                    discountType = discountType,
                                    discountValue = discountValStr,
                                    taxPercentage = taxPctStr,
                                    quantityTierBreaks = tiers
                                )
                            )
                        },
                        quote = quoteData,
                        version = quoteVersionData
                    )
                    1 -> CostBreakdownTab(
                        costComponents = costComponents,
                        version = quoteVersionData
                    )
                    2 -> PricingAndTiersTab(
                        version = quoteVersionData,
                        quantityTiers = quantityTiers
                    )
                    3 -> GovernanceAndReviewTab(
                        quote = quoteData,
                        version = quoteVersionData,
                        auditTrail = auditTrail,
                        onSubmitForReview = { onSubmitForReview(currentQuoteId) },
                        onOpenReviewDialog = { showReviewDialog = true },
                        onReconcile = { onReconcileQuote(currentQuoteId, currentVersionId) }
                    )
                    4 -> CommercialConversionTab(
                        quote = quoteData,
                        version = quoteVersionData,
                        eligibility = conversionEligibility,
                        commitment = commercialCommitment,
                        conversionResult = conversionResult,
                        events = commitmentEvents,
                        onCheckEligibility = { onCheckEligibility(currentQuoteId) },
                        onOpenConvertDialog = { showConvertDialog = true },
                        onNavigateToOrder = onNavigateToOrder
                    )
                    5 -> AiHandoffContractTab(
                        quote = quoteData,
                        version = quoteVersionData,
                        contract = handoffContract,
                        step03Contract = step03HandoffContract,
                        onExport = { onExportHandoff(currentQuoteId) }
                    )
                }
            }
        }
    }

    // Convert to Order Confirmation Dialog
    if (showConvertDialog) {
        AlertDialog(
            onDismissRequest = { showConvertDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, tint = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm Commercial Order Conversion", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "You are about to convert this approved quotation into a binding Commercial Customer Order in Sucharu Pro ERP.",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ContractField("Quotation ID", currentQuoteId)
                            ContractField("Ordered Quantity", " Pcs")
                            ContractField("Approved Total", " ")
                            ContractField("Customer", quoteData?.customerRef ?: "Default Customer")
                        }
                    }
                    OutlinedTextField(
                        value = convertPaymentTermsInput,
                        onValueChange = { convertPaymentTermsInput = it },
                        label = { Text("Payment Terms") },
                        colors = standardTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = convertNotesInput,
                        onValueChange = { convertNotesInput = it },
                        label = { Text("Conversion / Order Notes (Optional)") },
                        colors = standardTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConvertDialog = false
                        onConvertToOrder(
                            currentQuoteId,
                            ConvertQuotationToOrderRequestDto(
                                targetVersionNumber = quoteVersionData?.versionNumber ?: quoteData?.currentVersion,
                                requestedQuantity = quoteVersionData?.quantityBreakdown?.orderedQuantity ?: quoteData?.orderedQuantity,
                                customOrderNumber = customOrderNumberInput.takeIf { it.isNotBlank() },
                                priority = convertPriorityInput,
                                paymentTerms = convertPaymentTermsInput.takeIf { it.isNotBlank() },
                                notes = convertNotesInput.takeIf { it.isNotBlank() },
                                idempotencyKey = "CONV--"
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirm & Create Order", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConvertDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // Review Dialog
    if (showReviewDialog) {
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = { Text("Quotation Review & Approval", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Submit official management approval or rejection decision.",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = reviewApprovalChoice,
                                onClick = { reviewApprovalChoice = true },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF10B981))
                            )
                            Text("Approve Quote", color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = !reviewApprovalChoice,
                                onClick = { reviewApprovalChoice = false },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFEF4444))
                            )
                            Text("Reject Quote", color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    OutlinedTextField(
                        value = reviewReasonInput,
                        onValueChange = { reviewReasonInput = it },
                        label = { Text("Reason / Review Comments") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReviewDialog = false
                        onReviewQuote(
                            QuoteReviewRequestDto(
                                quoteId = currentQuoteId,
                                approved = reviewApprovalChoice,
                                reason = reviewReasonInput.takeIf { it.isNotBlank() }
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (reviewApprovalChoice) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                ) {
                    Text(if (reviewApprovalChoice) "Confirm Approval" else "Confirm Rejection")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReviewDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

// ============================================================
// TAB 0: QUOTE CONFIGURATION & ACTIONS
// ============================================================

@Composable
private fun QuoteConfigTab(
    calculationId: String,
    onCalculationIdChange: (String) -> Unit,
    jobTitle: String,
    onJobTitleChange: (String) -> Unit,
    customerRef: String,
    onCustomerRefChange: (String) -> Unit,
    customerNote: String,
    onCustomerNoteChange: (String) -> Unit,
    internalNote: String,
    onInternalNoteChange: (String) -> Unit,
    pricingMethod: String,
    onPricingMethodChange: (String) -> Unit,
    markupPct: String,
    onMarkupPctChange: (String) -> Unit,
    targetMarginPct: String,
    onTargetMarginPctChange: (String) -> Unit,
    discountType: String,
    onDiscountTypeChange: (String) -> Unit,
    discountVal: String,
    onDiscountValChange: (String) -> Unit,
    taxPct: String,
    onTaxPctChange: (String) -> Unit,
    overheadAllocationPct: String,
    onOverheadAllocationPctChange: (String) -> Unit,
    wastageCosted: Boolean,
    onWastageCostedChange: (Boolean) -> Unit,
    tierBreaks: String,
    onTierBreaksChange: (String) -> Unit,
    onCreateQuote: () -> Unit,
    onCalculateQuote: () -> Unit,
    quote: PrintingQuoteDto?,
    version: PrintingQuoteVersionDto?
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            // Header summary if exists
            quote?.let { q ->
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
                            Column {
                                Text("Quote #${q.quoteNumber}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF38BDF8))
                                Text(q.jobTitle, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            QuoteStatusBadge(q.status)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            InfoSnippet("Version", "v${q.currentVersion}")
                            InfoSnippet("Currency", q.currency)
                            InfoSnippet("Ordered Qty", "${q.orderedQuantity} pcs")
                            q.customerRef?.let { InfoSnippet("Customer Ref", it) }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("1. Provenance & Job Specification", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF38BDF8))

                    OutlinedTextField(
                        value = calculationId,
                        onValueChange = onCalculationIdChange,
                        label = { Text("Step 01 Calculation ID") },
                        leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color(0xFF38BDF8)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = standardTextFieldColors()
                    )

                    OutlinedTextField(
                        value = jobTitle,
                        onValueChange = onJobTitleChange,
                        label = { Text("Job Title") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = standardTextFieldColors()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = customerRef,
                            onValueChange = onCustomerRefChange,
                            label = { Text("Customer ID / Ref") },
                            modifier = Modifier.weight(1f),
                            colors = standardTextFieldColors()
                        )
                    }

                    OutlinedTextField(
                        value = customerNote,
                        onValueChange = onCustomerNoteChange,
                        label = { Text("Customer Visible Note") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = standardTextFieldColors()
                    )

                    OutlinedTextField(
                        value = internalNote,
                        onValueChange = onInternalNoteChange,
                        label = { Text("Internal Production Note") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = standardTextFieldColors()
                    )

                    Button(
                        onClick = onCreateQuote,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Quotation Header")
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("2. Costing & Pricing Engine Settings", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF38BDF8))

                    // Pricing method selector
                    Text("Pricing Strategy", fontSize = 13.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("COST_PLUS" to "Cost-Plus", "TARGET_MARGIN" to "Target Margin", "MANUAL" to "Manual").forEach { (key, label) ->
                            FilterChip(
                                selected = pricingMethod == key,
                                onClick = { onPricingMethodChange(key) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF38BDF8),
                                    selectedLabelColor = Color(0xFF0F172A)
                                )
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = markupPct,
                            onValueChange = onMarkupPctChange,
                            label = { Text("Markup %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            colors = standardTextFieldColors()
                        )
                        OutlinedTextField(
                            value = targetMarginPct,
                            onValueChange = onTargetMarginPctChange,
                            label = { Text("Target Margin %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            colors = standardTextFieldColors()
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = overheadAllocationPct,
                            onValueChange = onOverheadAllocationPctChange,
                            label = { Text("Overhead %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            colors = standardTextFieldColors()
                        )
                        OutlinedTextField(
                            value = taxPct,
                            onValueChange = onTaxPctChange,
                            label = { Text("Tax / VAT %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            colors = standardTextFieldColors()
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .clickable { onWastageCostedChange(!wastageCosted) }
                                .padding(8.dp)
                        ) {
                            Checkbox(
                                checked = wastageCosted,
                                onCheckedChange = onWastageCostedChange,
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF38BDF8))
                            )
                            Text("Cost Wastage Sheets", color = Color.White, fontSize = 13.sp)
                        }

                        OutlinedTextField(
                            value = discountVal,
                            onValueChange = onDiscountValChange,
                            label = { Text("Discount Value ($discountType)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            colors = standardTextFieldColors()
                        )
                    }

                    OutlinedTextField(
                        value = tierBreaks,
                        onValueChange = onTierBreaksChange,
                        label = { Text("Quantity Tier Breaks (comma separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = standardTextFieldColors()
                    )

                    Button(
                        onClick = onCalculateQuote,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Calculate Commercial Costing & Pricing")
                    }
                }
            }
        }
    }
}

// ============================================================
// TAB 1: COST BREAKDOWN (CANONICAL COMPONENTS)
// ============================================================

@Composable
private fun CostBreakdownTab(
    costComponents: List<PrintingCostComponentDto>,
    version: PrintingQuoteVersionDto?
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            version?.let { v ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Production Cost", fontSize = 13.sp, color = Color(0xFF94A3B8))
                            Text("৳ ${v.totalCost}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Unit Production Cost", fontSize = 13.sp, color = Color(0xFF94A3B8))
                            Text("৳ ${v.unitCost} / unit", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Cost Component Decomposition (Scope Matrix)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF38BDF8)
            )
        }

        if (costComponents.isEmpty()) {
            item {
                EmptyPlaceholderCard("No cost breakdown available. Run quote calculation first.")
            }
        } else {
            items(costComponents) { comp ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ComponentTypeBadge(comp.componentType)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(comp.description, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Qty: ${comp.quantity} ${comp.unit} @ ${comp.unitRate?.let { "৳ $it" } ?: "N/A"}",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                "Formula: ${comp.formulaReference}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("৳ ${comp.amount}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF10B981))
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// TAB 2: PRICING & QUANTITY TIERS
// ============================================================

@Composable
private fun PricingAndTiersTab(
    version: PrintingQuoteVersionDto?,
    quantityTiers: List<PrintingQuantityTierDto>
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            version?.pricing?.let { p ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Commercial Pricing Intelligence", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF38BDF8))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MetricBox("Base Selling Price", "৳ ${p.baseSellingPrice}", Color.White)
                            MetricBox("Discount", "৳ ${p.discountAmount} (${p.discountType})", Color(0xFFF59E0B))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MetricBox("Tax / VAT", "৳ ${p.taxAmount} (${p.taxPercentage}%)", Color(0xFF94A3B8))
                            MetricBox("Final Quote Total", "৳ ${p.finalQuoteTotal}", Color(0xFF10B981), isHighlight = true)
                        }

                        Divider(color = Color(0xFF334155))

                        Text("Margin & Break-Even Analysis", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF38BDF8))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MetricBox("Gross Profit", "৳ ${p.grossProfit}", Color(0xFF10B981))
                            MetricBox("Gross Margin %", "${p.grossMarginPercentage}%", Color(0xFF10B981))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MetricBox("Markup Amount", "৳ ${p.markupAmount} (${p.markupPercentage}%)", Color(0xFF38BDF8))
                            MetricBox("Contribution Margin", "${p.contributionMarginPercentage}%", Color(0xFF38BDF8))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MetricBox("Break-Even Price", "৳ ${p.breakEvenPrice}", Color(0xFFEF4444))
                            MetricBox("Break-Even Qty", "${p.breakEvenQuantity} units", Color(0xFFEF4444))
                        }
                    }
                }
            } ?: run {
                EmptyPlaceholderCard("No pricing calculated yet. Run quotation calculation.")
            }
        }

        item {
            Text("Volume Quantity-Tier Pricing Matrix", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF38BDF8))
        }

        if (quantityTiers.isEmpty()) {
            item {
                EmptyPlaceholderCard("No quantity tiers computed. Specify tier breaks in configuration.")
            }
        } else {
            items(quantityTiers) { tier ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (tier.isBaseTier) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${tier.tierQuantity} Units", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                if (tier.isBaseTier) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text("BASE ORDER", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text("Total: ৳ ${tier.finalTotal}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF10B981))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            InfoSnippet("Unit Cost", "৳ ${tier.unitCost}")
                            InfoSnippet("Selling / Unit", "৳ ${tier.sellingPricePerUnit}")
                            InfoSnippet("Margin", "${tier.grossMarginPercentage}%")
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// TAB 3: GOVERNANCE, REVIEW & AUDIT
// ============================================================

@Composable
private fun GovernanceAndReviewTab(
    quote: PrintingQuoteDto?,
    version: PrintingQuoteVersionDto?,
    auditTrail: List<QuoteAuditEventDto>,
    onSubmitForReview: () -> Unit,
    onOpenReviewDialog: () -> Unit,
    onReconcile: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Quotation Governance Actions", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF38BDF8))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onSubmitForReview,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Submit For Review")
                        }

                        Button(
                            onClick = onOpenReviewDialog,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Gavel, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Review / Approve")
                        }
                    }

                    OutlinedButton(
                        onClick = onReconcile,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Perform Financial Integrity & Mathematical Reconciliation")
                    }
                }
            }
        }

        item {
            Text("Lifecycle Audit Trail", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF38BDF8))
        }

        if (auditTrail.isEmpty()) {
            item {
                EmptyPlaceholderCard("No audit events recorded for this quotation yet.")
            }
        } else {
            items(auditTrail) { audit ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(audit.eventType, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF38BDF8))
                            Text("By: ${audit.actor}", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(audit.description, fontSize = 13.sp, color = Color.White)
                        if (audit.beforeStatus != null || audit.afterStatus != null) {
                            Text(
                                "Status: ${audit.beforeStatus ?: "N/A"} → ${audit.afterStatus ?: "N/A"}",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// TAB 4: AI HANDOFF CONTRACT
// ============================================================

@Composable
private fun AiHandoffContractTab(
    quote: PrintingQuoteDto?,
    version: PrintingQuoteVersionDto?,
    contract: Module17Step02PrintingQuotationHandoffContractDto?,
    step03Contract: CommercialCommitmentHandoffDto? = null,
    onExport: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("AI Agent Handoff Contract", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF38BDF8))
                            Text("Deterministic, read-only, cryptographically fingerprinted downstream payload", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        }
                        Button(
                            onClick = onExport,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        contract?.let { c ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("CONTRACT SPECIFICATION", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF10B981))
                        ContractField("Contract Version", c.contractVersion)
                        ContractField("Quote Number", c.quoteNumber)
                        ContractField("Job Title", c.jobTitle)
                        ContractField("Status", c.status)
                        ContractField("Ordered Quantity", "${c.orderedQuantity} pcs")
                        ContractField("Sellable Quantity", "${c.sellableQuantity} pcs")
                        ContractField("Wastage Quantity", "${c.wastageQuantity} pcs (${c.wastagePercentage}%)")
                        ContractField("Total Cost", "৳ ${c.totalCost}")
                        ContractField("Unit Cost", "৳ ${c.unitCost}")
                        ContractField("Final Quote Total", "৳ ${c.finalQuoteTotal}")
                        ContractField("Gross Margin", "${c.grossMarginPercentage}%")
                        ContractField("Break-Even Price", "৳ ${c.breakEvenPrice}")
                        ContractField("Break-Even Quantity", "${c.breakEvenQuantity} units")
                        ContractField("Integrity Hash", c.integrityHash)
                        ContractField("Costing Engine Ver.", c.costingEngineVersion)
                        ContractField("Pricing Engine Ver.", c.pricingEngineVersion)
                        ContractField("Reconciliation", c.reconciliationStatus)
                    }
                }
            }
        } ?: run {
            item {
                EmptyPlaceholderCard("Click Export to generate and inspect the downstream AI agent contract.")
            }
        }
    }
}

// ============================================================
// REUSABLE HELPER COMPOSABLES
// ============================================================

@Composable
private fun QuoteStatusBadge(status: String) {
    val (bg, fg) = when (status) {
        "DRAFT" -> Color(0xFF64748B) to Color.White
        "CALCULATED" -> Color(0xFF2563EB) to Color.White
        "REVIEW" -> Color(0xFFF59E0B) to Color.Black
        "APPROVED" -> Color(0xFF10B981) to Color.Black
        "REJECTED" -> Color(0xFFEF4444) to Color.White
        "EXPIRED" -> Color(0xFF475569) to Color.White
        else -> Color(0xFF64748B) to Color.White
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

@Composable
private fun ComponentTypeBadge(type: String) {
    Surface(
        color = Color(0xFF334155),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            type,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8)
        )
    }
}

@Composable
private fun InfoSnippet(label: String, value: String) {
    Column {
        Text(label, fontSize = 11.sp, color = Color(0xFF94A3B8))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
private fun MetricBox(label: String, value: String, valueColor: Color, isHighlight: Boolean = false) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isHighlight) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF0F172A))
            .padding(10.dp)
    ) {
        Text(label, fontSize = 11.sp, color = Color(0xFF94A3B8))
        Text(value, fontSize = if (isHighlight) 18.sp else 14.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
private fun ContractField(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = Color(0xFF94A3B8))
        Text(value, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
private fun EmptyPlaceholderCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(message, color = Color(0xFF94A3B8), fontSize = 13.sp)
        }
    }
}

@Composable
private fun standardTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF38BDF8),
    unfocusedBorderColor = Color(0xFF334155),
    focusedLabelColor = Color(0xFF38BDF8),
    unfocusedLabelColor = Color(0xFF94A3B8),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)

// =========================================================================
// MODULE 17 STEP 03: COMMERCIAL CONVERSION TAB
// =========================================================================

@Composable
private fun CommercialConversionTab(
    quote: PrintingQuoteDto?,
    version: PrintingQuoteVersionDto?,
    eligibility: ConversionEligibilityDto?,
    commitment: CommercialCommitmentDto?,
    conversionResult: ConversionResultDto?,
    events: List<CommercialCommitmentEventDto>,
    onCheckEligibility: () -> Unit,
    onOpenConvertDialog: () -> Unit,
    onNavigateToOrder: (String) -> Unit
) {
    val isApproved = quote?.status == "APPROVED"
    val isAlreadyConverted = commitment?.status == "CONVERTED" || conversionResult?.isSuccess == true || eligibility?.existingOrderId != null
    val targetOrderId = conversionResult?.orderId ?: commitment?.orderId ?: eligibility?.existingOrderId
    val targetOrderNumber = conversionResult?.orderNumber ?: commitment?.orderNumber

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Conversion & Commitment Authority", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            Text("Module 17 Step 03 â€¢ Approved Quotation to Canonical Order Bridge", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        }
                        QuoteStatusBadge(status = quote?.status ?: "UNKNOWN")
                    }

                    HorizontalDivider(color = Color(0xFF334155))

                    // Conversion Status Card
                    if (isAlreadyConverted && targetOrderId != null) {
                        Surface(
                            color = Color(0xFF065F46).copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Successfully Converted to Order", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                        Text("Order Number: #", color = Color(0xFF34D399), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                }
                                Button(
                                    onClick = { onNavigateToOrder(targetOrderId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Text("View Order in ERP", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // Eligibility check & Convert CTA
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onCheckEligibility,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                            ) {
                                Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check Conversion Eligibility")
                            }

                            Button(
                                onClick = onOpenConvertDialog,
                                enabled = isApproved && !isAlreadyConverted,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981),
                                    disabledContainerColor = Color(0xFF334155)
                                )
                            ) {
                                Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Convert to Order", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Eligibility feedback banner
                    eligibility?.let { el ->
                        Surface(
                            color = if (el.isEligible) Color(0xFF065F46).copy(alpha = 0.2f) else Color(0xFF7F1D1D).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (el.isEligible) Color(0xFF10B981) else Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    if (el.isEligible) "Ready for Commercial Order Conversion" else "Conversion Blocked by Business Rules",
                                    fontWeight = FontWeight.Bold,
                                    color = if (el.isEligible) Color(0xFF34D399) else Color(0xFFF87171),
                                    fontSize = 13.sp
                                )
                                if (el.reasons.isNotEmpty()) {
                                    el.reasons.forEach { r ->
                                        Text("â€¢ ", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Commercial Commitment Financial Snapshot
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Commercial Commitment Details", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    ContractField("Quotation ID", quote?.quoteId ?: "N/A")
                    ContractField("Committed Quantity", " Units")
                    ContractField("Approved Subtotal", " ")
                    ContractField("Approved Discount", " ")
                    ContractField("Approved Tax (VAT)", " ")
                    ContractField("Approved Grand Total", " ")
                    ContractField("Commitment Hash", commitment?.integrityHash ?: "PENDING_CONVERSION")
                }
            }
        }

        // Conversion Timeline & Audit Events
        if (events.isNotEmpty()) {
            item {
                Text("Conversion Audit Trail", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            }
            items(events) { evt ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(evt.eventType, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF38BDF8))
                            Text(evt.detailsJson ?: "", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                        Text(evt.actor, fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }
            }
        }
    }
}