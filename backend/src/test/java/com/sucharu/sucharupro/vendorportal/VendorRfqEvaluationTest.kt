package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorQuotationDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorRfqDataSource
import com.sucharu.sucharupro.data.repository.VendorQuotationRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRfqRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorCategory
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.service.vendorportal.VendorRfqEvaluationServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorRfqEvaluationTest {

    private lateinit var evalService: VendorRfqEvaluationServiceImpl
    private lateinit var rfqRepo: VendorRfqRepositoryImpl
    private lateinit var quoteRepo: VendorQuotationRepositoryImpl
    private lateinit var vendorRepo: VendorRepositoryImpl

    @Before
    fun setup() {
        rfqRepo = VendorRfqRepositoryImpl(FakeVendorRfqDataSource())
        quoteRepo = VendorQuotationRepositoryImpl(FakeVendorQuotationDataSource())
        vendorRepo = VendorRepositoryImpl(FakeVendorDataSource())
        evalService = VendorRfqEvaluationServiceImpl(rfqRepo, quoteRepo, vendorRepo)
    }

    @Test
    fun testEvaluatesQuotationGeneratesComparisonAndAwardsRfq() = runBlocking {
        vendorRepo.createVendor(
            Vendor(vendorId = "vnd-1", projectId = "proj-1", vendorCode = "VND-1", vendorName = "Alpha Paper", vendorCategory = VendorCategory.PAPER_SUPPLIER)
        )
        vendorRepo.createVendor(
            Vendor(vendorId = "vnd-2", projectId = "proj-1", vendorCode = "VND-2", vendorName = "Beta Paper", vendorCategory = VendorCategory.PAPER_SUPPLIER)
        )

        val rfq = VendorRfq(
            rfqId = "rfq-1",
            tenantId = "proj-1",
            projectId = "proj-1",
            rfqNumber = "RFQ-001",
            title = "Paper Supply",
            requestedBy = "staff-1",
            status = VendorRfqStatus.CLOSED,
            responseDeadline = System.currentTimeMillis() + 86400000L,
            createdBy = "staff-1"
        )
        rfqRepo.createRfq(rfq)

        val q1 = VendorQuotation(
            quotationId = "q-1",
            rfqId = "rfq-1",
            invitationId = "inv-1",
            vendorId = "vnd-1",
            projectId = "proj-1",
            tenantId = "proj-1",
            quotationNumber = "QTN-001",
            grandTotal = Money("1000.00"),
            status = VendorQuotationStatus.SUBMITTED,
            submittedBy = "vnd-user-1",
            createdBy = "vnd-user-1"
        )
        val q2 = VendorQuotation(
            quotationId = "q-2",
            rfqId = "rfq-1",
            invitationId = "inv-2",
            vendorId = "vnd-2",
            projectId = "proj-1",
            tenantId = "proj-1",
            quotationNumber = "QTN-002",
            grandTotal = Money("1200.00"),
            status = VendorQuotationStatus.SUBMITTED,
            submittedBy = "vnd-user-2",
            createdBy = "vnd-user-2"
        )
        quoteRepo.createQuotation(q1)
        quoteRepo.createQuotation(q2)

        // 1. Record Evaluation for q1
        val eval = VendorRfqEvaluation(
            evaluationId = "eval-1",
            rfqId = "rfq-1",
            quotationId = "q-1",
            vendorId = "vnd-1",
            tenantId = "proj-1",
            projectId = "proj-1",
            evaluatorUserId = "evaluator-1",
            scores = listOf(
                VendorRfqEvaluationScore(criterion = "Price", weightPercent = 50.0, rawScore = 90.0, weightedScore = 0.0),
                VendorRfqEvaluationScore(criterion = "Quality", weightPercent = 50.0, rawScore = 80.0, weightedScore = 0.0)
            ),
            decision = VendorRfqEvaluationDecision.RECOMMENDED_FOR_AWARD
        )
        val recordRes = evalService.recordEvaluation(eval, "proj-1", "evaluator-1")
        assertTrue(recordRes is DomainResult.Success)
        assertEquals(85.0, (recordRes as DomainResult.Success).data.totalScore, 0.01)

        // 2. Comparison snapshot
        val compRes = evalService.getComparisonSnapshot("rfq-1", "proj-1")
        assertTrue(compRes is DomainResult.Success)
        val snap = (compRes as DomainResult.Success).data
        assertEquals(2, snap.totalBidsReceived)
        assertEquals(Money("1000.00"), snap.lowestBidAmount)

        // 3. Award RFQ to q1
        val awardRes = evalService.awardRfq("rfq-1", "q-1", "Best price and evaluation score", "proj-1", "manager-1")
        assertTrue(awardRes is DomainResult.Success)
        val awardedRfq = (awardRes as DomainResult.Success).data
        assertEquals(VendorRfqStatus.AWARDED, awardedRfq.status)
        assertEquals("vnd-1", awardedRfq.awardDecision?.winningVendorId)

        // Winning quote is ACCEPTED, losing quote is REJECTED
        val q1After = (quoteRepo.findQuotationById("q-1", "proj-1") as DomainResult.Success).data
        val q2After = (quoteRepo.findQuotationById("q-2", "proj-1") as DomainResult.Success).data
        assertEquals(VendorQuotationStatus.ACCEPTED, q1After.status)
        assertEquals(VendorQuotationStatus.REJECTED, q2After.status)
    }
}
