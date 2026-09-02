package com.sucharu.sucharupro.ui.features.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.auth.model.VerificationType

/**
 * Unified Sucharu Graphics Contact Verification Screen (INFRA-03 Step 04 & Critical Auth Fix).
 */
@Composable
fun VerificationScreen(
    verificationType: VerificationType,
    onConfirmToken: (String) -> Unit,
    onRequestResendToken: () -> Unit,
    onNavigateToHome: () -> Unit,
    onBackClick: () -> Unit = onNavigateToHome,
    recipient: String? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    successMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var tokenInput by remember { mutableStateOf("") }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0B132B), Color(0xFF1C2541), Color(0xFF0B132B))
    )

    val cardBorderGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF0061A4), Color(0xFF9ECAFF), Color(0xFF0061A4))
    )

    val targetRecipientText = if (!recipient.isNullOrBlank()) {
        recipient
    } else {
        "your ${verificationType.name.lowercase()}"
    }

    Box(
        modifier = modifier.fillMaxSize().background(backgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
                .border(1.dp, cardBorderGradient, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2541).copy(alpha = 0.95f)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Navigation Bar with Back Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Previous Screen",
                            tint = Color(0xFF9ECAFF)
                        )
                    }
                    TextButton(onClick = onBackClick) {
                        Text(
                            text = "Back",
                            color = Color(0xFF9ECAFF),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Text(
                    text = "ACCOUNT VERIFICATION",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9ECAFF),
                    letterSpacing = 1.5.sp
                )

                Text(
                    text = "Enter the 6-digit verification code sent to $targetRecipientText",
                    fontSize = 12.sp,
                    color = Color(0xFFB7C8D8),
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                if (errorMessage != null) {
                    Surface(
                        color = Color(0xFF93000A),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFFDAD6),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (successMessage != null) {
                    Surface(
                        color = Color(0xFF00497D),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = successMessage,
                            color = Color(0xFFD1E4FF),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { input ->
                        tokenInput = input.trim()
                    },
                    label = { Text("Verification Code", color = Color(0xFFB7C8D8)) },
                    placeholder = { Text("e.g. 123456", color = Color(0xFF6C757D)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF9ECAFF),
                        unfocusedBorderColor = Color(0xFF4F6070)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                Button(
                    onClick = { if (tokenInput.isNotBlank()) onConfirmToken(tokenInput.trim()) },
                    enabled = !isLoading && tokenInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("VERIFY & ACTIVATE", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onRequestResendToken) {
                        Text("Resend Code", color = Color(0xFF9ECAFF), fontSize = 12.sp)
                    }
                    TextButton(onClick = onNavigateToHome) {
                        Text("Back to Sign In", color = Color(0xFFB7C8D8), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
