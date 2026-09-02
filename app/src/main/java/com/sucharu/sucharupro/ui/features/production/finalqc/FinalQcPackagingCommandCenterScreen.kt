package com.sucharu.sucharupro.ui.features.production.finalqc

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
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.sucharu.sucharupro.data.api.model.finalqc.*
import com.sucharu.sucharupro.domain.model.finalqc.*
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

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
fun FinalQcPackagingCommandCenterScreen(
    jobId: String,
    inspections: List<FinalQcInspectionResponseDto> = emptyList(),
    defects: List<DefectContainmentResponseDto> = emptyList(),
    packagingRecords: List<PackagingResponseDto> = emptyList(),
    releaseRecords: List<FinishedGoodsReleaseResponseDto> = emptyList(),
    varianceSummary: FinalQcPackagingVarianceResponseDto? = null,
    reconciliationResult: FinalQcPackagingReconciliationResponseDto? = null,
    handoffContract: Module17Step08FinalQcPackagingHandoffContractDto? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    successMessage: String? = null,
    isInspectionDialogOpen: Boolean = false,
    isDefectDialogOpen: Boolean = false,
    isPackagingDialogOpen: Boolean = false,
    isReleaseDialogOpen: Boolean = false,
    isHandoffDialogOpen: Boolean = false,
    activeInspectionId: String? = null,
    onNavigateBack: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onOpenInspectionDialog: () -> Unit = {},
    onCloseInspectionDialog: () -> Unit = {},
    onOpenDefectDialog: (String) -> Unit = {},
    onCloseDefectDialog: () -> Unit = {},
    onOpenPackagingDialog: (String) -> Unit = {},
    onClosePackagingDialog: () -> Unit = {},
    onOpenReleaseDialog: (String) -> Unit = {},
    onCloseReleaseDialog: () -> Unit = {},
    onFetchHandoffContract: () -> Unit = {},
    onCloseHandoffDialog: () -> Unit = {},
    onCreateInspection: (orderId: String, samplePlan: InspectionSamplePlanType, totalLot: BigDecimal, sampleSize: BigDecimal, inspectorId: String, inspectorName: String, notes: String?) -> Unit = { _, _, _, _, _, _, _ -> },
    onCompleteInspection: (inspectionId: String, acceptedQty: BigDecimal, rejectedQty: BigDecimal, reworkQty: BigDecimal, notes: String?) -> Unit = { _, _, _, _, _ -> },
    onRecordDefect: (inspectionId: String, stage: ProductionStageType, type: DefectClassificationType, severity: DefectSeverity, qty: BigDecimal, disposition: ContainmentDisposition, location: String, details: String) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onCreatePackaging: (inspectionId: String, type: PackagingType, unitsPerPkg: BigDecimal, pkgCount: Int, pallet: String?, cartonRange: String?, weight: BigDecimal?, packagedBy: String, notes: String?) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onAuthorizeRelease: (orderId: String, inspectionId: String, pkgId: String, qty: BigDecimal, dest: String, authBy: String, notes: String?) -> Unit = { _, _, _, _, _, _, _ -> }
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("ফাইনাল পরিদর্শন", "ত্রুটি নিয়ন্ত্রণ", "প্যাকেজিং স্লিপ", "ওয়্যারহাউস রিলিজ", "রিকনসিলিয়েশন")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "চূড়ান্ত গুণগত মান ও প্যাকেজিং কমান্ড সেন্টার",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "জব আইডি: $jobId | মান পরিদর্শন, ত্রুটি কোয়ারেন্টাইন ও ওয়্যারহাউস রিলিজ",
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
                val totalAccepted = inspections.fold(BigDecimal.ZERO) { acc, i -> acc.add(i.acceptedQuantity) }
                val totalRejected = inspections.fold(BigDecimal.ZERO) { acc, i -> acc.add(i.rejectedQuantity) }
                val totalPackaged = packagingRecords.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.totalPackagedQuantity) }
                val totalReleased = releaseRecords.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.releasedQuantity) }

                QualityKpiCard(
                    title = "গৃহীত মান (Accepted)",
                    value = "$totalAccepted",
                    subtitle = "পরিদর্শন পাস",
                    accentColor = AccentGreen,
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
                QualityKpiCard(
                    title = "বাতিল/ত্রুটি (Defects)",
                    value = "$totalRejected",
                    subtitle = "কোয়ারেন্টাইন ইউনিট",
                    accentColor = AccentRed,
                    icon = Icons.Default.Warning,
                    modifier = Modifier.weight(1f)
                )
                QualityKpiCard(
                    title = "প্যাকেজড (Packaged)",
                    value = "$totalPackaged",
                    subtitle = "${packagingRecords.sumOf { it.totalPackageCount }} কার্টুন",
                    accentColor = AccentCyan,
                    icon = Icons.Default.ShoppingCart,
                    modifier = Modifier.weight(1f)
                )
                QualityKpiCard(
                    title = "রিলিজড (Released)",
                    value = "$totalReleased",
                    subtitle = "SHA-256 সার্টিফাইড",
                    accentColor = AccentPurple,
                    icon = Icons.Default.Lock,
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
                    0 -> InspectionsTabContent(
                        inspections = inspections,
                        onOpenInspectionDialog = onOpenInspectionDialog,
                        onCompleteInspection = onCompleteInspection,
                        onOpenDefectDialog = onOpenDefectDialog,
                        onOpenPackagingDialog = onOpenPackagingDialog
                    )
                    1 -> DefectsTabContent(
                        defects = defects,
                        onOpenDefectDialog = { inspections.firstOrNull()?.inspectionId?.let { onOpenDefectDialog(it) } }
                    )
                    2 -> PackagingTabContent(
                        packagingRecords = packagingRecords,
                        onOpenPackagingDialog = { inspections.firstOrNull()?.inspectionId?.let { onOpenPackagingDialog(it) } },
                        onOpenReleaseDialog = onOpenReleaseDialog
                    )
                    3 -> ReleasesTabContent(
                        releases = releaseRecords,
                        onOpenReleaseDialog = {
                            val insp = inspections.firstOrNull()?.inspectionId ?: ""
                            onOpenReleaseDialog(insp)
                        }
                    )
                    4 -> ReconciliationTabContent(
                        variance = varianceSummary,
                        reconciliation = reconciliationResult
                    )
                }
            }
        }
    }

    // Dialogs
    if (isInspectionDialogOpen) {
        CreateInspectionDialog(
            jobId = jobId,
            onDismiss = onCloseInspectionDialog,
            onSubmit = { orderId, samplePlan, totalLot, sampleSize, inspId, inspName, notes ->
                onCreateInspection(orderId, samplePlan, totalLot, sampleSize, inspId, inspName, notes)
            }
        )
    }

    if (isDefectDialogOpen) {
        RecordDefectDialog(
            inspectionId = activeInspectionId ?: (inspections.firstOrNull()?.inspectionId ?: ""),
            onDismiss = onCloseDefectDialog,
            onSubmit = { inspId, stage, type, severity, qty, disp, loc, details ->
                onRecordDefect(inspId, stage, type, severity, qty, disp, loc, details)
            }
        )
    }

    if (isPackagingDialogOpen) {
        CreatePackagingDialog(
            inspectionId = activeInspectionId ?: (inspections.firstOrNull()?.inspectionId ?: ""),
            onDismiss = onClosePackagingDialog,
            onSubmit = { inspId, type, unitsPerPkg, count, pallet, range, weight, pkgBy, notes ->
                onCreatePackaging(inspId, type, unitsPerPkg, count, pallet, range, weight, pkgBy, notes)
            }
        )
    }

    if (isReleaseDialogOpen) {
        AuthorizeReleaseDialog(
            inspectionId = activeInspectionId ?: (inspections.firstOrNull()?.inspectionId ?: ""),
            packagingId = packagingRecords.firstOrNull()?.packagingId ?: "",
            onDismiss = onCloseReleaseDialog,
            onSubmit = { orderId, inspId, pkgId, qty, dest, authBy, notes ->
                onAuthorizeRelease(orderId, inspId, pkgId, qty, dest, authBy, notes)
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
// TAB 0: FINAL INSPECTION
// ─────────────────────────────────────────────────────────────

@Composable
private fun InspectionsTabContent(
    inspections: List<FinalQcInspectionResponseDto>,
    onOpenInspectionDialog: () -> Unit,
    onCompleteInspection: (String, BigDecimal, BigDecimal, BigDecimal, String?) -> Unit,
    onOpenDefectDialog: (String) -> Unit,
    onOpenPackagingDialog: (String) -> Unit
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
                    text = "ফাইনাল পরিদর্শন তালিকা (${inspections.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Button(
                    onClick = onOpenInspectionDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("নতুন পরিদর্শন শুরু করুন")
                }
            }
        }

        if (inspections.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "কোন ফাইনাল কোয়ালিটি পরিদর্শন রেকর্ড নেই",
                    description = "উৎপাদন লটের মান যাচাই করতে নতুন পরিদর্শন শুরু করুন।"
                )
            }
        } else {
            items(inspections) { insp ->
                InspectionCard(
                    inspection = insp,
                    onComplete = { acc, rej, rew, notes -> onCompleteInspection(insp.inspectionId, acc, rej, rew, notes) },
                    onRecordDefect = { onOpenDefectDialog(insp.inspectionId) },
                    onPackage = { onOpenPackagingDialog(insp.inspectionId) }
                )
            }
        }
    }
}

