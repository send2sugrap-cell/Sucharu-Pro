package com.sucharu.sucharupro.customercreditcontrol

import com.sucharu.sucharupro.data.datasource.customercreditcontrol.FakeCustomerCreditControlDataSource
import com.sucharu.sucharupro.data.repository.customercreditcontrol.CustomerCreditControlRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerCreditControlAuditEvent
import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerCreditProfileEntity
import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerPaymentTermsType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerCreditControlRepositoryTest {

    private lateinit var repository: CustomerCreditControlRepositoryImpl
    private val tenantId = "TENANT-REP-01"
    private val projectId = "PRJ-REP-01"
    private val customerId = "CUS-REP-01"

    @Before
    fun setup() {
        val ds = FakeCustomerCreditControlDataSource()
        repository = CustomerCreditControlRepositoryImpl(ds)
    }

    @Test
    fun testSaveAndRetrieveCreditProfile() = runBlocking {
        val profile = CustomerCreditProfileEntity(
            profileId = "PROF-001",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            creditLimit = BigDecimal("100000.0000"),
            paymentTermsType = CustomerPaymentTermsType.NET_30,
            creditDays = 30,
            requiresAdvance = false,
            financialHold = false
        )

        val saveRes = repository.saveProfile(profile)
        assertTrue(saveRes is DomainResult.Success)

        val getRes = repository.getProfileByCustomerId(tenantId, projectId, customerId)
        assertTrue(getRes is DomainResult.Success)
        val fetched = (getRes as DomainResult.Success).data
        assertNotNull(fetched)
        assertEquals(BigDecimal("100000.0000"), fetched?.creditLimit)
        assertEquals(CustomerPaymentTermsType.NET_30, fetched?.paymentTermsType)
    }

    @Test
    fun testRecordAndRetrieveAuditEvents() = runBlocking {
        val event = CustomerCreditControlAuditEvent(
            auditId = "AUD-001",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            actorId = "admin_user",
            actorRole = "ADMIN",
            action = "PLACE_FINANCIAL_HOLD",
            reason = "Payment default"
        )

        val recRes = repository.recordAuditEvent(event)
        assertTrue(recRes is DomainResult.Success)

        val auditRes = repository.getAuditEvents(tenantId, projectId, customerId)
        assertTrue(auditRes is DomainResult.Success)
        val audits = (auditRes as DomainResult.Success).data
        assertEquals(1, audits.size)
        assertEquals("PLACE_FINANCIAL_HOLD", audits[0].action)
        assertEquals("Payment default", audits[0].reason)
    }
}
