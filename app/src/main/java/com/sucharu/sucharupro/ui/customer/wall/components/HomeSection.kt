package com.sucharu.sucharupro.ui.customer.wall.components

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme

/**
 * Section Header Title for Redesigned Home / Sucharu Wall.
 */
@Composable
fun HomeSection(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = CustomerTheme.typography.sectionHeader.copy(fontWeight = FontWeight.Bold),
                    color = CustomerTheme.colors.primaryText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = CustomerTheme.typography.caption,
                    color = CustomerTheme.colors.secondaryText
                )
            }

            if (!actionLabel.isNullOrBlank() && onActionClick != null) {
                Text(
                    text = actionLabel,
                    style = CustomerTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = CustomerTheme.colors.accentPrimary,
                    modifier = Modifier.clickable { onActionClick() }
                )
            }
        }

        Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))

        content()
    }
}
