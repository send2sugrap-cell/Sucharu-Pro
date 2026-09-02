package com.sucharu.sucharupro.ui.navigation

import org.junit.Assert.*
import org.junit.Test

class PublicExperienceNavigationTest {

    @Test
    fun testAllPublicDestinationsAreMarkedPublic() {
        val publicDestinations = listOf(
            AppDestination.Public.Home,
            AppDestination.Public.About,
            AppDestination.Public.PrintingServices,
            AppDestination.Public.DigitalPrinting,
            AppDestination.Public.OffsetPrinting,
            AppDestination.Public.PackagingSolutions,
            AppDestination.Public.CorporateGifts,
            AppDestination.Public.Products,
            AppDestination.Public.Offers,
            AppDestination.Public.Portfolio,
            AppDestination.Public.Contact,
            AppDestination.Public.Location,
            AppDestination.Public.Faq,
            AppDestination.Public.Announcements,
            AppDestination.Public.PublicAiAssistant,
            AppDestination.Public.Login,
            AppDestination.Public.Register
        )

        for (dest in publicDestinations) {
            assertTrue("Destination ${dest.route} must be marked isPublic", dest.isPublic)
        }
    }

    @Test
    fun testGuestCanAccessPublicAiAssistantWithoutLogin() {
        val result = DeepLinkAuthorizer.authorizeDeepLink("public/ai-assistant", null)
        assertTrue(result is AppDestination.Public.PublicAiAssistant)
    }
}
