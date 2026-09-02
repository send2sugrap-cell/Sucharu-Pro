package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.VendorPortalAnalyticsNotificationSearchRepository
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalAnalyticsNotificationSearchService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalAnalyticsNotificationSearchServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalAnalyticsNotificationSearchApiTest {

    private lateinit var useCases: BackendUseCases

    private val tenantId = "PRJ-001"
    private val projectId = "PRJ-001"
    private val vendorId = "VND-API-01"

    private val vendorPrincipal = AuthenticatedPrincipal(
        userId = "vendor_rep_01",
        projectId = projectId,
        username = "vendor_rep",
        role = UserRole.VENDOR,
        vendorId = vendorId
    )

    @Before
    fun setup() {
        val portalDataSource = FakeVendorPortalAnalyticsNotificationSearchDataSource()
        val portalRepository = VendorPortalAnalyticsNotificationSearchRepositoryImpl(portalDataSource)

        val vendorDs = FakeVendorDataSource()
        val vendorRepo = VendorRepositoryImpl(vendorDs)

        val poDs = FakeVendorPurchaseOrderDataSource()
        val poRepo = VendorPurchaseOrderRepositoryImpl(poDs)

        val fakeTxManager = object : TransactionManager {
            override suspend fun <T> inTransaction(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required for mock tests")
            }
            override suspend fun <T> inReadOnly(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required for mock tests")
            }
        }

        val step10Service = VendorPortalAnalyticsNotificationSearchServiceImpl(
            repository = portalRepository,
            vendorRepository = vendorRepo,
            purchaseOrderRepository = poRepo
        )

        val customFactory = object : PostgresRepositoryFactory(fakeTxManager) {
            override fun createVendorRepository(tenantId: String) = vendorRepo
            override fun createVendorPortalAnalyticsNotificationSearchRepository(tenantId: String): VendorPortalAnalyticsNotificationSearchRepository = portalRepository
            override fun createVendorPortalAnalyticsNotificationSearchService(tenantId: String): VendorPortalAnalyticsNotificationSearchService = step10Service
        }

        useCases = BackendUseCases(fakeTxManager, customFactory)

        runBlocking {
            vendorRepo.createVendor(
                Vendor(
                    vendorId = vendorId,
                    projectId = projectId,
                    vendorCode = "VN-API-01",
                    vendorName = "API Test Partner Ltd",
                    vendorCategory = VendorCategory.RAW_MATERIALS,
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testUnifiedAnalyticsHubEndpoint() = runBlocking {
        val hub = useCases.getVendorPortalUnifiedAnalyticsHub(vendorPrincipal, "LAST_30_DAYS")
        assertEquals(vendorId, hub.vendorId)
        assertEquals("LAST_30_DAYS", hub.period)
        assertNotNull(hub.operational)
        assertNotNull(hub.financial)
        assertNotNull(hub.quality)
        assertNotNull(hub.performance)
        assertNotNull(hub.compliance)
        assertNotNull(hub.collaboration)
    }

    @Test
    fun testNotificationsApiEndpoints() = runBlocking {
        val emitReq = VendorPortalEmitNotificationRequest(
            category = "PURCHASE_ORDER",
            severity = "HIGH",
            title = "New Order Assigned",
            message = "Order PO-999 requires confirmation",
            deepLinkTarget = "/vendor-portal/purchase-orders/PO-999"
        )
        val notif = useCases.emitVendorPortalNotification(vendorPrincipal, emitReq)
        assertEquals("PURCHASE_ORDER", notif.category)
        assertEquals("HIGH", notif.severity)

        val list = useCases.listVendorPortalNotifications(vendorPrincipal)
        assertEquals(1, list.size)

        val unread = useCases.getVendorPortalNotificationUnreadCount(vendorPrincipal)
        assertEquals(1, unread.totalUnread)

        val marked = useCases.markVendorPortalNotificationAsRead(vendorPrincipal, notif.notificationId)
        assertTrue(marked)

        val unreadAfter = useCases.getVendorPortalNotificationUnreadCount(vendorPrincipal)
        assertEquals(0, unreadAfter.totalUnread)
    }

    @Test
    fun testWorkspaceSummaryEndpoint() = runBlocking {
        val summary = useCases.getVendorPortalUnifiedWorkspaceSummary(vendorPrincipal)
        assertEquals(vendorId, summary.vendorId)
        assertEquals("API Test Partner Ltd", summary.vendorName)
        assertTrue(summary.navigationSections.isNotEmpty())
    }
}
