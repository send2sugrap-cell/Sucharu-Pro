package com.sucharu.sucharupro.ui.customer.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sucharu.sucharupro.ui.navigation.AppDestination

enum class MockupType {
    POSTER, BUSINESS_CARD, LEAFLET, STICKER
}

data class OfferGalleryItem(
    val templateId: String,
    val templateCode: String,
    val badgeLabel: String?,
    val title: String,
    val category: String,
    val colorMode: String,
    val paperStock: String,
    val printSize: String,
    val finishing: String,
    val suitabilityDescription: String,
    val priceText: String,
    val perUnitRate: String,
    val mockupType: MockupType
)

/**
 * Modern High-Converting "আজকের বিশেষ অফার ডিজাইন গ্যালারি" Mobile UI Screen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpecialOfferGalleryScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onOrderClick: (OfferGalleryItem) -> Unit = {},
    onCustomizeClick: (OfferGalleryItem) -> Unit = {},
    onCustomOrderClick: () -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf("সব") }
    var previewItem by remember { mutableStateOf<OfferGalleryItem?>(null) }

    val categories = listOf("সব", "পোস্টার", "ভিজিটিং কার্ড", "লিফলেট", "স্টিকার", "অন্যান্য")

    val sampleOfferItems = listOf(
        OfferGalleryItem(
            templateId = "OFFER-001",
            templateCode = "#TMPL-101",
            badgeLabel = "বেস্টসেলার",
            title = "প্রিমিয়াম ইভেন্ট পোস্টার ডিজাইন",
            category = "পোস্টার",
            colorMode = "৪ কালার (CMYK)",
            paperStock = "১৫০ GSM আর্ট পেপার",
            printSize = "১৮\" × ২৩\" (Demy)",
            finishing = "গ্লস ল্যামিনেশন",
            suitabilityDescription = "প্রচারণা ও ইভেন্টের জন্য সেরা কোয়ালিটি",
            priceText = "৳ ১,২০০ / ১,০০০ পিস",
            perUnitRate = "(৳ ১.২০ / পিস)",
            mockupType = MockupType.POSTER
        ),
        OfferGalleryItem(
            templateId = "OFFER-002",
            templateCode = "#TMPL-203",
            badgeLabel = null,
            title = "প্রফেশনাল বিজনেস কার্ড",
            category = "ভিজিটিং কার্ড",
            colorMode = "৪ কালার (CMYK)",
            paperStock = "৩০০ GSM আর্ট কার্ড",
            printSize = "২\" × ৩.৫\" (স্ট্যান্ডার্ড)",
            finishing = "ম্যাট ফিনিশ + স্পট UV",
            suitabilityDescription = "কর্পোরেট পরিচয়ের জন্য নিখুঁত ডিজাইন",
            priceText = "৳ ৮০০ / ১,০০০ পিস",
            perUnitRate = "(৳ ০.৮০ / পিস)",
            mockupType = MockupType.BUSINESS_CARD
        ),
        OfferGalleryItem(
            templateId = "OFFER-003",
            templateCode = "#TMPL-307",
            badgeLabel = null,
            title = "রঙিন প্রচারপত্র (লিফলেট)",
            category = "লিফলেট",
            colorMode = "৪ কালার (CMYK)",
            paperStock = "১০০ GSM আর্ট পেপার",
            printSize = "A4 সাইজ",
            finishing = "কোন ফিনিশ নেই",
            suitabilityDescription = "দ্রুত এবং সস্তা প্রচারের জন্য আদর্শ",
            priceText = "৳ ১,৫০০ / ১,০০০ পিস",
            perUnitRate = "(৳ ১.৫০ / পিস)",
            mockupType = MockupType.LEAFLET
        )
    )

    val filteredItems = if (selectedCategory == "সব") sampleOfferItems else sampleOfferItems.filter { it.category == selectedCategory }

    if (previewItem != null) {
        OfferDesignPreviewDialog(
            item = previewItem!!,
            onDismiss = { previewItem = null },
            onOrderNow = {
                onOrderClick(previewItem!!)
                previewItem = null
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF1F5F9),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(bottom = 12.dp)
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1E293B)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "আজকের বিশেষ অফার ডিজাইন গ্যালারি",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Horizontal Category Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = cat == selectedCategory
                        Surface(
                            color = if (isSelected) Color(0xFFEA580C) else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFFEA580C) else Color(0xFFE2E8F0)),
                            modifier = Modifier.clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) Color.White else Color(0xFF475569),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Fixed Bottom Action Bar for Custom Order Request
            Surface(
                color = Color(0xFFEA580C),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onCustomOrderClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+ কাস্টম রিকোয়েস্ট / নতুন অর্ডার করুন (Custom Order)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(filteredItems, key = { it.templateId }) { item ->
                OfferFeatureShowcaseCard(
                    item = item,
                    onZoomClick = { previewItem = item },
                    onOrderClick = { onOrderClick(item) },
                    onCustomizeClick = { onCustomizeClick(item) }
                )
            }
        }
    }
}

/**
 * Feature Showcase Card Item.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OfferFeatureShowcaseCard(
    item: OfferGalleryItem,
    onZoomClick: () -> Unit,
    onOrderClick: () -> Unit,
    onCustomizeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Left Visual Mockup Showcase Box
                Box(
                    modifier = Modifier
                        .width(135.dp)
                        .height(175.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Realistic Graphic Mockup Rendering
                    MockupGraphicView(type = item.mockupType, title = item.title)

                    // Zoom Overlay Button at Bottom Right
                    Surface(
                        color = Color.White.copy(alpha = 0.9f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(28.dp)
                            .clickable { onZoomClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Zoom",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Right Technical Specs & Price/Action Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(175.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        // Title
                        Text(
                            text = item.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 19.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Technical Specification Pills
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SpecPill(
                                label = item.colorMode,
                                icon = Icons.Default.Palette,
                                bgColor = Color(0xFFE0F2FE),
                                textColor = Color(0xFF0369A1)
                            )
                            SpecPill(
                                label = item.paperStock,
                                icon = Icons.Default.Description,
                                bgColor = Color(0xFFFEF3C7),
                                textColor = Color(0xFFB45309)
                            )
                            SpecPill(
                                label = item.printSize,
                                icon = Icons.Default.Star,
                                bgColor = Color(0xFFDCFCE7),
                                textColor = Color(0xFF15803D)
                            )
                            SpecPill(
                                label = item.finishing,
                                icon = Icons.Default.AutoAwesome,
                                bgColor = Color(0xFFF3E8FF),
                                textColor = Color(0xFF6B21A8)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = item.suitabilityDescription,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Price & Actions
                    Column {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = item.priceText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFEA580C)
                            )
                            Text(
                                text = item.perUnitRate,
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(bottom = 1.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Primary Order Button
                            Button(
                                onClick = onOrderClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(34.dp)
                            ) {
                                Text(
                                    text = "অর্ডার করুন",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Secondary Customize Button
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clickable { onCustomizeClick() }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = null,
                                        tint = Color(0xFF475569),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "কাস্টমাইজ",
                                        color = Color(0xFF334155),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Top Right Ribbon Badge
            Surface(
                color = Color(0xFF0284C7),
                shape = RoundedCornerShape(bottomStart = 10.dp, topEnd = 16.dp),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(
                    text = if (item.badgeLabel != null) "${item.templateCode}\n${item.badgeLabel}" else item.templateCode,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Specification Badge Pill.
 */
