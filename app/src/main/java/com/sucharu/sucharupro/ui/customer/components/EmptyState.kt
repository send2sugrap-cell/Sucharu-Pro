package com.sucharu.sucharupro.ui.customer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme

/**
 * Friendly Empty State Card component for Customer & Affiliate feeds.
 */
@Composable
fun CustomerEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector = Icons.Default.Inbox
) {
    SucharuWallCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = CustomerTheme.spacing.xl
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = message,
                style = CustomerTheme.typography.title,
                color = CustomerTheme.colors.primaryText,
                textAlign = TextAlign.Center
            )

            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(CustomerTheme.spacing.xs))
                Text(
                    text = subtitle,
                    style = CustomerTheme.typography.caption,
                    color = CustomerTheme.colors.secondaryText,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
