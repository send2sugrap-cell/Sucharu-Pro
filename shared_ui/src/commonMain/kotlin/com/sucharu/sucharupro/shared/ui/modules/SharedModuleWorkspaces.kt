package com.sucharu.sucharupro.shared.ui.modules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Real Canonical Module Workspaces for Sucharu Pro ERP.
 * Integrates Module 03 (Sales Orders), Module 04 (13-Stage Production & Worker UI),
 * Module 07 (Finished Goods), Module 08/11 (Delivery Challan & Gate Pass), Module 09/14 (Finance & Bangla QR),
 * Module 12 (Vendor Subcontracting), Module 13 (Central Procurement), Module 18 (Prepress Imposition),
 * Module 19 (Substrate Stock Reservation), Module 20 (Affiliate), Module 21 (Machine OEE), Module 24 (Bengali PDF).
 */
@Composable
fun RouterCanonicalModuleWorkspace(
    moduleCode: String,
    onClose: () -> Unit
) {
    when (moduleCode) {
        "Module 03", "Orders" -> SalesOrderModuleScreen(onClose = onClose)
        "Module 04", "Production" -> ProductionWorkflowModuleScreen(onClose = onClose)
        "Module 07", "Module 08/11", "Module 08", "Module 24", "Delivery" -> FinishedGoodsAndDeliveryChallanModuleScreen(onClose = onClose)
        "Module 09/14", "Finance" -> FinanceBillingModuleScreen(onClose = onClose)
        "Module 12", "Module 13", "Procurement" -> CentralProcurementHubModuleScreen(onClose = onClose)
        "Module 18", "Module 16" -> PrepressImpositionModuleScreen(onClose = onClose)
        "Module 19" -> SubstrateStockReservationModuleScreen(onClose = onClose)
        "Module 20", "Affiliate" -> AffiliateGovernanceModuleScreen(onClose = onClose)
        "Module 21" -> MachineOeeTelemetryModuleScreen(onClose = onClose)
        else -> UniversalCanonicalModuleScreen(moduleCode = moduleCode, onClose = onClose)
    }
}

