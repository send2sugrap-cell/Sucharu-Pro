package com.sucharu.sucharupro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.theme.ButtonShape
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Standard Primary Action Button for Sucharu Pro.
 * Provides a filled Material 3 button with loading state, icons, and 48dp minimum touch target.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    shape: Shape = ButtonShape,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    ),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(
        defaultElevation = 1.dp,
        pressedElevation = 2.dp,
        disabledElevation = 0.dp
    ),
    contentPadding: PaddingValues = PaddingValues(
        horizontal = MaterialTheme.spacing.large,
        vertical = MaterialTheme.spacing.small
    )
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(
            minWidth = 88.dp,
            minHeight = MaterialTheme.spacing.buttonHeight
        ),
        enabled = enabled && !isLoading,
        shape = shape,
        colors = colors,
        elevation = elevation,
        contentPadding = contentPadding
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = colors.contentColor,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge
                )
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                    trailingIcon()
                }
            }
        }
    }
}

/**
 * Standard Outlined Button for secondary actions in Sucharu Pro.
 */
@Composable
fun AppOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    shape: Shape = ButtonShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(
        contentColor = MaterialTheme.colorScheme.primary,
        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    ),
    contentPadding: PaddingValues = PaddingValues(
        horizontal = MaterialTheme.spacing.large,
        vertical = MaterialTheme.spacing.small
    )
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(
            minWidth = 88.dp,
            minHeight = MaterialTheme.spacing.buttonHeight
        ),
        enabled = enabled && !isLoading,
        shape = shape,
        colors = colors,
        contentPadding = contentPadding
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge
                )
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                    trailingIcon()
                }
            }
        }
    }
}