@Composable
private fun InspectionCard(
    inspection: FinalQcInspectionResponseDto,
    onComplete: (BigDecimal, BigDecimal, BigDecimal, String?) -> Unit,
    onRecordDefect: () -> Unit,
    onPackage: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var completeAccepted by remember { mutableStateOf(inspection.totalLotQuantity.toPlainString()) }
    var completeRejected by remember { mutableStateOf("0.0000") }
    var completeRework by remember { mutableStateOf("0.0000") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(status = inspection.status)
                    Text(
                        text = inspection.inspectionId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "প্ল্যান: ${inspection.samplePlanType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentCyan
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("মোট লট: ${inspection.totalLotQuantity}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("স্যাম্পল সাইজ: ${inspection.sampleSize}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("ইন্সপেক্টর: ${inspection.inspectorName}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("গৃহীত: ${inspection.acceptedQuantity}", style = MaterialTheme.typography.bodyMedium, color = AccentGreen)
                Text("বাতিল: ${inspection.rejectedQuantity}", style = MaterialTheme.typography.bodyMedium, color = AccentRed)
                Text("রি-ওয়ার্ক: ${inspection.reworkQuantity}", style = MaterialTheme.typography.bodyMedium, color = AccentOrange)
            }

            if (inspection.status == "IN_PROGRESS") {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isExpanded = !isExpanded },
                        colors = ButtonDefaults.buttonColors(containerColor = CardBgElevated),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isExpanded) "লুকান" else "সাইনিং অফ করুন", color = TextPrimary)
                    }
                    Button(
                        onClick = onRecordDefect,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed.copy(alpha = 0.8f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ত্রুটি লগ")
                    }
                }

                if (isExpanded) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = completeAccepted,
                        onValueChange = { completeAccepted = it },
                        label = { Text("গৃহীত পরিমাণ (Accepted Qty)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = completeRejected,
                            onValueChange = { completeRejected = it },
                            label = { Text("বাতিল পরিমাণ (Rejected)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = completeRework,
                            onValueChange = { completeRework = it },
                            label = { Text("রি-ওয়ার্ক (Rework)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val acc = completeAccepted.toBigDecimalOrNull() ?: BigDecimal.ZERO
                            val rej = completeRejected.toBigDecimalOrNull() ?: BigDecimal.ZERO
                            val rew = completeRework.toBigDecimalOrNull() ?: BigDecimal.ZERO
                            onComplete(acc, rej, rew, "Inspection finalized on shop floor")
                            isExpanded = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("পরিদর্শন সমাপ্ত ও সাইন-অফ নিশ্চিত করুন")
                    }
                }
            } else {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onPackage,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("এই লটের জন্য প্যাকেজিং শুরু করুন")
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// TAB 1: DEFECT CONTAINMENT
// ─────────────────────────────────────────────────────────────

@Composable
private fun DefectsTabContent(
    defects: List<DefectContainmentResponseDto>,
    onOpenDefectDialog: () -> Unit
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
                    text = "ত্রুটি ও কোয়ারেন্টাইন রেকর্ড (${defects.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Button(
                    onClick = onOpenDefectDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("ত্রুটি কোয়ারেন্টাইন করুন")
                }
            }
        }

        if (defects.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "কোন ত্রুটি রেকর্ড নেই",
                    description = "সমস্ত পরিদর্শন পাস করেছে বা কোনো ত্রুটি রিপোর্ট করা হয়নি।"
                )
            }
        } else {
            items(defects) { defect ->
                DefectCard(defect = defect)
            }
        }
    }
}

