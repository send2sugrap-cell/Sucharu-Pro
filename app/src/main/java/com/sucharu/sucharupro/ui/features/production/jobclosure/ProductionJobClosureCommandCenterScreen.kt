package com.sucharu.sucharupro.ui.features.production.jobclosure

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
import com.sucharu.sucharupro.data.api.model.jobclosure.*
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
private val AccentGold = Color(0xFFE3B341)
private val TextPrimary = Color(0xFFF0F6FC)
private val TextSecondary = Color(0xFF8B949E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionJobClosureCommandCenterScreen(
    jobId: String,
    closureRecord: ProductionJobClosureResponseDto? = null,
    scorecard: ManufacturingPerformanceScorecardDto? = null,
    readinessAudit: JobClosureReadinessAuditDto? = null,
    handoffContract: Module17Step10JobClosureGovernanceHandoffContractDto? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    successMessage: String? = null,
    isCloseJobDialogOpen: Boolean = false,
    isAuditDetailsDialogOpen: Boolean = false,
    isHandoffDialogOpen: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onOpenCloseJobDialog: () -> Unit = {},
    onCloseCloseJobDialog: () -> Unit = {},
    onOpenAuditDetailsDialog: () -> Unit = {},
    onCloseAuditDetailsDialog: () -> Unit = {},
    onFetchHandoffContract: () -> Unit = {},
    onCloseHandoffDialog: () -> Unit = {},
    onAuditReadiness: (orderId: String) -> Unit = {},
    onCloseAndSealJob: (orderId: String, orderQty: BigDecimal, goodUnits: BigDecimal, estCost: BigDecimal, actCost: BigDecimal, varCost: BigDecimal, reworkUnits: BigDecimal, machineEff: BigDecimal, onTime: Boolean) -> Unit = { _, _, _, _, _, _, _, _, _ -> }
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("জব ক্লোজার ও ফাইনাল সিল", "প্রোভেন্যান্স ও লিনিয়েজ", "কেপিআই স্কোরকার্ড", "পোস্ট-মর্টেম ও অডিট", "এআই হ্যান্ডঅফ")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "উৎপাদন সমাপ্তি ও এন্টারপ্রাইজ গভর্ন্যান্স কমান্ড সেন্টার",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "জব আইডি: $jobId | ১০-স্টেপ ডিজিটাল সিল, প্রোভেন্যান্স ও পারফরম্যান্স অডিট",
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
            // Snackbars
            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    color = AccentRed.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = errorMessage, color = AccentRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            }

            if (!successMessage.isNullOrBlank()) {
                Surface(
                    color = AccentGreen.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = successMessage, color = AccentGreen, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            }

            // Top KPI Summary Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val status = closureRecord?.closureStatus ?: "OPEN"
                val grade = closureRecord?.scorecard?.performanceGrade ?: (scorecard?.performanceGrade ?: "N/A")
                val otif = closureRecord?.scorecard?.onTimeInFullPercentage ?: (scorecard?.onTimeInFullPercentage ?: BigDecimal.ZERO)
                val mfgIndex = closureRecord?.scorecard?.overallManufacturingIndex ?: (scorecard?.overallManufacturingIndex ?: BigDecimal.ZERO)

                ClosureKpiCard(
                    title = "ক্লোজার স্ট্যাটাস",
                    value = status,
                    subtitle = if (status == "GOVERNANCE_SEALED") "ডিজিটাল সিল্ড" else "প্রক্রিয়াধীন",
                    accentColor = if (status == "GOVERNANCE_SEALED") AccentGreen else AccentOrange,
                    icon = Icons.Default.Lock,
                    modifier = Modifier.weight(1f)
                )
                ClosureKpiCard(
                    title = "পারফরম্যান্স গ্রেড",
                    value = grade,
                    subtitle = "কম্পোজিট মান",
                    accentColor = AccentGold,
                    icon = Icons.Default.Star,
                    modifier = Modifier.weight(1f)
                )
                ClosureKpiCard(
                    title = "OTIF ডেলিভারি হার",
                    value = "$otif%",
                    subtitle = "সময়মতো ও সম্পূর্ণ",
                    accentColor = AccentCyan,
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
                ClosureKpiCard(
                    title = "ম্যানুফ্যাকচারিং ইনডেক্স",
                    value = "$mfgIndex",
                    subtitle = "আউট অব ১০০",
                    accentColor = AccentPurple,
                    icon = Icons.Default.Info,
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
                    0 -> MasterSealTabContent(
                        closureRecord = closureRecord,
                        onOpenCloseJobDialog = onOpenCloseJobDialog,
                        onAuditReadiness = { onAuditReadiness("ORD-$jobId") }
                    )
                    1 -> ProvenanceTabContent(provenanceGraph = closureRecord?.provenanceGraph)
                    2 -> ScorecardTabContent(scorecard = closureRecord?.scorecard ?: scorecard)
                    3 -> PostMortemTabContent(postMortem = closureRecord?.postMortemSummary)
                    4 -> HandoffTabContent(
                        contract = handoffContract,
                        onFetchContract = onFetchHandoffContract
                    )
                }
            }
        }
    }

    // Dialogs
    if (isCloseJobDialogOpen) {
        CloseAndSealJobDialog(
            jobId = jobId,
            onDismiss = onCloseCloseJobDialog,
            onSubmit = { orderId, orderQty, goodUnits, estCost, actCost, varCost, reworkUnits, machineEff, onTime ->
                onCloseAndSealJob(orderId, orderQty, goodUnits, estCost, actCost, varCost, reworkUnits, machineEff, onTime)
            }
        )
    }

    if (isHandoffDialogOpen && handoffContract != null) {
        JobClosureHandoffContractDialog(
            contract = handoffContract,
            onDismiss = onCloseHandoffDialog
        )
    }
}

