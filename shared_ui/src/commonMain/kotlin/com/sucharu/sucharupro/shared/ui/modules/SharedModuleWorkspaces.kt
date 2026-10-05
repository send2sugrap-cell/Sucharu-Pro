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
import androidx.compose.material.icons.filled.Person
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
 * Integrates Module 03 (Sales Orders), Module 04 (Production & Job Cards), Module 09/14 (Finance),
 * Module 20 (Affiliate), Multi-Stage State Machine, Job Bag Generation, and Printable QR Tickets.
 */
@Composable
fun RouterCanonicalModuleWorkspace(
    moduleCode: String,
    onClose: () -> Unit
) {
    when (moduleCode) {
        "Module 03", "Orders" -> SalesOrderModuleScreen(onClose = onClose)
        "Module 04", "Production" -> ProductionWorkflowModuleScreen(onClose = onClose)
        "Module 09/14", "Finance" -> FinanceBillingModuleScreen(onClose = onClose)
        "Module 20", "Affiliate" -> AffiliateGovernanceModuleScreen(onClose = onClose)
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
    var advanceStatusInput by remember { mutableStateOf("PAID_50_PERCENT") }

    var ordersList by remember {
        mutableStateOf(
            listOf(
                SalesOrderItem("SO-2026-881", "CALC-2026-904", "আহমেদ ট্রেডার্স", "১,০০০ পিস বুক ক্যাটালগ", "৳ ৪,৫০০", "২০২৬-১০-০৫", "৫০% অগ্রিম পরিশোধিত", "প্রোডাকশনে পাঠায়িত", Color(0xFFF59E0B)),
                SalesOrderItem("SO-2026-880", "CALC-2026-812", "সুমন এন্টারপ্রাইজ", "৫,০০০ পিস ক্যাশ মেমো", "৳ ৭,২০০", "২০২৬-১০-০৪", "পূর্ণ পরিশোধিত", "অনুমোদিত", Color(0xFF10B981)),
                SalesOrderItem("SO-2026-879", "CALC-2026-778", "আইটি ভিশন লিঃ", "৫০০ পিস ভিজটিং কার্ড", "৳ ১,২০০", "২০২৬-১০-০৪", "পূর্ণ পরিশোধিত", "সম্পন্ন", Color(0xFF00F0FF)),
                SalesOrderItem("SO-2026-878", "CALC-2026-650", "গ্রিন মাল্টিমিডিয়া", "২,০০০ পিস ফ্লায়ার", "৳ ৩,৮োর", "২০২৬-১০-০৩", "অগ্রিম বকেয়া", "পেমেন্ট বকেয়া", Color(0xFFEF4444))
            )
        )
    }

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
            title = "কোটেশন ইনটেক ও সেলস অর্ডার ম্যানেজমেন্ট (Sales Orders)",
            onClose = onClose
        )

        // KPI Summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModuleKpiChip("আজকের মোট সেলস", "৳ ১,৩৭,৫০০", "৮৪টি অর্ডার", Color(0xFF00F0FF), Modifier.weight(1f))
            ModuleKpiChip("অনুমোদিত কোটেশন", "৬১টি", "৭২% কনভার্সন", Color(0xFF10B981), Modifier.weight(1f))
            ModuleKpiChip("পেন্ডিং ইনটেক", "১৮টি", "রিভিউ দরকার", Color(0xFFF59E0B), Modifier.weight(1f))
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
                                    status = "নতুন ইনটেক",
                                    statusColor = Color(0xFF00F0FF)
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
                        text = "অর্ডার সেভ করুন ও প্রসেস করুন (SO-2026 Sequence)",
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
                    SalesOrderRow(order = order)
                    Spacer(modifier = Modifier.height(6.dp))
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
private fun SalesOrderRow(order: SalesOrderItem) {
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
                Text(text = "অগ্রিম: ${order.advanceStatus}", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = order.amount, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF00F0FF))
                Spacer(modifier = Modifier.width(12.dp))
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

// ============================================================================
// MODULE 04: 13-STAGE PRODUCTION EXECUTION & JOB CARD WORKFLOW
// ============================================================================
@Composable
fun ProductionWorkflowModuleScreen(onClose: () -> Unit) {
    var activeJobTicket: JobCardItem? by remember { mutableStateOf(null) }

    var jobCardsList by remember {
        mutableStateOf(
            listOf(
                JobCardItem("JC-2026-102", "SO-2026-881", "আহমেদ ট্রেডার্স", "বুক ক্যাটালগ ১০০০০ পিস", "Art Paper 150 GSM (20x30\")", "CMYK 4-Color (4 Plates)", "Heidelberg Speedmaster", "Thermal Matt + Die-Cut + Perfect Bind", "ON_PRESS", "প্রিন্টিং চলমান", "রফিক", Color(0xFF10B981)),
                JobCardItem("JC-2026-101", "SO-2026-880", "সুমন এন্টারপ্রাইজ", "বক্স প্যাকেজিং ৫০০০ পিস", "Box Board 300 GSM (25x37\")", "CMYK 4-Color (4 Plates)", "Automatic Die-Cutter", "Thermal Gloss + Die-Cut + Pasting", "POST_PRESS_FINISHING", "ডাই-কাটিং চলমান", "করিম", Color(0xFFF59E0B)),
                JobCardItem("JC-2026-100", "SO-2026-879", "আইটি ভিশন লিঃ", "ক্যালেন্ডার ২০২৬ ২০০০ পিস", "Art Card 300 GSM (23x36\")", "CMYK 4-Color (4 Plates)", "Wire-O Binder", "Thermal Gloss + Wire-O Bind", "QC_PASSED", "কিউসি সম্পন্ন", "জামাল", Color(0xFF00F0FF)),
                JobCardItem("JC-2026-099", "SO-2026-878", "গ্রিন মাল্টিমিডিয়া", "ফ্লায়ার ২০০০ পিস", "Art Paper 120 GSM (20x30\")", "2-Color (2 Plates)", "Offset Single Color", "Folding", "PENDING_PLATES", "সিটিপি প্লেট অপেক্ষমান", "রহিম", Color(0xFFEF4444))
            )
        )
    }

    val stateMachineStages = listOf(
        "PENDING_PLATES",
        "CTP_READY",
        "ON_PRESS",
        "POST_PRESS_FINISHING",
        "QC_PASSED",
        "READY_FOR_DISPATCH"
    )

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
                title = "প্রোডাকশন প্ল্যানিং, জব কার্ড (Job Bag) ও শপ-ফ্লোর পাইপলাইন",
                onClose = onClose
            )

            // Production Floor State Machine Pipeline Summary
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
                border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "প্রোডাকশন ফ্লোর স্টেট মেশিন (Floor State Machine)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        stateMachineStages.forEach { st ->
                            Surface(
                                color = Color(0xFF0284C7).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFF00F0FF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = st.replace("_", " "),
                                    fontSize = 8.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp)
                                )
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
                                    stageCode = "PENDING_PLATES",
                                    stageLabel = "সিটিপি প্লেট অপেক্ষমান",
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
        "PENDING_PLATES" -> Triple("CTP_READY", "সিটিপি প্লেট রেডি", Color(0xFF38BDF8))
        "CTP_READY" -> Triple("ON_PRESS", "প্রিন্টিং চলমান", Color(0xFF10B981))
        "ON_PRESS" -> Triple("POST_PRESS_FINISHING", "ফিনিশিং চলমান", Color(0xFFF59E0B))
        "POST_PRESS_FINISHING" -> Triple("QC_PASSED", "কিউসি সম্পন্ন", Color(0xFF00F0FF))
        "QC_PASSED" -> Triple("READY_FOR_DISPATCH", "ডেলিভারির জন্য প্রস্তুত", Color(0xFF10B981))
        else -> Triple("READY_FOR_DISPATCH", "ডেলিভারির জন্য প্রস্তুত", Color(0xFF10B981))
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
                Text(text = status, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
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
                Text(text = status, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
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
        "Module 15/18" -> "প্রডাকশন জব কস্টিং ও রেট কার্ড"
        "Module 19" -> "সাবস্ট্রেট স্টক রিজার্ভেশন"
        "Module 20" -> "আফিলিয়েট গভর্ন্যান্স Network"
        "Module 21" -> "মেশিন টেলিমেট্রি ও OEE"
        "Module 23" -> "ওয়ালেট ও পেআউট একাউন্টিং"
        "Module 24" -> "রিপোর্টস, এনালিটিক্স ও অডিট (15 CAT)"
        else -> "ইআরপি অপারেশনাল মডিউল"
    }
}
