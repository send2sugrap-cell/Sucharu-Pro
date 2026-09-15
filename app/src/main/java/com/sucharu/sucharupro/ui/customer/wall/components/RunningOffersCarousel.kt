package com.sucharu.sucharupro.ui.customer.wall.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme

/**
 * Running Offer Item Model.
 */
data class RunningOfferItem(
    val offerId: String,
    val title: String,
    val description: String,
    val couponCode: String,
    val badgeTag: String,
    val containerBgColor: Color,
    val accentColor: Color
)

/**
 * Running Offers Carousel (চলমান অফার সমূহ) displaying active discount packages in a horizontal LazyRow.
 */
@Composable
fun RunningOffersCarousel(
    modifier: Modifier = Modifier,
    onOfferClick: (offerId: String) -> Unit = {}
) {
    val offers = listOf(
        RunningOfferItem(
            offerId = "OFFER-20",
            title = "প্রথম অর্ডারে ২০% ছাড়",
            description = "সকল কমার্শিয়াল ও কাস্টম অফসেট প্রিন্টিং অর্ডারে বিশেষ ডিসকাউন্ট।",
            couponCode = "SUCHARU20",
            badgeTag = "২০% ছাড়",
            containerBgColor = Color(0xFFFFF3E0),
            accentColor = Color(0xFFE65100)
        ),
        RunningOfferItem(
            offerId = "OFFER-CARD",
            title = "১০০০ পিস ভিজিটিং কার্ড মাত্র ৳৭৫০",
            description = "৩০০ জিএসএম আর্ট কার্ড + প্রিমিয়াম ম্যাট ল্যামিনেশন ফিনিশ।",
            couponCode = "CARD750",
            badgeTag = "বিশেষ ডিল",
            containerBgColor = Color(0xFFE8F5E9),
            accentColor = Color(0xFF2E7D32)
        ),
        RunningOfferItem(
            offerId = "OFFER-COMBO",
            title = "ক্যাশ মেমো কম্বো প্যাক - ফ্রি ডেলিভারি",
            description = "৫ টি ক্যাশ মেমো বই অর্ডারে ঢাকা সিটিতে ফ্রি হোম ডেলিভারি।",
            couponCode = "FREEDEL",
            badgeTag = "ফ্রি ডেলিভারি",
            containerBgColor = Color(0xFFE3F2FD),
            accentColor = Color(0xFF1565C0)
        )
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocalOffer,
                contentDescription = null,
                tint = CustomerTheme.colors.accentPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "চলমান অফার সমূহ",
                style = CustomerTheme.typography.sectionHeader.copy(fontWeight = FontWeight.Bold),
                color = CustomerTheme.colors.primaryText
            )
        }

        Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(offers) { offer ->
                Card(
                    modifier = Modifier
                        .width(260.dp)
                        .clickable { onOfferClick(offer.offerId) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = offer.containerBgColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, offer.accentColor.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = offer.accentColor,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = offer.badgeTag,
                                    color = Color.White,
                                    style = CustomerTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }

                            Text(
                                text = "কুপন: ${offer.couponCode}",
                                style = CustomerTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                                color = offer.accentColor
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = offer.title,
                            style = CustomerTheme.typography.title.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1E293B)
                        )

                        Text(
                            text = offer.description,
                            style = CustomerTheme.typography.caption,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }
        }
    }
}
