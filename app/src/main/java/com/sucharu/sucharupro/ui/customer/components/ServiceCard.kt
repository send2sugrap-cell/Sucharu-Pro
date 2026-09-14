package com.sucharu.sucharupro.ui.customer.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme

/**
 * Service Card for highlighting customer printing services (e.g. Offset, Digital, Packaging).
 */
@Composable
fun ServiceCard(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    SucharuWallCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CustomerTheme.colors.accentPrimary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(CustomerTheme.spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = CustomerTheme.typography.title, color = CustomerTheme.colors.primaryText)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = description, style = CustomerTheme.typography.caption, color = CustomerTheme.colors.secondaryText)
            }
        }
    }
}
