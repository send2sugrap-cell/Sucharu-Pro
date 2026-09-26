package com.sucharu.sucharupro.ui.admin.pricing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Form 04 — Admin Pricing & Commercial Rules Management Workspace.
 */
@Composable
fun AdminPricingRulesScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminPricingRulesViewModel = viewModel { AdminPricingRulesViewModel() }
) {
    val config = viewModel.activePriceConfig
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Page Header
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "FORM 04 • Pricing & Commercial Rules",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "বাণিজ্যিক প্রাইসিং ও কমার্শিয়াল রুলস হাব",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "ক্যানোনিকাল প্রাইসিং রুলস, কোয়ান্টিটি টায়ার এবং ইমিউটেবল অর্ডার প্রাইস স্ন্যাপশট কন্ট্রোল",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 01: Base & Quantity Pricing
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "১. বেস প্রাইসিং ও কোয়ান্টিটি কনফিগ (Base Pricing)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = config.basePrice.toString(),
                        onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateActiveConfig(config.copy(basePrice = it)) } },
                        label = { Text("বেস প্রাইস (৳)", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = config.baseQuantity.toString(),
                        onValueChange = { v -> v.toIntOrNull()?.let { viewModel.updateActiveConfig(config.copy(baseQuantity = it)) } },
                        label = { Text("বেস পরিমাণ (Qty)", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = config.minOrderQuantity.toString(),
                        onValueChange = { v -> v.toIntOrNull()?.let { viewModel.updateActiveConfig(config.copy(minOrderQuantity = it)) } },
                        label = { Text("মিনিমাম অর্ডার পরিমাণ", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = config.maxOrderQuantity.toString(),
                        onValueChange = { v -> v.toIntOrNull()?.let { viewModel.updateActiveConfig(config.copy(maxOrderQuantity = it)) } },
                        label = { Text("ম্যাক্সিমাম অর্ডার পরিমাণ", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SECTION 02: Charges, Taxes & Delivery
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Calculate, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "২. ডেলিভারি চার্জ, ট্যাক্স ও সারচার্জ (Charges & Tax)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = config.insideDhakaDeliveryCharge.toString(),
                        onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateActiveConfig(config.copy(insideDhakaDeliveryCharge = it)) } },
                        label = { Text("ঢাকার ভেতরে ডেলিভারি (৳)", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = config.outsideDhakaDeliveryCharge.toString(),
                        onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateActiveConfig(config.copy(outsideDhakaDeliveryCharge = it)) } },
                        label = { Text("ঢাকার বাইরে ডেলিভারি (৳)", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SECTION 03: Interactive Evaluator & Invariant Test
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0284C7).copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, Color(0xFF0284C7))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "⚡ কমার্শিয়াল প্রাইস ইভালুয়েটর ও স্ন্যাপশট ইনভেরিয়েন্ট টেস্ট:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.runPriceEvaluation(1000) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("১০০০ পিস হিসাব করুন", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { viewModel.testHistoricalPriceInvariant() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ইনভেরিয়েন্ট টেস্ট", fontSize = 11.sp)
                    }
                }

                viewModel.evaluationResult?.let { eval ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(color = Color(0xFF1E293B), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "ইভালুয়েশন ফলাফল (1000 Pcs):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                            Text(text = "সর্বমোট মূল্য: ৳${eval.grandTotalAmount.toInt()} (Subtotal: ৳${eval.subtotalAmount.toInt()} + Delivery: ৳${eval.deliveryCharge.toInt()})", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save Price Config Button
        Button(
            onClick = { viewModel.savePriceConfig() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "প্রাইসিং কনফিগারেশন সেভ করুন", fontWeight = FontWeight.Bold)
        }

        viewModel.statusMessage?.let { msg ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = msg, color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
