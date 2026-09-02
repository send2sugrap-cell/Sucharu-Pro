package com.sucharu.sucharupro.ui.features.production.jobcosting

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.jobcosting.*
import java.math.BigDecimal

// Theme Palette (Deep Navy SaaS Analytics)
private val DeepNavyBg = Color(0xFF0D1117)
private val CardBg = Color(0xFF161B22)
private val CardBgElevated = Color(0xFF21262D)
private val BorderColor = Color(0xFF30363D)
private val AccentCyan = Color(0xFF58A6FF)
private val AccentGreen = Color(0xFF3FB950)
private val AccentOrange = Color(0xFFD29922)
private val AccentRed = Color(0xFFF85149)
private val AccentPurple = Color(0xFFBC8CFF)
private val TextPrimary = Color(0xFFF0F6FC)
private val TextSecondary = Color(0xFF8B949E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionJobCostingCommandCenterScreen(
    jobId: String,
    actualCostRecord: ProductionActualJobCostResponseDto? = null,
    varianceSummary: ProductionJobCostVarianceResponseDto? = null,
    reconciliationResult: ProductionJobCostingReconciliationResponseDto? = null,
    handoffContract: Module17Step09JobCostingVarianceHandoffContractDto? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    successMessage: String? = null,
    isCalculateCostDialogOpen: Boolean = false,
    isVarianceDialogOpen: Boolean = false,
    isHandoffDialogOpen: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onOpenCalculateCostDialog: () -> Unit = {},
    onCloseCalculateCostDialog: () -> Unit = {},
    onOpenVarianceDialog: () -> Unit = {},
    onCloseVarianceDialog: () -> Unit = {},
    onReconcile: () -> Unit = {},
    onFetchHandoffContract: () -> Unit = {},
    onCloseHandoffDialog: () -> Unit = {},
    onCalculateCost: (orderId: String, manufacturedQty: BigDecimal, packagingRate: BigDecimal, overheadRate: BigDecimal) -> Unit = { _, _, _, _ -> },
    onCalculateVariance: (quotedPrice: BigDecimal, estCost: BigDecimal, estMat: BigDecimal, estLab: BigDecimal, estMac: BigDecimal, orderQty: BigDecimal) -> Unit = { _, _, _, _, _, _ -> }
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("প্রকৃত উৎপাদন ব্যয়", "ভেরিয়েন্স ও মার্জিন", "মেটেরিয়াল ও লেবার", "মেশিন ও স্ক্র্যাপ", "৮-ওয়ে রিকনসিলিয়েশন")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "উৎপাদন প্রকৃত ব্যয় ও ভেরিয়েন্স কমান্ড সেন্টার",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "জব আইডি: $jobId | প্রকৃত খরচ হিসাব, ভেরিয়েন্স অডিট ও রিকনসিলিয়েশন",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AccentCyan)
                    }
                    IconButton(onClick = onFetchHandoffContract) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "AI Contract Handoff", tint = AccentGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepNavyBg)
            )
        },
        containerColor = DeepNavyBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Error / Success Snackbars
            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    color = AccentRed.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = errorMessage,
                        color = AccentRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (!successMessage.isNullOrBlank()) {
                Surface(
                    color = AccentGreen.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = successMessage,
                        color = AccentGreen,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Top KPI Banner Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val grandTotal = actualCostRecord?.grandTotalActualCost ?: BigDecimal.ZERO
                val unitCost = actualCostRecord?.actualUnitCost ?: BigDecimal.ZERO
                val totalVariance = varianceSummary?.totalCostVariance ?: BigDecimal.ZERO
                val actualMargin = varianceSummary?.actualGrossMarginPercentage ?: BigDecimal.ZERO

                CostKpiCard(
                    title = "মোট প্রকৃত ব্যয় (Total Cost)",
                    value = "৳$grandTotal",
                    subtitle = actualCostRecord?.costStatus ?: "NOT_CALCULATED",
                    accentColor = AccentCyan,
                    icon = Icons.Default.ShoppingCart,
                    modifier = Modifier.weight(1f)
                )
                CostKpiCard(
                    title = "ইউনিট ব্যয় (Unit Cost)",
                    value = "৳$unitCost",
                    subtitle = "প্রতি কপি/পিস",
                    accentColor = AccentPurple,
                    icon = Icons.Default.Info,
                    modifier = Modifier.weight(1f)
                )
                CostKpiCard(
                    title = "ব্যয় ভেরিয়েন্স (Variance)",
                    value = "৳$totalVariance",
                    subtitle = varianceSummary?.overallCostClassification ?: "N/A",
                    accentColor = if (totalVariance <= BigDecimal.ZERO) AccentGreen else AccentRed,
                    icon = Icons.Default.Warning,
                    modifier = Modifier.weight(1f)
                )
                CostKpiCard(
                    title = "গ্রস মার্জিন (Margin)",
                    value = "$actualMargin%",
                    subtitle = "প্রকৃত লাভজনকতা",
                    accentColor = AccentGreen,
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
            }

            // Tab Row
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = CardBg,
                contentColor = AccentCyan,
                edgePadding = 16.dp,
                divider = {}
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) AccentCyan else TextSecondary
                            )
                        }
                    )
                }
            }

            // Tab Content
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentCyan)
                }
            } else {
                when (selectedTab) {
                    0 -> ActualCostTabContent(
                        actualCost = actualCostRecord,
                        onOpenCalculateCostDialog = onOpenCalculateCostDialog,
                        onOpenVarianceDialog = onOpenVarianceDialog
                    )
                    1 -> VarianceTabContent(
                        variance = varianceSummary,
                        onOpenVarianceDialog = onOpenVarianceDialog,
                        onReconcile = onReconcile
                    )
                    2 -> MaterialLaborTabContent(actualCost = actualCostRecord)
                    3 -> MachineScrapTabContent(actualCost = actualCostRecord)
                    4 -> ReconciliationTabContent(
                        reconciliation = reconciliationResult,
                        onReconcile = onReconcile
                    )
                }
            }
        }
    }

    // Dialogs
    if (isCalculateCostDialogOpen) {
        CalculateActualCostDialog(
            jobId = jobId,
            onDismiss = onCloseCalculateCostDialog,
            onSubmit = { orderId, qty, pkgRate, ovhRate ->
                onCalculateCost(orderId, qty, pkgRate, ovhRate)
            }
        )
    }

    if (isVarianceDialogOpen) {
        CalculateVarianceDialog(
            actualTotalCost = actualCostRecord?.grandTotalActualCost ?: BigDecimal("20000.0000"),
            onDismiss = onCloseVarianceDialog,
            onSubmit = { quote, estTot, estMat, estLab, estMac, orderQty ->
                onCalculateVariance(quote, estTot, estMat, estLab, estMac, orderQty)
            }
        )
    }

    if (isHandoffDialogOpen && handoffContract != null) {
        HandoffContractDialog(
            contract = handoffContract,
            onDismiss = onCloseHandoffDialog
        )
    }
}