// ============================================================================
// MODULE 03: SALES QUOTATION & ORDER MANAGEMENT SCREEN
// ============================================================================
@Composable
fun SalesOrderModuleScreen(onClose: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var calculationIdInput by remember { mutableStateOf("") }
    var customerNameInput by remember { mutableStateOf("") }
    var jobTitleInput by remember { mutableStateOf("") }
    var quantityInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var advanceStatusInput by remember { mutableStateOf("PENDING_ADVANCE") }

    var showBanglaQrDialog by remember { mutableStateOf(false) }
    var activePaymentOrder: SalesOrderItem? by remember { mutableStateOf(null) }

    var ordersList by remember {
        mutableStateOf(
            listOf(
                SalesOrderItem("SO-2026-881", "CALC-2026-904", "আহমেদ ট্রেডার্স", "১,০০০ পিস বুক ক্যাটালগ", "৳ ৪,৫০০", "২০২৬-১০-০৫", "৫০% অগ্রিম বাকি", "PENDING_ADVANCE (লকড)", Color(0xFFEF4444)),
                SalesOrderItem("SO-2026-880", "CALC-2026-812", "সুমন এন্টারপ্রাইজ", "৫,০০০ পিস ক্যাশ মেমো", "৳ ৭,২০০", "২০২৬-১০-০৪", "পূর্ণ পরিশোধিত", "ADVANCE_PAID (আনলকড)", Color(0xFF10B981)),
                SalesOrderItem("SO-2026-879", "CALC-2026-778", "আইটি ভিশন লিঃ", "৫০০ পিস ভিজটিং কার্ড", "৳ ১,২০০", "২০২৬-১০-০৪", "পূর্ণ পরিশোধিত", "সম্পন্ন", Color(0xFF00F0FF)),
                SalesOrderItem("SO-2026-878", "CALC-2026-650", "গ্রিন মাল্টিমিডিয়া", "২,০০০ পিস ফ্লায়ার", "৳ ৩,৮০০", "২০২৬-১০-০৩", "অগ্রিম বকেয়া", "পেমেন্ট বকেয়া", Color(0xFFEF4444))
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF040914))
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ModuleTopBar(
                code = "Module 03",
                title = "কোটেশন ইনটেক, সেলস অর্ডার ও কমার্শিয়াল এডভান্স লক (Sales Orders)",
                onClose = onClose
            )

            // KPI Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ModuleKpiChip("আজকের মোট সেলস", "৳ ১,৩৭,৫০০", "৮৪টি অর্ডার", Color(0xFF00F0FF), Modifier.weight(1f))
                ModuleKpiChip("অনুমোদিত কোটেশন", "৬১টি", "৭২% কনভার্সন", Color(0xFF10B981), Modifier.weight(1f))
                ModuleKpiChip("এডভান্স লকড অর্ডার", "১৮টি", "৫০% অগ্রিম প্রয়োজন", Color(0xFFEF4444), Modifier.weight(1f))
            }

            // Task 10.1: Commercial Advance Payment Lock Warning Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1010)),
                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "কমার্শিয়াল এডভান্স পেমেন্ট লক ইঞ্জিন (Advance Lock Gatekeeper)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        Text(text = "সর্বনিম্ন ৫০% অগ্রিম পেমেন্ট জমা না হওয়া পর্যন্ত কোনো জব কার্ড কারখানা ফ্লোরে (Module 04) রিলিজ করা যাবে না।", fontSize = 11.sp, color = Color.White)
                    }
                }
            }

            // New Order Intake Form
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
                border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "+ নতুন কোটেশন ও সেলস অর্ডার ইনটেক ফর্ম (Sequence: SO-2026-XXXX)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModuleInputField("ক্যালকুলেশন আইডি (Optional)", calculationIdInput, { calculationIdInput = it }, Modifier.weight(0.8f))
                        ModuleInputField("কাস্টমারের নাম", customerNameInput, { customerNameInput = it }, Modifier.weight(1f))
                        ModuleInputField("জব টাইটেল / পণ্য", jobTitleInput, { jobTitleInput = it }, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModuleInputField("পরিমাণ (Quantity)", quantityInput, { quantityInput = it }, Modifier.weight(1f))
                        ModuleInputField("চুক্তি মূল্য (BDT)", amountInput, { amountInput = it }, Modifier.weight(1f))
                        ModuleInputField("অগ্রিম স্ট্যাটাস", advanceStatusInput, { advanceStatusInput = it }, Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = Color(0xFF0284C7),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable {
                            if (customerNameInput.isNotBlank() && jobTitleInput.isNotBlank()) {
                                val newOrderId = "SO-2026-${(882..999).random()}"
                                val calcId = calculationIdInput.ifBlank { "CALC-2026-${(100..999).random()}" }
                                ordersList = listOf(
                                    SalesOrderItem(
                                        id = newOrderId,
                                        calcId = calcId,
                                        customer = customerNameInput,
                                        jobTitle = jobTitleInput,
                                        amount = "৳ ${amountInput.ifBlank { "২,৫০০" }}",
                                        deliveryDate = "২০২৬-১০-১০",
                                        advanceStatus = advanceStatusInput,
                                        status = "PENDING_ADVANCE (লকড)",
                                        statusColor = Color(0xFFEF4444)
                                    )
                                ) + ordersList
                                customerNameInput = ""
                                jobTitleInput = ""
                                quantityInput = ""
                                amountInput = ""
                                calculationIdInput = ""
                            }
                        }
                    ) {
                        Text(
                            text = "অর্ডার সেভ করুন (PENDING_ADVANCE Lock Enabled)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Search & Orders Table
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
                border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "লাইভ সেলস অর্ডার রেজিস্ট্রি (${ordersList.size}টি রেকর্ড)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        ModuleInputField("খুঁজুন...", searchQuery, { searchQuery = it }, Modifier.width(200.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ordersList.forEach { order ->
                        SalesOrderRow(
                            order = order,
                            onPayBanglaQr = {
                                activePaymentOrder = order
                                showBanglaQrDialog = true
                            }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        // Task 10.2: Dynamic Bangla QR Code Modal Dialog
        if (showBanglaQrDialog && activePaymentOrder != null) {
            val order = activePaymentOrder!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { showBanglaQrDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
                    border = BorderStroke(2.dp, Color(0xFF00F0FF)),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clickable { /* prevent dismiss */ }
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "ডাইনামিক বাংলা কিউআর পেমেন্ট (EMVCo Bangla QR)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp).clickable { showBanglaQrDialog = false }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Dynamic QR Vector Display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "অর্ডার: ${order.id} • ${order.customer}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(text = "৫০% অগ্রিম পেমেন্ট: ৳ ২,২৫০.০০", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                                Text(text = "মার্চেন্ট: Sucharu Graphics (bKash/Nagad/BRAC)", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            }

                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(72.dp)) {
                                    val w = size.width
                                    val h = size.height
                                    drawRect(color = Color.Black, topLeft = Offset(0f, 0f), size = Size(w * 0.35f, h * 0.35f))
                                    drawRect(color = Color.Black, topLeft = Offset(w * 0.65f, 0f), size = Size(w * 0.35f, h * 0.35f))
                                    drawRect(color = Color.Black, topLeft = Offset(0f, h * 0.65f), size = Size(w * 0.35f, h * 0.35f))
                                    drawRect(color = Color.Black, topLeft = Offset(w * 0.4f, h * 0.4f), size = Size(w * 0.2f, h * 0.2f))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(text = "EMVCo Payload: 00020101021226310011BKASH0170000000052042741530305054072250.005802BD5916Sucharu Graphics6005Dhaka62150111INV-2026-8816304D1B9", fontSize = 9.sp, color = Color(0xFF00F0FF), lineHeight = 12.sp)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Task 10.3 & 10.4: Simulate IPN Callback & Auto-Unlock
                        Surface(
                            color = Color(0xFF10B981),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                // Transition order status to ADVANCE_PAID and unlock Job Card
                                ordersList = ordersList.map { item ->
                                    if (item.id == order.id) {
                                        item.copy(status = "ADVANCE_PAID (আনলকড)", statusColor = Color(0xFF10B981), advanceStatus = "৫০% পরিশোধিত (Trx: TRX-BKASH-8877)")
                                    } else item
                                }
                                showBanglaQrDialog = false
                            }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text(text = "✔ পেমেন্ট ভেরিফাই ও জব আনলক করুন (IPN Callback + Double Entry GL)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class SalesOrderItem(
    val id: String,
    val calcId: String,
    val customer: String,
    val jobTitle: String,
    val amount: String,
    val deliveryDate: String,
    val advanceStatus: String,
    val status: String,
    val statusColor: Color
)

@Composable
private fun SalesOrderRow(
    order: SalesOrderItem,
    onPayBanglaQr: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF111C33),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "${order.id} • ${order.customer}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "${order.jobTitle} • ক্যালি আইডি: ${order.calcId} • ডেলিভারি: ${order.deliveryDate}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                Text(text = "অগ্রিম: ${order.advanceStatus}", fontSize = 10.sp, color = if (order.advanceStatus.contains("পরিশোধিত")) Color(0xFF10B981) else Color(0xFFEF4444), fontWeight = FontWeight.Bold)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = order.amount, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF00F0FF))
                Spacer(modifier = Modifier.width(10.dp))

                if (order.status.contains("PENDING_ADVANCE") || order.status.contains("লকড")) {
                    Surface(
                        color = Color(0xFF10B981),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { onPayBanglaQr() }
                    ) {
                        Text(text = "পেমেন্ট করুন (QR)", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                } else {
                    Surface(
                        color = order.statusColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, order.statusColor)
                    ) {
                        Text(text = order.status, fontSize = 10.sp, color = order.statusColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
        }
    }
}

// ============================================================================
// MODULE 04: 13-STAGE PRODUCTION EXECUTION & SHOP-FLOOR WORKER UI SCREEN
// ============================================================================
@Composable
fun ProductionWorkflowModuleScreen(onClose: () -> Unit) {
    var activeJobTicket: JobCardItem? by remember { mutableStateOf(null) }

    // Task 8.2: Tactile Worker Execution State
    var workerOperatorId by remember { mutableStateOf("OP-102 (রফিক)") }
    var workerGoodSheets by remember { mutableStateOf("9500") }
    var workerWasteSheets by remember { mutableStateOf("500") }
    var activeWorkerStage by remember { mutableStateOf("PRINTING") }

    // Task 9.1: Multi-Stage Vendor Subcontracting Assignments
    var paperVendorInput by remember { mutableStateOf("প্যারামাউন্ট পেপার হাউস") }
    var paperCommitmentInput by remember { mutableStateOf("৳ ৩৬,০০০ (২০ রিম @ ৳ ১,৮০০)") }

    var ctpVendorInput by remember { mutableStateOf("আলমগীর সিটিপি বিউরো") }
    var ctpCommitmentInput by remember { mutableStateOf("৳ ১,৪োর (৪টি প্লেট @ ৳ ৩৫০)") }

    var pressOutsourcedInput by remember { mutableStateOf("নিউ ঢাকা অফসেট প্রেস (আউটসোর্সড)") }
    var pressCommitmentInput by remember { mutableStateOf("৳ ৪,০০০ (১০,০০০ imp @ ৳ ৪০০/k)") }

    var bindingVendorInput by remember { mutableStateOf("সাগর বাইন্ডিং ওয়ার্কস") }
    var bindingCommitmentInput by remember { mutableStateOf("৳ ২,৫০০ (পারফেক্ট বাইন্ডিং)") }

    val canonical13Stages = listOf(
        "DSN", "APR", "QC", "IA", "CTP", "PRT", "LAM", "FLD", "BND", "FQC", "PKG", "RDY", "DLV"
    )

    var jobCardsList by remember {
        mutableStateOf(
            listOf(
                JobCardItem("JC-2026-102", "SO-2026-881", "আহমেদ ট্রেডার্স", "বুক ক্যাটালগ ১০০০০ পিস", "Art Paper 150 GSM (20x30\")", "CMYK 4-Color (4 Plates)", "Heidelberg Speedmaster", "Thermal Matt + Die-Cut + Perfect Bind", "PRINTING", "৬. প্রিন্টিং (৭৫%)", "রফিক", Color(0xFF10B981)),
                JobCardItem("JC-2026-101", "SO-2026-880", "সুমন এন্টারপ্রাইজ", "বক্স প্যাকেজিং ৫০০০ পিস", "Box Board 300 GSM (25x37\")", "CMYK 4-Color (4 Plates)", "Automatic Die-Cutter", "Thermal Gloss + Die-Cut + Pasting", "LAMINATION", "৭. ল্যামিনেশন (৪০%)", "করিম", Color(0xFFF59E0B)),
                JobCardItem("JC-2026-100", "SO-2026-879", "আইটি ভিশন লিঃ", "ক্যালেন্ডার ২০২৬ ২০০০ পিস", "Art Card 300 GSM (23x36\")", "CMYK 4-Color (4 Plates)", "Wire-O Binder", "Thermal Gloss + Wire-O Bind", "FINAL_QC", "১০. ফাইনাল কিউসি", "জামাল", Color(0xFF00F0FF)),
                JobCardItem("JC-2026-099", "SO-2026-878", "গ্রিন মাল্টিমিডিয়া", "ফ্লায়ার ২০০০ পিস", "Art Paper 120 GSM (20x30\")", "2-Color (2 Plates)", "Offset Single Color", "Folding", "CTP", "৫. সিটিপি প্লেট মেকিং", "রহিম", Color(0xFFEF4444))
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF040914))
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ModuleTopBar(
                code = "Module 04",
                title = "১৩-স্টেপ ক্যানোনিকাল প্রডাকশন পাইপলাইন ও শপ-ফ্লোর ওয়ার্কার ইন্টারফেস",
                onClose = onClose
            )

            // Task 8.1: Canonical 13-Stage Production Pipeline Tracker
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
                border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "১৩-ধাপের ক্যানোনিকাল প্রডাকশন পাইপলাইন (Canonical 13-Stage Pipeline)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        canonical13Stages.forEachIndexed { idx, st ->
                            val isPassed = idx < 6
                            val isCurrent = idx == 5
                            Surface(
                                color = when {
                                    isCurrent -> Color(0xFF10B981)
                                    isPassed -> Color(0xFF0284C7).copy(alpha = 0.4f)
                                    else -> Color(0xFF132038)
                                },
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, if (isCurrent) Color(0xFF10B981) else Color(0xFF1E293B)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = st,
                                    fontSize = 8.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Task 9.1: Multi-Stage Vendor Subcontracting Assignments Panel (Module 12)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
                border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "মাল্টি-স্টেপ ভেন্ডর সাবকন্ট্রাক্টিং ও ওয়ার্ক অর্ডার জেনারেটর (Module 12)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA855F7))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModuleInputField("ক) পেপার সাপ্লায়ার ভেন্ডর", paperVendorInput, { paperVendorInput = it }, Modifier.weight(1f))
                        ModuleInputField("কাগজ বাবদ কস্ট (WO-2026-102-PAPER)", paperCommitmentInput, { paperCommitmentInput = it }, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModuleInputField("খ) সিটিপি প্লেট বিউরো ভেন্ডর", ctpVendorInput, { ctpVendorInput = it }, Modifier.weight(1f))
                        ModuleInputField("সিটিপি বিল (WO-2026-102-CTP)", ctpCommitmentInput, { ctpCommitmentInput = it }, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModuleInputField("গ) আউটসোর্সড প্রেস ভেন্ডর", pressOutsourcedInput, { pressOutsourcedInput = it }, Modifier.weight(1f))
                        ModuleInputField("প্রিন্টিং ছাপা বিল (WO-2026-102-PRESS)", pressCommitmentInput, { pressCommitmentInput = it }, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModuleInputField("ঘ) বাইন্ডিং ও পোস্ট-প্রেস ভেন্ডর", bindingVendorInput, { bindingVendorInput = it }, Modifier.weight(1f))
                        ModuleInputField("বাইন্ডিং চুক্তি (WO-2026-102-BIND)", bindingCommitmentInput, { bindingCommitmentInput = it }, Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = Color(0xFFA855F7),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { /* Issue Work Orders */ }
                    ) {
                        Text(
                            text = "ওয়ার্ক অর্ডার ইস্যু করুন (Auto Work Order Sequences: WO-2026-XXXX-STEP)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Task 8.2: Tactile Shop-Floor Tablet / Smartphone Worker Interface (min 56dp touch target)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF06152B)),
                border = BorderStroke(2.dp, Color(0xFF10B981)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "শপ-ফ্লোর প্রেস রুম ওয়ার্কার ইন্টারফেস (Tactile Worker UI)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                        Surface(color = Color(0xFF10B981).copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                            Text(text = "লাইভ অপারেটর মোড (Min 56dp Target)", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModuleInputField("অপারেটর নাম/আইডি", workerOperatorId, { workerOperatorId = it }, Modifier.weight(1f))
                        ModuleInputField("ভালো প্রস্তুতকৃত শিট", workerGoodSheets, { workerGoodSheets = it }, Modifier.weight(1f))
                        ModuleInputField("ওয়েস্ট/নষ্ট শিট", workerWasteSheets, { workerWasteSheets = it }, Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Single-Tap Large Touch Target Action Buttons (Min 56dp height)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            color = Color(0xFF0284C7),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp)
                                .clickable {
                                    activeWorkerStage = "PRINTING_RUNNING"
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "▶ স্টেজ শুরু করুন (START STAGE)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            color = Color(0xFF10B981),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp)
                                .clickable {
                                    activeWorkerStage = "STAGE_COMPLETED"
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "✔ স্টেজ সম্পন্ন করুন (COMPLETE)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Active Job Cards Table
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
                border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "কারখানা লাইভ জব কার্ড (Job Bag) ট্র্যাকার (${jobCardsList.size}টি রানিং জব)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Surface(
                            color = Color(0xFF0284C7),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable {
                                val newJc = JobCardItem(
                                    id = "JC-2026-${(103..999).random()}",
                                    orderId = "SO-2026-${(882..999).random()}",
                                    customer = "নতুন কাস্টমার",
                                    title = "কাস্টম প্রিন্টিং জব",
                                    substrate = "Art Paper 150 GSM",
                                    ctp = "CMYK 4-Color (4 Plates)",
                                    machine = "Offset Press",
                                    finishing = "Lamination & Trim",
                                    stageCode = "CTP",
                                    stageLabel = "৫. সিটিপি প্লেট মেকিং",
                                    operator = "অপারেটর অসামঞ্জস্য",
                                    statusColor = Color(0xFFEF4444)
                                )
                                jobCardsList = listOf(newJc) + jobCardsList
                            }
                        ) {
                            Text(text = "+ অটো জব কার্ড জেনারেট করুন", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    jobCardsList.forEach { jc ->
                        JobCardRow(
                            jc = jc,
                            onAdvanceStage = {
                                val nextStage = getNextStage(jc.stageCode)
                                val updatedList = jobCardsList.map { item ->
                                    if (item.id == jc.id) {
                                        item.copy(stageCode = nextStage.first, stageLabel = nextStage.second, statusColor = nextStage.third)
                                    } else item
                                }
                                jobCardsList = updatedList
                            },
                            onPrintTicket = {
                                activeJobTicket = jc
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Task 6.4: Printable Job Ticket / QR Code Modal Dialog
        if (activeJobTicket != null) {
            val ticket = activeJobTicket!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { activeJobTicket = null },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
                    border = BorderStroke(2.dp, Color(0xFF00F0FF)),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clickable { /* prevent click dismiss */ }
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "প্রিন্টাবল জব টিকিট ও শপ-ফ্লোর কিউআর ব্যাগ (Job Bag)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp).clickable { activeJobTicket = null }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Job Bag Header
                        Surface(
                            color = Color(0xFF111C33),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF00B4D8)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "জব আইডি: ${ticket.id} (${ticket.orderId})", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF00F0FF))
                                    Text(text = "কাস্টমার: ${ticket.customer} • বিবরণ: ${ticket.title}", fontSize = 11.sp, color = Color.White)
                                    Text(text = "অপারেটর: ${ticket.operator} • স্টেজ: ${ticket.stageLabel}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                }

                                // Shop-Floor Vector QR Graphic
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.size(48.dp)) {
                                        val w = size.width
                                        val h = size.height
                                        drawRect(color = Color.Black, topLeft = Offset(0f, 0f), size = Size(w * 0.35f, h * 0.35f))
                                        drawRect(color = Color.Black, topLeft = Offset(w * 0.65f, 0f), size = Size(w * 0.35f, h * 0.35f))
                                        drawRect(color = Color.Black, topLeft = Offset(0f, h * 0.65f), size = Size(w * 0.35f, h * 0.35f))
                                        drawRect(color = Color.Black, topLeft = Offset(w * 0.4f, h * 0.4f), size = Size(w * 0.2f, h * 0.2f))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Job Technical Specs Table
                        Text(text = "কারখানা টেকনিক্যাল স্পেসিফিকেশন ব্রেকডাউন:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF))
                        Spacer(modifier = Modifier.height(6.dp))

                        SpecDetailRow("সাবস্ট্রেট (Paper)", ticket.substrate)
                        SpecDetailRow("সিটিপি / প্লেট (CTP)", ticket.ctp)
                        SpecDetailRow("প্রিন্টিং প্রেস (Machine)", ticket.machine)
                        SpecDetailRow("পোস্ট-প্রেস ফিনিশিং", ticket.finishing)

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                color = Color(0xFF10B981),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).clickable { activeJobTicket = null }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                    Text(text = "প্রিন্ট টিকিট (Print Job Bag)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                color = Color(0xFF334155),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).clickable { activeJobTicket = null }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                    Text(text = "বন্ধ করুন", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
private fun SpecDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = Color(0xFF94A3B8))
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

private data class JobCardItem(
    val id: String,
    val orderId: String,
    val customer: String,
    val title: String,
    val substrate: String,
    val ctp: String,
    val machine: String,
    val finishing: String,
    val stageCode: String,
    val stageLabel: String,
    val operator: String,
    val statusColor: Color
)

@Composable
private fun JobCardRow(
    jc: JobCardItem,
    onAdvanceStage: () -> Unit,
    onPrintTicket: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF111C33),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "${jc.id} (${jc.orderId}) • ${jc.customer}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "${jc.title} • ${jc.machine}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                }

                Surface(
                    color = jc.statusColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, jc.statusColor)
                ) {
                    Text(text = jc.stageLabel, fontSize = 10.sp, color = jc.statusColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "স্পেক্স: ${jc.substrate} | ${jc.ctp} | ${jc.finishing}", fontSize = 10.sp, color = Color(0xFF00F0FF))

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF0284C7),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable { onAdvanceStage() }
                ) {
                    Text(text = "পরবর্তী স্টেজ ➔", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }

                Surface(
                    color = Color(0xFF132038),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFF00B4D8)),
                    modifier = Modifier.clickable { onPrintTicket() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "প্রিন্টিং টিকিট (QR)", color = Color(0xFF00F0FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun getNextStage(currentStage: String): Triple<String, String, Color> {
    return when (currentStage) {
        "CTP" -> Triple("PRINTING", "৬. প্রিন্টিং চলমান", Color(0xFF10B981))
        "PRINTING" -> Triple("LAMINATION", "৭. ল্যামিনেশন চলমান", Color(0xFFF59E0B))
        "LAMINATION" -> Triple("FOLDING", "৮. ফোল্ডিং চলমান", Color(0xFF38BDF8))
        "FOLDING" -> Triple("BINDING", "৯. বাইন্ডিং চলমান", Color(0xFFA855F7))
        "BINDING" -> Triple("FINAL_QC", "১০. ফাইনাল কিউসি", Color(0xFF00F0FF))
        "FINAL_QC" -> Triple("PACKAGING", "১১. প্যাকিং সম্পন্ন", Color(0xFF10B981))
        "PACKAGING" -> Triple("READY", "১২. ডেলিভারির জন্য প্রস্তুত", Color(0xFF10B981))
        else -> Triple("DELIVERED", "১৩. ডেলিভার্ড সম্পন্ন", Color(0xFF10B981))
    }
}

// ============================================================================
// MODULE 07 / 08 / 11 / 24: FINISHED GOODS, DELIVERY CHALLAN & BENGALI PDF
// ============================================================================
@Composable
fun FinishedGoodsAndDeliveryChallanModuleScreen(onClose: () -> Unit) {
    var dispatchQuantityInput by remember { mutableStateOf("500") }
    var receiverNameInput by remember { mutableStateOf("তানভির হোসেন (আহমেদ ট্রেডার্স)") }
    var courierTrackingInput by remember { mutableStateOf("STEADFAST-998877") }
    var showPrintableChallanDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040914))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ModuleTopBar(
            code = "Module 07/08/11/24",
            title = "ফিনিশড গুডস ইনভেন্টরি, ডেলিভারি চালান ও বাংলা ইউনিকোড পিডিএফ",
            onClose = onClose
        )

        // Task 11.2: Due Settlement Delivery Lock Gatekeeper Warning Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1010)),
            border = BorderStroke(1.dp, Color(0xFFEF4444)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "বকেয়া সেটেলমেন্ট ডেলিভারি লক (Due Settlement Dispatch Lock Engine)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                    Text(text = "বকেয়া বিল (Net Due > 0) পরিশোধ না করা পর্যন্ত ডেলিভারি চালান ও গেটপাস জেনারেট সম্পূর্ণ নিষিদ্ধ। (কাস্টমার ক্রেডিট পলিসি না থাকলে চাবিকুঞ্জ লকড)", fontSize = 11.sp, color = Color.White)
                }
            }
        }

        // Task 11.1: Finished Goods Inventory Inward & Lot Tracking
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
            border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "১. ফিনিশড গুডস ইনভেন্টরি লট রেজিস্ট্রি (Module 07 Inventory Lot Inward)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF))
                Spacer(modifier = Modifier.height(10.dp))

                FgLotRow("FG-2026-102-L1", "বুক ক্যাটালগ (SO-2026-881)", "১,০০০ পিস (২০টি কার্টন)", "অন-হ্যান্ড FG স্টক: ৫১০ পিস", "৫০% প্রস্তুত", Color(0xFF10B981))
                Spacer(modifier = Modifier.height(6.dp))
                FgLotRow("FG-2026-101-L1", "বক্স প্যাকেজিং (SO-2026-880)", "৫,০০০ পিস (১০০টি বন্ডল)", "অন-হ্যান্ড FG স্টক: ৫,০০০ পিস", "১০০% প্রস্তুত", Color(0xFF10B981))
            }
        }

        // Task 11.3: Delivery Challan & Gate Pass Dispatch Hub
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
            border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "২. ডেলিভারি চালান ও গেটপাস ক্রিয়েশন ফর্ম (Task 11.3 & 11.4)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF))
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModuleInputField("গ্রাহক/প্রাপকের নাম", receiverNameInput, { receiverNameInput = it }, Modifier.weight(1f))
                    ModuleInputField("ডেলিভারি পরিমাণ (Pcs)", dispatchQuantityInput, { dispatchQuantityInput = it }, Modifier.weight(1f))
                    ModuleInputField("কুরিয়ার / ড্রাইভার আইডি", courierTrackingInput, { courierTrackingInput = it }, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = Color(0xFF0284C7),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { showPrintableChallanDialog = true }
                    ) {
                        Text(
                            text = "ডেলিভারি চালান ও সিকিউর গেটপাস জেনারেট করুন (DC-2026-XXXX / GP-2026-XXXX)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }

    // Task 11.4: Bengali Unicode Challan, Cash Memo & Gate Pass PDF Print Modal
    if (showPrintableChallanDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable { showPrintableChallanDialog = false },
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
                border = BorderStroke(2.dp, Color(0xFF00F0FF)),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clickable { /* prevent dismiss */ }
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "প্রিন্টাবল বাংলা ইউনিকোড ডেলিভারি চালান ও গেটপাস (Kalpurush / Nikosh)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp).clickable { showPrintableChallanDialog = false }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bengali Unicode Document Canvas Preview
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "সুচারু গ্রাফিক্স অ্যান্ড প্রিন্টিং প্রেস", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            Text(text = "৬৮/১ পুরানা পল্টন লাইন, ঢাকা-১০০০ • ফোন: ০১৭০০-০০০০০", fontSize = 10.sp, color = Color.DarkGray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Black))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "চালান নং: DC-2026-881\nগেটপাস নং: GP-2026-881", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text(text = "তারিখ: ০৫ অক্টোবর ২০২৬\nট্র্যাকিং: STEADFAST-998877", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(text = "প্রাপক: আহমেদ ট্রেডার্স (স্বত্বাধিকারী: তানভির হোসেন)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)

                            Spacer(modifier = Modifier.height(10.dp))

                            // Table
                            Surface(
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Color.Black),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = "বিবরণ ও স্পেসিফিকেশন", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        Text(text = "ডেলিভারি পরিমাণ (যুক্তাক্ষর: ক্ত, ক্ষ, ষ্ণ)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = "বুক ক্যাটালগ (Art Paper 150 GSM, CMYK 4-Color, Perfect Bind)", fontSize = 10.sp, color = Color.Black)
                                        Text(text = "৫০০ পিস (১০টি কার্টন)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "____________________\nপ্রস্তুতকারীর স্বাক্ষর", fontSize = 10.sp, color = Color.Black, textAlign = TextAlign.Center)
                                Text(text = "____________________\nপ্রাপকের স্বাক্ষর ও সিল", fontSize = 10.sp, color = Color.Black, textAlign = TextAlign.Center)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            color = Color(0xFF10B981),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).clickable { showPrintableChallanDialog = false }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text(text = "প্রিন্ট চালান (PDF / Print)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(
                            color = Color(0xFF334155),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).clickable { showPrintableChallanDialog = false }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text(text = "বন্ধ করুন", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FgLotRow(lotId: String, name: String, totalPack: String, fgStock: String, progress: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF111C33),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "$lotId • $name", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "প্যাকিং: $totalPack • $fgStock", fontSize = 10.sp, color = Color(0xFF94A3B8))
            }

            Surface(color = color.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                Text(text = progress, fontSize = 9.sp, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
}

// ============================================================================
// MODULE 12/13: CENTRAL PROCUREMENT HUB & VENDOR SUBCONTRACTING SCREEN
// ============================================================================
@Composable
fun CentralProcurementHubModuleScreen(onClose: () -> Unit) {
    var isVendorScopedView by remember { mutableStateOf(false) }

    val procurementSteps = listOf(
        "WO Issued", "Accepted", "ASN Notice", "GRN Inward", "3-Way Match", "QC Passed", "Clearance Queue"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040914))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ModuleTopBar(
            code = "Module 12/13",
            title = "সেন্ট্রাল প্রকিউরমেন্ট হাব, ভেন্ডর সাবকন্ট্রাক্টিং ও ৩-ওয়ে ম্যাচিং",
            onClose = onClose
        )

        // View Mode Selector (Admin Procurement vs Subcontractor Scoped View)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                color = if (!isVendorScopedView) Color(0xFF0284C7) else Color(0xFF1E293B),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable { isVendorScopedView = false }
            ) {
                Text(text = "অ্যাডমিন প্রকিউরমেন্ট ভিউ", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
            Surface(
                color = if (isVendorScopedView) Color(0xFFA855F7) else Color(0xFF1E293B),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable { isVendorScopedView = true }
            ) {
                Text(text = "সাবকন্ট্রাক্টর ভেন্ডর ওয়ার্কস্পেস ভিউ (Data Masked)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }

        if (!isVendorScopedView) {
            // Task 9.2: 27-Stage Procurement Hub Lifecycle Stepper
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
                border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "প্রকিউরমেন্ট ও সাবকন্ট্রাক্ট লাইফসাইকেল ট্র্যাকার (27-Stage Pipeline)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        procurementSteps.forEachIndexed { idx, st ->
                            val isCompleted = idx <= 4
                            Surface(
                                color = if (isCompleted) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFF132038),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, if (isCompleted) Color(0xFF10B981) else Color(0xFF1E293B)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = st,
                                    fontSize = 8.sp,
                                    color = if (isCompleted) Color.White else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Task 9.3: Zero Shadow Ledger Double-Entry AP Posting
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "জিরো শ্যাডো লেজার ডাবল-এন্ট্রি একাউন্টিং স্ট্যাটাস (Zero Shadow Ledger)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "DEBIT: Job Direct Costs / WIP (Module 18) • ৳ ৪৩,৯০০\nCREDIT: Vendor Accounts Payable (Module 15 GL) • ৳ ৪৩,৯০০",
                        fontSize = 11.sp,
                        color = Color.White,
                        lineHeight = 16.sp
                    )
                }
            }

            // Active Work Orders Table
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
                border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "সক্রিয় সাবকন্ট্রাক্ট ওয়ার্ক অর্ডার রেজিস্ট্রি (Active Work Orders)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))

                    ProcurementWoRow("WO-2026-102-PAPER", "প্যারামাউন্ট পেপার হাউস", "Art Paper 150 GSM (20 Reams)", "৳ ৩৬,০০০", "GRN verified (3-Way Match OK)", Color(0xFF10B981))
                    Spacer(modifier = Modifier.height(6.dp))
                    ProcurementWoRow("WO-2026-102-CTP", "আলমগীর সিটিপি বিউরো", "4-Color CTP Plates (4 Plates)", "৳ ১,৪০০", "3-Way Matched", Color(0xFF10B981))
                    Spacer(modifier = Modifier.height(6.dp))
                    ProcurementWoRow("WO-2026-102-PRESS", "নিউ ঢাকা অফসেট প্রেস", "10,000 Impressions Run", "৳ ৪,০০০", "ASN Received", Color(0xFFF59E0B))
                    Spacer(modifier = Modifier.height(6.dp))
                    ProcurementWoRow("WO-2026-102-BIND", "সাগর বাইন্ডিং ওয়ার্কস", "Perfect Binding 10,000 Copies", "৳ ২,৫০০", "WO Issued", Color(0xFF38BDF8))
                }
            }
        } else {
            // Task 9.4: Scoped Vendor Subcontractor View (Strict Customer Data Masking)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
                border = BorderStroke(1.dp, Color(0xFFA855F7)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "সাবকন্ট্রাক্টর ওয়ার্কস্পেস (WO: WO-2026-102-PRESS)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA855F7))
                        Surface(color = Color(0xFFA855F7).copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                            Text(text = "STRICT DATA MASKED", fontSize = 10.sp, color = Color(0xFFA855F7), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    VendorMaskedSpecRow("ওয়ার্ক অর্ডার আইডি", "WO-2026-102-PRESS")
                    VendorMaskedSpecRow("কাস্টমার আইডেন্টিটি", "[RESTRICTED / VENDOR MASKED]")
                    VendorMaskedSpecRow("জব টেকনিক্যাল স্পেক্স", "১০,০০০ প্রেস ইমপ্রেশন (Art Paper 150 GSM, CMYK 4-Color)")
                    VendorMaskedSpecRow("ডেলিভারি সময়সীমা", "২০২৬-১০-০৭ বিকাল ৫:০০ টা")
                    VendorMaskedSpecRow("ড্রপ-অফ লোকেশন", "সুচারু প্রো কারখানা, পুরানা পল্টন, ঢাকা")
                    VendorMaskedSpecRow("চুক্তি মূল্যের সেলস মার্জিন", "[RESTRICTED / VENDOR MASKED]")

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        color = Color(0xFF10B981),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { /* Update Vendor Execution Status */ }
                    ) {
                        Text(
                            text = "ASN (Advance Shipping Notice) জমা দিন ➔",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProcurementWoRow(woId: String, vendor: String, spec: String, amount: String, status: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF111C33),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "$woId • $vendor", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = spec, fontSize = 10.sp, color = Color(0xFF94A3B8))
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = amount, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF00F0FF))
                Surface(color = color.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                    Text(text = status, fontSize = 9.sp, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun VendorMaskedSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = Color(0xFF94A3B8))
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (value.contains("MASKED")) Color(0xFFA855F7) else Color.White)
    }
}

// ============================================================================
// MODULE 18: PREPRESS IMPOSITION & GANG-RUN OPTIMIZATION SCREEN
// ============================================================================
@Composable
fun PrepressImpositionModuleScreen(onClose: () -> Unit) {
    var selectedParentSheet by remember { mutableStateOf("23\" x 36\" (Crown Sheet)") }
    var selectedGsm by remember { mutableStateOf("150 GSM") }
    var grainDirection by remember { mutableStateOf("LONG_GRAIN") }
    var isGrainAligned by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040914))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ModuleTopBar(
            code = "Module 18/16",
            title = "প্রেপ্রেস অপ্টিমাইজেশন, প্যারেন্ট শিট ও গ্যাং-রান ইম্পোজিশন ইঞ্জিন",
            onClose = onClose
        )

        // Task 7.1: Parent Sheet & Substrate Parameter Modeling
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
            border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "১. প্যারেন্ট শিট ও পেপার গ্রেইন ডিরেকশন প্যারামিটার (Task 7.1)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF))
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModuleInputField("প্যারেন্ট শিট ডাইমেনশন", selectedParentSheet, { selectedParentSheet = it }, Modifier.weight(1f))
                    ModuleInputField("কাগজের ওয়েট (GSM)", selectedGsm, { selectedGsm = it }, Modifier.weight(1f))
                    ModuleInputField("ফাইবার/গ্রেইন ডিরেকশন", grainDirection, {
                        grainDirection = it
                        isGrainAligned = !it.contains("SHORT", ignoreCase = true)
                    }, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Paper Grain Alignment Warning Badge
                Surface(
                    color = if (isGrainAligned) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isGrainAligned) Color(0xFF10B981) else Color(0xFFF59E0B))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isGrainAligned) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isGrainAligned) Color(0xFF10B981) else Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isGrainAligned)
                                "পারফেক্ট গ্রেইন ডিরেকশন (LONG GRAIN) — ভাজ বা ফোল্ডিংয়ে কাগজ ফাটবে না।"
                            else
                                "সতর্কতা: SHORT GRAIN ফোল্ডিং লাইনের বিপরীত — ভাজ করলে ক্র্যাকিং হতে পারে!",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isGrainAligned) Color(0xFF10B981) else Color(0xFFF59E0B)
                        )
                    }
                }
            }
        }

        // Task 7.2: Multi-Job Gang-Run Optimization Engine
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
            border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "২. মাল্টি-জব গ্যাং-রান ২ডি ইম্পোজিশন ইঞ্জিন (Task 7.2)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF))
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "মার্জিন: ৩ মিমি ব্লিড (Bleed) + ১০ মিমি প্রেস গ্রিপার (Gripper Margin)", fontSize = 11.sp, color = Color(0xFF94A3B8))

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "প্যারেন্ট শিট ইউটিলাইজেশন (Sheet Utilization)", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Text(text = "৮৯.৪%", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                        Text(text = "অপচয় / অফকাট: ১০.৬%", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    }

                    // 2D Master Sheet Preview Canvas
                    Box(
                        modifier = Modifier
                            .size(width = 160.dp, height = 90.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF111C33))
                            .border(1.dp, Color(0xFF00F0FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val cols = 3
                            val rows = 2
                            val cellW = (w - 20) / cols
                            val cellH = (h - 16) / rows

                            // Gripper margin bar on bottom
                            drawRect(color = Color(0xFFD97706), topLeft = Offset(0f, h - 8f), size = Size(w, 8f))

                            for (r in 0 until rows) {
                                for (c in 0 until cols) {
                                    drawRect(
                                        color = Color(0xFF0284C7).copy(alpha = 0.6f),
                                        topLeft = Offset(10f + c * cellW + 2f, 4f + r * cellH + 2f),
                                        size = Size(cellW - 4f, cellH - 4f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                GangRunJobRow("SO-2026-881", "বুক ক্যাটালগ (৬ up)", "Art Paper 150 GSM", "৳ ৪,৫০০", Color(0xFF10B981))
                Spacer(modifier = Modifier.height(4.dp))
                GangRunJobRow("SO-2026-880", "ক্যাশ মেমো (১২ up)", "Art Paper 150 GSM", "৳ ৭,২০০", Color(0xFF00F0FF))
            }
        }
    }
}

@Composable
private fun GangRunJobRow(orderId: String, name: String, substrate: String, amount: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF111C33),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "$orderId • $name", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = substrate, fontSize = 10.sp, color = Color(0xFF94A3B8))
            }
            Text(text = amount, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

// ============================================================================
// MODULE 19: SUBSTRATE STOCK RESERVATION GATEWAY SCREEN
// ============================================================================
@Composable
fun SubstrateStockReservationModuleScreen(onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040914))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ModuleTopBar(
            code = "Module 19",
            title = "সাবস্ট্রেট স্টক রিজার্ভেশন ও একটিভ হোল্ড গেটওয়ে",
            onClose = onClose
        )

        // Task 7.3: Stock Reservation Gateway Formula Header
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
            border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "ক্যানোনিকাল ইনভেন্টরি রিজার্ভেশন সমীকরণ (Canonical Reservation Equation):", fontSize = 12.sp, color = Color(0xFF94A3B8))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Available Stock = On-Hand Stock - Active Holds",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00F0FF)
                )
                Text(
                    text = "সফ্ট হোল্ড (Quotation 2h Hold) • হার্ড হোল্ড (Job Card Hard Allocation)",
                    fontSize = 10.sp,
                    color = Color(0xFF10B981)
                )
            }
        }

        // Substrate Stock Table with Active Holds
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
            border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "সাবস্ট্রেট স্টক রেজিস্ট্রি ও লাইভ হোল্ড স্ট্যাটাস (Zero Shadow Tables)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))

                StockReservationRow("SKU-AP-150", "Art Paper 150 GSM (20x30\")", onHand = 50, softHold = 5, hardHold = 12, available = 33)
                Spacer(modifier = Modifier.height(6.dp))
                StockReservationRow("SKU-AC-300", "Art Card 300 GSM (23x36\")", onHand = 30, softHold = 2, hardHold = 8, available = 20)
                Spacer(modifier = Modifier.height(6.dp))
                StockReservationRow("SKU-OP-080", "Offset Paper 80 GSM (20x30\")", onHand = 80, softHold = 10, hardHold = 25, available = 45)
            }
        }
    }
}

