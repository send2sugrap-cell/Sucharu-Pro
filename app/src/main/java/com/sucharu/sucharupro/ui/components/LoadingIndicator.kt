package com.sucharu.sucharupro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Centered Loading Indicator with optional message for Sucharu Pro screens.
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    message: String? = null,
    size: Dp = 44.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = modifier
            .padding(MaterialTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size),
            color = color,
            strokeWidth = 3.5.dp
        )
        if (!message.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Full-screen or container blocking Loading Overlay for asynchronous tasks (Saving job, generating invoice PDF).
 */
@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    message: String? = null,
    overlayColor: Color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Intercept click events
                    ),
                contentAlignment = Alignment.Center
            ) {
                AppCard(
                    modifier = Modifier.padding(MaterialTheme.spacing.large),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(MaterialTheme.spacing.large)
                ) {
                    LoadingIndicator(
                        message = message ?: "Processing...",
                        size = 36.dp
                    )
                }
            }
        }
    }
}
