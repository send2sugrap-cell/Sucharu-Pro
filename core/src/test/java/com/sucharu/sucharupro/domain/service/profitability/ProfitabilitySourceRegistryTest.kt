package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.businessintegrity.Module16FinancialHandoffContract
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ProfitabilitySourceRegistryTest {

    private lateinit var sourceRegistry: ProfitabilitySourceRegistryImpl
    private val tenantId = "TENANT-001"
    private val projectId = "PROJ-101"

    @Before
    fun setUp() {
        val fakeHandoffAdapter = object : Module16FinancialHandoffAdapter {
            override suspend fun getVerifiedFinancialHandoff(
                tenantId: String,
                projectId: String,
                periodId: String
            ): DomainResult<ValidatedFinancialHandoff> {
                return DomainResult.Success(
                    ValidatedFinancialHandoff(
                        contract = Module16FinancialHandoffContract(
                            tenantId = tenantId,
                            projectId = projectId,
                            periodId = periodId,
                            periodCode = "2026-M08",
                            isPeriodClosed = false,
                            closureCertificateChecksum = null,
                            isLedgerBalanced = true
                        ),
                        integrityStatus = SourceIntegrityStatus.VERIFIED,
                        isLedgerBalanced = true,
                        isPeriodClosed = false,
                        hasValidClosureCertificate = false,
                        validationNotes = emptyList()
                    )
                )
            }

            override suspend fun verifyPeriodIntegrityStatus(
                tenantId: String,
                projectId: String,
                periodId: String
            ): DomainResult<SourceIntegrityStatus> {
                return DomainResult.Success(SourceIntegrityStatus.VERIFIED)
            }
        }

        sourceRegistry = ProfitabilitySourceRegistryImpl(fakeHandoffAdapter)
    }

    @Test
    fun testRevenueProvenanceValidation() {
        val validRev = RevenueProvenance(
            id = "REV-1",
            tenantId = tenantId,
            projectId = projectId,
            canonicalSourceType = RevenueSourceType.CUSTOMER_INVOICE,
            canonicalSourceId = "INV-101",
            recognizedAmount = BigDecimal("5000.0000")
        )
        val resValid = sourceRegistry.validateRevenueProvenance(validRev)
        assertTrue(resValid is DomainResult.Success)

        val invalidRev = RevenueProvenance(
            id = "REV-2",
            tenantId = "",
            projectId = projectId,
            canonicalSourceType = RevenueSourceType.CUSTOMER_INVOICE,
            canonicalSourceId = "INV-102",
            recognizedAmount = BigDecimal("-100.0000")
        )
        val resInvalid = sourceRegistry.validateRevenueProvenance(invalidRev)
        assertTrue(resInvalid is DomainResult.Error)
    }

    @Test
    fun testCostAttributionValidation() {
        val validCost = CostAttributionReference(
            id = "ATTR-1",
            tenantId = tenantId,
            projectId = projectId,
            sourceType = CostAttributionSourceType.EXPENSE,
            sourceId = "EXP-101",
            componentType = CostComponentType.MATERIAL,
            sourceAmount = BigDecimal("1500.0000"),
            attributableAmount = BigDecimal("1500.0000")
        )
        val resValid = sourceRegistry.validateCostAttribution(validCost)
        assertTrue(resValid is DomainResult.Success)

        val invalidCost = CostAttributionReference(
            id = "ATTR-2",
            tenantId = tenantId,
            projectId = projectId,
            sourceType = CostAttributionSourceType.EXPENSE,
            sourceId = "",
            componentType = CostComponentType.MATERIAL,
            sourceAmount = BigDecimal("1500.0000"),
            attributableAmount = BigDecimal("-50.0000")
        )
        val resInvalid = sourceRegistry.validateCostAttribution(invalidCost)
        assertTrue(resInvalid is DomainResult.Error)
    }

    @Test
    fun testDuplicateSourceDetection() {
        val rev1 = RevenueProvenance("R1", tenantId, projectId, RevenueSourceType.CUSTOMER_INVOICE, "INV-100", recognizedAmount = BigDecimal("1000.0000"))
        val rev2 = RevenueProvenance("R2", tenantId, projectId, RevenueSourceType.CUSTOMER_INVOICE, "INV-100", recognizedAmount = BigDecimal("1000.0000")) // duplicate!

        val cost1 = CostAttributionReference("C1", tenantId, projectId, CostAttributionSourceType.EXPENSE, "EXP-1", CostComponentType.MATERIAL, attributableAmount = BigDecimal("500.0000"))
        val cost2 = CostAttributionReference("C2", tenantId, projectId, CostAttributionSourceType.EXPENSE, "EXP-1", CostComponentType.MATERIAL, attributableAmount = BigDecimal("500.0000")) // duplicate!

        val duplicates = sourceRegistry.detectDuplicateSources(listOf(rev1, rev2), listOf(cost1, cost2))
        assertEquals(2, duplicates.size)
        assertTrue(duplicates.any { it.contains("Duplicate revenue source") })
        assertTrue(duplicates.any { it.contains("Duplicate cost attribution") })
    }

    @Test
    fun testSourceReadinessEvaluation() = runBlocking {
        val res = sourceRegistry.evaluateSourceReadiness(tenantId, projectId, "PER-2026-M08")
        assertTrue(res is DomainResult.Success)
        val readiness = (res as DomainResult.Success).data

        assertEquals(SourceIntegrityStatus.VERIFIED, readiness.module15HandoffStatus)
        assertTrue(readiness.isLedgerBalanced)
        assertTrue(readiness.directExpensesAvailable)
        assertTrue(readiness.vendorPayablesAvailable)
    }
}
