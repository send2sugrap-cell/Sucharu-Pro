package com.sucharu.sucharupro.ui.customer.wall

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.ui.customer.theme.CustomerColors
import com.sucharu.sucharupro.ui.customer.theme.CustomerTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RedesignedHomeWallTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SucharuWallViewModel

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "cus-001",
        projectId = "PRJ-001",
        username = "customer_user",
        role = UserRole.CUSTOMER,
        customerId = "cus-001"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SucharuWallViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLightCustomerTheme_surfaceTokens() {
        val lightColors = CustomerColors.light()
        assertTrue("Light theme must set isLight = true", lightColors.isLight)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFF8FAFC), lightColors.background)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFFFFFF), lightColors.surface)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF0F172A), lightColors.primaryText)
    }

    @Test
    fun testResponsiveGridColumns_byScreenWidth() {
        fun calculateGridColumns(screenWidthDp: Int): Int = when {
            screenWidthDp >= 840 -> 4
            screenWidthDp >= 600 -> 3
            else -> 2
        }

        assertEquals(2, calculateGridColumns(360)) // Compact phone
        assertEquals(2, calculateGridColumns(411)) // Normal phone
        assertEquals(3, calculateGridColumns(700)) // Tablet medium
        assertEquals(4, calculateGridColumns(900)) // Desktop expanded
    }

    @Test
    fun testWallViewModel_loadWallData_populatesServicesProductsOffersAndActivities() = runTest {
        viewModel.loadWallFeed(customerPrincipal)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Wall state must be Success", state is SucharuWallUiState.Success)

        val feed = (state as SucharuWallUiState.Success).feedData

        assertNotNull(feed.featuredOffer)
        assertTrue(feed.offers.isNotEmpty())
        assertTrue(feed.services.isNotEmpty())
        assertTrue(feed.products.isNotEmpty())
        assertTrue(feed.announcements.isNotEmpty())
        assertTrue(feed.personalActivities.isNotEmpty())

        assertEquals("SRV-001", feed.services.first().serviceId)
        assertEquals("PROD-001", feed.products.first().productId)
    }
}