@Composable
private fun StockReservationRow(sku: String, name: String, onHand: Int, softHold: Int, hardHold: Int, available: Int) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF111C33),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "$sku • $name", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "অন-হ্যান্ড: $onHand রিম | সফট হোল্ড: $softHold | হার্ড হোল্ড: $hardHold", fontSize = 10.sp, color = Color(0xFF94A3B8))
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = "ফ্রি স্টক: $available রিম", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                Surface(color = Color(0xFF0284C7).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                    Text(text = "সক্রিয় হোল্ড", fontSize = 9.sp, color = Color(0xFF00F0FF), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }
    }
}

// ============================================================================
// MODULE 21: MACHINE TELEMETRY & OEE FRAMEWORK SCREEN
// ============================================================================
@Composable
fun MachineOeeTelemetryModuleScreen(onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040914))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ModuleTopBar(
            code = "Module 21",
            title = "মেশিন টেলিমেট্রি ও OEE এনালিটিক্স (Overall Equipment Effectiveness)",
            onClose = onClose
        )

        // Task 8.3: OEE Formula KPI Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModuleKpiChip("গড় OEE স্কোর", "৮৬.৮%", "OEE = Availability x Performance x Quality", Color(0xFF10B981), Modifier.weight(1f))
            ModuleKpiChip("মেশিন এভেইল্যাবিলিটি", "৯২.৪%", "ডাউনটাইম: ১.২ ঘন্টা", Color(0xFF00F0FF), Modifier.weight(1f))
            ModuleKpiChip("পারফরম্যান্স রেট", "৯৫.০%", "স্পিড: ৮,৫০০ imp/hr", Color(0xFFF59E0B), Modifier.weight(1f))
            ModuleKpiChip("কোয়ালিটি রেট", "৯৮.৮%", "ওয়েস্ট শিট: ১.২%", Color(0xFFA855F7), Modifier.weight(1f))
        }

        // Live Machine Telemetry & OEE Breakdown
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
            border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "কারখানা লাইভ মেশিন ও ইসিইউ টেলিমেট্রি স্ট্যাটাস (OEE Breakdown)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))

                MachineOeeRow("MAC-01", "Heidelberg Speedmaster SM-102", oee = "৮৭.৪%", avail = "৯২.০%", perf = "৯৬.০%", qual = "৯৮.৮%", status = "রানিং (ONLINE)", statusColor = Color(0xFF10B981))
                Spacer(modifier = Modifier.height(6.dp))
                MachineOeeRow("MAC-02", "Automatic Die-Cutter Pro", oee = "৮২.১%", avail = "৮৮.০%", perf = "৯৪.০%", qual = "৯৯.২%", status = "ডাউনটাইম (Plate Wash)", statusColor = Color(0xFFF59E0B))
                Spacer(modifier = Modifier.height(6.dp))
                MachineOeeRow("MAC-03", "Wire-O Automatic Binder", oee = "৯১.০%", avail = "৯৫.০%", perf = "৯৭.০%", qual = "৯৮.৬%", status = "রানিং (ONLINE)", statusColor = Color(0xFF10B981))
            }
        }
    }
}

