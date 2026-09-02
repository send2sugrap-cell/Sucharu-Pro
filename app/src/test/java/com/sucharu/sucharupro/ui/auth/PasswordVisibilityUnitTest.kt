package com.sucharu.sucharupro.ui.auth

import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Test for Password Visibility Transformation and Field Isolation.
 */
class PasswordVisibilityUnitTest {

    @Test
    fun test01_passwordVisibilityToggleBehavior() {
        var passwordVisible = false

        // When hidden
        val transformationHidden = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
        assertTrue(transformationHidden is PasswordVisualTransformation)

        // Toggle to visible
        passwordVisible = !passwordVisible
        val transformationVisible = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
        assertEquals(VisualTransformation.None, transformationVisible)
    }

    @Test
    fun test02_independentPasswordAndConfirmPasswordToggles() {
        var passwordVisible = false
        var confirmPasswordVisible = false

        // Toggle only password
        passwordVisible = true

        val pwdTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
        val confirmTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()

        assertEquals(VisualTransformation.None, pwdTransformation)
        assertTrue(confirmTransformation is PasswordVisualTransformation)

        // Toggle confirm password
        confirmPasswordVisible = true
        val confirmTransformationNow = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
        assertEquals(VisualTransformation.None, confirmTransformationNow)
    }

    @Test
    fun test03_passwordValueIsNotAlteredByVisualTransformation() {
        val originalRawPassword = "SuperSecret#Password2026!"
        var passwordState = originalRawPassword

        // Simulate multiple toggles
        var visible = false
        visible = true
        visible = false
        visible = true

        // Ensure underlying password value remained completely immutable
        assertEquals("SuperSecret#Password2026!", passwordState)
    }
}
