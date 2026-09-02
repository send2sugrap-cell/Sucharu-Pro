package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorCapabilityDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPurchaseOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorServiceRateDataSource
import com.sucharu.sucharupro.data.repository.VendorCapabilityRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorPurchaseOrderRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorServiceRateRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.service.vendor.VendorPurchaseOrderServiceImpl
import com.sucharu.sucharupro.domain.service.vendor.VendorServiceRateServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPurchaseOrderTenantIsolationTest {

    private lateinit var poService: VendorPurchaseOrderServiceImpl
    private lateinit var vendorRepo: VendorRepositoryImpl

    @Before
    fun setUp() {
        runBlocking {
            vendorRepo = VendorRepositoryImpl(FakeVendorDataSource())
            val capRepo = VendorCapabilityRepositoryImpl(FakeVendorCapabilityDataSource())
            val rateRepo = VendorServiceRateRepositoryImpl(FakeVendorServiceRateDataSource())
            val poRepo = VendorPurchaseOrderRepositoryImpl(FakeVendorPurchaseOrderDataSource())

            val rateService = VendorServiceRateServiceImpl(vendorRepo, capRepo, rateRepo)
            poService = VendorPurchaseOrderServiceImpl(vendorRepo, capRepo, rateService, poRepo)

            // Seed vendor in Tenant Alpha
            vendorRepo.createVendor(
                Vendor(
                    vendorId = "v_alpha",
                    projectId = "tenant_alpha",
                    vendorCode = "VND-ALPHA",
                    vendorName = "Alpha Print",
                    vendorType = VendorType.SERVICE_PROVIDER,
                    vendorCategory = VendorCategory.PRINTING,
                    status = VendorStatus.ACTIVE
                )
            )

            // Seed vendor in Tenant Beta
            vendorRepo.createVendor(
                Vendor(
                    vendorId = "v_beta",
                    projectId = "tenant_beta",
                    vendorCode = "VND-BETA",
                    vendorName = "Beta Print",
                    vendorType = VendorType.SERVICE_PROVIDER,
                    vendorCategory = VendorCategory.PRINTING,
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun `test tenant alpha cannot access tenant beta purchase order`() = runBlocking {
        val item = VendorPurchaseOrderItem(
            itemId = "poi_1",
            purchaseOrderId = "",
            itemDescription = "Lamination Rolls",
            quantity = BigDecimal("10.00"),
            unitRate = Money(150.0),
            lineTotal = Money(1500.0)
        )

        val createBeta = poService.createOrder(
            projectId = "tenant_beta",
            vendorId = "v_beta",
            requestedBy = "usr_beta",
            items = listOf(item)
        )
        assertTrue(createBeta is DomainResult.Success)
        val betaPoId = (createBeta as DomainResult.Success).data.purchaseOrderId

        // Tenant Alpha attempting to read Tenant Beta's PO
        val readFromAlpha = poService.getOrderById("tenant_alpha", betaPoId)
        assertTrue(readFromAlpha is DomainResult.Error)

        // Tenant Alpha listing POs sees 0
        val listAlpha = poService.listOrders("tenant_alpha")
        assertTrue(listAlpha is DomainResult.Success)
        assertEquals(0, (listAlpha as DomainResult.Success).data.size)

        // Tenant Beta listing POs sees 1
        val listBeta = poService.listOrders("tenant_beta")
        assertTrue(listBeta is DomainResult.Success)
        assertEquals(1, (listBeta as DomainResult.Success).data.size)
    }
}
