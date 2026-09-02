package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerActivity
import com.sucharu.sucharupro.domain.model.customer.CustomerNote
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import kotlinx.coroutines.flow.Flow

/**
 * Data source abstraction for Customer Management.
 *
 * Implemented by [FakeCustomerDataSource] today, and easily swappable with
 * Room or REST API implementations tomorrow without changing domain or UI code.
 */
interface CustomerDataSource {

    /**
     * Continuous reactive stream of all customers.
     */
    fun observeCustomers(): Flow<List<Customer>>

    /**
     * One-shot fetch of the customer collection.
     */
    suspend fun fetchCustomers(): DomainResult<List<Customer>>

    /**
     * One-shot fetch of a single customer by [customerId].
     */
    suspend fun fetchCustomerById(customerId: String): DomainResult<Customer>

    /**
     * Inserts a new customer record.
     */
    suspend fun insertCustomer(customer: Customer): DomainResult<Customer>

    /**
     * Updates an existing customer record.
     */
    suspend fun updateCustomer(customer: Customer): DomainResult<Customer>

    /**
     * Modifies the operational status of a customer record.
     */
    suspend fun setCustomerStatus(customerId: String, status: CustomerStatusType): DomainResult<Customer>

    /**
     * Continuous reactive stream of notes for a given [customerId].
     */
    fun observeCustomerNotes(customerId: String): Flow<List<CustomerNote>>

    /**
     * Inserts a new internal note for a customer.
     */
    suspend fun insertCustomerNote(note: CustomerNote): DomainResult<CustomerNote>

    /**
     * Updates an existing customer note.
     */
    suspend fun updateCustomerNote(note: CustomerNote): DomainResult<CustomerNote>

    /**
     * Deletes a customer note by ID.
     */
    suspend fun deleteCustomerNote(noteId: String, customerId: String): DomainResult<Unit>

    /**
     * Toggles importance flag for a customer note.
     */
    suspend fun toggleImportantNote(noteId: String, customerId: String): DomainResult<CustomerNote>

    /**
     * Continuous reactive stream of operational activities for a given [customerId].
     */
    fun observeCustomerActivities(customerId: String): Flow<List<CustomerActivity>>

    /**
     * Records a new customer operational activity event.
     */
    suspend fun insertCustomerActivity(activity: CustomerActivity): DomainResult<CustomerActivity>

    /**
     * Sets or clears the customer operational follow-up target date.
     */
    suspend fun setCustomerFollowUp(customerId: String, followUpAt: String?): DomainResult<Customer>
}
