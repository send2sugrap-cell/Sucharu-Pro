package com.sucharu.sucharupro.shared.ui.wall

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared Multiplatform Server-Driven Public Home Wall & Customer Portal Workspace.
 * Integrates Forms 01–06, Module 07 (Product Master), Module 02 (Customer Portal),
 * Module 14 (Ledger Balance), Audience Offer Eligibility Engine, and Guest Phone OTP Flow.
 */
@Composable
fun SharedPublicWallWorkspace(
    modifier: Modifier = Modifier,
    onOpenAdmin: () -> Unit = {},
    onOpenCalculator: () -> Unit = {}
) {
    var activeWallTab by remember { mutableIntStateOf(0) } // 0: Home Wall, 1: Product Catalog, 2: Offers, 3: My Area
    var isUserLoggedIn by remember { mutableStateOf(false) }
    var showOtpModal by remember { mutableStateOf(false) }
    var otpPhoneNumber by remember { mutableStateOf("") }
    var otpCodeInput by remember { mutableStateOf("") }
    var otpStepSent by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF040914))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Navigation & User Portal Bar
            Surface(
                color = Color(0xFF0A1224),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "সুচারু গ্রাফিক্স ওয়াল",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFF0284C7).copy(alpha = 0.3f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "SERVER-DRIVEN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00F0FF),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = Color(0xFF334155),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { onOpenAdmin() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "এডমিন প্যানেল", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (!isUserLoggedIn) {
                            Surface(
                                color = Color(0xFF0284C7),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { showOtpModal = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "লগইন / অনবোর্ডিং", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Surface(
                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF10B981)),
                                modifier = Modifier.clickable { activeWallTab = 3 }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "মাই এরিয়া (অনলাইন)", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Wall Sub-Tabs Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WallTabTile("১. হোম দেয়াল", isActive = activeWallTab == 0, modifier = Modifier.weight(1f)) { activeWallTab = 0 }
                WallTabTile("২. প্রোডাক্ট গ্যালারি", isActive = activeWallTab == 1, modifier = Modifier.weight(1f)) { activeWallTab = 1 }
                WallTabTile("৩. অফার হাব", isActive = activeWallTab == 2, modifier = Modifier.weight(1f)) { activeWallTab = 2 }
                WallTabTile("৪. মাই এরিয়া", isActive = activeWallTab == 3, modifier = Modifier.weight(1f)) {
                    if (!isUserLoggedIn) showOtpModal = true else activeWallTab = 3
                }
            }

            // Tab Views
            when (activeWallTab) {
                0 -> HomeWallTab(onOpenCalculator = onOpenCalculator, onClaimOffer = { showOtpModal = true })
                1 -> ProductCatalogTab(onOpenCalculator = onOpenCalculator)
                2 -> SpecialOffersTab(isLoggedIn = isUserLoggedIn, onRequestOtp = { showOtpModal = true })
                3 -> CustomerMyAreaTab(onOpenCalculator = onOpenCalculator, onLogout = { isUserLoggedIn = false; activeWallTab = 0 })
            }
        }

        // Guest Onboarding Phone OTP Modal
        if (showOtpModal) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable { showOtpModal = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
                    border = BorderStroke(1.5.dp, Color(0xFF00F0FF)),
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
                                Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "ফোন ওটিপি কাস্টমার অনবোর্ডিং", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { showOtpModal = false }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (!otpStepSent) {
                            Text(text = "আপনার ১১ ডিজিট মোবাইল নম্বর ইনপুট দিন:", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = otpPhoneNumber,
                                onValueChange = { otpPhoneNumber = it },
                                placeholder = { Text("017XXXXXXXX", color = Color(0xFF64748B)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00F0FF),
                                    unfocusedBorderColor = Color(0xFF1E293B),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Surface(
                                color = Color(0xFF0284C7),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (otpPhoneNumber.length >= 11) {
                                            otpStepSent = true
                                        }
                                    }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                    Text(text = "ওটিপি কোড পাঠান (Send OTP)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Text(text = "$otpPhoneNumber নম্বরে পাঠানো ৬-ডিজিট ওটিপি দিন:", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = otpCodeInput,
                                onValueChange = { otpCodeInput = it },
                                placeholder = { Text("123456", color = Color(0xFF64748B)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF10B981),
                                    unfocusedBorderColor = Color(0xFF1E293B),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Surface(
                                color = Color(0xFF10B981),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        isUserLoggedIn = true
                                        showOtpModal = false
                                        otpStepSent = false
                                        activeWallTab = 3
                                    }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                    Text(text = "ওটিপি যাচাই ও লগইন সম্পন্ন করুন", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
private fun WallTabTile(title: String, isActive: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        color = if (isActive) Color(0xFF0284C7) else Color(0xFF0A1224),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isActive) Color(0xFF00F0FF) else Color(0xFF1E293B)),
        modifier = modifier.clickable { onClick() }
    ) {
        Box(modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp), contentAlignment = Alignment.Center) {
            Text(text = title, color = Color.White, fontSize = 11.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

// ============================================================================
// TAB 1: HOME WALL TAB
// ============================================================================
@Composable
private fun HomeWallTab(onOpenCalculator: () -> Unit, onClaimOffer: () -> Unit) {
    // Dynamic Hero Offer Banner
    Surface(
        color = Color(0xFF1E3A8A),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = Color(0xFFD97706), shape = RoundedCornerShape(6.dp)) {
                    Text(text = "১৫% ছাড় • ঈদ স্পেশাল অফার", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
                Text(text = "SUCHARU SPECIAL", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "বুল্ক প্রিন্টিং ও কর্পোরেট গিফট স্পেশাল ডিল", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(text = "কাস্টম বিজনেস কার্ড, ব্রোশিয়ার, ডায়েরি ও ক্যালেন্ডার অর্ডারে দ্রুত ডেলিভারি", fontSize = 12.sp, color = Color(0xFF94A3B8))

            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(color = Color(0xFF0284C7), shape = RoundedCornerShape(8.dp), modifier = Modifier.clickable { onOpenCalculator() }) {
                    Text(text = "কোটেশন দেখুন", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
                Surface(color = Color(0xFF10B981), shape = RoundedCornerShape(8.dp), modifier = Modifier.clickable { onClaimOffer() }) {
                    Text(text = "অফার দাবি করুন", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Featured Products Showcase (Module 07 Integration)
    Text(text = "জনপ্রিয় প্রিন্টিং সেবাসমূহ (Module 07 Catalog)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
    Spacer(modifier = Modifier.height(8.dp))

    val services = listOf(
        "অফসেট কমার্শিয়াল প্রিন্টিং" to "বিজনেস কার্ড, লেটারহেড, প্যাড ও ব্রোশিয়ার • স্টার্টিং ৳ ১,২০০",
        "ডিজিটাল ফাস্ট প্রিন্টিং" to "জরুরী অর্ডারের জন্য সেম-ডে ডেলিভারি • স্টার্টিং ৳ ৫০০",
        "প্যাকেজিং ও ডাই-কাটিং" to "কাস্টম কার্টন, পেপার ব্যাগ ও বক্স • স্টার্টিং ৳ ৫,০০০",
        "কর্পোরেট ডায়েরি ও ক্যালেন্ডার" to "২০২৭ নিউ ইয়ার কাস্টম ডায়েরি প্রাক-অর্ডার • স্টার্টিং ৳ ৩,৫০০"
    )

    services.forEach { (title, desc) ->
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).clickable { onOpenCalculator() },
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
            border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f))
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF10B981).copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = desc, fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

// ============================================================================
// TAB 2: PRODUCT CATALOG TAB (MODULE 07)
// ============================================================================
@Composable
private fun ProductCatalogTab(onOpenCalculator: () -> Unit) {
    Text(text = "Module 07 • ক্যানোনিকাল প্রোডাক্ট ক্যাটালগ", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
    Spacer(modifier = Modifier.height(8.dp))

    val products = listOf(
        Triple("PRD-101", "অফসেট ভিজিটিং কার্ড (৩০০ জিএসএম)", "৳ ১,২০০ / ১০০০ পিস"),
        Triple("PRD-102", "ক্যাশ মেমো ও চালান বই (২ পার্ট)", "৳ ৩,৫০০ / ৫০টি বই"),
        Triple("PRD-103", "বুক ক্যাটালগ ও ব্রোশিয়ার (১৬ পেজ)", "৳ ২৫,০০০ / ১০০০ পিস"),
        Triple("PRD-104", "কাস্টম প্যাকেজিং কার্টন বক্স", "৳ ৪৫,০০০ / ৫০০০ পিস")
    )

    products.forEach { (code, name, price) ->
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).clickable { onOpenCalculator() },
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
            border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f))
        ) {
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = "$code • $name", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "মূল্য: $price", fontSize = 11.sp, color = Color(0xFF00F0FF))
                }
                Surface(color = Color(0xFF0284C7), shape = RoundedCornerShape(6.dp)) {
                    Text(text = "হিসাব করুন", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}

// ============================================================================
// TAB 3: SPECIAL OFFERS TAB
// ============================================================================
@Composable
private fun SpecialOffersTab(isLoggedIn: Boolean, onRequestOtp: () -> Unit) {
    Text(text = "বিশেষ প্রচারণামূলক অফার ও ডিসকাউন্ট হাব", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
    Spacer(modifier = Modifier.height(8.dp))

    val offers = listOf(
        OfferItem("OFFER-EID", "১৫% ছাড় • ঈদ উৎসব স্পেশাল", "GUEST ELIGIBLE", true, Color(0xFF10B981)),
        OfferItem("OFFER-CORP", "কর্পোরেট ১০,০০০+ ক্যাটালগ ২০% ফ্ল্যাট ডিসকাউন্ট", "CUSTOMER ONLY", isLoggedIn, Color(0xFF00F0FF)),
        OfferItem("OFFER-AFF", "অ্যাফিলিয়েট ডাবল কমিশন বোনাস (১০%)", "AFFILIATE ONLY", isLoggedIn, Color(0xFFA855F7))
    )

    offers.forEach { offer ->
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
            border = BorderStroke(1.dp, offer.badgeColor.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = offer.badgeColor.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, offer.badgeColor)) {
                        Text(text = offer.badge, fontSize = 9.sp, color = offer.badgeColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Text(text = offer.id, fontSize = 10.sp, color = Color(0xFF94A3B8))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = offer.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))

                if (offer.isEligible) {
                    Surface(color = Color(0xFF10B981), shape = RoundedCornerShape(6.dp), modifier = Modifier.clickable { }) {
                        Text(text = "অফার দাবি করুন (Eligible)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                    }
                } else {
                    Surface(color = Color(0xFF1E293B), shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, Color(0xFFEF4444)), modifier = Modifier.clickable { onRequestOtp() }) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "লগইন প্রয়োজন (Locked)", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private data class OfferItem(val id: String, val title: String, val badge: String, val isEligible: Boolean, val badgeColor: Color)

// ============================================================================
// TAB 4: CUSTOMER MY AREA TAB (MODULE 02 & MODULE 14)
// ============================================================================
@Composable
private fun CustomerMyAreaTab(onOpenCalculator: () -> Unit, onLogout: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Customer Profile Header Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
            border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF0284C7)), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "আহমেদ ট্রেডার্স (কাস্টমার)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "ফোন: 01711223344 • মেম্বারশিপ: VIP", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    }
                }

                Surface(color = Color(0xFF1E293B), shape = RoundedCornerShape(6.dp), modifier = Modifier.clickable { onLogout() }) {
                    Text(text = "লগআউট", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }

        // Ledger & Account Balance
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
            border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Module 14 • অ্যাকাউন্ট লেজার ও ডিউ ব্যালেন্স", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF))
                    Surface(color = Color(0xFF0284C7), shape = RoundedCornerShape(6.dp), modifier = Modifier.clickable { onOpenCalculator() }) {
                        Text(text = "+ নতুন কোটেশন", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(text = "মোট কেনাকাটা: ৳ ৪৫,০০০", fontSize = 11.sp, color = Color.White)
                        Text(text = "পরিশোধিত: ৳ ২৫,০০০", fontSize = 11.sp, color = Color(0xFF10B981))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "বকেয়া (Due): ৳ ২০,০০০", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444))
                    }
                }
            }
        }

        // Active Orders List
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
            border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "চলতি অর্ডার ও ট্র্যাকিং স্ট্যাটাস", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))

                CustomerOrderRow("SO-2026-881", "১,০০০ পিস বুক ক্যাটালগ", "প্রোডাকশনে রয়েছে", Color(0xFFF59E0B))
                Spacer(modifier = Modifier.height(4.dp))
                CustomerOrderRow("SO-2026-880", "৫,০০০ পিস ক্যাশ মেমো", "অনুমোদিত (Advance Paid)", Color(0xFF10B981))
            }
        }
    }
}

@Composable
private fun CustomerOrderRow(id: String, title: String, status: String, color: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF111C33), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(text = "$id • $title", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Surface(color = color.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, color)) {
                Text(text = status, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
}
