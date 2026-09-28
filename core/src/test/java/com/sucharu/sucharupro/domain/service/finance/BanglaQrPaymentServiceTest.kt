package com.sucharu.sucharupro.domain.service.finance

import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentStatus
import com.sucharu.sucharupro.domain.model.finance.BanglaQrBankProvider
import com.sucharu.sucharupro.domain.model.finance.BanglaQrMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BanglaQrPaymentServiceTest {

    private lateinit var service: BanglaQrPaymentService

    @Before
    fun setUp() {
        service = BanglaQrPaymentService()
    }

    @Test
    fun `getStaticMerchantQrConfig_returnsBracBankMerchantConfiguration`() {
        val config = service.getStaticMerchantQrConfig()

        assertNotNull(config)
        assertEquals(BanglaQrMode.STATIC, config.mode)
        assertEquals(BanglaQrBankProvider.BRAC_BANK, config.bankProvider)
        assertEquals("Sucharu Graphics & Printing", config.merchantName)
        assertTrue(config.isMerchantQrConfigured)
    }

    @Test
    fun `recordStaticBanglaQrPayment_createsCanonicalCustomerPaymentWithoutAutoPaidState`() {
        val payment = service.recordStaticBanglaQrPayment(
            customerId = "CUST-1001",
            invoiceId = "INV-1001",
            amount = BigDecimal("410.00"),
            transactionReference = "TRX-BRAC-998877",
            tenantId = "TENANT-001",
            actorId = "CUST-1001"
        )

        assertNotNull(payment)
        assertEquals("CUST-1001", payment.customerId)
        assertEquals("INV-1001", payment.invoiceId)
        assertEquals(BigDecimal("410.00"), payment.amount)
        assertEquals(CustomerPaymentMethod.BANGLA_QR, payment.paymentMethod)
        assertEquals("TRX-BRAC-998877", payment.referenceNumber)

        // CRITICAL INVARIANT PROOF:
        // Status MUST BE RECORDED, NOT CONFIRMED/PAID automatically!
        assertEquals("Payment status MUST be RECORDED, not auto-PAID!", CustomerPaymentStatus.RECORDED, payment.status)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `recordStaticBanglaQrPayment_rejectsBlankTransactionReference`() {
        service.recordStaticBanglaQrPayment(
            customerId = "CUST-1001",
            invoiceId = "INV-1001",
            amount = BigDecimal("410.00"),
            transactionReference = "   " // Blank reference MUST be rejected!
        )
    }
}