@Composable
private fun MachineOeeRow(code: String, name: String, oee: String, avail: String, perf: String, qual: String, status: String, statusColor: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF111C33),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "$code • $name", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "এভেইল্যাবিলিটি: $avail | পারফরম্যান্স: $perf | কোয়ালিটি: $qual", fontSize = 10.sp, color = Color(0xFF94A3B8))
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = "OEE: $oee", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF00F0FF))
                Surface(color = statusColor.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                    Text(text = status, fontSize = 9.sp, color = statusColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }
    }
}

// ============================================================================
// MODULE 09/14: FINANCE, BILLING & INVOICING SCREEN
// ============================================================================
@Composable
fun FinanceBillingModuleScreen(onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040914))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ModuleTopBar(
            code = "Module 09/14",
            title = "ফিন্যান্স, মেমো, কাস্টমার ইনভয়েসিং ও ক্যাশ লেজার",
            onClose = onClose
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModuleKpiChip("আজকের নগদ আদায়", "৳ ১,২২,৩০০", "১৪টি ট্রানজেকশন", Color(0xFF10B981), Modifier.weight(1f))
            ModuleKpiChip("মোট বকেয়া রিসিভেবল", "৳ ৪,৫৮,০০০", "২৮টি কাস্টমার অ্যাকাউন্ট", Color(0xFFEF4444), Modifier.weight(1f))
            ModuleKpiChip("মাসিক নিট রেভিনিউ", "৳ ৩৮,৫০,০০০", "লক্ষ্যমাত্রা অর্জিত", Color(0xFF00F0FF), Modifier.weight(1f))
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
            border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "সাম্প্রতিক কাস্টমার ইনভয়েস ও পেমেন্ট সেটেলমেন্ট", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))

                FinanceInvoiceRow("INV-2026-402", "আহমেদ ট্রেডার্স", "৳ ৪৫,০০০", "৳ ২৫,০০০ (আংশিক পরিশোধ)", "বকেয়া: ৳ ২০,০০০", Color(0xFFF59E0B))
                Spacer(modifier = Modifier.height(6.dp))
                FinanceInvoiceRow("INV-2026-401", "সুমন এন্টারপ্রাইজ", "৳ ৭,২০০", "৳ ৭,২০০ (পূর্ণ পরিশোধ)", "পরিশোধিত", Color(0xFF10B981))
                Spacer(modifier = Modifier.height(6.dp))
                FinanceInvoiceRow("INV-2026-400", "গ্রিন মাল্টিমিডিয়া", "৳ ৩,৮০০", "৳ ০ (সম্পূর্ণ বকেয়া)", "বকেয়া: ৳ ৩,৮০০", Color(0xFFEF4444))
            }
        }
    }
}

