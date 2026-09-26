package com.sucharu.sucharupro.domain.service.wall

import com.sucharu.sucharupro.data.repository.ServerDrivenWallRepositoryImpl
import com.sucharu.sucharupro.domain.model.offer.AudienceType
import com.sucharu.sucharupro.domain.model.wall.SectionLayoutType
import com.sucharu.sucharupro.domain.model.wall.SectionType
import com.sucharu.sucharupro.domain.model.wall.ServerDrivenWallConfig
import com.sucharu.sucharupro.domain.model.wall.WallCategoryType
import com.sucharu.sucharupro.domain.model.wall.WallPublishStatus
import com.sucharu.sucharupro.domain.model.wall.WallSectionConfig
import com.sucharu.sucharupro.domain.model.wall.WallSectionItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ServerDrivenWallServiceTest {

    private lateinit var repository: ServerDrivenWallRepositoryImpl
    private lateinit var service: ServerDrivenWallService

    @Before
    fun setUp() {
        repository = ServerDrivenWallRepositoryImpl()
        service = ServerDrivenWallService(repository)
    }

    @Test
    fun `resolveServerDrivenWall_returnsActiveSectionsForGuestAudience`() = runBlocking {
        val publicWall = ServerDrivenWallConfig(
            wallId = "WALL-TEST-PUBLIC",
            wallType = WallCategoryType.PUBLIC,
            wallTitle = "Test Public Wall",
            versionNumber = 1,
            status = WallPublishStatus.PUBLISHED,
            isActivePublished = true,
            publicVisibility = true,
            guestVisibility = true,
            sections = listOf(
                WallSectionConfig(
                    sectionId = "SEC-01",
                    wallId = "WALL-TEST-PUBLIC",
                    sectionType = SectionType.PRODUCT_GALLERY,
                    sectionName = "Featured Printing Products",
                    layoutType = SectionLayoutType.GRID,
                    displayOrder = 1,
                    isVisible = true,
                    items = listOf(WallSectionItem("I-01", "SEC-01", productId = "PROD-101", displayOrder = 1, createdAt = "2026-09-26T14:45:00Z")),
                    createdAt = "2026-09-26T14:45:00Z"
                )
            ),
            createdAt = "2026-09-26T14:45:00Z",
            updatedAt = "2026-09-26T14:45:00Z",
            createdBy = "ADMIN-001"
        )

        service.saveWallConfig(publicWall)

        val resolved = service.resolveServerDrivenWall(WallCategoryType.PUBLIC, AudienceType.GUEST)

        assertNotNull(resolved)
        assertEquals("WALL-TEST-PUBLIC", resolved.wallId)
        assertEquals(1, resolved.sections.size)
        assertEquals("SEC-01", resolved.sections.first().sectionId)
    }

    @Test
    fun `publishWallVersion_createsImmutablePublishingSnapshot`() = runBlocking {
        val wall = ServerDrivenWallConfig(
            wallId = "WALL-TEST-PUB-VER",
            wallType = WallCategoryType.CUSTOMER,
            wallTitle = "Customer Exclusive Wall",
            createdAt = "2026-09-26T14:45:00Z",
            updatedAt = "2026-09-26T14:45:00Z",
            createdBy = "ADMIN-001"
        )

        service.saveWallConfig(wall)
        val version = service.publishWallVersion("WALL-TEST-PUB-VER", "ADMIN-001")

        assertNotNull(version)
        assertEquals(WallPublishStatus.PUBLISHED, version.status)
        assertEquals(2, version.versionNumber)
    }
}
