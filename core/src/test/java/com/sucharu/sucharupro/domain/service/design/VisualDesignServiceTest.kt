package com.sucharu.sucharupro.domain.service.design

import com.sucharu.sucharupro.data.repository.VisualDesignRepositoryImpl
import com.sucharu.sucharupro.domain.model.design.DesignPublishStatus
import com.sucharu.sucharupro.domain.model.design.DesignTargetType
import com.sucharu.sucharupro.domain.model.design.VisualDesignConfiguration
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VisualDesignServiceTest {

    private lateinit var repository: VisualDesignRepositoryImpl
    private lateinit var service: VisualDesignService

    @Before
    fun setUp() {
        repository = VisualDesignRepositoryImpl()
        service = VisualDesignService(repository)
    }

    @Test
    fun `createDesignStudioConfig persists valid visual design configuration`() = runBlocking {
        val config = VisualDesignConfiguration(
            designId = "DSGN-TEST-001",
            designName = "Custom Gallery Theme",
            targetType = DesignTargetType.PRODUCT_GALLERY_CARD,
            cardWidthDp = 340,
            cardHeightDp = 440,
            backgroundColorHex = "#0F172A",
            createdAt = "2026-09-26T11:00:00Z",
            updatedAt = "2026-09-26T11:00:00Z",
            createdBy = "ADMIN-001"
        )

        val created = service.createDesignStudioConfig(config)
        assertNotNull(created)
        assertEquals("DSGN-TEST-001", created.designId)
        assertEquals("#0F172A", created.backgroundColorHex)
    }

    @Test
    fun `publishDesignStudioVersion creates immutable version snapshot`() = runBlocking {
        val config = VisualDesignConfiguration(
            designId = "DSGN-TEST-002",
            designName = "Special Offer Card Theme",
            targetType = DesignTargetType.OFFER_CARD,
            createdAt = "2026-09-26T11:00:00Z",
            updatedAt = "2026-09-26T11:00:00Z",
            createdBy = "ADMIN-001"
        )

        service.createDesignStudioConfig(config)
        val published = service.publishDesignStudioVersion("DSGN-TEST-002", "ADMIN-001")

        assertEquals(DesignPublishStatus.PUBLISHED, published.status)
        assertTrue(published.isActivePublished)

        val versions = repository.getDesignVersions("DSGN-TEST-002")
        assertEquals(1, versions.size)
        assertEquals(published.versionNumber, versions.first().versionNumber)
    }

    @Test
    fun `duplicateDesignStudioConfig creates isolated draft configuration`() = runBlocking {
        val config = VisualDesignConfiguration(
            designId = "DSGN-TEST-003",
            designName = "Original Master Style",
            targetType = DesignTargetType.WALL_CARD,
            createdAt = "2026-09-26T11:00:00Z",
            updatedAt = "2026-09-26T11:00:00Z",
            createdBy = "ADMIN-001"
        )

        service.createDesignStudioConfig(config)
        val duplicated = service.duplicateDesignStudioConfig("DSGN-TEST-003", "Copy of Master Style", "ADMIN-001")

        assertNotNull(duplicated)
        assertEquals(DesignPublishStatus.DRAFT, duplicated.status)
        assertEquals("Copy of Master Style", duplicated.designName)
        assertTrue(duplicated.designId != "DSGN-TEST-003")
    }
}
