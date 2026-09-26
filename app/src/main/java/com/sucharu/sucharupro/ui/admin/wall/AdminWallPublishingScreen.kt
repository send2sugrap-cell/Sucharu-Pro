package com.sucharu.sucharupro.ui.admin.wall

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ViewCarousel
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sucharu.sucharupro.domain.model.offer.AudienceType
import com.sucharu.sucharupro.domain.model.wall.WallCategoryType
import com.sucharu.sucharupro.domain.model.wall.WallPublishStatus

/**
 * Form 06 — Admin Wall / Section / Publishing Control Workspace.
 */
@Composable
fun AdminWallPublishingScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminWallPublishingViewModel = viewModel { AdminWallPublishingViewModel() }
) {
    val wall = viewModel.activeWallConfig
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
                        text = "FORM 06 • Server-Driven Wall & Section Publishing",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "সার্ভার-ড্রিভেন ওয়াল ক্যানোনিকাল কন্ট্রোল হাব",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "পাবলিক, গেস্ট, কাস্টমার ও এ্যাফিলিয়েট ওয়ালের সেকশন কম্পোজিশন, অডিয়েন্স ভিউ ও সার্ভার রেন্ডারিং কন্ট্রোল",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 01: Wall Identity
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Dashboard, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "১. ওয়াল আইডেন্টিটি ও ক্যাটাগরি (Wall Selection)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = wall.wallTitle,
                    onValueChange = { v -> viewModel.updateActiveWall(wall.copy(wallTitle = v)) },
                    label = { Text("ওয়াল শিরোনাম (Wall Title)", color = Color(0xFF94A3B8)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(text = "ওয়াল টাইপ সিলেক্ট করুন (Wall Type):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WallCategoryType.entries.forEach { type ->
                        val isSelected = wall.wallType == type
                        Surface(
                            color = if (isSelected) Color(0xFF0284C7) else Color(0xFF334155),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { viewModel.updateActiveWall(wall.copy(wallType = type)) }
                        ) {
                            Text(text = type.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SECTION 02: Audience Visibility Toggles
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "২. ওয়াল অডিয়েন্স ভিউ ফিল্টার (Audience Visibility)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))

                ToggleRow("পাবলিক অডিয়েন্সের জন্য ভিজিবল (Public)", wall.publicVisibility) {
                    viewModel.updateActiveWall(wall.copy(publicVisibility = it))
                }
                ToggleRow("গেস্ট ব্যবহারকারীর জন্য ভিজিবল (Guest)", wall.guestVisibility) {
                    viewModel.updateActiveWall(wall.copy(guestVisibility = it))
                }
                ToggleRow("কাস্টমার প্যানেলে ভিজিবল (Customer)", wall.customerVisibility) {
                    viewModel.updateActiveWall(wall.copy(customerVisibility = it))
                }
                ToggleRow("এ্যাফিলিয়েট প্যানেলে ভিজিবল (Affiliate)", wall.affiliateVisibility) {
                    viewModel.updateActiveWall(wall.copy(affiliateVisibility = it))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SECTION 03: Interactive Wall Resolver Simulator
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0284C7).copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, Color(0xFF0284C7))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "⚡ সার্ভার-ড্রিভেন ওয়াল রেজোলিউশন টেস্ট সিমুলেটর:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.testResolveWall(AudienceType.GUEST) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Guest Wall Resolve", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { viewModel.testResolveWall(AudienceType.CUSTOMER) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Customer Wall Resolve", fontSize = 11.sp)
                    }
                }

                viewModel.resolvedWallResponse?.let { res ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(color = Color(0xFF1E293B), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "সার্ভার রেজোলিউশন রেজাল্ট (${res.audienceType}):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            Text(text = "Wall: ${res.wallTitle} | Total Resolved Sections: ${res.sections.size}", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save & Publish Action Buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { viewModel.saveDraft() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "ড্রাফট সেভ করুন", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { viewModel.publishWall() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "ওয়াল পাবলিশ করুন", fontWeight = FontWeight.Bold)
            }
        }

        viewModel.statusMessage?.let { msg ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = msg, color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
