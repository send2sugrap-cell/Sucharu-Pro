package com.sucharu.sucharupro.vendorpayable

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.vendorpayable.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.vendorpayable.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorpayable.*
import com.sucharu.sucharupro.domain.service.vendorpayable.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPayableServiceTest {

    private lateinit var dataSource: FakeVendorPayableDataSource
    private lateinit var repository: VendorPayableRepositoryImpl
    private lateinit var service: VendorPayableServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val vendorId = "VEND-1001"

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "USER-STAFF-1",
        projectId = projectId,
        username = "staff1",
        role = UserRole.STAFF
    )

    private val managerPrincipal = AuthenticatedPrincipal(
        userId = "USER-MGR-1",
        projectId = projectId,
        username = "manager1",
        role = UserRole.MANAGER
    )

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "USER-ADMIN-1",
        projectId = projectId,
        username = "admin1",
        role = UserRole.ADMIN
    )

    @Before
    fun setup() {
        dataSource = FakeVendorPayableDataSource()
        repository = VendorPayableRepositoryImpl(dataSource)
        service = VendorPayableServiceImpl(repository, tenantId)
    }

    @Test
    fun testFullPayableLifecycle_DraftSubmitApprove() = runBlocking {
        // 1. Create Draft
        val createCmd = CreateVendorPayableCommand(
            vendorId = vendorId,
            originalAmount = BigDecimal("8500.00"),
            currency = "BDT",
            issueDate = System.currentTimeMillis(),
            paymentTerms = VendorPayablePaymentTerms.NET_15,
            description = "Die Cutting & Embossing Moulds"
        )
        val createRes = service.createPayable(staffPrincipal, createCmd)
        assertTrue(createRes is DomainResult.Success)
        val payable = (createRes as DomainResult.Success).data
        assertEquals(VendorPayableStatus.DRAFT, payable.status)
        assertEquals("USER-STAFF-1", payable.createdBy)

        // 2. Edit Draft
        val updateCmd = UpdateVendorPayableCommand(
            originalAmount = BigDecimal("8900.00"),
            description = "Die Cutting & Embossing Moulds + Setup"
        )
        val updateRes = service.updatePayableDraft(staffPrincipal, payable.payableId, updateCmd)
        assertTrue(updateRes is DomainResult.Success)
        val updated = (updateRes as DomainResult.Success).data
        assertEquals(BigDecimal("8900.0000"), updated.originalAmount)

        // 3. Submit
        val submitRes = service.submitPayable(staffPrincipal, payable.payableId)
        assertTrue(submitRes is DomainResult.Success)
        val submitted = (submitRes as DomainResult.Success).data
        assertEquals(VendorPayableStatus.SUBMITTED, submitted.status)

        // 4. Approve by Manager (SoD satisfied)
        val approveRes = service.approvePayable(managerPrincipal, payable.payableId, "Approved by Accounts Manager")
        assertTrue(approveRes is DomainResult.Success)
        val approved = (approveRes as DomainResult.Success).data
        assertEquals(VendorPayableStatus.APPROVED, approved.status)
        assertEquals("USER-MGR-1", approved.approvedBy)

        // 5. Verify Audit Trail
        val auditsRes = service.getPayableAuditTrail(managerPrincipal, payable.payableId)
        assertTrue(auditsRes is DomainResult.Success)
        val audits = (auditsRes as DomainResult.Success).data
        assertEquals(4, audits.size) // CREATED, UPDATED, SUBMITTED, APPROVED
    }

    @Test
    fun testRejectAndResubmitWorkflow() = runBlocking {
        val createRes = service.createPayable(
            staffPrincipal,
            CreateVendorPayableCommand(
                vendorId = vendorId,
                originalAmount = BigDecimal("2000.00"),
                description = "Emergency Solvent Ink Batch",
                autoSubmit = true
            )
        )
        val payable = (createRes as DomainResult.Success).data
        assertEquals(VendorPayableStatus.SUBMITTED, payable.status)

        // Reject by Manager
        val rejectRes = service.rejectPayable(managerPrincipal, payable.payableId, "Missing delivery challan scan")
        assertTrue(rejectRes is DomainResult.Success)
        val rejected = (rejectRes as DomainResult.Success).data
        assertEquals(VendorPayableStatus.REJECTED, rejected.status)
        assertEquals("Missing delivery challan scan", rejected.rejectionReason)

        // Staff updates and resubmits
        val editRes = service.updatePayableDraft(
            staffPrincipal,
            payable.payableId,
            UpdateVendorPayableCommand(notes = "Delivery challan attached")
        )
        assertTrue(editRes is DomainResult.Success)

        val resubmitRes = service.submitPayable(staffPrincipal, payable.payableId)
        assertTrue(resubmitRes is DomainResult.Success)
        assertEquals(VendorPayableStatus.SUBMITTED, (resubmitRes as DomainResult.Success).data.status)
    }

    @Test
    fun testCancelAndVoidWorkflows() = runBlocking {
        // Test Cancel
        val createRes = service.createPayable(
            staffPrincipal,
            CreateVendorPayableCommand(
                vendorId = vendorId,
                originalAmount = BigDecimal("3000.00"),
                description = "Duplicate Vendor Bill"
            )
        )
        val payable1 = (createRes as DomainResult.Success).data

        val cancelRes = service.cancelPayable(staffPrincipal, payable1.payableId, "Duplicate entry entered in error")
        assertTrue(cancelRes is DomainResult.Success)
        assertEquals(VendorPayableStatus.CANCELLED, (cancelRes as DomainResult.Success).data.status)

        // Test Void on Approved Payable
        val createRes2 = service.createPayable(
            staffPrincipal,
            CreateVendorPayableCommand(
                vendorId = vendorId,
                originalAmount = BigDecimal("5000.00"),
                description = "Cancelled Order Raw Material",
                autoSubmit = true
            )
        )
        val payable2 = (createRes2 as DomainResult.Success).data
        service.approvePayable(managerPrincipal, payable2.payableId, "Approved initially")

        val voidRes = service.voidPayable(managerPrincipal, payable2.payableId, "Order cancelled by client before delivery")
        assertTrue(voidRes is DomainResult.Success)
        assertEquals(VendorPayableStatus.VOIDED, (voidRes as DomainResult.Success).data.status)
    }
}
