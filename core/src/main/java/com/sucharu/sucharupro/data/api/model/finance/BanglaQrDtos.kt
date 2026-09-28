package com.sucharu.sucharupro.data.api.model.finance

import kotlinx.serialization.Serializable

@Serializable
data class BanglaQrConfigDto(
    val configId: String = "CFG-BRAC-QR-01",
    val merchantName: String = "Sucharu Graphics & Printing",
    val merchantAccountNo: String = "1501200000001",
    val bracBankQrAssetPath: String = "drawable/brac_bank_bangla_qr_merchant",
    val mode: String = "STATIC",
    val bankProvider: String = "BRAC_BANK",
    val isMerchantQrConfigured: Boolean = true,
    val paymentInstructionBn: String
)

@Serializable
data class RecordBanglaQrPaymentRequestDto(
    val customerId: String,
    val invoiceId: String,
    val amount: String,
    val transactionReference: String
)
