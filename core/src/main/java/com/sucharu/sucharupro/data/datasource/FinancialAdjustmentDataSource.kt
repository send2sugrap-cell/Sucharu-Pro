package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.CustomerCreditNote
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustment
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentActivityEvent
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.VendorDebitNote
import kotlinx.coroutines.flow.Flow

/**
 * Data Source contract for Financial Adjustments, Credit Notes, and Debit Notes (Module 09 Step 07).
 */
interface FinancialAdjustmentDataSource {

    suspend fun insertAdjustment(adjustment: FinancialAdjustment): Boolean

    suspend fun updateAdjustment(adjustment: FinancialAdjustment): Boolean

    suspend fun getAdjustmentById(adjustmentId: String): FinancialAdjustment?

    suspend fun getAdjustmentByNumber(projectId: String, adjustmentNo: String): FinancialAdjustment?

    suspend fun getAdjustmentByIdempotencyKey(projectId: String, idempotencyKey: String): FinancialAdjustment?

    suspend fun getAdjustmentsByReference(
        projectId: String,
        referenceType: FinancialReferenceType,
        referenceId: String
    ): List<FinancialAdjustment>

    fun observeAdjustments(projectId: String): Flow<List<FinancialAdjustment>>

    fun observeCustomerAdjustments(projectId: String, customerId: String): Flow<List<FinancialAdjustment>>

    fun observeVendorAdjustments(projectId: String, vendorId: String): Flow<List<FinancialAdjustment>>

    suspend fun insertCreditNote(creditNote: CustomerCreditNote): Boolean

    suspend fun getCreditNoteById(creditNoteId: String): CustomerCreditNote?

    suspend fun getCreditNoteByAdjustmentId(adjustmentId: String): CustomerCreditNote?

    fun observeCreditNotes(projectId: String): Flow<List<CustomerCreditNote>>

    suspend fun insertDebitNote(debitNote: VendorDebitNote): Boolean

    suspend fun getDebitNoteById(debitNoteId: String): VendorDebitNote?

    suspend fun getDebitNoteByAdjustmentId(adjustmentId: String): VendorDebitNote?

    fun observeDebitNotes(projectId: String): Flow<List<VendorDebitNote>>

    suspend fun insertActivityEvent(event: FinancialAdjustmentActivityEvent): Boolean

    suspend fun getActivityEvents(entityId: String): List<FinancialAdjustmentActivityEvent>

    suspend fun generateNextAdjustmentNo(projectId: String): String

    suspend fun generateNextCreditNoteNo(projectId: String): String

    suspend fun generateNextDebitNoteNo(projectId: String): String
}
