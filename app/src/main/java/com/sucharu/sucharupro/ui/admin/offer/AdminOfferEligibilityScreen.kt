package com.sucharu.sucharupro.ui.admin.offer

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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storefront
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
import com.sucharu.sucharupro.domain.model.offer.PromotionalOffer
import com.sucharu.sucharupro.domain.model.offer.WallType

/**
 * Form 03 — Admin Offer & Audience Eligibility Management Workspace.
 */
@Composable
fun AdminOfferEligibilityScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminOfferEligibilityViewModel = viewModel { AdminOfferEligibilityViewModel() }
) {
    val offer = viewModel.activeOfferState
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
                        text = "FORM 03 • Offer & Audience Eligibility",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "অফার ও অডিয়েন্স এলিজিবিলিটি কনফিগারেশন",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "অফার রিডিম এলিজিবিলিটি এবং ওয়াল ভিজিবিলিটি ফিল্টারের পৃথকীকরণ হাব",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 01: Offer Identity
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.LocalOffer, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "১. অফার আইডেন্টিটি (Offer Identity)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = offer.offerName,
                        onValueChange = { v -> viewModel.updateActiveOffer(offer.copy(offerName = v)) },
                        label = { Text("অফারের নাম", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = offer.offerCode,
                        onValueChange = { v -> viewModel.updateActiveOffer(offer.copy(offerCode = v)) },
                        label = { Text("অফার কোড", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = offer.description ?: "",
                    onValueChange = { v -> viewModel.updateActiveOffer(offer.copy(description = v)) },
                    label = { Text("অফার বিবরণ", color = Color(0xFF94A3B8)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SECTION 02: Offer Eligibility (Who can REDEEM)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "২. অফার এলিজিবিলিটি — রিডিম পারমিশন (Who Can REDEEM)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))

                ToggleRow("সবাই অফার রিডিম করতে পারবে (Everyone = ON)", offer.isEveryoneEligible) {
                    viewModel.updateActiveOffer(offer.copy(isEveryoneEligible = it))
                }
                ToggleRow("গেস্ট কাস্টমার রিডিম করতে পারবে (Guest Eligible)", offer.isGuestEligible) {
                    viewModel.updateActiveOffer(offer.copy(isGuestEligible = it))
                }
                ToggleRow("রেজিস্টার্ড কাস্টমার রিডিম করতে পারবে (Customer Eligible)", offer.isCustomerEligible) {
                    viewModel.updateActiveOffer(offer.copy(isCustomerEligible = it))
                }
                ToggleRow("এ্যাফিলিয়েট পার্টনার রিডিম করতে পারবে (Affiliate Eligible)", offer.isAffiliateEligible) {
                    viewModel.updateActiveOffer(offer.copy(isAffiliateEligible = it))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SECTION 06: Wall Visibility (Where DISPLAYED)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Storefront, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "৩. ওয়াল ভিজিবিলিটি — ডিসপ্লে ফিল্টার (Where DISPLAYED)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))

                ToggleRow("পাবলিক/গেস্ট ওয়ালে দেখাবে (Public Wall Visible)", offer.publicGuestWallVisible) {
                    viewModel.updateActiveOffer(offer.copy(publicGuestWallVisible = it))
                }
                ToggleRow("কাস্টমার ওয়ালে দেখাবে (Customer Wall Visible)", offer.customerWallVisible) {
                    viewModel.updateActiveOffer(offer.copy(customerWallVisible = it))
                }
                ToggleRow("এ্যাফিলিয়েট ওয়ালে দেখাবে (Affiliate Wall Visible)", offer.affiliateWallVisible) {
                    viewModel.updateActiveOffer(offer.copy(affiliateWallVisible = it))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SECTION 07: Invariant Verification Matrix Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0284C7).copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, Color(0xFF0284C7))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "⚡ অডিয়েন্স এলিজিবিলিটি ও ওয়াল ভিজিবিলিটি সেপারেশন চেক matrix:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• Affiliate Eligibility = ${if (offer.isAffiliateEligible) "ON" else "OFF"} | Affiliate Wall = ${if (offer.affiliateWallVisible) "ON" else "OFF"}",
                    fontSize = 11.sp,
                    color = Color.White
                )
                Text(
                    text = "• Customer Eligibility = ${if (offer.isCustomerEligible) "ON" else "OFF"} | Customer Wall = ${if (offer.customerWallVisible) "ON" else "OFF"}",
                    fontSize = 11.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "✓ Eligibility (Who REDEEMS) and Wall Visibility (Where DISPLAYED) are 100% separate independent flags!",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save Action Button
        Button(
            onClick = { viewModel.saveOffer() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "অফার ও অডিয়েন্স কনফিগারেশন সেভ করুন", fontWeight = FontWeight.Bold)
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
