package com.sucharu.sucharupro.ui.features.auth

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
import com.sucharu.sucharupro.data.auth.model.PasswordRecoveryRequestDto

/**
 * Unified Sucharu Graphics Account Recovery Request Screen (INFRA-03 Step 04).
 * Enforces account enumeration defense with generic UI confirmation messaging.
 */
@Composable
fun ForgotPasswordScreen(
    onRequestRecovery: (PasswordRecoveryRequestDto) -> Unit,
    onNavigateToResetConfirm: () -> Unit,
    onNavigateToLogin: () -> Unit,
    isLoading: Boolean = false,
    confirmationMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var identifier by remember { mutableStateOf("") }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0B132B), Color(0xFF1C2541), Color(0xFF0B132B))
    )

    val cardBorderGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF0061A4), Color(0xFF9ECAFF), Color(0xFF0061A4))
    )

    Box(
        modifier = modifier.fillMaxSize().background(backgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f).padding(16.dp).border(1.dp, cardBorderGradient, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2541).copy(alpha = 0.95f)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "RECOVER ACCOUNT",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9ECAFF),
                    letterSpacing = 1.5.sp
                )

                Text(
                    text = "Enter your registered email or phone to receive password reset instructions.",
                    fontSize = 12.sp,
                    color = Color(0xFFB7C8D8),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                if (confirmationMessage != null) {
                    Surface(
                        color = Color(0xFF00497D), shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Text(text = confirmationMessage, color = Color(0xFFD1E4FF), modifier = Modifier.padding(12.dp))
                    }
                }

                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text("Email or Phone Number", color = Color(0xFFB7C8D8)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF9ECAFF), unfocusedBorderColor = Color(0xFF4F6070)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                Button(
                    onClick = {
                        if (identifier.isNotBlank()) {
                            onRequestRecovery(PasswordRecoveryRequestDto(identifier = identifier.trim()))
                        }
                    },
                    enabled = !isLoading && identifier.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("SEND RECOVERY INSTRUCTIONS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onNavigateToResetConfirm) {
                        Text("I Have a Token", color = Color(0xFF9ECAFF), fontSize = 12.sp)
                    }
                    TextButton(onClick = onNavigateToLogin) {
                        Text("Back to Sign In", color = Color(0xFFB7C8D8), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
