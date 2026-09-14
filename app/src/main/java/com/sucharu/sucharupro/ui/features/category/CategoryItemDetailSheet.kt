package com.sucharu.sucharupro.ui.features.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.sucharu.sucharupro.data.repository.category.CategoryItemDetail

/**
 * Modal BottomSheet Spec Sheet for Category Item Details.
 * Displays title, sample gallery, paper GSM specs, quantity selector, pricing breakdown,
 * and ERP order placement action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryItemDetailSheet(
    item: CategoryItemDetail?,
    onDismiss: () -> Unit,
    onOrderRequestClick: (item: CategoryItemDetail) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    if (item == null) return

    var selectedQuantity by remember { mutableStateOf("500 Pcs") }
    var customerInstructions by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.4f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with Icon & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(item.bgColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = item.iconColor,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF263238)
                    )
                    Text(
                        text = "ক্যাটাগরি: ${item.categoryKey} • কমার্শিয়াল পিন্ট সেবা",
                        fontSize = 12.sp,
                        color = Color(0xFF0284C7),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sample Gallery Carousel Showcase
            Text(
                text = "প্রিন্ট ও স্যাম্পল গ্যালারি",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF263238)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SampleGalleryCard("ম্যাট ল্যামিনেশন", item.bgColor, item.iconColor, modifier = Modifier.weight(1f))
                SampleGalleryCard("স্পট ইউভি গ্লস", item.bgColor, item.iconColor, modifier = Modifier.weight(1f))
                SampleGalleryCard("গোল্ড ফয়েল", item.bgColor, item.iconColor, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Full Specifications Section
            Text(
                text = "প্রিন্টিং স্পেসিফিকেশন ও মেটেরিয়াল",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF263238)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.subtitleGsm,
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Order Quantity Selection
            Text(
                text = "অর্ডার পরিমাণ সিলেক্ট করুন",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF263238)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("500 Pcs", "1000 Pcs", "2500 Pcs", "5000 Pcs").forEach { qty ->
                    val isSelected = selectedQuantity == qty
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF0284C7) else Color(0xFFF1F5F9))
                            .border(1.dp, if (isSelected) Color(0xFF0284C7) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .clickable { selectedQuantity = qty },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = qty,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF334155)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Price Breakdown Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF6F8FA), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "একক প্রাইস রেট",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = item.estimatedPriceFormatted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "মোট আনুমানিক খরচ",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "৳ ১,৮৫০.০০",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Customer Instructions Field
            OutlinedTextField(
                value = customerInstructions,
                onValueChange = { customerInstructions = it },
                label = { Text("বিশেষ কোনো ইনস্ট্রাকশন / নোট (ঐচ্ছিক)", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Order / Quotation CTA Button
            Button(
                onClick = {
                    onDismiss()
                    onOrderRequestClick(item)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0284C7),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "অর্ডার করুন (Order Now)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SampleGalleryCard(
    label: String,
    bgColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, iconColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF334155)
            )
        }
    }
}