@Composable
private fun FinanceInvoiceRow(invId: String, customer: String, total: String, paid: String, status: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF111C33),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "$invId • $customer", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "মোট: $total • আদায়: $paid", fontSize = 10.sp, color = Color(0xFF94A3B8))
            }

            Surface(
                color = color.copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, color)
            ) {
                Text(text = status, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

// ============================================================================
// MODULE 20: AFFILIATE GOVERNANCE & COMMISSION WALLET
// ============================================================================
@Composable
fun AffiliateGovernanceModuleScreen(onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040914))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ModuleTopBar(
            code = "Module 20",
            title = "অ্যাফিলিয়েট পার্টনার নেটওয়ার্ক ও কমিশন ওয়ালেট (Affiliate Governance)",
            onClose = onClose
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModuleKpiChip("সক্রিয় পার্টনার", "২৮ জন", "↑ +৮ জন এই মাসে", Color(0xFF00F0FF), Modifier.weight(1f))
            ModuleKpiChip("রেফারেল বিক্রি", "৳ ১,৭৩,২০০", "২৮টি সাকসেসফুল সেলস", Color(0xFF10B981), Modifier.weight(1f))
            ModuleKpiChip("কমিশন বাকি", "৳ ১২,৪৫০", "৫ জন পার্টনারের পেআউট", Color(0xFFF59E0B), Modifier.weight(1f))
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
            border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "অ্যাফিলিয়েট পার্টনার লেজার ও উইথড্রয়াল রিকুয়েস্ট", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))

                AffiliatePartnerRow("AFF-102", "তানভির হাসান (ঢাকা প্রসেস)", "১২টি অর্ডার", "৳ ৫৮,০০০", "৳ ২,৯০০ (প্রসেসিং)", Color(0xFFF59E0B))
                Spacer(modifier = Modifier.height(6.dp))
                AffiliatePartnerRow("AFF-101", "মাহমুদ পারভেজ (চিটাগাং প্রিন্ট)", "৯টি অর্ডার", "৳ ৪৫,০০০", "৳ ২,২৫০ (অনুমোদিত)", Color(0xFF10B981))
                Spacer(modifier = Modifier.height(6.dp))
                AffiliatePartnerRow("AFF-100", "সাকিব আইটি হাব", "৭টি অর্ডার", "৳ ৭০,২০০", "৳ ৩,৫০০ (অনুমোদিত)", Color(0xFF10B981))
            }
        }
    }
}

