package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorQuotationDataSource
import com.sucharu.sucharupro.data.repository.VendorQuotationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendorportal.VendorQuotation
import com.sucharu.sucharupro.domain.model.vendorportal.VendorQuotationItem
import com.sucharu.sucharupro.domain.model.vendorportal.VendorQuotationRevision
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorQuotationRepositoryTest {

    private lateinit var repository: VendorQuotationRepositoryImpl

    @Before
    fun setup() {
        repository = VendorQuotationRepositoryImpl(FakeVendorQuotationDataSource())
    }

    @Test
    fun testCreatesAndRetrievesQuotation() = runBlocking {
        val quotation = VendorQuotation(
            quotationId = "q-1",
            rfqId = "rfq-1",
            invitationId = "inv-1",
            vendorId = "vnd-1",
            projectId = "proj-1",
            tenantId = "tenant-1",
            quotationNumber = "QTN-001",
            items = listOf(
                VendorQuotationItem(
                    quotationItemId = "qi-1",
                    quotationId = "q-1",
                    rfqItemId = "rfq-item-1",
                    quantity = BigDecimal("10.00"),
                    unitPrice = Money("100.00"),
                    lineTotal = Money("1000.00")
                )
            ),
            subtotal = Money("1000.00"),
            grandTotal = Money("1000.00"),
            createdBy = "user-1"
        )

        val createRes = repository.createQuotation(quotation)
        assertTrue(createRes is DomainResult.Success)

        val findRes = repository.findQuotationById("q-1", "tenant-1")
        assertTrue(findRes is DomainResult.Success)
        assertEquals("QTN-001", (findRes as DomainResult.Success).data.quotationNumber)
    }

    @Test
    fun testRecordsAndListsRevisions() = runBlocking {
        val rev = VendorQuotationRevision(
            revisionId = "rev-1",
            quotationId = "q-1",
            rfqId = "rfq-1",
            vendorId = "vnd-1",
            tenantId = "tenant-1",
            projectId = "proj-1",
            revisionNumber = 1,
            reasonForRevision = "Updated discount",
            snapshotSubtotal = Money("1000.00"),
            snapshotGrandTotal = Money("1000.00"),
            itemsSnapshotJson = "item-1:10@100=1000",
            revisedBy = "user-1"
        )

        repository.recordRevision(rev)
        val list = (repository.listRevisionsByQuotation("q-1", "tenant-1") as DomainResult.Success).data
        assertEquals(1, list.size)
        assertEquals(1, list[0].revisionNumber)
    }
}
