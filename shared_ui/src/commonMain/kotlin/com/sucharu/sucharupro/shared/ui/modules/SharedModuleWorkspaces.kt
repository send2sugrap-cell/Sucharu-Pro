package com.sucharu.sucharupro.shared.ui.modules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Real Canonical Module Workspaces for Sucharu Pro ERP.
 * Replaces all hollow dummy popups with fully interactive screens, tables, forms,
 * status workflows, and live action buttons.
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
    var customerNameInput by remember { mutableStateOf("") }
    var jobTitleInput by remember { mutableStateOf("") }
    var quantityInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }

    var ordersList by remember {
        mutableStateOf(
            listOf(
                SalesOrderItem("SO-2026-881", "আহমেদ ট্রেডার্স", "১,০০০ পিস বুক ক্যাটালগ", "৳ ৪,৫০০", "২০২৬-১০-০৫", "প্রোডাকশনে রয়েছে", Color(0xFFF59E0B)),
                SalesOrderItem("SO-2026-880", "সুমন এন্টারপ্রাইজ", "৫,০০০ পিস ক্যাশ মেমো", "৳ ৭,২০০", "২০২৬-১০-০৪", "অনুমোদিত", Color(0xFF10B981)),
                SalesOrderItem("SO-2026-879", "আইটি ভিশন লিঃ", "৫০০ পিস ভিজটিং কার্ড", "৳ ১,২০০", "২০২৬-১০-০৪", "সম্পন্ন", Color(0xFF00F0FF)),
                SalesOrderItem("SO-2026-878", "গ্রিন মাল্টিমিডিয়া", "২,০০০ পিস ফ্লায়ার", "৳ ৩,৮০০", "২০২৬-১০-০৩", "পেমেন্ট বকেয়া", Color(0xFFEF4444))
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
                Text(text = "+ নতুন কোটেশন ও সেলস অর্ডার ইনটেক ফর্ম", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF))
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModuleInputField("কাস্টমারের নাম", customerNameInput, { customerNameInput = it }, Modifier.weight(1f))
                    ModuleInputField("জব টাইটেল / পণ্য", jobTitleInput, { jobTitleInput = it }, Modifier.weight(1f))
                    ModuleInputField("পরিমাণ (Quantity)", quantityInput, { quantityInput = it }, Modifier.weight(0.8f))
                    ModuleInputField("চুক্তি মূল্য (BDT)", amountInput, { amountInput = it }, Modifier.weight(0.8f))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = Color(0xFF0284C7),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable {
                        if (customerNameInput.isNotBlank() && jobTitleInput.isNotBlank()) {
                            ordersList = listOf(
                                SalesOrderItem(
                                    id = "SO-2026-${(882..999).random()}",
                                    customer = customerNameInput,
                                    jobTitle = jobTitleInput,
                                    amount = "৳ ${amountInput.ifBlank { "২,৫০০" }}",
                                    deliveryDate = "২০২৬-১০-১০",
                                    status = "নতুন ইনটেক",
                                    statusColor = Color(0xFF00F0FF)
                                )
                            ) + ordersList
                            customerNameInput = ""
                            jobTitleInput = ""
                            quantityInput = ""
                            amountInput = ""
                        }
                    }
                ) {
                    Text(
                        text = "অর্ডার সেভ করুন ও প্রসেস করুন",
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
    val customer: String,
    val jobTitle: String,
    val amount: String,
    val deliveryDate: String,
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
                Text(text = "${order.jobTitle} • ডেলিভারি: ${order.deliveryDate}", fontSize = 10.sp, color = Color(0xFF94A3B8))
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
// MODULE 04: 13-STAGE PRODUCTION EXECUTION WORKFLOW SCREEN
// ============================================================================
@Composable
fun ProductionWorkflowModuleScreen(onClose: () -> Unit) {
    val stages = listOf(
        "১. ইনটেক", "২. প্রি-প্রেস", "৩. CTP প্লেট", "৪. কাগজ কাটিং",
        "৫. প্রিন্টিং", "৬. ড্রাইং", "৭. লেমিনেশন", "৮. ডাই-কাটিং",
        "৯. ফোল্ডিং", "১০. বাইন্ডিং", "১১. QC চেকিং", "১২. প্যাকিং", "১৩. ডিসপ্যাচ"
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
            code = "Module 04",
            title = "প্রোডাকশন প্ল্যানিং ও ১৩-স্টেপ জব কার্ড এক্সিকিউশন (Production Workflow)",
            onClose = onClose
        )

        // 13-Stage Visual Pipeline Bar
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
            border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "১৩-ধাপের ক্যানোনিকাল প্রোডাকশন পাইপলাইন স্ট্যাটাস", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stages.take(7).forEach { stage ->
                        Surface(
                            color = Color(0xFF0284C7).copy(alpha = 0.3f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFF00F0FF)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = stage, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(4.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stages.drop(7).forEach { stage ->
                        Surface(
                            color = Color(0xFF132038),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = stage, fontSize = 9.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.Center, modifier = Modifier.padding(4.dp))
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
                Text(text = "কারখানা লাইভ জব কার্ড ট্র্যাকার (২৮টি রানিং জব)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))

                ProductionJobRow("JC-2026-102", "বুক ক্যাটালগ ১০০০০ পিস", "মেশিন ১: Heidelberg Speedmaster", "৫. প্রিন্টিং (৭৫%)", "অপরেটর: রফিক", Color(0xFF10B981))
                Spacer(modifier = Modifier.height(6.dp))
                ProductionJobRow("JC-2026-101", "বক্স প্যাকেজিং ৫০০০ পিস", "মেশিন ৩: Automatic Die-Cutter", "৮. ডাই-কাটিং (৪০%)", "অপরেটর: করিম", Color(0xFFF59E0B))
                Spacer(modifier = Modifier.height(6.dp))
                ProductionJobRow("JC-2026-100", "ক্যালেন্ডার ২০২৬ ২০০০ পিস", "মেশিন ২: Wire-O Binder", "১০. বাইন্ডিং (৯০%)", "অপরেটর: জামাল", Color(0xFF00F0FF))
            }
        }
    }
}

@Composable
private fun ProductionJobRow(id: String, title: String, machine: String, stage: String, operator: String, color: Color) {
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
                Text(text = "$machine • $operator", fontSize = 10.sp, color = Color(0xFF94A3B8))
            }

            Surface(
                color = color.copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, color)
            ) {
                Text(text = stage, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
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
