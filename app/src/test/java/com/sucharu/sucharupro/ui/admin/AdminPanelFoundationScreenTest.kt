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

        assertEquals("Dark Navy background", 0xFF090E17, colors.background.value.toLong() shr 32 or (colors.background.value.toLong() and 0xFFFFFFFFL))
        assertEquals("Dark Slate surface", 0xFF131D2E, colors.surface.value.toLong() shr 32 or (colors.surface.value.toLong() and 0xFFFFFFFFL))
    }
}