// ─────────────────────────────────────────────────────────────
// TAB 0: ACTUAL COST SUMMARY
// ─────────────────────────────────────────────────────────────

@Composable
private fun ActualCostTabContent(
    actualCost: ProductionActualJobCostResponseDto?,
    onOpenCalculateCostDialog: () -> Unit,
    onOpenVarianceDialog: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "প্রকৃত উৎপাদন ব্যয় সারসংক্ষেপ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Button(
                    onClick = onOpenCalculateCostDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("প্রকৃত ব্যয় হিসাব করুন")
                }
            }
        }

        if (actualCost == null) {
            item {
                EmptyStateCard(
                    title = "কোন প্রকৃত ব্যয়ের হিসাব পাওয়া যায়নি",
                    description = "শপ-ফ্লোর মেটেরিয়াল ও লেবার টাইম রেকর্ড থেকে প্রকৃত খরচ প্রস্তুত করতে হিসাব বাটনে চাপ দিন।"
                )
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("কাঁচামাল মোট খরচ (Material):", color = TextSecondary)
                            Text("৳${actualCost.totalMaterialCost}", color = AccentCyan, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("শ্রম মোট খরচ (Labor):", color = TextSecondary)
                            Text("৳${actualCost.totalLaborCost}", color = AccentGreen, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("মেশিন মোট খরচ (Machine):", color = TextSecondary)
                            Text("৳${actualCost.totalMachineCost}", color = AccentOrange, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("কোয়ালিটি স্ক্র্যাপ ক্ষতি (Scrap Loss):", color = TextSecondary)
                            Text("৳${actualCost.totalQualityScrapCost}", color = AccentRed, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("রিওয়ার্ক কনভার্সন খরচ (Rework):", color = TextSecondary)
                            Text("৳${actualCost.totalReworkCost}", color = AccentOrange, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("প্যাকেজিং মোট খরচ (Packaging):", color = TextSecondary)
                            Text("৳${actualCost.totalPackagingCost}", color = AccentCyan, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ওভারহেড বরাদ্দ (Overhead 10%):", color = TextSecondary)
                            Text("৳${actualCost.totalOverheadAllocatedCost}", color = AccentPurple, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("সর্বমোট প্রকৃত ব্যয় (Grand Total):", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("৳${actualCost.grandTotalActualCost}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AccentCyan)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("প্রতি ইউনিট প্রকৃত ব্যয় (Actual Unit Cost):", color = TextSecondary)
                            Text("৳${actualCost.actualUnitCost}", color = AccentPurple, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onOpenVarianceDialog,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("কোটেশন ও এস্টিমেশনের সাথে ভেরিয়েন্স অডিট করুন")
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// TAB 1: VARIANCE ANALYSIS
// ─────────────────────────────────────────────────────────────

@Composable
private fun VarianceTabContent(
    variance: ProductionJobCostVarianceResponseDto?,
    onOpenVarianceDialog: () -> Unit,
    onReconcile: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "উৎপাদন ব্যয় ভেরিয়েন্স ও মার্জিন বিশ্লেষণ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Button(
                    onClick = onOpenVarianceDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("ভেরিয়েন্স বিশ্লেষণ চালান")
                }
            }
        }

        if (variance == null) {
            item {
                EmptyStateCard(
                    title = "কোন ভেরিয়েন্স বিশ্লেষণ রেকর্ড নেই",
                    description = "কোটেশন প্রাইস ও এস্টিমেটেড কস্টের সাথে ভেরিয়েন্স বিশ্লেষণ চালান।"
                )
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        VarianceItemRow("কাঁচামাল ব্যয় ভেরিয়েন্স (Material)", variance.materialVariance, variance.materialVariancePercentage, variance.materialCostClassification)
                        VarianceItemRow("শ্রম ব্যয় ভেরিয়েন্স (Labor)", variance.laborVariance, variance.laborVariancePercentage, variance.laborCostClassification)
                        VarianceItemRow("মেশিন ব্যয় ভেরিয়েন্স (Machine)", variance.machineVariance, variance.machineVariancePercentage, variance.machineCostClassification)
                        HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        VarianceItemRow("সর্বমোট উৎপাদন ব্যয় ভেরিয়েন্স (Total Cost)", variance.totalCostVariance, variance.totalCostVariancePercentage, variance.overallCostClassification)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ইউনিট ব্যয় ভেরিয়েন্স:", color = TextSecondary)
                            Text("৳${variance.unitCostVariance}", color = AccentPurple, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("প্রকৃত গ্রস প্রফিট (Actual Gross Profit):", color = TextSecondary)
                            Text("৳${variance.actualGrossProfit}", color = AccentGreen, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("গ্রস মার্জিন ডেল্টা (Margin Delta):", color = TextSecondary)
                            Text("${variance.grossMarginPercentageDelta}%", color = if (variance.grossMarginPercentageDelta >= BigDecimal.ZERO) AccentGreen else AccentRed, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = onReconcile,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("৮-ওয়ে উৎপাদন ব্যয় রিকনসিলিয়েশন ও সার্টিফিকেট অনুমোদন")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VarianceItemRow(
    label: String,
    variance: BigDecimal,
    percentage: BigDecimal,
    classification: String
) {
    val isFavorable = classification == "FAVORABLE"
    val color = if (isFavorable) AccentGreen else if (classification == "UNFAVORABLE") AccentRed else TextSecondary

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(text = "শ্রেণী: $classification ($percentage%)", style = MaterialTheme.typography.bodySmall, color = color)
        }
        Text(
            text = "৳$variance",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ─────────────────────────────────────────────────────────────
// TAB 2: MATERIAL & LABOR BREAKDOWN
// ─────────────────────────────────────────────────────────────

@Composable
private fun MaterialLaborTabContent(actualCost: ProductionActualJobCostResponseDto?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("কাঁচামাল খরচ ব্রেকডাউন (${actualCost?.materialBreakdown?.size ?: 0})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        if (actualCost?.materialBreakdown.isNullOrEmpty()) {
            item { EmptyStateCard("কোন কাঁচামাল খরচের রেকর্ড নেই", "কাঁচামাল ব্যবহারের তথ্য লোড করা যায়নি।") }
        } else {
            items(actualCost?.materialBreakdown ?: emptyList()) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(item.materialName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AccentCyan)
                        Text("কোড: ${item.materialCode} | পরিমাণ: ${item.actualQuantity} ${item.unitOfMeasure}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("ইউনিট দর: ৳${item.actualUnitPrice} | মোট খরচ: ৳${item.actualCost}", style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text("শ্রম খরচ ব্রেকডাউন (${actualCost?.laborBreakdown?.size ?: 0})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        if (actualCost?.laborBreakdown.isNullOrEmpty()) {
            item { EmptyStateCard("কোন শ্রম খরচের রেকর্ড নেই", "অপারেটর টাইম ট্র্যাকিং রেকর্ড পাওয়া যায়নি।") }
        } else {
            items(actualCost?.laborBreakdown ?: emptyList()) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(item.stageName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AccentGreen)
                        Text("সেটআপ: ${item.actualSetupHours}h | রান: ${item.actualRunHours}h | রেট: ৳${item.actualHourlyRate}/h", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("মোট শ্রম খরচ: ৳${item.actualLaborCost}", style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// TAB 3: MACHINE & SCRAP VALUATION
// ─────────────────────────────────────────────────────────────

@Composable
private fun MachineScrapTabContent(actualCost: ProductionActualJobCostResponseDto?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("মেশিন অপারেশন ও ডাউনটাইম খরচ (${actualCost?.machineBreakdown?.size ?: 0})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        if (actualCost?.machineBreakdown.isNullOrEmpty()) {
            item { EmptyStateCard("কোন মেশিন খরচের রেকর্ড নেই", "টেলিমেট্রি রান লগ পাওয়া যায়নি।") }
        } else {
            items(actualCost?.machineBreakdown ?: emptyList()) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(item.machineName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AccentOrange)
                        Text("রানিং আওয়ার: ${item.actualMachineHours}h | ডাউনটাইম: ${item.recordedDowntimeHours}h", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("ডাউনটাইম ক্ষতি: ৳${item.downtimeCostImpact} | মোট মেশিন খরচ: ৳${item.actualMachineCost}", style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text("কোয়ালিটি ডিফেক্ট ও স্ক্র্যাপ মূল্যায়ন (${actualCost?.scrapReworkBreakdown?.size ?: 0})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        if (actualCost?.scrapReworkBreakdown.isNullOrEmpty()) {
            item { EmptyStateCard("কোন স্ক্র্যাপ রেকর্ড নেই", "লটে কোন স্ক্র্যাপ বা ডিফেক্ট পাওয়া যায়নি।") }
        } else {
            items(actualCost?.scrapReworkBreakdown ?: emptyList()) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("স্টেজ: ${item.stageType} | ডিফেক্ট: ${item.defectType}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AccentRed)
                        Text("বাতিল পরিমাণ: ${item.scrappedQuantity} | স্ক্র্যাপ ক্ষতি: ৳${item.scrapMaterialLoss}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("স্যালভেজ রিকভারি: ৳${item.scrapSalvageRecoveryValue} | নিট কোয়ালিটি ক্ষতি: ৳${item.netQualityCost}", style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// TAB 4: RECONCILIATION & CERTIFICATE
// ─────────────────────────────────────────────────────────────

@Composable
private fun ReconciliationTabContent(
    reconciliation: ProductionJobCostingReconciliationResponseDto?,
    onReconcile: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "৮-ওয়ে উৎপাদন ব্যয় রিকনসিলিয়েশন ও সার্টিফাইড হ্যাশ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Button(
                    onClick = onReconcile,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("রিকনসিল করুন")
                }
            }
        }

        if (reconciliation != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (reconciliation.isFullyReconciled) AccentGreen.copy(alpha = 0.1f) else AccentRed.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (reconciliation.isFullyReconciled) AccentGreen else AccentRed)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                if (reconciliation.isFullyReconciled) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (reconciliation.isFullyReconciled) AccentGreen else AccentRed
                            )
                            Text(
                                text = if (reconciliation.isFullyReconciled) "৮-ওয়ে রিকনসিলিয়েশন সম্পূর্ণ সফল (100% Verified)" else "রিকনসিলিয়েশন অসঙ্গতি সনাক্ত হয়েছে!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (reconciliation.isFullyReconciled) AccentGreen else AccentRed
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        ReconCheckRow("১. পরিকল্পিত BOM বনাম প্রকৃত কাঁচামাল ব্যবহার", reconciliation.bomQuantitiesReconciled)
                        ReconCheckRow("২. শপ-ফ্লোর অপারেটর সেটআপ ও রান আওয়ার ভেরিফিকেশন", reconciliation.laborHoursReconciled)
                        ReconCheckRow("৩. মেশিন অপারেশন ও ডাউনটাইম আওয়ার অডিট", reconciliation.machineHoursReconciled)
                        ReconCheckRow("৪. স্ক্র্যাপ ক্ষতি ও রিওয়ার্ক খরচ সামঞ্জস্যতা", reconciliation.scrapReworkValuationConsistent)
                        ReconCheckRow("৫. প্যাকেজিং সামগ্রী ও কার্টুন খরচ ব্যালেন্স", reconciliation.packagingCostBalanced)
                        ReconCheckRow("৬. সর্বমোট প্রকৃত ব্যয়ের গাণিতিক শুদ্ধতা", reconciliation.actualCostMathBalanced)
                        ReconCheckRow("৭. ক্রিপ্টোগ্রাফিক SHA-256 কস্ট সার্টিফিকেট ভেরিফিকেশন", reconciliation.varianceIntegrityHashValid)
                        ReconCheckRow("৮. মাল্টি-টেন্যান্ট ডেটা বাউন্ডারি ও সিকিউরিটি আইসোলেশন", reconciliation.multiTenantIsolationVerified)

                        Spacer(Modifier.height(12.dp))
                        Surface(
                            color = CardBgElevated,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("SHA-256 কস্ট সার্টিফিকেট ইন্টিগ্রিটি হ্যাশ:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                Text(reconciliation.certificateHash, color = AccentPurple, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        } else {
            item {
                EmptyStateCard(
                    title = "রিকনসিলিয়েশন সম্পন্ন হয়নি",
                    description = "প্রকৃত উৎপাদন ব্যয়ের ৮-ওয়ে অডিট সম্পাদন করতে রিকনসিল বাটনে চাপ দিন।"
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// DIALOGS & HELPER COMPONENTS
// ─────────────────────────────────────────────────────────────

@Composable
private fun ReconCheckRow(label: String, isPassed: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        Icon(
            if (isPassed) Icons.Default.CheckCircle else Icons.Default.Close,
            contentDescription = null,
            tint = if (isPassed) AccentGreen else AccentRed,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun CostKpiCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = accentColor)
        }
    }
}

@Composable
private fun EmptyStateCard(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
private fun CalculateActualCostDialog(
    jobId: String,
    onDismiss: () -> Unit,
    onSubmit: (orderId: String, manufacturedQty: BigDecimal, packagingRate: BigDecimal, overheadRate: BigDecimal) -> Unit
) {
    var orderId by remember { mutableStateOf("ORD-$jobId") }
    var goodQty by remember { mutableStateOf("5000.0000") }
    var pkgRate by remember { mutableStateOf("25.0000") }
    var ovhRate by remember { mutableStateOf("0.1000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("প্রকৃত উৎপাদন ব্যয় হিসাব করুন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = orderId, onValueChange = { orderId = it }, label = { Text("অর্ডার আইডি") })
                OutlinedTextField(value = goodQty, onValueChange = { goodQty = it }, label = { Text("গৃহীত ভালো পণ্যের পরিমাণ (Units)") })
                OutlinedTextField(value = pkgRate, onValueChange = { pkgRate = it }, label = { Text("কার্টুন প্যাকেজিং রেট (৳/কার্টুন)") })
                OutlinedTextField(value = ovhRate, onValueChange = { ovhRate = it }, label = { Text("ওভারহেড বরাদ্দ রেট (যেমন 0.10 = 10%)") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val gQty = goodQty.toBigDecimalOrNull() ?: BigDecimal("5000.0000")
                    val pRate = pkgRate.toBigDecimalOrNull() ?: BigDecimal("25.0000")
                    val oRate = ovhRate.toBigDecimalOrNull() ?: BigDecimal("0.1000")
                    onSubmit(orderId, gQty, pRate, oRate)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
            ) { Text("হিসাব নিশ্চিত করুন") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun CalculateVarianceDialog(
    actualTotalCost: BigDecimal,
    onDismiss: () -> Unit,
    onSubmit: (quotedPrice: BigDecimal, estCost: BigDecimal, estMat: BigDecimal, estLab: BigDecimal, estMac: BigDecimal, orderQty: BigDecimal) -> Unit
) {
    var quotePrice by remember { mutableStateOf("30000.0000") }
    var estTotal by remember { mutableStateOf("20000.0000") }
    var estMat by remember { mutableStateOf("15000.0000") }
    var estLab by remember { mutableStateOf("3000.0000") }
    var estMac by remember { mutableStateOf("2000.0000") }
    var orderQty by remember { mutableStateOf("5000.0000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("উৎপাদন ব্যয় ভেরিয়েন্স বিশ্লেষণ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("প্রকৃত মোট খরচ: ৳$actualTotalCost", color = AccentCyan, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = quotePrice, onValueChange = { quotePrice = it }, label = { Text("কোটেশন বিক্রয়মূল্য (Quoted Price)") })
                OutlinedTextField(value = estTotal, onValueChange = { estTotal = it }, label = { Text("পরিকল্পিত মোট ব্যয় (Estimated Total Cost)") })
                OutlinedTextField(value = estMat, onValueChange = { estMat = it }, label = { Text("পরিকল্পিত কাঁচামাল খরচ (Est Material)") })
                OutlinedTextField(value = estLab, onValueChange = { estLab = it }, label = { Text("পরিকল্পিত শ্রম খরচ (Est Labor)") })
                OutlinedTextField(value = estMac, onValueChange = { estMac = it }, label = { Text("পরিকল্পিত মেশিন খরচ (Est Machine)") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qPrice = quotePrice.toBigDecimalOrNull() ?: BigDecimal("30000.0000")
                    val eTot = estTotal.toBigDecimalOrNull() ?: BigDecimal("20000.0000")
                    val eMat = estMat.toBigDecimalOrNull() ?: BigDecimal("15000.0000")
                    val eLab = estLab.toBigDecimalOrNull() ?: BigDecimal("3000.0000")
                    val eMac = estMac.toBigDecimalOrNull() ?: BigDecimal("2000.0000")
                    val oQty = orderQty.toBigDecimalOrNull() ?: BigDecimal("5000.0000")
                    onSubmit(qPrice, eTot, eMat, eLab, eMac, oQty)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) { Text("ভেরিয়েন্স বিশ্লেষণ চালান") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun HandoffContractDialog(
    contract: Module17Step09JobCostingVarianceHandoffContractDto,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Module 17 Step 09 AI Handoff Contract") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Contract Version: ${contract.contractVersion}", color = AccentCyan, style = MaterialTheme.typography.bodySmall)
                Text("Job ID: ${contract.executionJobId}", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                Text("Cost Status: ${contract.costStatus}", color = AccentGreen, style = MaterialTheme.typography.bodySmall)
                Text("Actual Total Cost: ৳${contract.actualTotalCost}", color = AccentCyan, style = MaterialTheme.typography.bodySmall)
                Text("Total Variance: ৳${contract.totalCostVariance} (${contract.overallCostClassification})", color = if (contract.overallCostClassification == "FAVORABLE") AccentGreen else AccentRed, style = MaterialTheme.typography.bodySmall)
                Text("Actual Unit Cost: ৳${contract.actualUnitCost}", color = AccentPurple, style = MaterialTheme.typography.bodySmall)
                Text("Actual Gross Profit: ৳${contract.actualGrossProfit}", color = AccentGreen, style = MaterialTheme.typography.bodySmall)
                Text("Gross Margin Delta: ${contract.grossMarginPercentageDelta}%", color = AccentGreen, style = MaterialTheme.typography.bodySmall)
                Text("Is Fully Reconciled: ${contract.isFullyReconciled}", color = AccentGreen, style = MaterialTheme.typography.bodySmall)
                Text("Cost Certificate Hash: ${contract.costCertificateHash}", color = AccentPurple, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("ঠিক আছে") }
        }
    )
}
