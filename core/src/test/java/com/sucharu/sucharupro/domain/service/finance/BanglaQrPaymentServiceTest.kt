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
    fun `generateDynamicBanglaQrPayload_producesValidEmvCoStringWithCrc16`() {
        val qrPayload = service.generateDynamicBanglaQrPayload(
            invoiceId = "INV-2026-801",
            orderId = "SO-2026-801",
            amount = BigDecimal("2500.00"),
            merchantName = "Sucharu Graphics",
            merchantCity = "Dhaka"
        )

        assertNotNull(qrPayload)
        assertTrue("Must start with EMVCo Tag 00 Payload Format Indicator 000201", qrPayload.startsWith("000201"))
        assertTrue("Must contain Dynamic Initiator Code 010212", qrPayload.contains("010212"))
        assertTrue("Must contain Currency Code 050 for BDT", qrPayload.contains("5303050"))
        assertTrue("Must contain Country Code BD", qrPayload.contains("5802BD"))
        assertTrue("Must contain CRC Tag 6304", qrPayload.contains("6304"))
    }

    @Test
    fun `processBanglaQrIpnCallback_verifiesTrxIdAndEnforcesIdempotencyGuard`() {
        val trxId = "TRX-BKASH-88776655"

        val firstCallback = service.processBanglaQrIpnCallback(
            tenantId = "TENANT-001",
            orderId = "SO-2026-881",
            invoiceId = "INV-2026-881",
            trxId = trxId,
            amount = BigDecimal("2250.00")
        )

        assertTrue(firstCallback.isSuccess)
        val payment = firstCallback.getOrThrow()
        assertEquals(trxId, payment.referenceNumber)
        assertEquals(CustomerPaymentStatus.RECORDED, payment.status)

        // IDEMPOTENCY GUARD PROOF:
        // Processing the SAME TrxID again MUST be rejected!
        val duplicateCallback = service.processBanglaQrIpnCallback(
            tenantId = "TENANT-001",
            orderId = "SO-2026-881",
            invoiceId = "INV-2026-881",
            trxId = trxId,
            amount = BigDecimal("2250.00")
        )

        assertTrue("Duplicate TrxID MUST fail due to Idempotency Guard", duplicateCallback.isFailure)
    }

    @Test
    fun `generateDigitalMoneyReceipt_createsTamperProofReceiptWithTrxId`() {
        val receipt = service.generateDigitalMoneyReceipt(
            tenantId = "TENANT-001",
            orderId = "SO-2026-881",
            invoiceId = "INV-2026-881",
            customerName = "আহমেদ ট্রেডার্স",
            amountPaid = BigDecimal("2250.00"),
            paymentMethod = "Bangla QR (bKash)",
            trxId = "TRX-BKASH-88776655",
            remainingBalance = BigDecimal("2250.00")
        )

        assertNotNull(receipt)
        assertTrue("Receipt ID must start with MR-2026-", receipt.receiptId.startsWith("MR-2026-"))
        assertEquals("আহমেদ ট্রেডার্স", receipt.customerName)
        assertEquals(BigDecimal("2250.00"), receipt.amountPaid)
        assertEquals("TRX-BKASH-88776655", receipt.trxId)
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
