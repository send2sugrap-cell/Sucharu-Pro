package com.sucharu.sucharupro.vendorpayable

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.vendorpayable.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.vendorpayable.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorpayable.*
import com.sucharu.sucharupro.domain.service.vendorpayable.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicInteger

class VendorPayableConcurrencyTest {

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

    private val managerPrincipal1 = AuthenticatedPrincipal(
        userId = "USER-MGR-1",
        projectId = projectId,
        username = "manager1",
        role = UserRole.MANAGER
    )

    private val managerPrincipal2 = AuthenticatedPrincipal(
        userId = "USER-MGR-2",
        projectId = projectId,
        username = "manager2",
        role = UserRole.MANAGER
    )

    @Before
    fun setup() {
        dataSource = FakeVendorPayableDataSource()
        repository = VendorPayableRepositoryImpl(dataSource)
        service = VendorPayableServiceImpl(repository, tenantId)
    }

    @Test
    fun testConcurrentPaymentAllocationsPreventOverAllocation() = runBlocking {
        // Payable of 10,000.00
        val createRes = service.createPayable(
            staffPrincipal,
            CreateVendorPayableCommand(
                vendorId = vendorId,
                originalAmount = BigDecimal("10000.00"),
                description = "Offset Printing Inks",
                autoSubmit = true
            )
        )
        val payable = (createRes as DomainResult.Success).data
        service.approvePayable(managerPrincipal1, payable.payableId, "Approved")

        // Concurrent allocations: Req 1 = 7,000.00, Req 2 = 5,000.00
        // Total requested = 12,000.00 > 10,000.00
        val alloc1 = async(Dispatchers.IO) {
            service.allocatePayment(
                managerPrincipal1,
                payable.payableId,
                AllocateVendorPayablePaymentCommand(
                    amount = BigDecimal("7000.00"),
                    paymentMethod = VendorPayablePaymentMethod.BANK,
                    paymentReference = "TRX-CONC-1"
                )
            )
        }
        val alloc2 = async(Dispatchers.IO) {
            service.allocatePayment(
                managerPrincipal2,
                payable.payableId,
                AllocateVendorPayablePaymentCommand(
                    amount = BigDecimal("5000.00"),
                    paymentMethod = VendorPayablePaymentMethod.BANK,
                    paymentReference = "TRX-CONC-2"
                )
            )
        }

        val results = awaitAll(alloc1, alloc2)

        // Exactly one must succeed and one must fail (or both sum <= 10,000)
        val successCount = results.count { it is DomainResult.Success }
        val errorCount = results.count { it is DomainResult.Error }
        assertEquals(1, successCount)
        assertEquals(1, errorCount)

        val finalPayableRes = service.getPayableById(managerPrincipal1, payable.payableId)
        assertTrue(finalPayableRes is DomainResult.Success)
        val finalPayable = (finalPayableRes as DomainResult.Success).data

        assertTrue(finalPayable.outstandingAmount >= BigDecimal.ZERO)
        assertTrue(finalPayable.paidAmount <= finalPayable.originalAmount)
    }

    @Test
    fun testConcurrentPayableCreation() = runBlocking {
        val count = 20
        val successCount = AtomicInteger(0)

        val deferreds = (1..count).map { idx ->
            async(Dispatchers.IO) {
                val res = service.createPayable(
                    staffPrincipal,
                    CreateVendorPayableCommand(
                        vendorId = vendorId,
                        originalAmount = BigDecimal("${idx * 100}.00"),
                        description = "Concurrent Payable #$idx"
                    )
                )
                if (res is DomainResult.Success) {
                    successCount.incrementAndGet()
                }
            }
        }

        deferreds.awaitAll()
        assertEquals(count, successCount.get())

        val listRes = service.listPayables(staffPrincipal, limit = 50)
        assertTrue(listRes is DomainResult.Success)
        assertEquals(count, (listRes as DomainResult.Success).data.size)
    }
}
