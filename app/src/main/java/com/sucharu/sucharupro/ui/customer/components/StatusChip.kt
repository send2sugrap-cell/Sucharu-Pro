package com.sucharu.sucharupro.ui.customer.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme

/**
 * Mobile-First Status Chip with indicator dot and pill shape.
 */
@Composable
fun StatusChip(
    label: String,
    modifier: Modifier = Modifier,
    dotColor: Color = CustomerTheme.colors.success,
    containerColor: Color = dotColor.copy(alpha = 0.15f),
    contentColor: Color = CustomerTheme.colors.primaryText,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(CustomerTheme.spacing.chipCornerRadius),
        color = containerColor,
        border = BorderStroke(1.dp, dotColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = CustomerTheme.spacing.md, vertical = CustomerTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(CustomerTheme.spacing.sm))
            Text(text = label, style = CustomerTheme.typography.caption, color = contentColor)
        }
    }
}
