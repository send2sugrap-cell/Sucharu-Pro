package com.sucharu.sucharupro.ui.admin

import com.sucharu.sucharupro.ui.admin.theme.AdminColors
import com.sucharu.sucharupro.ui.admin.theme.AdminSpacing
import com.sucharu.sucharupro.ui.admin.theme.AdminTypography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AdminPanelFoundationScreenTest {

    @Test
    fun testAdminFoundationScreen_themeAndComponentIntegration() {
        val colors = AdminColors()
        val typography = AdminTypography()
        val spacing = AdminSpacing()

        assertNotNull("AdminColors must instantiate correctly", colors)
        assertNotNull("AdminTypography must instantiate correctly", typography)
        assertNotNull("AdminSpacing must instantiate correctly", spacing)

        assertEquals(androidx.compose.ui.graphics.Color(0xFF090E17), colors.background)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF131D2E), colors.surface)
    }
}
