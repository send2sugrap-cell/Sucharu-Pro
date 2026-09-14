package com.sucharu.sucharupro.ui.customer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme

/**
 * Featured Offer Banner Card with gradient highlight, discount badge, and action button.
 */
@Composable
fun FeaturedOfferCard(
    title: String,
    description: String,
    discountTag: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SucharuWallCard(
        modifier = modifier,
        containerColor = CustomerTheme.colors.accentContainer,
        borderColor = CustomerTheme.colors.accentPrimary,
        cornerRadius = CustomerTheme.spacing.offerCardCornerRadius
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                StatusChip(label = discountTag, dotColor = CustomerTheme.colors.accentPrimary)
                Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))
                Text(text = title, style = CustomerTheme.typography.sectionHeader, color = CustomerTheme.colors.primaryText)
                Spacer(modifier = Modifier.height(CustomerTheme.spacing.xs))
                Text(text = description, style = CustomerTheme.typography.caption, color = CustomerTheme.colors.secondaryText)
            }
        }
    }
}
