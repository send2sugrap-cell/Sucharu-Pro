package com.sucharu.sucharupro.ui.customer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme

/**
 * Recoverable Error State Card component for Customer & Affiliate experiences.
 */
@Composable
fun CustomerErrorState(
    errorMessage: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    SucharuWallCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = CustomerTheme.colors.surface,
        borderColor = CustomerTheme.colors.error,
        contentPadding = CustomerTheme.spacing.xl
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Unable to Load Data",
                style = CustomerTheme.typography.title,
                color = CustomerTheme.colors.primaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(CustomerTheme.spacing.xs))

            Text(
                text = errorMessage,
                style = CustomerTheme.typography.caption,
                color = CustomerTheme.colors.secondaryText,
                textAlign = TextAlign.Center
            )
        }
    }
}
