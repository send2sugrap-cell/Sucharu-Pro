package com.sucharu.sucharupro.ui.features.auth

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sucharu.sucharupro.core.validation.CustomerValidation
import com.sucharu.sucharupro.data.auth.model.RegisterRequestDto
import com.sucharu.sucharupro.ui.features.auth.components.ContactPhoneOption
import com.sucharu.sucharupro.ui.features.auth.components.ContactPickerHelper
import com.sucharu.sucharupro.ui.features.auth.components.SelectContactPhoneDialog

/**
 * Unified Sucharu Graphics Public User Registration Screen (INFRA-03 Step 04 & Authentication Gap-Fix).
 *
 * Features:
 * - End-to-end customer registration flow
 * - Android contact/phonebook picker with multi-number support
 * - Independent password and confirm-password visibility toggles
 * - Client-side validation with real-time feedback
 * - Protection against duplicate submissions
 */
@Composable
fun RegisterScreen(
    onRegisterSubmit: (RegisterRequestDto) -> Unit,
    onNavigateToLogin: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var displayName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var referralCode by rememberSaveable { mutableStateOf("") }
    var termsAccepted by rememberSaveable { mutableStateOf(true) }

    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    var hasAttemptedSubmit by rememberSaveable { mutableStateOf(false) }
    var phonePickNotice by rememberSaveable { mutableStateOf<String?>(null) }

    // Multi-number contact selection state
    var contactPhoneOptions by remember { mutableStateOf<List<ContactPhoneOption>?>(null) }

    // Contact Picker Launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri ->
        if (contactUri != null) {
            val options = ContactPickerHelper.extractPhoneNumbers(context, contactUri)
            when {
                options.isEmpty() -> {
                    phonePickNotice = "No phone number found in the selected contact. Please enter manually."
                }
                options.size == 1 -> {
                    phone = options.first().normalizedNumber
                    phonePickNotice = null
                }
                else -> {
                    contactPhoneOptions = options
                    phonePickNotice = null
                }
            }
        }
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            phonePickNotice = null
            contactPickerLauncher.launch(null)
        } else {
            phonePickNotice = "Contacts permission denied. You can enter your mobile number manually."
        }
    }

    fun handlePickContact() {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            phonePickNotice = null
            contactPickerLauncher.launch(null)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    // Validations
    val nameError = if (hasAttemptedSubmit && displayName.trim().length < 2) {
        "Full name is required (minimum 2 characters)."
    } else null

    val phoneError = if (hasAttemptedSubmit && phone.isBlank() && email.isBlank()) {
        "Mobile number or email address is required."
    } else if (phone.isNotBlank() && CustomerValidation.validatePrimaryPhone(phone) != null) {
        CustomerValidation.validatePrimaryPhone(phone)
    } else null

    val emailError = if (email.isNotBlank() && CustomerValidation.validateEmail(email) != null) {
        CustomerValidation.validateEmail(email)
    } else null

    val passwordError = if (hasAttemptedSubmit && password.length < 8) {
        "Password must be at least 8 characters long."
    } else null

    val confirmPasswordError = if (hasAttemptedSubmit && confirmPassword != password) {
        "Passwords do not match."
    } else null

    val termsError = if (hasAttemptedSubmit && !termsAccepted) {
        "You must accept the Terms of Service to register."
    } else null

    val isFormValid = displayName.trim().length >= 2 &&
            (phone.isNotBlank() || email.isNotBlank()) &&
            phoneError == null &&
            emailError == null &&
            password.length >= 8 &&
            confirmPassword == password &&
            termsAccepted

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

    // Multi-number selection dialog
    contactPhoneOptions?.let { options ->
        SelectContactPhoneDialog(
            options = options,
            onSelectOption = { selected ->
                phone = selected.normalizedNumber
                contactPhoneOptions = null
            },
            onDismiss = {
                contactPhoneOptions = null
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient),
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
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CREATE ACCOUNT",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9ECAFF),
                    letterSpacing = 1.5.sp
                )

                Text(
                    text = "Sucharu Graphics Commercial Printing Ecosystem",
                    fontSize = 11.sp,
                    color = Color(0xFFB7C8D8),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (errorMessage != null) {
                    Surface(
                        color = Color(0xFF93000A),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFFDAD6),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (phonePickNotice != null) {
                    Surface(
                        color = Color(0xFF003258),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = phonePickNotice!!,
                            color = Color(0xFFD1E4FF),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Full Name
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Full Name *", color = Color(0xFFB7C8D8)) },
                    placeholder = { Text("e.g. Rahim Ahmed", color = Color(0xFF4F6070)) },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it, color = Color(0xFFFFB4AB), fontSize = 11.sp) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF9ECAFF),
                        unfocusedBorderColor = Color(0xFF4F6070),
                        errorBorderColor = Color(0xFFFFB4AB)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                // Mobile Number with Contacts Picker Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Mobile Number", color = Color(0xFFB7C8D8)) },
                        placeholder = { Text("01XXXXXXXXX", color = Color(0xFF4F6070)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = phoneError != null,
                        supportingText = phoneError?.let { { Text(it, color = Color(0xFFFFB4AB), fontSize = 11.sp) } },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF9ECAFF),
                            unfocusedBorderColor = Color(0xFF4F6070),
                            errorBorderColor = Color(0xFFFFB4AB)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = { handlePickContact() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF9ECAFF)),
                        border = BorderStroke(1.dp, Color(0xFF0061A4)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 14.dp),
                        modifier = Modifier.padding(top = if (phoneError != null) 0.dp else 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Contacts,
                            contentDescription = "Pick phone number from contacts",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Contacts", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Email Address
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address (Optional)", color = Color(0xFFB7C8D8)) },
                    placeholder = { Text("name@example.com", color = Color(0xFF4F6070)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it, color = Color(0xFFFFB4AB), fontSize = 11.sp) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF9ECAFF),
                        unfocusedBorderColor = Color(0xFF4F6070),
                        errorBorderColor = Color(0xFFFFB4AB)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                // Password with Visibility Toggle
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (Min 8 characters) *", color = Color(0xFFB7C8D8)) },
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
                    isError = passwordError != null,
                    supportingText = passwordError?.let { { Text(it, color = Color(0xFFFFB4AB), fontSize = 11.sp) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF9ECAFF),
                        unfocusedBorderColor = Color(0xFF4F6070),
                        errorBorderColor = Color(0xFFFFB4AB)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                // Confirm Password with Visibility Toggle
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password *", color = Color(0xFFB7C8D8)) },
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
                    isError = confirmPasswordError != null,
                    supportingText = confirmPasswordError?.let { { Text(it, color = Color(0xFFFFB4AB), fontSize = 11.sp) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF9ECAFF),
                        unfocusedBorderColor = Color(0xFF4F6070),
                        errorBorderColor = Color(0xFFFFB4AB)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                // Referral Code (Optional)
                OutlinedTextField(
                    value = referralCode,
                    onValueChange = { referralCode = it },
                    label = { Text("Affiliate Referral Code (Optional)", color = Color(0xFFB7C8D8)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF9ECAFF),
                        unfocusedBorderColor = Color(0xFF4F6070)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // Terms & Conditions Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                ) {
                    Checkbox(
                        checked = termsAccepted,
                        onCheckedChange = { termsAccepted = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF0061A4),
                            uncheckedColor = if (termsError != null) Color(0xFFFFB4AB) else Color(0xFF4F6070)
                        )
                    )
                    Text("I accept Terms of Service & Privacy Policy", color = Color(0xFFB7C8D8), fontSize = 11.sp)
                }

                if (termsError != null) {
                    Text(
                        text = termsError,
                        color = Color(0xFFFFB4AB),
                        fontSize = 11.sp,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(start = 12.dp, bottom = 12.dp)
                    )
                }

                // Register Account Button
                Button(
                    onClick = {
                        hasAttemptedSubmit = true
                        if (isFormValid && !isLoading) {
                            val normalizedPhone = phone.trim().takeIf { it.isNotBlank() }?.let {
                                CustomerValidation.normalizePhoneNumber(it)
                            }
                            onRegisterSubmit(
                                RegisterRequestDto(
                                    displayName = displayName.trim(),
                                    email = email.trim().ifBlank { null },
                                    phone = normalizedPhone ?: phone.trim().ifBlank { null },
                                    password = password,
                                    affiliateReferralCode = referralCode.trim().ifBlank { null }
                                )
                            )
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0061A4),
                        disabledContainerColor = Color(0xFF0061A4).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("CREATE ACCOUNT", fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Already have an account?", color = Color(0xFFB7C8D8), fontSize = 12.sp)
                    TextButton(onClick = onNavigateToLogin) {
                        Text("Sign In", color = Color(0xFF9ECAFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
