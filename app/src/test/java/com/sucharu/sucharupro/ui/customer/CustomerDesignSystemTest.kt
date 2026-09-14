package com.sucharu.sucharupro.ui.customer

import com.sucharu.sucharupro.ui.customer.theme.CustomerColors
import com.sucharu.sucharupro.ui.customer.theme.CustomerSpacing
import com.sucharu.sucharupro.ui.customer.theme.CustomerTypography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerDesignSystemTest {

    @Test
    fun testCustomerColors_mobileFirstDefaults() {
        val colors = CustomerColors()
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
    fun testCustomerTypography_mobileFontHierarchy() {
        val typography = CustomerTypography()
        assertEquals(22, typography.header.fontSize.value.toInt())
        assertEquals(17, typography.sectionHeader.fontSize.value.toInt())
        assertEquals(15, typography.title.fontSize.value.toInt())
        assertEquals(14, typography.body.fontSize.value.toInt())
        assertEquals(12, typography.caption.fontSize.value.toInt())
        assertEquals(24, typography.number.fontSize.value.toInt())
        assertEquals(14, typography.button.fontSize.value.toInt())
    }

    @Test
    fun testCustomerSpacing_touchTargetAccessibilityMetrics() {
        val spacing = CustomerSpacing()
        assertEquals(16, spacing.cardCornerRadius.value.toInt())
        assertEquals(20, spacing.offerCardCornerRadius.value.toInt())
        assertTrue("Touch target min must be at least 48dp for accessibility", spacing.touchTargetMin.value >= 48f)
        assertTrue("Button min height must be at least 48dp for accessibility", spacing.buttonMinHeight.value >= 48f)
    }
}
