package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorPortalSettlementDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalSettlementRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendorportal.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalSettlementRepositoryTest {

    private lateinit var dataSource: FakeVendorPortalSettlementDataSource
    private lateinit var repository: VendorPortalSettlementRepositoryImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val vendorId = "VND-001"

    @Before
    fun setup() {
        dataSource = FakeVendorPortalSettlementDataSource()
        repository = VendorPortalSettlementRepositoryImpl(dataSource)
    }

    @Test
    fun testSaveAndFindAcknowledgement() = runBlocking {
        val ack = VendorPortalSettlementAcknowledgement(
            acknowledgementId = "ACK-101",
            settlementId = "SETTL-101",
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            acknowledgedBy = "vendor_user",
            idempotencyKey = "IDEM-101"
        )
        val saveRes = repository.saveAcknowledgement(ack)
        assertTrue(saveRes is DomainResult.Success)

        val findRes = repository.findAcknowledgementById(tenantId, projectId, vendorId, "ACK-101")
        assertTrue(findRes is DomainResult.Success)
        assertEquals("SETTL-101", (findRes as DomainResult.Success).data?.settlementId)

        val findByIdem = repository.findAcknowledgementByIdempotencyKey(tenantId, projectId, vendorId, "IDEM-101")
        assertTrue(findByIdem is DomainResult.Success)
        assertNotNull((findByIdem as DomainResult.Success).data)
    }

    @Test
    fun testReconciliationCasePersistenceAndEvents() = runBlocking {
        val rc = VendorPortalReconciliationCase(
            caseId = "REC-201",
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            caseNumber = "REC-201-NUM",
            subject = "Underpayment investigation",
            claimedAmount = Money(BigDecimal("1000.00")),
            systemAmount = Money(BigDecimal("800.00")),
            varianceAmount = Money(BigDecimal("200.00")),
            createdBy = "vendor_rep"
        )
        repository.saveReconciliationCase(rc)

        val event = VendorPortalReconciliationEvent(
            eventId = "EVT-1",
            caseId = "REC-201",
            actorId = "vendor_rep",
            actorRole = "VENDOR",
            action = "COMMENT",
            remarks = "Sent breakdown email"
        )
        val appendRes = repository.appendReconciliationEvent(tenantId, projectId, vendorId, "REC-201", event)
        assertTrue(appendRes is DomainResult.Success)

        val list = repository.listReconciliationCases(tenantId, projectId, vendorId)
        assertTrue(list is DomainResult.Success)
        assertEquals(1, (list as DomainResult.Success).data.size)
    }

    @Test
    fun testFinancialEvidenceRepository() = runBlocking {
        val ev = VendorPortalFinancialSettlementEvidence(
            evidenceId = "EVD-01",
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            entityType = "SETTLEMENT",
            entityId = "SETTL-101",
            fileName = "bank_advice.pdf",
            fileUrl = "s3://vault/bank_advice.pdf",
            uploadedBy = "vendor_finance"
        )
        val saveRes = repository.saveEvidence(ev)
        assertTrue(saveRes is DomainResult.Success)

        val listRes = repository.listEvidence(tenantId, projectId, vendorId, entityType = "SETTLEMENT")
        assertTrue(listRes is DomainResult.Success)
        assertEquals(1, (listRes as DomainResult.Success).data.size)
    }
}