// ─────────────────────────────────────────────────────────────
// TAB 0: MASTER CLOSURE SEAL
// ─────────────────────────────────────────────────────────────

@Composable
private fun MasterSealTabContent(
    closureRecord: ProductionJobClosureResponseDto?,
    onOpenCloseJobDialog: () -> Unit,
    onAuditReadiness: () -> Unit
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
                    text = "মাস্টার উৎপাদন সমাপ্তি সিল ও ডিজিটাল স্বাক্ষর",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onAuditReadiness,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("প্রি-ক্লোজার অডিট")
                    }
                    Button(
                        onClick = onOpenCloseJobDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("জব সিল ও সমাপ্ত করুন")
                    }
                }
            }
        }

        if (closureRecord == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("উৎপাদন জব এখনো সমাপ্ত ও সিল করা হয়নি", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text("সমস্ত আগের ধাপসমূহ (১-৯) সম্পন্ন করে জব সিল বাটনে চাপ দিয়ে চূড়ান্ত গভর্ন্যান্স সিল প্রস্তুত করুন।", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        } else {
            val cert = closureRecord.masterCertificate
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen)
                            Text(
                                text = "ম্যানুফ্যাকচারিং ডিজিটাল মাস্টার সিল সার্টিফাইড",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AccentGreen
                            )
                        }

                        HorizontalDivider(color = BorderColor)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("সার্টিফিকেট আইডি:", color = TextSecondary)
                            Text(cert.certificateId, color = AccentCyan, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("মোট গৃহীত ভালো পণ্য:", color = TextSecondary)
                            Text("${cert.totalGoodUnitsReleased} Units", color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("সর্বমোট প্রকৃত উৎপাদন ব্যয়:", color = TextSecondary)
                            Text("৳${cert.grandTotalActualCost}", color = AccentCyan, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("মোট ব্যয় ভেরিয়েন্স:", color = TextSecondary)
                            Text("৳${cert.totalCostVariance} (${cert.overallCostClassification})", color = if (cert.overallCostClassification == "FAVORABLE") AccentGreen else AccentRed, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("সামগ্রিক ম্যানুফ্যাকচারিং স্কোর:", color = TextSecondary)
                            Text("${cert.overallManufacturingScore} / 100", color = AccentGold, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("অনুমোদনকারী প্ল্যান্ট ম্যানেজার:", color = TextSecondary)
                            Text(cert.sealedBy, color = TextPrimary)
                        }

                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = CardBgElevated,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("ক্রিপ্টোগ্রাফিক মাস্টার SHA-256 সিল হ্যাশ:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                Text(cert.masterSealHash, color = AccentPurple, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// TAB 1: 10-STEP PROVENANCE LINEAGE GRAPH
// ─────────────────────────────────────────────────────────────

@Composable
private fun ProvenanceTabContent(provenanceGraph: ProductionJobProvenanceGraphDto?) {
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
                    text = "১০-স্টেপ অখণ্ড ডিজিটাল প্রোভেন্যান্স লিনিয়েজ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (provenanceGraph != null) {
                    Text(
                        text = if (provenanceGraph.isChainUnbroken) "১০/১০ স্টেজ ভেরিফাইড" else "চেইন অসম্পূর্ণ",
                        color = if (provenanceGraph.isChainUnbroken) AccentGreen else AccentRed,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (provenanceGraph == null || provenanceGraph.nodes.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("প্রোভেন্যান্স লিনিয়েজ রেকর্ড লোড করা যায়নি", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
            }
        } else {
            items(provenanceGraph.nodes) { node ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AccentCyan.copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${node.stepNumber}",
                                    fontWeight = FontWeight.Bold,
                                    color = AccentCyan,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(node.stepName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("এনটিটি: ${node.canonicalEntityName} (${node.canonicalEntityId})", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// TAB 2: MANUFACTURING SCORECARD
// ─────────────────────────────────────────────────────────────

@Composable
private fun ScorecardTabContent(scorecard: ManufacturingPerformanceScorecardDto?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("উৎপাদন কার্যকারিতা ও কেপিআই মূল্যায়ন", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        if (scorecard == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("কোন স্কোরকার্ড রেকর্ড পাওয়া যায়নি", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    }
                }
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
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("সময়মতো সম্পূর্ণ ডেলিভারি (OTIF):", color = TextSecondary)
                            Text("${scorecard.onTimeInFullPercentage}%", color = AccentCyan, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("প্রথমবারেই সঠিক উৎপাদন (Right-First-Time):", color = TextSecondary)
                            Text("${scorecard.rightFirstTimePercentage}%", color = AccentGreen, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("বাজেট আনুগত্য সূচক (Cost Adherence Index):", color = TextSecondary)
                            Text("${scorecard.costAdherenceIndex}%", color = AccentPurple, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("মেশিন দক্ষতা সূচক (Machine Efficiency):", color = TextSecondary)
                            Text("${scorecard.machineEfficiencyIndex}%", color = AccentOrange, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("উপাদান কোয়ালিটি ইল্ড (Quality Yield):", color = TextSecondary)
                            Text("${scorecard.qualityYieldPercentage}%", color = AccentCyan, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = BorderColor)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("সামগ্রিক ম্যানুফ্যাকচারিং সূচক (OMI):", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("${scorecard.overallManufacturingIndex} (${scorecard.performanceGrade})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AccentGold)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// TAB 3: POST-MORTEM & AUDIT
// ─────────────────────────────────────────────────────────────

@Composable
private fun PostMortemTabContent(postMortem: ProductionPostMortemSummaryDto?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("উৎপাদন পোস্ট-মর্টেম ও অপারেশনাল সুপারিশ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        if (postMortem == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("পোস্ট-মর্টেম সারাংশ লোড করা যায়নি", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    }
                }
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
                        Text("প্রধান ডাউনটাইম চালকসমূহ:", color = AccentOrange, fontWeight = FontWeight.Bold)
                        if (postMortem.primaryDowntimeDrivers.isEmpty()) {
                            Text("• কোন উল্লেখযোগ্য ডাউনটাইম পাওয়া যায়নি।", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        } else {
                            postMortem.primaryDowntimeDrivers.forEach { Text("• $it", color = TextPrimary, style = MaterialTheme.typography.bodySmall) }
                        }

                        Spacer(Modifier.height(4.dp))
                        Text("স্ক্র্যাপ ও কোয়ালিটি ডিফেক্ট সারাংশ:", color = AccentRed, fontWeight = FontWeight.Bold)
                        if (postMortem.scrapAndDefectTakeaways.isEmpty()) {
                            Text("• স্ক্র্যাপের হার সাধারণ সীমার মধ্যে ছিল।", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        } else {
                            postMortem.scrapAndDefectTakeaways.forEach { Text("• $it", color = TextPrimary, style = MaterialTheme.typography.bodySmall) }
                        }

                        Spacer(Modifier.height(4.dp))
                        Text("ভবিষ্যতের জন্য অপারেশনাল সুপারিশ:", color = AccentGreen, fontWeight = FontWeight.Bold)
                        if (postMortem.operationalRecommendations.isEmpty()) {
                            Text("• স্ট্যান্ডার্ড অপারেটিং প্রসিডিউর (SOP) মেনে কাজ সফল হয়েছে।", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        } else {
                            postMortem.operationalRecommendations.forEach { Text("• $it", color = TextPrimary, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// TAB 4: AI & ENTERPRISE HANDOFF
// ─────────────────────────────────────────────────────────────

@Composable
private fun HandoffTabContent(
    contract: Module17Step10JobClosureGovernanceHandoffContractDto?,
    onFetchContract: () -> Unit
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
                Text("এন্টারপ্রাইজ এআই ও ক্রস-মডিউল হ্যান্ডঅফ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Button(
                    onClick = onFetchContract,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("হ্যান্ডঅফ এক্সপোর্ট করুন")
                }
            }
        }

        if (contract != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Module 17 Step 10 Handoff Specification", color = AccentCyan, fontWeight = FontWeight.Bold)
                        Text("Version: ${contract.contractVersion} | Job ID: ${contract.executionJobId}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        HorizontalDivider(color = BorderColor)
                        HandoffCheckItem("Module 07 Inventory Finished Goods Stock Receipt", contract.crossModuleInventoryConfirmed)
                        HandoffCheckItem("Module 08 Dispatch & Delivery Readiness Handover", contract.crossModuleDeliveryConfirmed)
                        HandoffCheckItem("Module 15 General Ledger Capitalization Notification", contract.crossModuleFinanceConfirmed)
                        HandoffCheckItem("Module 16 Actual Job Profitability Intelligence Lock", contract.crossModuleProfitabilityLocked)
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("হ্যান্ডঅফ ডেটা দেখতে এক্সপোর্ট বাটনে চাপ দিন", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// DIALOGS & HELPER COMPONENTS
// ─────────────────────────────────────────────────────────────

@Composable
private fun HandoffCheckItem(label: String, isConfirmed: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        Icon(
            if (isConfirmed) Icons.Default.CheckCircle else Icons.Default.Close,
            contentDescription = null,
            tint = if (isConfirmed) AccentGreen else AccentRed,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ClosureKpiCard(
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
private fun CloseAndSealJobDialog(
    jobId: String,
    onDismiss: () -> Unit,
    onSubmit: (orderId: String, orderQty: BigDecimal, goodUnits: BigDecimal, estCost: BigDecimal, actCost: BigDecimal, varCost: BigDecimal, reworkUnits: BigDecimal, machineEff: BigDecimal, onTime: Boolean) -> Unit
) {
    var orderId by remember { mutableStateOf("ORD-$jobId") }
    var orderQty by remember { mutableStateOf("5000.0000") }
    var goodUnits by remember { mutableStateOf("5000.0000") }
    var estCost by remember { mutableStateOf("20000.0000") }
    var actCost by remember { mutableStateOf("20000.0000") }
    var varCost by remember { mutableStateOf("0.0000") }
    var reworkUnits by remember { mutableStateOf("0.0000") }
    var machineEff by remember { mutableStateOf("85.0000") }
    var onTime by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("জব ফাইনাল সিল ও সমাপ্তি নিশ্চিতকরণ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = orderId, onValueChange = { orderId = it }, label = { Text("অর্ডার আইডি") })
                OutlinedTextField(value = orderQty, onValueChange = { orderQty = it }, label = { Text("অর্ডার পরিমাণ (Units)") })
                OutlinedTextField(value = goodUnits, onValueChange = { goodUnits = it }, label = { Text("গৃহীত ভালো পণ্যের পরিমাণ") })
                OutlinedTextField(value = estCost, onValueChange = { estCost = it }, label = { Text("পরিকল্পিত মোট ব্যয় (৳)") })
                OutlinedTextField(value = actCost, onValueChange = { actCost = it }, label = { Text("প্রকৃত মোট ব্যয় (৳)") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val oQty = orderQty.toBigDecimalOrNull() ?: BigDecimal("5000.0000")
                    val gUnits = goodUnits.toBigDecimalOrNull() ?: BigDecimal("5000.0000")
                    val eCost = estCost.toBigDecimalOrNull() ?: BigDecimal("20000.0000")
                    val aCost = actCost.toBigDecimalOrNull() ?: BigDecimal("20000.0000")
                    val vCost = varCost.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val rUnits = reworkUnits.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val mEff = machineEff.toBigDecimalOrNull() ?: BigDecimal("85.0000")
                    onSubmit(orderId, oQty, gUnits, eCost, aCost, vCost, rUnits, mEff, onTime)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) { Text("সিল ও সমাপ্তি নিশ্চিত করুন") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun JobClosureHandoffContractDialog(
    contract: Module17Step10JobClosureGovernanceHandoffContractDto,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Module 17 Step 10 AI Handoff Contract") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Contract Version: ${contract.contractVersion}", color = AccentCyan, style = MaterialTheme.typography.bodySmall)
                Text("Job ID: ${contract.executionJobId}", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                Text("Closure Status: ${contract.closureStatus}", color = AccentGreen, style = MaterialTheme.typography.bodySmall)
                Text("Overall Manufacturing Index: ${contract.overallManufacturingIndex} (${contract.performanceGrade})", color = AccentGold, style = MaterialTheme.typography.bodySmall)
                Text("OTIF %: ${contract.onTimeInFullPercentage}% | RFT %: ${contract.rightFirstTimePercentage}%", color = AccentCyan, style = MaterialTheme.typography.bodySmall)
                Text("Total Good Units: ${contract.totalGoodUnitsReleased}", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                Text("Master Seal Hash: ${contract.masterClosureSealHash}", color = AccentPurple, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("ঠিক আছে") }
        }
    )
}
