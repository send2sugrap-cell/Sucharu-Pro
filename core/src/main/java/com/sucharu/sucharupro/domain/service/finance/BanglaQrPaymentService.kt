package com.sucharu.sucharupro.domain.service.finance

import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPayment
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentStatus
import com.sucharu.sucharupro.domain.model.finance.BanglaQrPaymentConfig
import java.math.BigDecimal
import java.util.UUID

/**
 * Service for Static BRAC Bank Bangla QR Payment Integration.
 *
 * Handles merchant QR configuration, transaction reference recording,
 * and canonical CustomerPayment creation without creating duplicate payment ledgers or fake auto-PAID states.
 */
class BanglaQrPaymentService {

    private val staticConfig = BanglaQrPaymentConfig()

    fun getStaticMerchantQrConfig(): BanglaQrPaymentConfig {
        return staticConfig
    }

    /**
     * Records a Customer Payment via Static Bangla QR.
     * Requires explicit transaction reference number (e.g. Bank/MFS TRX ID).
     *
     * (CRITICAL INVARIANT: Does NOT set status to PAID automatically!)
     */
    fun recordStaticBanglaQrPayment(
        customerId: String,
        invoiceId: String,
        amount: BigDecimal,
        transactionReference: String,
        tenantId: String = "TENANT-001",
        projectId: String = "TENANT-001",
        actorId: String = "system"
    ): CustomerPayment {
        require(customerId.isNotBlank()) { "Customer ID cannot be blank." }
        require(invoiceId.isNotBlank()) { "Invoice ID cannot be blank." }
        require(amount > BigDecimal.ZERO) { "Payment amount must be positive." }
        require(transactionReference.isNotBlank()) { "Transaction reference number is required for Bangla QR payments." }

        val paymentId = "PAY-QR-" + UUID.randomUUID().toString().take(8).uppercase()
        val paymentNumber = "PAY-" + UUID.randomUUID().toString().take(6).uppercase()

        return CustomerPayment(
            paymentId = paymentId,
            tenantId = tenantId,
            projectId = projectId,
            paymentNumber = paymentNumber,
            customerId = customerId,
            customerFinancialAccountId = "ACC-$customerId",
            invoiceId = invoiceId,
            amount = amount,
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.BANGLA_QR,
            paymentDate = System.currentTimeMillis(),
            referenceNumber = transactionReference,
            externalReference = "BRAC_BANK_STATIC_QR:$transactionReference",
            notes = "Recorded via Static BRAC Bank Bangla QR",
            status = CustomerPaymentStatus.RECORDED, // Recorded status, requires finance confirmation!
            createdBy = actorId,
            updatedBy = actorId
        )
    }
}