@Composable
private fun SpecPill(
    label: String,
    icon: ImageVector,
    bgColor: Color,
    textColor: Color
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Realistic Mockup Visual Component.
 */
@Composable
private fun MockupGraphicView(
    type: MockupType,
    title: String
) {
    val gradient = when (type) {
        MockupType.POSTER -> Brush.verticalGradient(listOf(Color(0xFF312E81), Color(0xFF7E22CE), Color(0xFFBE185D)))
        MockupType.BUSINESS_CARD -> Brush.horizontalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))
        MockupType.LEAFLET -> Brush.verticalGradient(listOf(Color(0xFF0284C7), Color(0xFF0D9488)))
        MockupType.STICKER -> Brush.linearGradient(listOf(Color(0xFFD97706), Color(0xFFEA580C)))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "সুচারু প্রিন্টিং",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Full Screen High-Res Design Preview Dialog.
 */
@Composable
private fun OfferDesignPreviewDialog(
    item: OfferGalleryItem,
    onDismiss: () -> Unit,
    onOrderNow: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    MockupGraphicView(type = item.mockupType, title = item.title)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = item.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${item.colorMode} • ${item.paperStock} • ${item.printSize} • ${item.finishing}",
                    fontSize = 11.sp,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = item.priceText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFEA580C)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("বন্ধ করুন", color = Color(0xFF64748B))
                    }

                    Button(
                        onClick = onOrderNow,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("এই অফার অর্ডার করুন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
