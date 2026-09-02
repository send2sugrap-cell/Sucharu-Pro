package com.sucharu.sucharupro.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Sanitized, Security-Compliant Exception Views (INFRA-03 Step 06).
 *
 * Ensures technical stack traces and internal business rules are never leaked to client UI.
 */
@Composable
fun SecurityStateView(
    destination: AppDestination.Security,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B132B))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF1C2541),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val (headerTitle, messageText, buttonText) = when (destination) {
                    is AppDestination.Security.VerificationRequired -> Triple(
                        "VERIFICATION REQUIRED",
                        "Please verify your email address or mobile phone number to activate your account features.",
                        "Complete Verification"
                    )
                    is AppDestination.Security.AccountUnavailable -> Triple(
                        "ACCOUNT UNAVAILABLE",
                        "Your account is currently unavailable. Please contact Sucharu Graphics support for assistance.",
                        "Back to Home"
                    )
                    is AppDestination.Security.SecurityReview -> Triple(
                        "SECURITY REVIEW IN PROGRESS",
                        "Your account is undergoing a routine security audit. Operations will resume shortly.",
                        "Back to Home"
                    )
                    is AppDestination.Security.SessionExpired -> Triple(
                        "SESSION EXPIRED",
                        "Your authentication session has expired or was revoked. Please sign in again.",
                        "Sign In"
                    )
                    is AppDestination.Security.Forbidden -> Triple(
                        "ACCESS RESTRICTED",
                        "You do not have authorization to view this resource or perform this action.",
                        "Return to Workspace"
                    )
                    is AppDestination.Security.NotFound -> Triple(
                        "PAGE NOT FOUND",
                        "The requested route does not exist or has been relocated.",
                        "Back to Home"
                    )
                }

                Text(
                    text = headerTitle,
                    fontWeight = FontWeight.Bold,
                    color = if (destination is AppDestination.Security.Forbidden || destination is AppDestination.Security.AccountUnavailable) Color(0xFFFFB4AB) else Color(0xFF9ECAFF),
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = messageText,
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNavigateHome,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00497D))
                ) {
                    Text(buttonText, color = Color.White)
                }
            }
        }
    }
}
