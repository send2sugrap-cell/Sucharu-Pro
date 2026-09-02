package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorRfqDataSource
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRfqRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorCategory
import com.sucharu.sucharupro.domain.model.vendorportal.VendorRfq
import com.sucharu.sucharupro.domain.model.vendorportal.VendorRfqItem
import com.sucharu.sucharupro.domain.model.vendorportal.VendorRfqStatus
import com.sucharu.sucharupro.domain.service.vendorportal.VendorRfqServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorRfqServiceTest {

    private lateinit var service: VendorRfqServiceImpl
    private lateinit var rfqRepository: VendorRfqRepositoryImpl
    private lateinit var vendorRepository: VendorRepositoryImpl

    @Before
    fun setup() {
        rfqRepository = VendorRfqRepositoryImpl(FakeVendorRfqDataSource())
        vendorRepository = VendorRepositoryImpl(FakeVendorDataSource())
        service = VendorRfqServiceImpl(rfqRepository, vendorRepository)
    }

    @Test
    fun testCreatesPublishesAndOpensRfq() = runBlocking {
        val rfq = VendorRfq(
            rfqId = "rfq-1",
            tenantId = "proj-1",
            projectId = "proj-1",
            rfqNumber = "RFQ-001",
            title = "Paper Supply",
            requestedBy = "user-1",
            responseDeadline = System.currentTimeMillis() + 86400000L,
            items = listOf(
                VendorRfqItem(
                    rfqItemId = "item-1",
                    rfqId = "rfq-1",
                    sequenceNumber = 1,
                    description = "Paper A4",
                    quantity = BigDecimal("100.00"),
                    targetUnitPrice = Money("400.00")
                )
            ),
            createdBy = "user-1"
        )

        val createRes = service.createRfq(rfq, "proj-1", "user-1")
        assertTrue(createRes is DomainResult.Success)

        val pubRes = service.publishRfq("rfq-1", "proj-1", "user-1")
        assertTrue(pubRes is DomainResult.Success)
        assertEquals(VendorRfqStatus.PUBLISHED, (pubRes as DomainResult.Success).data.status)

        val openRes = service.openRfq("rfq-1", "proj-1", "user-1")
        assertTrue(openRes is DomainResult.Success)
        assertEquals(VendorRfqStatus.OPEN, (openRes as DomainResult.Success).data.status)
    }

    @Test
    fun testInvitesVendorAndAcknowledges() = runBlocking {
        vendorRepository.createVendor(
            Vendor(
                vendorId = "vnd-1",
                projectId = "proj-1",
                vendorCode = "VND-001",
                vendorName = "Paper Corp",
                vendorCategory = VendorCategory.PAPER_SUPPLIER
            )
        )

        val rfq = VendorRfq(
            rfqId = "rfq-1",
            tenantId = "proj-1",
            projectId = "proj-1",
            rfqNumber = "RFQ-001",
            title = "Paper Supply",
            requestedBy = "user-1",
            responseDeadline = System.currentTimeMillis() + 86400000L,
            items = listOf(
                VendorRfqItem(
                    rfqItemId = "item-1",
                    rfqId = "rfq-1",
                    sequenceNumber = 1,
                    description = "Paper A4",
                    quantity = BigDecimal("100.00")
                )
            ),
            createdBy = "user-1"
        )
        service.createRfq(rfq, "proj-1", "user-1")

        val invRes = service.inviteVendor("rfq-1", "vnd-1", "proj-1", "proj-1", "user-1")
        assertTrue(invRes is DomainResult.Success)

        val ackRes = service.acknowledgeInvitation("rfq-1", "vnd-1", "proj-1", "vnd-user-1")
        assertTrue(ackRes is DomainResult.Success)
    }
}
