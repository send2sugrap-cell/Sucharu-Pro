package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPerformanceDataSource
import com.sucharu.sucharupro.data.repository.VendorPerformanceRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.service.vendor.VendorPerformanceServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant

class VendorComplianceWorkflowTest {

    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var repo: VendorPerformanceRepositoryImpl
    private lateinit var service: VendorPerformanceServiceImpl

    @Before
    fun setUp() {
        runBlocking {
            vendorRepo = VendorRepositoryImpl(FakeVendorDataSource())
            val ds = FakeVendorPerformanceDataSource()
            repo = VendorPerformanceRepositoryImpl(ds)
            service = VendorPerformanceServiceImpl(
                performanceRepository = repo,
                vendorRepository = vendorRepo
            )

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "VND-01",
                    projectId = "PRJ-01",
                    vendorCode = "V001",
                    vendorName = "Vendor 1",
                    status = VendorStatus.ACTIVE
                )
            )

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "VND-02",
                    projectId = "PRJ-01",
                    vendorCode = "V002",
                    vendorName = "Vendor 2",
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testComplianceSubmissionAndVerificationWorkflow() = runBlocking {
        val req = VendorComplianceRequirement(
            requirementId = "COMP-REQ-01",
            projectId = "PRJ-01",
            tenantId = "PRJ-01",
            requirementType = ComplianceRequirementType.TAX_VAT,
            code = "TIN-01",
            name = "Tax Clearance Certificate",
            description = "Tax clearance",
            mandatory = true,
            riskLevel = ComplianceRiskLevel.HIGH,
            validityDays = 365,
            createdBy = "admin"
        )
        service.createComplianceRequirement(req)

        val now = Instant.now()
        val record = VendorComplianceRecord(
            recordId = "REC-001",
            projectId = "PRJ-01",
            tenantId = "PRJ-01",
            vendorId = "VND-01",
            requirementId = "COMP-REQ-01",
            requirementCode = "TIN-01",
            requirementName = "Tax Clearance Certificate",
            requirementType = ComplianceRequirementType.TAX_VAT,
            mandatory = true,
            effectiveDate = now,
            expiryDate = now.plusSeconds(86400 * 365),
            evidenceList = listOf(
                VendorComplianceEvidence(
                    evidenceId = "EVD-01",
                    recordId = "REC-001",
                    projectId = "PRJ-01",
                    tenantId = "PRJ-01",
                    evidenceType = ComplianceEvidenceType.DOCUMENT,
                    fileName = "tax_cert.pdf",
                    fileUrl = "https://storage.sucharu.com/tax_cert.pdf",
                    uploadedBy = "vendor_rep"
                )
            ),
            createdBy = "vendor_rep"
        )

        // Submit
        val submitRes = service.submitComplianceRecord(record)
        assertTrue(submitRes is DomainResult.Success)
        val submitted = (submitRes as DomainResult.Success).data
        assertEquals(ComplianceVerificationStatus.PENDING, submitted.verificationStatus)

        // Verify
        val verifyRes = service.verifyComplianceRecord(
            projectId = "PRJ-01",
            recordId = "REC-001",
            verifiedBy = "compliance_officer",
            verified = true,
            notes = "Authentic tax clearance document"
        )
        assertTrue(verifyRes is DomainResult.Success)
        val verified = (verifyRes as DomainResult.Success).data
        assertEquals(ComplianceVerificationStatus.VERIFIED, verified.verificationStatus)
        assertEquals(ComplianceStatus.VERIFIED, verified.status)
        assertEquals("compliance_officer", verified.verifiedBy)
    }

    @Test
    fun testComplianceRejectionWorkflow() = runBlocking {
        val now = Instant.now()
        val record = VendorComplianceRecord(
            recordId = "REC-002",
            projectId = "PRJ-01",
            tenantId = "PRJ-01",
            vendorId = "VND-02",
            requirementId = "COMP-REQ-02",
            requirementCode = "ISO-9001",
            requirementName = "ISO 9001 Certificate",
            requirementType = ComplianceRequirementType.QUALITY_CERTIFICATION,
            mandatory = false,
            effectiveDate = now,
            expiryDate = now.plusSeconds(86400 * 365),
            createdBy = "vendor_rep"
        )
        service.submitComplianceRecord(record)

        val rejectRes = service.verifyComplianceRecord(
            projectId = "PRJ-01",
            recordId = "REC-002",
            verifiedBy = "compliance_officer",
            verified = false,
            rejectionReason = "Expired ISO certificate copy submitted",
            notes = "Please re-upload current certification"
        )
        assertTrue(rejectRes is DomainResult.Success)
        val rejected = (rejectRes as DomainResult.Success).data
        assertEquals(ComplianceVerificationStatus.REJECTED, rejected.verificationStatus)
        assertEquals(ComplianceStatus.REJECTED, rejected.status)
        assertEquals("Expired ISO certificate copy submitted", rejected.rejectionReason)
    }
}
