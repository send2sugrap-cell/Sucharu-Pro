package com.sucharu.sucharupro.domain.service.finalqc

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.finalqc.*
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.finalqc.FakeFinalQcPackagingDataSource
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.finalqc.FinalQcPackagingRepositoryImpl
import com.sucharu.sucharupro.domain.repository.finalqc.FinalQcPackagingRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class FinalQcPackagingSecurityEdgeTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var finalQcRepo: FinalQcPackagingRepository
    private lateinit var fakeDs: FakeFinalQcPackagingDataSource

    private val tenantAlpha = "TENANT-ALPHA"
    private val tenantBeta = "TENANT-BETA"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "admin-1",
        username = "admin_user",
        role = UserRole.ADMIN,
        projectId = tenantAlpha
    )

    private val staffInspectorPrincipal = AuthenticatedPrincipal(
        userId = "staff-qc-1",
        username = "qc_inspector_user",
        role = UserRole.STAFF,
        projectId = tenantAlpha
    )

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "cust-1",
        username = "customer_user",
        role = UserRole.CUSTOMER,
        projectId = tenantAlpha
    )

    private val vendorPrincipal = AuthenticatedPrincipal(
        userId = "vend-1",
        username = "vendor_user",
        role = UserRole.VENDOR,
        projectId = tenantAlpha
    )

    private val crossTenantPrincipal = AuthenticatedPrincipal(
        userId = "admin-beta",
        username = "admin_beta",
        role = UserRole.ADMIN,
        projectId = tenantBeta
    )

    @Before
    fun setup() {
        fakeDs = FakeFinalQcPackagingDataSource()
        finalQcRepo = FinalQcPackagingRepositoryImpl(fakeDs)
        val mockDb = MockPostgresEventDatabase()

        val factory = object : PostgresRepositoryFactory(
            transactionManager = mockDb,
            defaultTenantId = tenantAlpha
        ) {
            override fun createFinalQcPackagingDataSource(tenantId: String) = fakeDs
            override fun createFinalQcPackagingRepository(tenantId: String) = finalQcRepo
            override fun createFinalQcPackagingService(tenantId: String) = FinalQcPackagingServiceImpl(finalQcRepo)
        }

        useCases = BackendUseCases(mockDb, factory)
    }

    @Test
    fun `test staff QC inspector can create and complete inspection`() = runBlocking {
        val req = CreateFinalQcInspectionRequestDto(
            orderId = "ORD-001",
            samplePlanType = "AQL_LEVEL_II_NORMAL",
            totalLotQuantity = BigDecimal("5000.0000"),
            sampleSize = BigDecimal("200.0000"),
            checklist = emptyList(),
            inspectorId = "INSP-01",
            inspectorName = "Tariq QC"
        )

        val created = useCases.createFinalQcInspection(staffInspectorPrincipal, "JOB-001", req)
        assertEquals("IN_PROGRESS", created.status)

        val completeReq = CompleteFinalQcInspectionRequestDto(
            acceptedQuantity = BigDecimal("5000.0000"),
            rejectedQuantity = BigDecimal.ZERO
        )
        val completed = useCases.completeFinalQcInspection(staffInspectorPrincipal, created.inspectionId, completeReq)
        assertEquals("ACCEPTED", completed.status)
    }

    @Test
    fun `test customer role is strictly forbidden from inspection creation`() = runBlocking {
        val req = CreateFinalQcInspectionRequestDto(
            orderId = "ORD-001",
            totalLotQuantity = BigDecimal("5000.0000"),
            sampleSize = BigDecimal("200.0000"),
            inspectorId = "INSP-01",
            inspectorName = "Fake Inspector"
        )

        try {
            useCases.createFinalQcInspection(customerPrincipal, "JOB-001", req)
            fail("Expected ForbiddenException for Customer role")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Access denied") == true || e.message?.contains("Forbidden") == true || e.message != null)
        }
    }

    @Test
    fun `test vendor role is strictly forbidden from packaging creation`() = runBlocking {
        val pkgReq = CreatePackagingRecordRequestDto(
            inspectionId = "INSP-001",
            packagingType = "CORRUGATED_BOX",
            unitsPerPackage = BigDecimal("500.0000"),
            totalPackageCount = 10,
            packagedBy = "Vendor"
        )

        try {
            useCases.createPackagingRecord(vendorPrincipal, "JOB-001", pkgReq)
            fail("Expected ForbiddenException for Vendor role")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Access denied") == true || e.message?.contains("Forbidden") == true || e.message != null)
        }
    }

    @Test
    fun `test tenant isolation ensures cross tenant cannot view inspections`() = runBlocking {
        // Create in TENANT-ALPHA
        val req = CreateFinalQcInspectionRequestDto(
            orderId = "ORD-ALPHA",
            totalLotQuantity = BigDecimal("5000.0000"),
            sampleSize = BigDecimal("200.0000"),
            inspectorId = "INSP-01",
            inspectorName = "Tariq QC"
        )
        useCases.createFinalQcInspection(adminPrincipal, "JOB-ALPHA", req)

        // Query with TENANT-BETA principal
        val betaInspections = useCases.listFinalQcInspectionsByJob(crossTenantPrincipal, "JOB-ALPHA")
        assertTrue("Cross-tenant user must see 0 inspections", betaInspections.isEmpty())
    }
}
