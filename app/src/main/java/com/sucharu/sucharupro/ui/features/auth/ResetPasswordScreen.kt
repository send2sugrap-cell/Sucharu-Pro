package com.sucharu.sucharupro.ui.features.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.auth.model.PasswordRecoveryConfirmDto

/**
 * Unified Sucharu Graphics Reset Password Screen (INFRA-03 Step 04 & Authentication Gap-Fix).
 * Features independent password visibility controls for new and confirmation password fields.
 */
@Composable
fun ResetPasswordScreen(
    onResetSubmit: (PasswordRecoveryConfirmDto) -> Unit,
    onNavigateToLogin: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var tokenInput by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var revokeSessions by rememberSaveable { mutableStateOf(true) }

    var newPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0B132B), Color(0xFF1C2541), Color(0xFF0B132B))
    )

    val cardBorderGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF0061A4), Color(0xFF9ECAFF), Color(0xFF0061A4))
    )

    val passwordMatch = newPassword == confirmPassword
    val canSubmit = tokenInput.isNotBlank() && newPassword.length >= 8 && passwordMatch && !isLoading

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
                    text = "RESET PASSWORD",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9ECAFF),
                    letterSpacing = 1.5.sp
                )

                Text(
                    text = "Provide your reset token and new password.",
                    fontSize = 12.sp,
                    color = Color(0xFFB7C8D8),
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                if (errorMessage != null) {
                    Surface(
                        color = Color(0xFF93000A), shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Text(text = errorMessage, color = Color(0xFFFFDAD6), modifier = Modifier.padding(12.dp))
                    }
                }

                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("Reset Token", color = Color(0xFFB7C8D8)) },
                    placeholder = { Text("Enter security token", color = Color(0xFF4F6070)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF9ECAFF), unfocusedBorderColor = Color(0xFF4F6070)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password (Min 8 characters)", color = Color(0xFFB7C8D8)) },
                    singleLine = true,
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val icon = if (newPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val desc = if (newPasswordVisible) "Hide password" else "Show password"
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(imageVector = icon, contentDescription = desc, tint = Color(0xFF9ECAFF))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF9ECAFF), unfocusedBorderColor = Color(0xFF4F6070)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password", color = Color(0xFFB7C8D8)) },
                    singleLine = true,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val icon = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val desc = if (confirmPasswordVisible) "Hide password" else "Show password"
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(imageVector = icon, contentDescription = desc, tint = Color(0xFF9ECAFF))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = if (passwordMatch) Color(0xFF9ECAFF) else Color(0xFFBA1A1A),
                        unfocusedBorderColor = Color(0xFF4F6070)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Checkbox(
                        checked = revokeSessions,
                        onCheckedChange = { revokeSessions = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0061A4))
                    )
                    Text("Revoke active sessions on other devices", color = Color(0xFFB7C8D8), fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        if (canSubmit) {
                            onResetSubmit(
                                PasswordRecoveryConfirmDto(
                                    token = tokenInput.trim(),
                                    newPassword = newPassword,
                                    revokeSessions = revokeSessions
                                )
                            )
                        }
                    },
                    enabled = canSubmit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("UPDATE PASSWORD", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onNavigateToLogin) {
                    Text("Back to Sign In", color = Color(0xFF9ECAFF), fontSize = 12.sp)
                }
            }
        }
    }
}
