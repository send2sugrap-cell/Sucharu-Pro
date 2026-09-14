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
 * Customer / Affiliate Notice and Announcement Card.
 */
@Composable
fun AnnouncementCard(
    title: String,
    message: String,
    dateFormatted: String,
    modifier: Modifier = Modifier,
    tag: String = "ANNOUNCEMENT"
) {
    SucharuWallCard(
        modifier = modifier
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(label = tag, dotColor = CustomerTheme.colors.info)
                Text(text = dateFormatted, style = CustomerTheme.typography.caption, color = CustomerTheme.colors.mutedText)
            }
            Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))
            Text(text = title, style = CustomerTheme.typography.title, color = CustomerTheme.colors.primaryText)
            Spacer(modifier = Modifier.height(CustomerTheme.spacing.xs))
            Text(text = message, style = CustomerTheme.typography.body, color = CustomerTheme.colors.secondaryText)
        }
    }
}
