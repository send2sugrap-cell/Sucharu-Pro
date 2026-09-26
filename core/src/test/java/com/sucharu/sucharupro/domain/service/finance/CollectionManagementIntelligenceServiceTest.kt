package com.sucharu.sucharupro.domain.service.finance

import com.sucharu.sucharupro.domain.model.finance.ReceivableAgingCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CollectionManagementIntelligenceServiceTest {

    private lateinit var service: CollectionManagementIntelligenceService

    @Before
    fun setUp() {
        service = CollectionManagementIntelligenceService()
    }

    @Test
    fun `buildCollectionManagementSummary_computesAgingAndHighExposureItems`() {
        val summary = service.buildCollectionManagementSummary()

        assertNotNull(summary)
        assertEquals(3, summary.intelligenceItems.size)
        assertEquals(BigDecimal("555000.00"), summary.totalOutstandingReceivables)
        assertEquals(BigDecimal("470000.00"), summary.totalOverdueReceivables)
        assertEquals(1, summary.highExposureCustomerCount)

        val overdue90Items = service.filterItemsByAgingCategory(summary, ReceivableAgingCategory.OVERDUE_90_PLUS)
        assertEquals(1, overdue90Items.size)
        assertEquals("CUST-1002", overdue90Items.first().customerId)
    }
}
