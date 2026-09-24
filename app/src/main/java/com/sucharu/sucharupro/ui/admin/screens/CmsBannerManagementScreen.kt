package com.sucharu.sucharupro.ui.admin.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.sucharu.sucharupro.data.api.model.cms.CmsBannerDto
import com.sucharu.sucharupro.data.api.model.cms.CmsDesignTemplateDto
import com.sucharu.sucharupro.data.api.model.cms.CreateCmsBannerRequestDto
import com.sucharu.sucharupro.data.api.model.cms.CreateCmsDesignTemplateRequestDto
import com.sucharu.sucharupro.data.repository.cms.InMemoryCmsRepository

/**
 * Module 25: Admin Panel Dynamic CMS Banner, Product Categories & Design Template Management Screen.
 */
@Composable
fun CmsBannerManagementScreen(
    modifier: Modifier = Modifier,
    cmsRepository: InMemoryCmsRepository = remember { InMemoryCmsRepository() }
) {
    val scrollState = rememberScrollState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Banners, 1: Design Templates

    val bannersList = remember { mutableStateListOf<CmsBannerDto>().apply { addAll(cmsRepository.getAllBanners()) } }
    val templatesList = remember { mutableStateListOf<CmsDesignTemplateDto>().apply { addAll(cmsRepository.getAllDesignTemplates()) } }

    // Banner Inputs
    var newTitle by remember { mutableStateOf("") }
    var newDescription by remember { mutableStateOf("") }
    var newDiscountTag by remember { mutableStateOf("১৫% ছাড়") }
    var newImageUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1562654501-a0ccc0fc3fb1?w=800&q=80") }

    // Template Inputs
    var tCategory by remember { mutableStateOf("অফসেট প্রিন্টিং") }
    var tTitle by remember { mutableStateOf("") }
    var tColorMode by remember { mutableStateOf("৪ কালার (CMYK)") }
    var tPaperStock by remember { mutableStateOf("৩০০ GSM আর্ট কার্ড") }
    var tPrintSize by remember { mutableStateOf("স্ট্যান্ডার্ড সাইজ") }
    var tFinishing by remember { mutableStateOf("গ্লস ল্যামিনেশন") }
    var tPrice by remember { mutableStateOf("৳ ১,২০০ / ১,০০০ পিস") }
    var tPerUnit by remember { mutableStateOf("(৳ ১.২০ / পিস)") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Top Header
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Module 25 • CMS Wall, Services & Product Design Management",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "পাবলিক ওয়াল ও সার্ভিস সিএমএস আপলোড হাব",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "আমাদের সেবা সমূহ ও জনপ্রিয় পণ্যসমূহের গ্যালারি ডিজাইন আপলোড এবং টেকনিক্যাল ডিটেল্স এডিট হাব",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "CMS ACTIVE",
                        color = Color(0xFF10B981),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tab Selector Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = if (selectedTab == 0) Color(0xFF0284C7) else Color(0xFF1E293B),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).clickable { selectedTab = 0 }
            ) {
                Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text("১. ব্যানার সিএমএস", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Surface(
                color = if (selectedTab == 1) Color(0xFF0284C7) else Color(0xFF1E293B),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).clickable { selectedTab = 1 }
            ) {
                Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text("২. প্রোডাক্ট ডিজাইন গ্যালারি", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Surface(
                color = if (selectedTab == 2) Color(0xFF0284C7) else Color(0xFF1E293B),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).clickable { selectedTab = 2 }
            ) {
                Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text("৩. অর্ডার ফরম টেক্সট সিএমএস", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            // TAB 0: Banner Upload Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "নতুন ব্যানার আপলোড ও অফার তৈরি করুন",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("ব্যানার শিরোনাম / অফারের নাম", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newDescription,
                        onValueChange = { newDescription = it },
                        label = { Text("অফারের বিস্তারিত বিবরণ", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newDiscountTag,
                            onValueChange = { newDiscountTag = it },
                            label = { Text("অফার ট্যাগ (যেমন: ১৫% ছাড়)", color = Color(0xFF94A3B8)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = newImageUrl,
                            onValueChange = { newImageUrl = it },
                            label = { Text("ইমেজ ইউআরএল", color = Color(0xFF94A3B8)) },
                            modifier = Modifier.weight(2f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (newTitle.isNotBlank()) {
                                val created = cmsRepository.createBanner(
                                    CreateCmsBannerRequestDto(
                                        title = newTitle,
                                        description = newDescription.ifBlank { "পাবলিক ওয়ালে লাইভ অফার জেনারেটেড" },
                                        imageUrl = newImageUrl,
                                        discountTag = newDiscountTag.ifBlank { "SPECIAL DEAL" }
                                    )
                                )
                                bannersList.add(0, created)
                                newTitle = ""
                                newDescription = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "পাবলিক ওয়ালে ব্যানার প্রকাশ করুন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            bannersList.forEachIndexed { index, banner ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = banner.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = banner.description, fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                        Switch(
                            checked = banner.isActive,
                            onCheckedChange = { isChecked ->
                                val updated = cmsRepository.toggleBannerStatus(banner.bannerId, isChecked)
                                if (updated != null) bannersList[index] = updated
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF10B981))
                        )
                    }
                }
            }
        } else if (selectedTab == 1) {
            // TAB 1: Product Design Template & Technical Spec Upload
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "সেবা ও পণ্যের জন্য নতুন ডিজাইন টেমপ্লেট ও টেকনিক্যাল স্পেক আপলোড করুন", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = tCategory,
                        onValueChange = { tCategory = it },
                        label = { Text("ক্যাটাগরি বা সেবার নাম (যেমন: অফসেট প্রিন্টিং / ভিজিটিং কার্ড / প্যাকেজিং)", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = tTitle,
                        onValueChange = { tTitle = it },
                        label = { Text("ডিজাইনের নাম (যেমন: প্রিমিয়াম ইভেন্ট পোস্টার ডিজাইন)", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = tColorMode, onValueChange = { tColorMode = it }, label = { Text("Color Mode", color = Color(0xFF94A3B8)) }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = tPaperStock, onValueChange = { tPaperStock = it }, label = { Text("Paper Stock (GSM)", color = Color(0xFF94A3B8)) }, modifier = Modifier.weight(1f), singleLine = true)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = tPrintSize, onValueChange = { tPrintSize = it }, label = { Text("Print Size", color = Color(0xFF94A3B8)) }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = tFinishing, onValueChange = { tFinishing = it }, label = { Text("Finishing", color = Color(0xFF94A3B8)) }, modifier = Modifier.weight(1f), singleLine = true)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = tPrice, onValueChange = { tPrice = it }, label = { Text("Price (যেমন: ৳ ১,২০০ / ১,০০০ পিস)", color = Color(0xFF94A3B8)) }, modifier = Modifier.weight(1.5f), singleLine = true)
                        OutlinedTextField(value = tPerUnit, onValueChange = { tPerUnit = it }, label = { Text("Per Unit Rate", color = Color(0xFF94A3B8)) }, modifier = Modifier.weight(1f), singleLine = true)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (tTitle.isNotBlank()) {
                                val created = cmsRepository.createDesignTemplate(
                                    CreateCmsDesignTemplateRequestDto(
                                        categoryName = tCategory,
                                        title = tTitle,
                                        colorMode = tColorMode,
                                        paperStock = tPaperStock,
                                        printSize = tPrintSize,
                                        finishing = tFinishing,
                                        priceText = tPrice,
                                        perUnitRate = tPerUnit
                                    )
                                )
                                templatesList.add(0, created)
                                tTitle = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "ডিজাইন টেমপ্লেট ও স্পেক গ্যালারিতে যোগ করুন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            templatesList.forEach { template ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = Color(0xFF0284C7), shape = RoundedCornerShape(4.dp)) {
                                Text(text = template.categoryName, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = template.templateCode, color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = template.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "${template.colorMode} • ${template.paperStock} • ${template.printSize} • ${template.finishing}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = template.priceText, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF10B981))
                    }
                }
            }
        } else {
            // TAB 2: Order Form Text & Pricing Dynamic CMS Settings
            val currentOrderConfig = remember { cmsRepository.getOrderFormConfig() }
            var cfgTitle by remember { mutableStateOf(currentOrderConfig.formTitle) }
            var cfgBadge by remember { mutableStateOf(currentOrderConfig.quickCheckoutBadge) }
            var cfgSubtitle by remember { mutableStateOf(currentOrderConfig.formSubtitle) }
            var cfgSec1 by remember { mutableStateOf(currentOrderConfig.customerSectionTitle) }
            var cfgSec2 by remember { mutableStateOf(currentOrderConfig.designSectionTitle) }
            var cfgSec3 by remember { mutableStateOf(currentOrderConfig.instructionSectionTitle) }
            var cfgPlaceholder by remember { mutableStateOf(currentOrderConfig.instructionPlaceholder) }
            var cfgButton by remember { mutableStateOf(currentOrderConfig.confirmButtonText) }
            var cfgDhakaCharge by remember { mutableStateOf(currentOrderConfig.insideDhakaDeliveryCharge.toInt().toString()) }
            var cfgOutsideCharge by remember { mutableStateOf(currentOrderConfig.outsideDhakaDeliveryCharge.toInt().toString()) }
            var isSavedMessage by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "অর্ডার চেকআউট ফরমের ডাইনামিক টেক্সট সিএমএস কন্ট্রোল", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = cfgTitle,
                        onValueChange = { cfgTitle = it },
                        label = { Text("ফরমের হেডার শিরোনাম", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = cfgBadge,
                        onValueChange = { cfgBadge = it },
                        label = { Text("ফরমের কুইক ট্যাগ ব্যাজ", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = cfgSubtitle,
                        onValueChange = { cfgSubtitle = it },
                        label = { Text("ফরমের সাবটাইটেল নির্দেশিকা", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = cfgSec1,
                        onValueChange = { cfgSec1 = it },
                        label = { Text("১ম সেকশন লেবেল (Customer Info)", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = cfgSec2,
                        onValueChange = { cfgSec2 = it },
                        label = { Text("২য় সেকশন লেবেল (Design Choice)", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = cfgSec3,
                        onValueChange = { cfgSec3 = it },
                        label = { Text("৩য় সেকশন লেবেল (Special Instructions)", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = cfgPlaceholder,
                        onValueChange = { cfgPlaceholder = it },
                        label = { Text("বিশেষ নির্দেশনা ইনপুট ফিল্ডের প্লেসহোল্ডার", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = cfgButton,
                        onValueChange = { cfgButton = it },
                        label = { Text("অর্ডার কনফার্ম বাটন টেক্সট", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = cfgDhakaCharge,
                            onValueChange = { cfgDhakaCharge = it },
                            label = { Text("ঢাকার ভেতরে ডেলিভারি চার্জ (৳)", color = Color(0xFF94A3B8)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = cfgOutsideCharge,
                            onValueChange = { cfgOutsideCharge = it },
                            label = { Text("ঢাকার বাইরে ডেলিভারি চার্জ (৳)", color = Color(0xFF94A3B8)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isSavedMessage) {
                        Text(
                            text = "✓ অর্ডার ফরমের টেক্সট ও প্রাইসিং সেটিং সফলভাবে সেভ হয়েছে!",
                            color = Color(0xFF10B981),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            val updatedConfig = com.sucharu.sucharupro.data.api.model.cms.CmsOrderFormConfigDto(
                                formTitle = cfgTitle,
                                quickCheckoutBadge = cfgBadge,
                                formSubtitle = cfgSubtitle,
                                customerSectionTitle = cfgSec1,
                                designSectionTitle = cfgSec2,
                                instructionSectionTitle = cfgSec3,
                                instructionPlaceholder = cfgPlaceholder,
                                confirmButtonText = cfgButton,
                                insideDhakaDeliveryCharge = cfgDhakaCharge.toDoubleOrNull() ?: 60.0,
                                outsideDhakaDeliveryCharge = cfgOutsideCharge.toDoubleOrNull() ?: 120.0
                            )
                            cmsRepository.updateOrderFormConfig(updatedConfig)
                            isSavedMessage = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "ফরমের সেটিং সেভ করুন", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
