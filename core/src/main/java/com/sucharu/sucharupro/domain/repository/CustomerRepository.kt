package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerActivity
import com.sucharu.sucharupro.domain.model.customer.CustomerNote
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface contract for Customer Management in Sucharu Pro.
 *
 * Provides reactive streams and CRUD operations for managing customer records.
 *
 * Architecture boundary:
 *  - Lives in the DOMAIN layer (independent of UI, Room, Retrofit, Firebase).
 *  - All collection streams emit reactive [Flow<List<Customer>>].
 *  - Mutative operations return [DomainResult].
 */
interface CustomerRepository {

    /**
     * Reactive stream of all active and managed customers.
     */
    fun getCustomers(): Flow<List<Customer>>

    /**
     * Reactive stream observing a single customer by [customerId].
     * Emits null if the customer does not exist.
     */
    fun getCustomerById(customerId: String): Flow<Customer?>

    /**
     * Direct suspend lookup of a customer by [customerId].
     */
    suspend fun findCustomerById(customerId: String): DomainResult<Customer>

    /**
     * Adds a new customer to the repository.
     */
    suspend fun addCustomer(customer: Customer): DomainResult<Customer>

    /**
     * Updates an existing customer record.
     */
    suspend fun updateCustomer(customer: Customer): DomainResult<Customer>

    /**
     * Updates customer operational status and logs a status change activity event.
     */
    suspend fun setCustomerStatus(customerId: String, status: CustomerStatusType): DomainResult<Customer>

    /**
     * Safely deactivates a customer by transitioning status to [CustomerStatusType.INACTIVE].
     * Historical profile, notes, activities, and metadata remain intact.
     */
    suspend fun deactivateCustomer(customerId: String): DomainResult<Customer>

    /**
     * Reactivates an inactive, blocked, or archived customer back to [CustomerStatusType.ACTIVE].
     */
    suspend fun reactivateCustomer(customerId: String): DomainResult<Customer>

    /**
     * Soft-deletes a customer by updating their status to [CustomerStatusType.ARCHIVED].
     * Preserves historical ledger and order association integrity.
     */
    suspend fun archiveCustomer(customerId: String): DomainResult<Customer>

    /**
     * Restores an archived customer back to [CustomerStatusType.ACTIVE].
     */
    suspend fun restoreCustomer(customerId: String): DomainResult<Customer>

    /**
     * Finds potential duplicate customer by normalized primary phone or email.
     * Optionally excludes [excludeCustomerId] (e.g. when editing the current customer).
     */
    suspend fun findDuplicateCustomer(phone: String, email: String?, excludeCustomerId: String? = null): Customer?

    /**
     * Searches customers matching [query] across name, code, phone, email, and contact person.
     * If [query] is blank, emits all customers.
     */
    fun searchCustomers(query: String): Flow<List<Customer>>

    /**
     * Filters customers by optional [type] and [status].
     */
    fun filterCustomers(
        type: CustomerType? = null,
        status: CustomerStatusType? = null
    ): Flow<List<Customer>>

    /**
     * Refreshes customer data from the underlying data source.
     */
    suspend fun refreshCustomers(): Result<Unit>

    /**
     * Observes internal customer notes strictly isolated for [customerId].
     */
    fun observeCustomerNotes(customerId: String): Flow<List<CustomerNote>>

    /**
     * Adds a new internal note for the specified customer.
     */
    suspend fun addCustomerNote(note: CustomerNote): DomainResult<CustomerNote>

    /**
     * Updates an existing internal customer note.
     */
    suspend fun updateCustomerNote(note: CustomerNote): DomainResult<CustomerNote>

    /**
     * Deletes a customer note by ID with customer isolation verification.
     */
    suspend fun deleteCustomerNote(noteId: String, customerId: String): DomainResult<Unit>

    /**
     * Toggles importance flag for a customer note.
     */
    suspend fun toggleImportantNote(noteId: String, customerId: String): DomainResult<CustomerNote>

    /**
     * Observes customer operational lifecycle activities strictly isolated for [customerId].
     */
    fun observeCustomerActivities(customerId: String): Flow<List<CustomerActivity>>

    /**
     * Sets or clears the customer operational follow-up timestamp.
     */
    suspend fun setFollowUpDate(customerId: String, followUpAt: String?): DomainResult<Customer>
}
