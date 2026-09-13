package com.sucharu.sucharupro.ui.admin.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.admin.theme.AdminTheme

/**
 * Reusable Admin Card component adhering to the Sucharu Pro dark-mode-first design direction.
 *
 * @param modifier Optional modifier for sizing or positioning.
 * @param onClick Optional click listener.
 * @param containerColor Card background color.
 * @param borderColor Border stroke color.
 * @param accentBarColor Optional top or left accent color stripe.
 * @param contentPadding Internal padding for card content.
 * @param content Slot content rendered inside the card column.
 */
@Composable
fun AdminCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = AdminTheme.colors.surface,
    borderColor: Color = AdminTheme.colors.border,
    accentBarColor: Color? = null,
    contentPadding: Dp = AdminTheme.spacing.cardPadding,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(AdminTheme.spacing.cardCornerRadius)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column {
            if (accentBarColor != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 0.dp)
                        .background(accentBarColor)
                        .padding(vertical = 2.dp)
                )
            }
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    }
}
