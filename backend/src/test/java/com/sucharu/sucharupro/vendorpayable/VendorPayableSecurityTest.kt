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

class VendorPayableSecurityTest {

    private lateinit var dataSource: FakeVendorPayableDataSource
    private lateinit var repository: VendorPayableRepositoryImpl
    private lateinit var service: VendorPayableServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val vendorId = "VEND-1001"

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "USER-CUS-1",
        projectId = projectId,
        username = "customer1",
        role = UserRole.CUSTOMER
    )

    private val affiliatePrincipal = AuthenticatedPrincipal(
        userId = "USER-AFF-1",
        projectId = projectId,
        username = "affiliate1",
        role = UserRole.AFFILIATE
    )

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

    @Before
    fun setup() {
        dataSource = FakeVendorPayableDataSource()
        repository = VendorPayableRepositoryImpl(dataSource)
        service = VendorPayableServiceImpl(repository, tenantId)
    }

    @Test
    fun testExternalPrincipalsDeniedAccess() = runBlocking {
        val cmd = CreateVendorPayableCommand(
            vendorId = vendorId,
            originalAmount = BigDecimal("1500.00"),
            description = "Unauthorized Attempt"
        )

        // Customer denied
        val cusRes = service.createPayable(customerPrincipal, cmd)
        assertTrue(cusRes is DomainResult.Error)

        // Affiliate denied
        val affRes = service.createPayable(affiliatePrincipal, cmd)
        assertTrue(affRes is DomainResult.Error)
    }

    @Test
    fun testStaffCannotApproveOrVoidPayables() = runBlocking {
        val createRes = service.createPayable(
            staffPrincipal,
            CreateVendorPayableCommand(
                vendorId = vendorId,
                originalAmount = BigDecimal("2500.00"),
                description = "Box Making Board",
                autoSubmit = true
            )
        )
        val payable = (createRes as DomainResult.Success).data

        // Staff tries to approve -> Denied
        val staffApproveRes = service.approvePayable(staffPrincipal, payable.payableId)
        assertTrue(staffApproveRes is DomainResult.Error)

        // Staff tries to void -> Denied
        val staffVoidRes = service.voidPayable(staffPrincipal, payable.payableId, "Void attempt")
        assertTrue(staffVoidRes is DomainResult.Error)
    }

    @Test
    fun testSelfApprovalDeniedForManagers() = runBlocking {
        // Manager creates payable
        val createRes = service.createPayable(
            managerPrincipal,
            CreateVendorPayableCommand(
                vendorId = vendorId,
                originalAmount = BigDecimal("12000.00"),
                description = "Direct Sourced Binding Cloth",
                autoSubmit = true
            )
        )
        val payable = (createRes as DomainResult.Success).data

        // Same manager tries to approve their own created payable -> Denied by SoD
        val selfApproveRes = service.approvePayable(managerPrincipal, payable.payableId)
        assertTrue(selfApproveRes is DomainResult.Error)
        assertTrue((selfApproveRes as DomainResult.Error).message.contains("Separation of duties"))
    }
}