@Composable
private fun DefectCard(defect: DefectContainmentResponseDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SeverityBadge(severity = defect.severity)
                    Text(
                        text = defect.defectType,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "স্টেজ: ${defect.rootCauseStage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentCyan
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("ত্রুটি পরিমাণ: ${defect.defectQuantity}", style = MaterialTheme.typography.bodyMedium, color = AccentRed, fontWeight = FontWeight.Bold)
                Text("নিষ্পত্তি: ${defect.disposition}", style = MaterialTheme.typography.bodyMedium, color = AccentOrange)
                Text("লোকেশন: ${defect.quarantineLocation}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = "কারণ বিবরণ: ${defect.rootCauseDetails}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// TAB 2: PACKAGING
// ─────────────────────────────────────────────────────────────

@Composable
private fun PackagingTabContent(
    packagingRecords: List<PackagingResponseDto>,
    onOpenPackagingDialog: () -> Unit,
    onOpenReleaseDialog: (String) -> Unit
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
                    text = "প্যাকেজিং ও বারকোড স্লিপ (${packagingRecords.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Button(
                    onClick = onOpenPackagingDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("নতুন প্যাকেজিং তৈরি করুন")
                }
            }
        }

        if (packagingRecords.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "কোন প্যাকেজিং রেকর্ড নেই",
                    description = "গৃহীত পণ্যের জন্য কার্টুন ও প্যাকিং স্লিপ তৈরি করুন।"
                )
            }
        } else {
            items(packagingRecords) { pkg ->
                PackagingCard(
                    packaging = pkg,
                    onAuthorizeRelease = { onOpenReleaseDialog(pkg.inspectionId) }
                )
            }
        }
    }
}

