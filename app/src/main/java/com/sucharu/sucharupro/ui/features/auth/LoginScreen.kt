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
import com.sucharu.sucharupro.data.auth.model.LoginRequestDto

/**
 * Unified Sucharu Graphics Login Screen (INFRA-03 Step 04 & Authentication Gap-Fix).
 * Enforces server-authoritative authentication flow with password visibility control and success feedback.
 */
@Composable
fun LoginScreen(
    onLoginSubmit: (LoginRequestDto) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    successMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var identifier by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0B132B),
            Color(0xFF1C2541),
            Color(0xFF0B132B)
        )
    )

    val cardBorderGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF0061A4),
            Color(0xFF9ECAFF),
            Color(0xFF0061A4)
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
                .fillMaxWidth(0.9f)
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "SUCHARU GRAPHICS",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9ECAFF),
                    letterSpacing = 2.sp
                )

                Text(
                    text = "Commercial Printing ERP",
                    fontSize = 12.sp,
                    color = Color(0xFFB7C8D8),
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                if (successMessage != null) {
                    Surface(
                        color = Color(0xFF004F2E),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = successMessage,
                            color = Color(0xFF6CFF95),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (errorMessage != null) {
                    Surface(
                        color = Color(0xFF93000A),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFFDAD6),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text("Email or Mobile Number", color = Color(0xFFB7C8D8)) },
                    placeholder = { Text("name@example.com or 01XXXXXXXXX", color = Color(0xFF4F6070)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF9ECAFF),
                        unfocusedBorderColor = Color(0xFF4F6070)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = Color(0xFFB7C8D8)) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val desc = if (passwordVisible) "Hide password" else "Show password"
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = icon, contentDescription = desc, tint = Color(0xFF9ECAFF))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF9ECAFF),
                        unfocusedBorderColor = Color(0xFF4F6070)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onNavigateToForgotPassword) {
                        Text("Forgot Password?", color = Color(0xFF9ECAFF), fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = {
                        if (identifier.isNotBlank() && password.isNotBlank()) {
                            onLoginSubmit(LoginRequestDto(identifier = identifier.trim(), password = password))
                        }
                    },
                    enabled = !isLoading && identifier.isNotBlank() && password.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(top = 8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("SIGN IN", fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Don't have an account?", color = Color(0xFFB7C8D8), fontSize = 12.sp)
                    TextButton(onClick = onNavigateToRegister) {
                        Text("Register", color = Color(0xFF9ECAFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
