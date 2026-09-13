package com.sucharu.sucharupro.ui.admin

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.ui.admin.shell.AdminNavigationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminModuleUiIntegrationTest {

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "adm-001",
        projectId = "PRJ-001",
        username = "admin_user",
        role = UserRole.ADMIN
    )

    @Test
    fun testModuleClassification_all25ModulesClassified() {
        val groups = AdminNavigationRegistry.getAuthorizedGroups(adminPrincipal)
        assertTrue("Admin navigation groups must exist for Modules 00-24", groups.isNotEmpty())

        val mappedModules = groups.flatMap { it.items }.map { it.canonicalModule }
        assertTrue(mappedModules.any { it.contains("Module 00") })
        assertTrue(mappedModules.any { it.contains("Module 01") })
        assertTrue(mappedModules.any { it.contains("Module 02") })
        assertTrue(mappedModules.any { it.contains("Module 04") })
        assertTrue(mappedModules.any { it.contains("Module 07") })
        assertTrue(mappedModules.any { it.contains("Module 09") })
        assertTrue(mappedModules.any { it.contains("Module 20") })
        assertTrue(mappedModules.any { it.contains("Module 21") })
        assertTrue(mappedModules.any { it.contains("Module 24") })
    }

    @Test
    fun testAdminScreenThemeAndTokens_instantiateCleanly() {
        val themeColors = com.sucharu.sucharupro.ui.admin.theme.AdminColors()
        val themeTypography = com.sucharu.sucharupro.ui.admin.theme.AdminTypography()

        assertNotNull(themeColors.background)
        assertNotNull(themeColors.surface)
        assertNotNull(themeTypography.pageTitle)
        assertEquals(24, themeTypography.pageTitle.fontSize.value.toInt())
    }
}