@Composable
private fun PackagingCard(
    packaging: PackagingResponseDto,
    onAuthorizeRelease: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = packaging.packagingId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = packaging.packagingType,
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentCyan
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("মোট প্যাকেজড: ${packaging.totalPackagedQuantity}", style = MaterialTheme.typography.bodyMedium, color = AccentGreen, fontWeight = FontWeight.Bold)
                Text("কার্টুন সংখ্যা: ${packaging.totalPackageCount}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text("কার্টুন রেঞ্জ: ${packaging.cartonNumbersRange ?: "N/A"}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }

            Spacer(Modifier.height(6.dp))
            Surface(
                color = CardBgElevated,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "প্যাকিং স্লিপ বারকোড: ${packaging.packagingSlipBarcode}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = AccentCyan
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onAuthorizeRelease,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("ওয়্যারহাউস রিলিজ ও সার্টিফিকেট তৈরি করুন")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// TAB 3: WAREHOUSE RELEASE
// ─────────────────────────────────────────────────────────────

@Composable
private fun ReleasesTabContent(
    releases: List<FinishedGoodsReleaseResponseDto>,
    onOpenReleaseDialog: () -> Unit
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
                    text = "ওয়্যারহাউস রিলিজ সার্টিফিকেট (${releases.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Button(
                    onClick = onOpenReleaseDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("রিলিজ অনুমোদন করুন")
                }
            }
        }

        if (releases.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "কোন রিলিজ সার্টিফিকেট ইস্যু করা হয়নি",
                    description = "প্যাকেজড গুডস ওয়্যারহাউস বা ডিসপ্যাচে রিলিজ দিতে অনুমোদন করুন।"
                )
            }
        } else {
            items(releases) { release ->
                ReleaseCard(release = release)
            }
        }
    }
}