@Composable
private fun AffiliatePartnerRow(id: String, partner: String, orders: String, sales: String, commission: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF111C33),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "$id • $partner", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "রেফারেল: $orders • মোট সেলস: $sales", fontSize = 10.sp, color = Color(0xFF94A3B8))
            }

            Surface(
                color = color.copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, color)
            ) {
                Text(text = "কমিশন: $commission", fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }
    }
}

// ============================================================================
// UNIVERSAL CANONICAL MODULE SCREEN (M00, M01, M02, M05, M06, M07, M08, etc.)
// ============================================================================
@Composable
fun UniversalCanonicalModuleScreen(moduleCode: String, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040914))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ModuleTopBar(
            code = moduleCode,
            title = getCanonicalModuleName(moduleCode),
            onClose = onClose
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModuleKpiChip("মডিউল স্ট্যাটাস", "সক্রিয় (ONLINE)", "HTTP 200 OK", Color(0xFF10B981), Modifier.weight(1f))
            ModuleKpiChip("অডিট রেকর্ডস", "১৪২টি এন্ট্রি", "ভেরিফায়েড", Color(0xFF00F0FF), Modifier.weight(1f))
            ModuleKpiChip("সিকিউরিটি লেভেল", "L3 এনক্রিপ্টেড", "অ্যাডমিন এক্সেস", Color(0xFFA855F7), Modifier.weight(1f))
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
            border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "লাইভ অপারেশনাল ডাটা ও কন্ট্রোল প্যানেল", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))

                UniversalRecordRow("REC-2026-01", "অপারেশনাল ফাইল প্রি-প্রেস ড্রাফট", "২০২৬-১০-০৪ ২০:৩০", "অনুমোদিত", Color(0xFF10B981))
                Spacer(modifier = Modifier.height(6.dp))
                UniversalRecordRow("REC-2026-02", "ইনভেন্টরি রিজার্ভেশন ও স্টক চেক", "২০২৬-১০-০৪ ১৯:১৫", "প্রসেসিং", Color(0xFFF59E0B))
                Spacer(modifier = Modifier.height(6.dp))
                UniversalRecordRow("REC-2026-03", "সিস্টেম অডিট ও পারমিশন আপডেট", "২০২৬-১০-০৪ ১৮:০০", "সম্পন্ন", Color(0xFF00F0FF))
            }
        }
    }
}

