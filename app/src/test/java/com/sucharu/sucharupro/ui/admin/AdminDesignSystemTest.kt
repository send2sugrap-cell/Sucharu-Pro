package com.sucharu.sucharupro.ui.admin

import com.sucharu.sucharupro.ui.admin.theme.AdminColors
import com.sucharu.sucharupro.ui.admin.theme.AdminSpacing
import com.sucharu.sucharupro.ui.admin.theme.AdminTypography
import com.sucharu.sucharupro.ui.admin.theme.AdminWindowSizeClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminDesignSystemTest {

    @Test
    fun testAdminColors_tokenDefaults() {
        val colors = AdminColors()
        assertNotNull(colors.background)
        assertNotNull(colors.surface)
        assertNotNull(colors.elevatedSurface)
        assertNotNull(colors.border)
        assertNotNull(colors.primaryText)
        assertNotNull(colors.secondaryText)
        assertNotNull(colors.accentPrimary)
        assertNotNull(colors.accentPurple)
        assertNotNull(colors.success)
        assertNotNull(colors.warning)
        assertNotNull(colors.error)
    }

    @Test
    fun testAdminTypography_textStylesConfigured() {
        val typography = AdminTypography()
        assertEquals(24, typography.pageTitle.fontSize.value.toInt())
        assertEquals(18, typography.sectionTitle.fontSize.value.toInt())
        assertEquals(16, typography.cardTitle.fontSize.value.toInt())
        assertEquals(14, typography.body.fontSize.value.toInt())
        assertEquals(12, typography.caption.fontSize.value.toInt())
        assertEquals(26, typography.kpiNumber.fontSize.value.toInt())
        assertEquals(14, typography.buttonText.fontSize.value.toInt())
    }

    @Test
    fun testAdminSpacing_accessibilityMetrics() {
        val spacing = AdminSpacing()
        assertEquals(16, spacing.cardCornerRadius.value.toInt())
        assertEquals(20, spacing.dialogCornerRadius.value.toInt())
        assertTrue("Button min height must be at least 48dp for touch target accessibility", spacing.buttonMinHeight.value >= 48f)
        assertTrue("Touch target min must be at least 48dp for accessibility", spacing.touchTargetMin.value >= 48f)
    }

    @Test
    fun testAdminWindowSizeClass_enumCoverage() {
        val classes = AdminWindowSizeClass.entries
        assertEquals(3, classes.size)
        assertTrue(classes.contains(AdminWindowSizeClass.COMPACT))
        assertTrue(classes.contains(AdminWindowSizeClass.MEDIUM))
        assertTrue(classes.contains(AdminWindowSizeClass.EXPANDED))
    }
}
