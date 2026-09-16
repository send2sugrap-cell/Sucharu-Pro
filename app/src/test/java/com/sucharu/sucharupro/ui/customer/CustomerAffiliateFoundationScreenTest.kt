package com.sucharu.sucharupro.ui.customer

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.ui.customer.components.CustomerBottomTab
import com.sucharu.sucharupro.ui.customer.theme.CustomerColors
import com.sucharu.sucharupro.ui.customer.theme.CustomerSpacing
import com.sucharu.sucharupro.ui.customer.theme.CustomerTypography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerAffiliateFoundationScreenTest {

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "cus-001",
        projectId = "PRJ-001",
        username = "customer_user",
        role = UserRole.CUSTOMER,
        customerId = "cus-001"
    )

    private val affiliatePrincipal = AuthenticatedPrincipal(
        userId = "aff-001",
        projectId = "PRJ-001",
        username = "affiliate_user",
        role = UserRole.AFFILIATE,
        affiliateId = "aff-001"
    )

    @Test
    fun testCustomerThemeAndTokens_instantiateCleanly() {
        val colors = CustomerColors()
        val typography = CustomerTypography()
        val spacing = CustomerSpacing()

        assertNotNull(colors)
        assertNotNull(typography)
        assertNotNull(spacing)

        assertEquals(androidx.compose.ui.graphics.Color(0xFF090E17), colors.background)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF131D2E), colors.surface)
    }

    @Test
    fun testBottomNavigationTabs_coverage() {
        val tabs = CustomerBottomTab.entries
        assertEquals(1, tabs.size)
        assertTrue(tabs.contains(CustomerBottomTab.HOME))
    }

    @Test
    fun testPrincipals_roleProperties() {
        assertEquals(UserRole.CUSTOMER, customerPrincipal.role)
        assertEquals("cus-001", customerPrincipal.effectiveCustomerId)

        assertEquals(UserRole.AFFILIATE, affiliatePrincipal.role)
        assertEquals("aff-001", affiliatePrincipal.effectiveAffiliateId)
    }
}