@Composable
private fun ReleaseCard(release: FinishedGoodsReleaseResponseDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = release.releaseId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Surface(
                    color = AccentGreen.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = release.status,
                        color = AccentGreen,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("রিলিজড পরিমাণ: ${release.releasedQuantity}", style = MaterialTheme.typography.bodyMedium, color = AccentGreen, fontWeight = FontWeight.Bold)
                Text("গন্তব্য: ${release.destination}", style = MaterialTheme.typography.bodyMedium, color = AccentCyan)
                Text("অনুমোদনকারী: ${release.authorizedBy}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }

            Spacer(Modifier.height(8.dp))
            Surface(
                color = CardBgElevated,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "SHA-256 ইন্টিগ্রিটি হ্যাশ:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = release.integrityHash,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = AccentPurple
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// TAB 4: RECONCILIATION
// ─────────────────────────────────────────────────────────────

@Composable
private fun ReconciliationTabContent(
    variance: FinalQcPackagingVarianceResponseDto?,
    reconciliation: FinalQcPackagingReconciliationResponseDto?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "৮-ওয়ে কোয়ালিটি ও প্যাকেজিং রিকনসিলিয়েশন অডিট",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
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
                        ReconCheckRow("১. শপ-ফ্লোর আউটপুট বনাম পরিদর্শন লট ব্যালেন্স", reconciliation.outputMatchedInspectionLot)
                        ReconCheckRow("২. স্যাম্পল প্ল্যান ধারাবাহিকতা ও কভারেজ", reconciliation.samplePlanConsistent)
                        ReconCheckRow("৩. ত্রুটি ও কোয়ারেন্টাইন অ্যাকাউন্টিং ব্যালেন্স", reconciliation.defectAccountingBalanced)
                        ReconCheckRow("৪. শূন্য আনকন্টেইন্ড ক্রিটিক্যাল ডিফেক্টস", reconciliation.zeroUncontainedCriticalDefects)
                        ReconCheckRow("৫. গৃহীত কোয়ালিটি বনাম প্যাকেজড পরিমাণ", reconciliation.packagingQuantityMatchesAccepted)
                        ReconCheckRow("৬. ক্রিপ্টোগ্রাফিক SHA-256 সার্টিফিকেট হ্যাশ ভেরিফিকেশন", reconciliation.releaseCertificateHashValid)
                        ReconCheckRow("৭. মাল্টি-টেন্যান্ট আইসোলেশন ও RLS বাউন্ডারি চেক", reconciliation.multiTenantIsolationVerified)
                    }
                }
            }
        }

        if (variance != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "কোয়ালিটি ও প্যাকেজিং ভেরিয়েন্স মেট্রিক্স",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("সামগ্রিক কোয়ালিটি ইল্ড (Yield):", color = TextSecondary)
                            Text("${variance.overallQualityYieldPercentage}%", color = AccentGreen, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ডিফেক্ট রেট (Defect Rate):", color = TextSecondary)
                            Text("${variance.defectRatePercentage}%", color = AccentRed, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("প্যাকেজিং ব্যালেন্স ভেরিয়েন্স:", color = TextSecondary)
                            Text("${variance.packagingBalanceVariance} units", color = AccentCyan, fontWeight = FontWeight.Bold)
                        }
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
private fun QualityKpiCard(
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
private fun StatusBadge(status: String) {
    val color = when (status) {
        "ACCEPTED" -> AccentGreen
        "CONDITIONALLY_ACCEPTED" -> AccentCyan
        "REWORK_REQUIRED" -> AccentOrange
        "REJECTED" -> AccentRed
        else -> TextSecondary
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = status,
            color = color,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun SeverityBadge(severity: String) {
    val color = when (severity) {
        "CRITICAL" -> AccentRed
        "MAJOR" -> AccentOrange
        "MINOR" -> AccentCyan
        else -> TextSecondary
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = severity,
            color = color,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
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
private fun CreateInspectionDialog(
    jobId: String,
    onDismiss: () -> Unit,
    onSubmit: (orderId: String, samplePlan: InspectionSamplePlanType, totalLot: BigDecimal, sampleSize: BigDecimal, inspId: String, inspName: String, notes: String?) -> Unit
) {
    var orderId by remember { mutableStateOf("ORD-$jobId") }
    var totalLot by remember { mutableStateOf("5000.0000") }
    var sampleSize by remember { mutableStateOf("200.0000") }
    var inspectorId by remember { mutableStateOf("INSP-01") }
    var inspectorName by remember { mutableStateOf("QC Lead Inspector") }
    var notes by remember { mutableStateOf("Standard print QC checklist verified") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন ফাইনাল কোয়ালিটি পরিদর্শন শুরু করুন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = orderId, onValueChange = { orderId = it }, label = { Text("অর্ডার আইডি") })
                OutlinedTextField(value = totalLot, onValueChange = { totalLot = it }, label = { Text("মোট লট পরিমাণ") })
                OutlinedTextField(value = sampleSize, onValueChange = { sampleSize = it }, label = { Text("স্যাম্পল সাইজ") })
                OutlinedTextField(value = inspectorName, onValueChange = { inspectorName = it }, label = { Text("ইন্সপেক্টর নাম") })
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("পরিদর্শন নোট") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lot = totalLot.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val smp = sampleSize.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    onSubmit(orderId, InspectionSamplePlanType.AQL_LEVEL_II_NORMAL, lot, smp, inspectorId, inspectorName, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
            ) { Text("পরিদর্শন শুরু করুন") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun RecordDefectDialog(
    inspectionId: String,
    onDismiss: () -> Unit,
    onSubmit: (String, ProductionStageType, DefectClassificationType, DefectSeverity, BigDecimal, ContainmentDisposition, String, String) -> Unit
) {
    var defectQty by remember { mutableStateOf("50.0000") }
    var location by remember { mutableStateOf("QUARANTINE_BAY_1") }
    var details by remember { mutableStateOf("Ink viscosity drop caused registration drift") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ত্রুটি ও কোয়ারেন্টাইন রেকর্ড করুন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("পরিদর্শন আইডি: $inspectionId", style = MaterialTheme.typography.bodySmall, color = AccentCyan)
                OutlinedTextField(value = defectQty, onValueChange = { defectQty = it }, label = { Text("ত্রুটি পরিমাণ (Defect Qty)") })
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("কোয়ারেন্টাইন লোকেশন") })
                OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("মূল কারণ বিবরণ") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = defectQty.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    onSubmit(inspectionId, ProductionStageType.PRINTING, DefectClassificationType.PRINTING_DEFECT, DefectSeverity.MAJOR, qty, ContainmentDisposition.QUARANTINED, location, details)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
            ) { Text("কোয়ারেন্টাইন নিশ্চিত করুন") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun CreatePackagingDialog(
    inspectionId: String,
    onDismiss: () -> Unit,
    onSubmit: (String, PackagingType, BigDecimal, Int, String?, String?, BigDecimal?, String, String?) -> Unit
) {
    var unitsPerPkg by remember { mutableStateOf("500.0000") }
    var pkgCount by remember { mutableStateOf("10") }
    var pallet by remember { mutableStateOf("PALLET-01") }
    var cartonRange by remember { mutableStateOf("BOX 01/10 to 10/10") }
    var packagedBy by remember { mutableStateOf("Shop Floor Packer Lead") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("প্যাকেজিং ও প্যাকিং স্লিপ তৈরি করুন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("পরিদর্শন আইডি: $inspectionId", style = MaterialTheme.typography.bodySmall, color = AccentCyan)
                OutlinedTextField(value = unitsPerPkg, onValueChange = { unitsPerPkg = it }, label = { Text("প্রতি প্যাকেজে ইউনিট") })
                OutlinedTextField(value = pkgCount, onValueChange = { pkgCount = it }, label = { Text("মোট কার্টুন সংখ্যা") })
                OutlinedTextField(value = cartonRange, onValueChange = { cartonRange = it }, label = { Text("কার্টুন নম্বর রেঞ্জ") })
                OutlinedTextField(value = packagedBy, onValueChange = { packagedBy = it }, label = { Text("প্যাকিং অপারেটর") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val upp = unitsPerPkg.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val cnt = pkgCount.toIntOrNull() ?: 1
                    onSubmit(inspectionId, PackagingType.CORRUGATED_BOX, upp, cnt, pallet, cartonRange, BigDecimal("25.0000"), packagedBy, "Packed & Barcoded")
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
            ) { Text("প্যাকিং নিশ্চিত করুন") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun AuthorizeReleaseDialog(
    inspectionId: String,
    packagingId: String,
    onDismiss: () -> Unit,
    onSubmit: (orderId: String, inspectionId: String, packagingId: String, qty: BigDecimal, dest: String, authBy: String, notes: String?) -> Unit
) {
    var releaseQty by remember { mutableStateOf("5000.0000") }
    var destination by remember { mutableStateOf("WAREHOUSE_FINISHED_GOODS") }
    var authorizedBy by remember { mutableStateOf("Plant QC Manager") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ওয়্যারহাউস রিলিজ ও সার্টিফিকেট অনুমোদন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("প্যাকেজিং আইডি: $packagingId", style = MaterialTheme.typography.bodySmall, color = AccentCyan)
                OutlinedTextField(value = releaseQty, onValueChange = { releaseQty = it }, label = { Text("রিলিজ পরিমাণ (Units)") })
                OutlinedTextField(value = destination, onValueChange = { destination = it }, label = { Text("গন্তব্য (Destination)") })
                OutlinedTextField(value = authorizedBy, onValueChange = { authorizedBy = it }, label = { Text("অনুমোদনকারী কর্মকর্তা") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = releaseQty.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    onSubmit("ORD-FINAL", inspectionId, packagingId, qty, destination, authorizedBy, "Certified for delivery")
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) { Text("SHA-256 সার্টিফাইড রিলিজ অনুমোদন") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun HandoffContractDialog(
    contract: Module17Step08FinalQcPackagingHandoffContractDto,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Module 17 Step 08 AI Handoff Contract") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Contract Version: ${contract.contractVersion}", color = AccentCyan, style = MaterialTheme.typography.bodySmall)
                Text("Job ID: ${contract.executionJobId}", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                Text("Status: ${contract.finalInspectionStatus}", color = AccentGreen, style = MaterialTheme.typography.bodySmall)
                Text("Accepted Qty: ${contract.totalGoodQuantityAccepted}", color = AccentGreen, style = MaterialTheme.typography.bodySmall)
                Text("Defect Qty: ${contract.totalDefectQuantity}", color = AccentRed, style = MaterialTheme.typography.bodySmall)
                Text("Quality Yield: ${contract.qualityYieldPercentage}%", color = AccentGreen, style = MaterialTheme.typography.bodySmall)
                Text("Total Cartons: ${contract.totalPackagedCartons}", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                Text("Barcode: ${contract.packagingSlipBarcode}", color = AccentCyan, style = MaterialTheme.typography.bodySmall)
                Text("Release Status: ${contract.releaseStatus}", color = AccentPurple, style = MaterialTheme.typography.bodySmall)
                Text("SHA-256 Hash: ${contract.releaseCertificateHash}", color = AccentPurple, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("ঠিক আছে") }
        }
    )
}