@Composable
private fun UniversalRecordRow(id: String, title: String, time: String, status: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF111C33),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "$id • $title", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "টাইমস্ট্যাম্প: $time", fontSize = 10.sp, color = Color(0xFF94A3B8))
            }

            Surface(
                color = color.copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, color)
            ) {
                Text(text = status, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

// ============================================================================
// SHARED MODULE HELPER COMPONENTS
// ============================================================================
@Composable
private fun ModuleTopBar(code: String, title: String, onClose: () -> Unit) {
    Surface(
        color = Color(0xFF0A1224),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFF0284C7).copy(alpha = 0.3f),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onClose() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = code, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF))
                    Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }

            Surface(
                color = Color(0xFF334155),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable { onClose() }
            ) {
                Text(text = "বন্ধ করুন", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
    }
}

@Composable
private fun ModuleKpiChip(label: String, value: String, sub: String, accentColor: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0A1224),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, fontSize = 10.sp, color = Color(0xFF94A3B8))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(text = sub, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)
        }
    }
}

@Composable
private fun ModuleInputField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, fontSize = 10.sp, color = Color(0xFF94A3B8))
        Spacer(modifier = Modifier.height(2.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00F0FF),
                unfocusedBorderColor = Color(0xFF1E293B),
                focusedContainerColor = Color(0xFF040914),
                unfocusedContainerColor = Color(0xFF040914),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
    }
}

