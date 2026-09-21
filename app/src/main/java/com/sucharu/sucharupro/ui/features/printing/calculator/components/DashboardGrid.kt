package com.sucharu.sucharupro.ui.features.printing.calculator.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SectorCardItem(
    val id: Int,
    val title: String,
    val englishTitle: String,
    val description: String,
    val icon: ImageVector,
    val color: androidx.compose.ui.graphics.Color
)

@Composable
fun DashboardGrid(
    onSectorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val sectors = listOf(
        SectorCardItem(
            id = 1,
            title = "১. অফসেট কমার্শিয়াল প্রজেক্ট",
            englishTitle = "Offset Commercial Job",
            description = "ফ্লায়ার, ব্রোশার, পোস্টার, প্যাড, ভিজিটিং কার্ড, ফোল্ডার ও কমার্শিয়াল জব",
            icon = Icons.Default.Print,
            color = MaterialTheme.colorScheme.primary
        ),
        SectorCardItem(
            id = 2,
            title = "২. ওয়েডিং ও ইনভিটেশন কার্ড",
            englishTitle = "Wedding & Invitation Card",
            description = "বিয়ের কার্ড, এনভেলপ, ইনসার্ট, ফয়েল ও প্রিমিয়াম ইমবসিং কার্ড",
            icon = Icons.Default.CardGiftcard,
            color = MaterialTheme.colorScheme.secondary
        ),
        SectorCardItem(
            id = 3,
            title = "৩. ডিজিটাল ব্যানার ও ভিনাইল",
            englishTitle = "Digital Large Format",
            description = "ফ্লেক্স ব্যানার, স্টার ফ্লেক্স, ভিনাইল, ফ্রস্টেড, বেকলিট ও ক্যানভাস",
            icon = Icons.Default.DisplaySettings,
            color = MaterialTheme.colorScheme.tertiary
        ),
        SectorCardItem(
            id = 4,
            title = "৪. বই ও ক্যাটালগ পাবলিকেশন",
            englishTitle = "Book & Catalog Publication",
            description = "বই, ক্যাটালগ, ম্যাগাজিন, ফর্মা গ্রুপিং, সিগনেচার ও পেপার স্পাইন",
            icon = Icons.Default.MenuBook,
            color = MaterialTheme.colorScheme.primary
        ),
        SectorCardItem(
            id = 5,
            title = "৫. এক্সিকিউটিভ ডায়েরি ও নোটবুক",
            englishTitle = "Diary & Hardbound Notebook",
            description = "হার্ডকভার ডায়েরি, রেক্সিন বাউন্ড, প্রিমিয়াম PU লেদার ও ফোম প্যাডেড",
            icon = Icons.Default.Book,
            color = MaterialTheme.colorScheme.secondary
        ),
        SectorCardItem(
            id = 6,
            title = "৬. কর্পোরেট গিফট ও মার্চেন্ডাইজ",
            englishTitle = "Gift & Promotional Items",
            description = "মগ, টি-শার্ট, ক্রেস্ট, কি-রিং, পেনড্রাইভ, সাবলিমেশন ও লেজার এনগ্রেভ",
            icon = Icons.Default.Redeem,
            color = MaterialTheme.colorScheme.tertiary
        ),
        SectorCardItem(
            id = 7,
            title = "৭. রাবার সিল ও স্ট্যাম্প",
            englishTitle = "Stamp & Rubber Seal",
            description = "সেলফ-ইঙ্কিং ফ্ল্যাশ স্ট্যাম্প, রাবার সিল ও এক্রিলিক লেজার সিল",
            icon = Icons.Default.Approval,
            color = MaterialTheme.colorScheme.primary
        ),
        SectorCardItem(
            id = 8,
            title = "৮. প্যাকেজিং কার্টন ও বক্স",
            englishTitle = "Packaging & Carton Box",
            description = "ফোল্ডিং কার্টন, সুইট বক্স, রিজিড শক্ত বক্স ও মাস্টার কেস",
            icon = Icons.Default.Inventory2,
            color = MaterialTheme.colorScheme.secondary
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(
            text = "সুচারু গ্রাফিক্স — স্মার্ট বাণিজ্যিক প্রিন্টিং কোটেশন হাব",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Text(
            text = "সঠিক খরচের আনুমানিক হিসেব পেতে যেকোনো সেক্টরে ট্যাপ করুন:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(sectors) { sector ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clickable { onSectorSelected(sector.id) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = sector.color.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = sector.icon,
                                    contentDescription = sector.title,
                                    tint = sector.color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = sector.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = sector.description,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.5.sp,
                                lineHeight = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            }
        }
    }
}
