package com.sucharu.sucharupro.domain.service.content

import com.sucharu.sucharupro.data.repository.ContentFoundationRepositoryImpl
import com.sucharu.sucharupro.domain.model.content.ContentFoundation
import com.sucharu.sucharupro.domain.model.content.PublicationStatus
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.product.ProductType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContentFoundationServiceTest {

    private lateinit var repository: ContentFoundationRepositoryImpl
    private lateinit var service: ContentFoundationService

    @Before
    fun setUp() {
        repository = ContentFoundationRepositoryImpl()
        service = ContentFoundationService(repository)
    }

    @Test
    fun `createContentRecord persists valid content foundation entity`() = runBlocking {
        val record = ContentFoundation(
            contentId = "CNT-2026-001",
            contentType = ProductType.FINISHED_PRODUCT,
            productId = "PROD-101",
            productName = "2027 Executive Desktop Calendar",
            productCode = "SKU-CAL-2027",
            categoryName = "Calendars",
            templateCode = "#TMPL-CAL-01",
            title = "Executive Desk Calendar 2027",
            subtitle = "Custom printed 12-page calendar",
            unit = InventoryUnit.PCS,
            minimumQuantity = 100,
            availableQuantity = 500,
            publicationStatus = PublicationStatus.DRAFT,
            createdAt = "2026-09-26T10:00:00Z",
            updatedAt = "2026-09-26T10:00:00Z",
            createdBy = "ADMIN-001"
        )

        val created = service.createContentRecord(record)
        assertNotNull(created)
        assertEquals("CNT-2026-001", created.contentId)
        assertEquals("SKU-CAL-2027", created.productCode)
    }

    @Test
    fun `publishContent changes status to PUBLISHED`() = runBlocking {
        val record = ContentFoundation(
            contentId = "CNT-2026-002",
            contentType = ProductType.PRINTING_JOB,
            productName = "Custom Visiting Card",
            productCode = "SKU-VC-300",
            categoryName = "Visiting Cards",
            templateCode = "#TMPL-VC-02",
            title = "300 GSM Matte Spot UV Visiting Card",
            unit = InventoryUnit.PCS,
            minimumQuantity = 1000,
            publicationStatus = PublicationStatus.DRAFT,
            createdAt = "2026-09-26T10:00:00Z",
            updatedAt = "2026-09-26T10:00:00Z",
            createdBy = "ADMIN-001"
        )

        service.createContentRecord(record)
        val published = service.publishContent("CNT-2026-002", "ADMIN-001")

        assertEquals(PublicationStatus.PUBLISHED, published.publicationStatus)
        assertTrue(published.isCurrentlyPublishable("2026-09-26T12:00:00Z"))
    }

    @Test
    fun `scheduleContent sets scheduled timestamps and publication state`() = runBlocking {
        val record = ContentFoundation(
            contentId = "CNT-2026-003",
            contentType = ProductType.GIFT_PROMOTIONAL,
            productName = "Eid Promotional Gift Box",
            productCode = "SKU-GIFT-EID",
            categoryName = "Packaging",
            templateCode = "#TMPL-EID-GIFT",
            title = "Eid Special Bulk Gift Box",
            unit = InventoryUnit.PCS,
            minimumQuantity = 50,
            publicationStatus = PublicationStatus.DRAFT,
            createdAt = "2026-09-26T10:00:00Z",
            updatedAt = "2026-09-26T10:00:00Z",
            createdBy = "ADMIN-001"
        )

        service.createContentRecord(record)
        val scheduled = service.scheduleContent(
            contentId = "CNT-2026-003",
            startAt = "2026-10-01T00:00:00Z",
            endAt = "2026-10-15T23:59:59Z",
            actorId = "ADMIN-001"
        )

        assertEquals(PublicationStatus.SCHEDULED, scheduled.publicationStatus)
        assertEquals("2026-10-01T00:00:00Z", scheduled.scheduledStartAt)
    }
}
