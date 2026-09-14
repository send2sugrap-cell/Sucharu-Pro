package com.sucharu.sucharupro.ui.customer.wall.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.customer.components.FeaturedOfferCard
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme
import com.sucharu.sucharupro.ui.customer.wall.WallOfferItem

/**
 * Promotional Banner Carousel using HorizontalPager.
 */
@Composable
fun HomeBannerPager(
    offers: List<WallOfferItem>,
    modifier: Modifier = Modifier,
    onOfferClick: (offerId: String) -> Unit = {}
) {
    if (offers.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { offers.size })

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val offer = offers[page]
            FeaturedOfferCard(
                title = offer.title,
                description = offer.description,
                discountTag = offer.discountTag,
                actionLabel = "Claim Offer",
                onActionClick = { onOfferClick(offer.offerId) }
            )
        }

        if (offers.size > 1) {
            Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(offers.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(if (isSelected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) CustomerTheme.colors.accentPrimary
                                else CustomerTheme.colors.mutedText.copy(alpha = 0.4f)
                            )
                    )
                }
            }
        }
    }
}
