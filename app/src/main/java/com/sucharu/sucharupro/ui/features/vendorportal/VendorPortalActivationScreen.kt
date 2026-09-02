package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Vendor Portal Membership Activation Screen (Module 13 Step 01).
 *
 * Allows invited vendor users to activate their membership using their secure token.
 */
@Composable
fun VendorPortalActivationScreen(
    onActivateSubmit: (invitationToken: String) -> Unit,
    onNavigateBackToLogin: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    successMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var token by remember { mutableStateOf("") }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF070D1E),
            Color(0xFF0F1B38),
            Color(0xFF070D1E)
        )
    )

    val cardBorderGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF0066CC),
            Color(0xFF00E5FF),
            Color(0xFF0066CC)
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
            colors = CardDefaults.cardColors(containerColor = Color(0xFF132042).copy(alpha = 0.96f)),
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
                    text = "MEMBERSHIP ACTIVATION",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = Color(0xFF00E5FF)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Activate Portal Access",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Enter the 16-character token provided in your invitation",
                    fontSize = 12.sp,
                    color = Color(0xFF90A4AE),
                    modifier = Modifier.padding(top = 2.dp, bottom = 20.dp)
                )

                if (errorMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF93000A).copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFFB4AB),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (successMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20).copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = successMessage,
                            color = Color(0xFF81C784),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it.uppercase() },
                    label = { Text("Invitation Token") },
                    placeholder = { Text("e.g. VPT-A1B2C3D4E5F6G7H8") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xFFE2E8F0),
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = Color(0xFF00E5FF),
                        unfocusedLabelColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onActivateSubmit(token) },
                    enabled = !isLoading && token.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Activate Membership", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onNavigateBackToLogin,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "← Back to Portal Login",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
