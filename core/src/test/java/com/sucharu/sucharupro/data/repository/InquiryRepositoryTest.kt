package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.FakeInquiryDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.order.Inquiry
import com.sucharu.sucharupro.domain.model.order.InquiryRequirement
import com.sucharu.sucharupro.domain.model.order.InquirySource
import com.sucharu.sucharupro.domain.model.order.InquiryStatusType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InquiryRepositoryTest {

    private lateinit var dataSource: FakeInquiryDataSource
    private lateinit var repository: InquiryRepositoryImpl

    @Before
    fun setUp() {
        dataSource = FakeInquiryDataSource()
        repository = InquiryRepositoryImpl(dataSource)
    }

    @Test
    fun test01_createInquiry_success() = runBlocking {
        val newInquiry = Inquiry(
            inquiryId = "inq-test-01",
            inquiryNumber = "INQ-999991",
            customerId = "cus-003",
            status = InquiryStatusType.NEW,
            source = InquirySource.WHATSAPP,
            items = listOf(
                InquiryRequirement(
                    itemId = "item-01",
                    productName = "Leaflets",
                    description = "150 GSM Art Paper, 4 Color Print",
                    quantity = 2000
                )
            ),
            createdAt = "2026-08-15T12:00:00Z",
            updatedAt = "2026-08-15T12:00:00Z"
        )

        val result = repository.createInquiry(newInquiry)
        assertTrue(result.isSuccess)
        val created = (result as DomainResult.Success).data
        assertEquals("inq-test-01", created.inquiryId)

        val fetched = repository.findInquiryById("inq-test-01")
        assertTrue(fetched.isSuccess)
        assertEquals("INQ-999991", (fetched as DomainResult.Success).data.inquiryNumber)
    }

    @Test
    fun test02_duplicateInquiryId_rejected() = runBlocking {
        val duplicateInquiry = Inquiry(
            inquiryId = "inq-001", // already in FakeInquiryDataSource
            inquiryNumber = "INQ-UNIQUE",
            customerId = "cus-001",
            createdAt = "2026-08-15T12:00:00Z",
            updatedAt = "2026-08-15T12:00:00Z"
        )

        val result = repository.createInquiry(duplicateInquiry)
        assertTrue(result.isError)
        assertTrue((result as DomainResult.Error).message.contains("already exists"))
    }

    @Test
    fun test03_getInquiry_success() = runBlocking {
        val streamResult = repository.getInquiryById("inq-001").first()
        assertNotNull(streamResult)
        assertEquals("INQ-000001", streamResult?.inquiryNumber)
        assertEquals("cus-001", streamResult?.customerId)
    }

    @Test
    fun test04_updateInquiry_preservesIdAndCreatedTimestamp() = runBlocking {
        val original = (repository.findInquiryById("inq-001") as DomainResult.Success).data
        val updatedInput = original.copy(
            notes = "Updated customer delivery deadline: Sept 1st",
            createdAt = "TAMPERED_TIMESTAMP"
        )

        val result = repository.updateInquiry(updatedInput)
        assertTrue(result.isSuccess)
        val updated = (result as DomainResult.Success).data

        assertEquals("inq-001", updated.inquiryId)
        assertEquals(original.createdAt, updated.createdAt) // Preserved
        assertEquals("Updated customer delivery deadline: Sept 1st", updated.notes)
    }

    @Test
    fun test05_updateInquiry_missingInquiry_rejected() = runBlocking {
        val nonExistent = Inquiry(
            inquiryId = "inq-non-existent",
            inquiryNumber = "INQ-404",
            customerId = "cus-001",
            createdAt = "2026-08-15T12:00:00Z",
            updatedAt = "2026-08-15T12:00:00Z"
        )

        val result = repository.updateInquiry(nonExistent)
        assertTrue(result.isError)
    }

    @Test
    fun test06_getInquiriesForCustomer_isolated() = runBlocking {
        val cus1Inquiries = repository.getInquiriesForCustomer("cus-001").first()
        val cus2Inquiries = repository.getInquiriesForCustomer("cus-002").first()

        assertTrue(cus1Inquiries.all { it.customerId == "cus-001" })
        assertTrue(cus2Inquiries.all { it.customerId == "cus-002" })
        assertFalse(cus1Inquiries.any { it.customerId == "cus-002" })
    }

    @Test
    fun test07_observeInquiries_emitsUpdates() = runBlocking {
        val initialCount = repository.getInquiries().first().size

        repository.createInquiry(
            Inquiry(
                inquiryId = "inq-stream-test",
                inquiryNumber = "INQ-STREAM",
                customerId = "cus-001",
                createdAt = "2026-08-15T12:00:00Z",
                updatedAt = "2026-08-15T12:00:00Z"
            )
        )

        val updatedCount = repository.getInquiries().first().size
        assertEquals(initialCount + 1, updatedCount)
    }

    @Test
    fun test08_invalidStatusTransition_rejected() = runBlocking {
        // inq-001 is QUOTED
        // QUOTED -> NEW is invalid
        val result = repository.updateInquiryStatus("inq-001", InquiryStatusType.NEW)
        assertTrue(result.isError)
        assertTrue((result as DomainResult.Error).message.contains("Invalid status transition"))
    }

    @Test
    fun test09_deleteInquiry_success() = runBlocking {
        val deleteResult = repository.deleteInquiry("inq-002")
        assertTrue(deleteResult.isSuccess)

        val fetched = repository.findInquiryById("inq-002")
        assertTrue(fetched.isError)
    }
}
