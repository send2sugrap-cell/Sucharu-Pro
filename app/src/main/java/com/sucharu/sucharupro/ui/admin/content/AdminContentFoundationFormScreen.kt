package com.sucharu.sucharupro.ui.admin.content

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sucharu.sucharupro.domain.model.content.PublicationStatus

/**
 * Admin Form 01 — Content & Product Foundation Management Screen.
 */
@Composable
fun AdminContentFoundationFormScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminContentFoundationViewModel = viewModel { AdminContentFoundationViewModel() }
) {
    val scrollState = rememberScrollState()

    var editingId by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var productName by remember { mutableStateOf("") }
    var productCode by remember { mutableStateOf("") }
    var categoryName by remember { mutableStateOf("অফসেট প্রিন্টিং") }
    var templateCode by remember { mutableStateOf("#TMPL-101") }
    var badgeText by remember { mutableStateOf("বেস্টসেলার") }
    var availableQty by remember { mutableStateOf("500") }
    var selectedStatus by remember { mutableStateOf(PublicationStatus.DRAFT) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Header Banner
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
                        text = "FORM 01 • Content & Product Foundation",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "কনটেন্ট ও প্রোডাক্ট ফাউন্ডেশন সিএমএস",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "গ্যালারি, প্রোডাক্ট ও পাবলিক ওয়ালের ক্যানোনিকাল মেটাডেটা ও পাবলিশিং লাইফসাইকেল কন্ট্রোল",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Form Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "১. প্রোডাক্ট মেটাডেটা ও আইডেন্টিটি (Basic Identity)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("কনটেন্ট শিরোনাম (Title)", color = Color(0xFF94A3B8)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("সাবটাইটেল / সংক্ষিপ্ত বিবরণ (Subtitle)", color = Color(0xFF94A3B8)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = productName, onValueChange = { productName = it }, label = { Text("প্রোডাক্ট নাম", color = Color(0xFF94A3B8)) }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = productCode, onValueChange = { productCode = it }, label = { Text("প্রোডাক্ট কোড (SKU)", color = Color(0xFF94A3B8)) }, modifier = Modifier.weight(1f), singleLine = true)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = categoryName, onValueChange = { categoryName = it }, label = { Text("ক্যাটাগরি", color = Color(0xFF94A3B8)) }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = templateCode, onValueChange = { templateCode = it }, label = { Text("টেমপ্লেট কোড", color = Color(0xFF94A3B8)) }, modifier = Modifier.weight(1f), singleLine = true)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = badgeText, onValueChange = { badgeText = it }, label = { Text("ব্যাজ টেক্সট (যেমন: বেস্টসেলার)", color = Color(0xFF94A3B8)) }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = availableQty, onValueChange = { availableQty = it }, label = { Text("উপলব্ধ পরিমাণ (Qty)", color = Color(0xFF94A3B8)) }, modifier = Modifier.weight(1f), singleLine = true)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "পাবলিশিং লাইফসাইকেল স্ট্যাটাস (Publishing Lifecycle):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PublicationStatus.entries.forEach { status ->
                        val isSelected = selectedStatus == status
                        Surface(
                            color = if (isSelected) Color(0xFF0284C7) else Color(0xFF334155),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { selectedStatus = status }
                        ) {
                            Text(text = status.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.createOrUpdateContent(
                                contentId = editingId,
                                title = title,
                                subtitle = subtitle,
                                description = description,
                                productName = productName,
                                productCode = productCode,
                                categoryName = categoryName,
                                templateCode = templateCode,
                                badgeText = badgeText,
                                availableQty = availableQty.toIntOrNull() ?: 0,
                                publicationStatus = selectedStatus
                            )
                            title = ""
                            subtitle = ""
                            editingId = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "কনটেন্ট ও প্রোডাক্ট মেটাডেটা রেকর্ড সেভ করুন", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Existing Records List
        Text(text = "বিদ্যমান ক্যানোনিকাল কনটেন্ট ও প্রোডাক্ট রেকর্ডস (${viewModel.contentRecords.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
        Spacer(modifier = Modifier.height(8.dp))

        viewModel.contentRecords.forEach { record ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = Color(0xFF0284C7), shape = RoundedCornerShape(4.dp)) {
                            Text(text = record.categoryName, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Surface(
                            color = when (record.publicationStatus) {
                                PublicationStatus.PUBLISHED -> Color(0xFF10B981)
                                PublicationStatus.SCHEDULED -> Color(0xFFF59E0B)
                                else -> Color(0xFF64748B)
                            },
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(text = record.publicationStatus.name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = record.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "Code: ${record.productCode} • Template: ${record.templateCode}", fontSize = 11.sp, color = Color(0xFF94A3B8))

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (record.publicationStatus != PublicationStatus.PUBLISHED) {
                            Button(
                                onClick = { viewModel.publishRecord(record.contentId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("পাবলিশ করুন", fontSize = 11.sp)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.unpublishRecord(record.contentId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("আনপাবলিশ করুন", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
