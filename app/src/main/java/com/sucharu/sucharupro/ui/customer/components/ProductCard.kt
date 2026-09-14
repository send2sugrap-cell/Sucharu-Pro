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
 * Product Item Card displaying title, category tag, price per unit, and order trigger.
 */
@Composable
fun ProductCard(
    title: String,
    category: String,
    priceFormatted: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    SucharuWallCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Column {
            StatusChip(label = category, dotColor = CustomerTheme.colors.accentPurple)
            Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))
            Text(text = title, style = CustomerTheme.typography.title, color = CustomerTheme.colors.primaryText)
            Spacer(modifier = Modifier.height(CustomerTheme.spacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = priceFormatted, style = CustomerTheme.typography.bodyBold, color = CustomerTheme.colors.success)
                Text(text = "Order Now ->", style = CustomerTheme.typography.caption, color = CustomerTheme.colors.accentPrimary)
            }
        }
    }
}
