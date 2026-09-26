package com.sucharu.sucharupro.ui.admin.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sucharu.sucharupro.domain.model.design.DesignPublishStatus
import com.sucharu.sucharupro.domain.model.design.VisualDesignConfiguration

/**
 * Form 02 — Admin Visual Design Studio Complete Presentation Control Workspace.
 */
@Composable
fun AdminVisualDesignStudioScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminVisualDesignStudioViewModel = viewModel { AdminVisualDesignStudioViewModel() }
) {
    val activeConfig = viewModel.activeDesignState

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // LEFT AREA: Studio Controls
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "FORM 02 • Admin Visual Design Studio",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                        Text(
                            text = activeConfig.designName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Surface(
                        color = if (activeConfig.status == DesignPublishStatus.PUBLISHED) Color(0xFF10B981) else Color(0xFFF59E0B),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = activeConfig.status.name,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Layout & Dimensions Control Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "১. কার্ড ও লেআউট সাইজ (Card & Layout)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = activeConfig.cardWidthDp.toString(),
                            onValueChange = { v ->
                                v.toIntOrNull()?.let { viewModel.updateActiveDesign(activeConfig.copy(cardWidthDp = it)) }
                            },
                            label = { Text("Width (dp)", color = Color(0xFF94A3B8)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = activeConfig.cardHeightDp.toString(),
                            onValueChange = { v ->
                                v.toIntOrNull()?.let { viewModel.updateActiveDesign(activeConfig.copy(cardHeightDp = it)) }
                            },
                            label = { Text("Height (dp)", color = Color(0xFF94A3B8)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Background, Border & Radius Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "২. ব্যাকগ্রাউন্ড, বর্ডার ও রেডিয়াস (Style & Border)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = activeConfig.backgroundColorHex,
                            onValueChange = { v -> viewModel.updateActiveDesign(activeConfig.copy(backgroundColorHex = v)) },
                            label = { Text("Background Hex", color = Color(0xFF94A3B8)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = activeConfig.borderColorHex,
                            onValueChange = { v -> viewModel.updateActiveDesign(activeConfig.copy(borderColorHex = v)) },
                            label = { Text("Border Hex", color = Color(0xFF94A3B8)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = activeConfig.borderRadiusDp.toString(),
                        onValueChange = { v -> v.toIntOrNull()?.let { viewModel.updateActiveDesign(activeConfig.copy(borderRadiusDp = it)) } },
                        label = { Text("Corner Radius (dp)", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Element Visibility Toggles Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "৩. এলিমেন্ট ভিউ ফিল্টার (Element Visibility Toggles)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.height(8.dp))

                    ToggleRow("ছবি / মকআপ দৃশ্যমান (Show Image)", activeConfig.showImage) {
                        viewModel.updateActiveDesign(activeConfig.copy(showImage = it))
                    }
                    ToggleRow("ব্যাজ ট্যাগ দৃশ্যমান (Show Badge)", activeConfig.showBadge) {
                        viewModel.updateActiveDesign(activeConfig.copy(showBadge = it))
                    }
                    ToggleRow("টেকনিক্যাল স্পেক্স দৃশ্যমান (Show Specs)", activeConfig.showSpecs) {
                        viewModel.updateActiveDesign(activeConfig.copy(showSpecs = it))
                    }
                    ToggleRow("দাম দৃশ্যমান (Show Price)", activeConfig.showPrice) {
                        viewModel.updateActiveDesign(activeConfig.copy(showPrice = it))
                    }
                    ToggleRow("অর্ডার বাটন দৃশ্যমান (Show CTA Button)", activeConfig.showCta) {
                        viewModel.updateActiveDesign(activeConfig.copy(showCta = it))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. CTA Button Styling Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "৪. অ্যাকশন বাটন ডিজাইন (CTA Button Style)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = activeConfig.ctaButtonText,
                        onValueChange = { v -> viewModel.updateActiveDesign(activeConfig.copy(ctaButtonText = v)) },
                        label = { Text("Button Text", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = activeConfig.ctaButtonColorHex,
                        onValueChange = { v -> viewModel.updateActiveDesign(activeConfig.copy(ctaButtonColorHex = v)) },
                        label = { Text("Button Color Hex", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.saveDraft() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ড্রাফট সেভ", fontSize = 12.sp)
                }

                Button(
                    onClick = { viewModel.publishActiveDesign() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("পাবলিশ করুন", fontSize = 12.sp)
                }

                Button(
                    onClick = { viewModel.duplicateCurrentDesign() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ডুপ্লিকেট", fontSize = 12.sp)
                }
            }

            viewModel.statusMessage?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = msg, color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // RIGHT AREA: Real-time Live Preview Workspace
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFF020617))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "লাইভ কাস্টমার চাক্ষুষ প্রিভিউ (Live Preview)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dynamic Sample Card Rendered strictly from activeConfig State
            val bgColor = parseColorHex(activeConfig.backgroundColorHex, Color(0xFF1E293B))
            val borderColor = parseColorHex(activeConfig.borderColorHex, Color(0xFF334155))
            val ctaColor = parseColorHex(activeConfig.ctaButtonColorHex, Color(0xFFEA580C))

            Card(
                shape = RoundedCornerShape(activeConfig.borderRadiusDp.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor),
                border = if (activeConfig.borderEnable) BorderStroke(activeConfig.borderWidthDp.dp, borderColor) else null,
                modifier = Modifier
                    .width(activeConfig.cardWidthDp.dp)
                    .height(activeConfig.cardHeightDp.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(activeConfig.paddingTopDp.dp)
                ) {
                    if (activeConfig.showImage) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF334155)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(40.dp))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (activeConfig.showBadge) {
                        Surface(color = Color(0xFF0284C7), shape = RoundedCornerShape(4.dp)) {
                            Text(text = "বেস্টসেলার #TMPL-101", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (activeConfig.showTitle) {
                        Text(text = "১০০০ মেট ফিনিশ ভিজিটিং কার্ড", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    if (activeConfig.showSubtitle) {
                        Text(text = "৪ কালার (CMYK) • ৩০০ GSM আর্ট কার্ড", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (activeConfig.showPrice) {
                        Text(text = "৳ ৩৫০ / ১,০০০ পিস", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF10B981))
                    }

                    if (activeConfig.showCta) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(containerColor = ctaColor),
                            shape = RoundedCornerShape(activeConfig.ctaBorderRadiusDp.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text(text = activeConfig.ctaButtonText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = Color.White)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF10B981))
        )
    }
}

private fun parseColorHex(hex: String, fallback: Color): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) Color(colorInt or 0xFF00000000) else Color(colorInt)
    } catch (e: Exception) {
        fallback
    }
}