private fun getCanonicalModuleName(code: String): String {
    return when (code) {
        "Module 00" -> "সিস্টেম কনফিগারেশন ও সিকিউরিটি"
        "Module 01" -> "ইউজার ও ক্যাপাবিলিটি ম্যাট্রিক্স"
        "Module 02" -> "কাস্টমার ও কন্টাক্ট ম্যানেজমেন্ট"
        "Module 03" -> "কোটেশন ও সেলস অর্ডার"
        "Module 04" -> "প্রডাকশন এক্সিকিউশন (১৩টি ধাপ)"
        "Module 05" -> "ডিজাইন ও প্রি-প্রেস প্রুফিং"
        "Module 06" -> "CTP প্লেট আউটপুট ও QC"
        "Module 07" -> "ফিনিশড গুডস ইনভেন্টরি"
        "Module 08/11" -> "ডেলিভারি চালান ও ডিসপ্যাচ"
        "Module 09/14" -> "ফিন্যান্স, মেমো ও ইনভয়েসিং"
        "Module 12" -> "ভেন্ডর সাবকন্ট্রাক্টিং ও ওয়ার্ক অর্ডার"
        "Module 13" -> "সেন্ট্রাল প্রকিউরমেন্ট হাব ও ৩-ওয়ে ম্যাচিং"
        "Module 15/18" -> "প্রডাকশন জব কস্টিং ও রেট কার্ড"
        "Module 19" -> "সাবস্ট্রেট স্টক রিজার্ভেশন"
        "Module 20" -> "আফিলিয়েট গভর্ন্যান্স Network"
        "Module 21" -> "মেশিন টেলিমেট্রি ও OEE"
        "Module 23" -> "ওয়ালেট ও পেআউট একাউন্টিং"
        "Module 24" -> "রিপোর্টস, এনালিটিক্স ও অডিট (15 CAT)"
        else -> "ইআরপি অপারেশনাল মডিউল"
    }
}
