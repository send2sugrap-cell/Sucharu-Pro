package com.sucharu.sucharupro.domain.service.finance

import com.sucharu.sucharupro.domain.model.finance.CollectionAttentionCategory
import com.sucharu.sucharupro.domain.model.finance.CollectionAttentionItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class FinancialControlCenterServiceTest {

    private lateinit var service: FinancialControlCenterService

    @Before
    fun setUp() {
        service = FinancialControlCenterService()
    }

    @Test
    fun `calculateFinancialControlCenterSummary_computesTotalsAndAgingDistribution`() {
        val summary = service.calculateFinancialControlCenterSummary()

        assertNotNull(summary)
        assertEquals(BigDecimal("555000.00"), summary.totalOutstanding)
        assertEquals(BigDecimal("470000.00"), summary.overdueAmount)
        assertEquals(BigDecimal("85000.00"), summary.currentDue)
        assertEquals(3, summary.attentionItems.size)
    }

    @Test
    fun `filterAttentionItemsByCategory_filtersOverdueItemsCorrectly`() {
        val summary = service.calculateFinancialControlCenterSummary()
        val overdueItems = service.filterAttentionItemsByCategory(summary, CollectionAttentionCategory.OVERDUE)

        assertEquals(1, overdueItems.size)
        assertEquals("CUST-1001", overdueItems.first().customerId)
    }
}
