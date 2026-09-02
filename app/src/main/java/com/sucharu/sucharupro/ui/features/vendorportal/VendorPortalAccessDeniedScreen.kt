package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Vendor Portal Access Denied / Suspended Screen (Module 13 Step 01).
 *
 * Rendered when a vendor account or user membership is suspended, revoked, or blocked by policy.
 */
@Composable
fun VendorPortalAccessDeniedScreen(
    reason: String = "Your vendor portal account or membership is not active.",
    onRetryOrLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF070D1E),
            Color(0xFF1A0D18),
            Color(0xFF070D1E)
        )
    )

    val cardBorderGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFFF5252),
            Color(0xFFFFB4AB),
            Color(0xFFFF5252)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 480.dp)
                .padding(16.dp)
                .border(1.dp, cardBorderGradient, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1019).copy(alpha = 0.96f)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SECURITY NOTICE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = Color(0xFFFF5252)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Access Restricted",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1522)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = reason,
                        color = Color(0xFFFFB4AB),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "If you believe this is an error, please contact the Sucharu Pro system administrator or procurement operations.",
                    fontSize = 12.sp,
                    color = Color(0xFF90A4AE),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRetryOrLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A1E2D)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Return to Login", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                }
            }
        }
    }
}
