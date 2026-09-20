package com.sucharu.sucharupro.ui.admin.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Cash & Collection Breakdown Widget matching Image 2.
 */
@Composable
fun AdminCashCollectionCard(
    modifier: Modifier = Modifier,
    totalCollected: String = "৳ ১,৩৭,৫০০",
    receivedAmount: String = "৳ ১,২২,৩০০",
    dueAmount: String = "৳ ৪২,১০০",
    onNavigateToDestination: (AppDestination) -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNavigateToDestination(AppDestination.Admin.Finance) }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00B4D8).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = "ক্যাশ ও কালেকশন", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text(text = "আয়, পেমেন্ট ও বকেয়া অবস্থা", fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                    }
                }

                Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(text = totalCollected, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(text = "↑ +১২.৮% আজকের সংগ্রহ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Received Chip
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF064E3B),
                    border = BorderStroke(1.dp, Color(0xFF10B981))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(text = receivedAmount, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = "পেমেন্ট পেয়েছি", fontSize = 8.5.sp, color = Color(0xFFA7F3D0))
                        }
                    }
                }

                // Due Chip
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF78350F),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(text = dueAmount, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = "বকেয়া আছে", fontSize = 8.5.sp, color = Color(0xFFFDE68A))
                        }
                    }
                }
            }
        }
    }
}
