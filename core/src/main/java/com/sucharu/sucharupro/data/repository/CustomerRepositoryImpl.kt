package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.CustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerActivity
import com.sucharu.sucharupro.domain.model.customer.CustomerNote
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Production-ready implementation of [CustomerRepository].
 *
 * Delegates low-level storage operations to [CustomerDataSource], providing reactive
 * streams, search, and filtering capabilities.
 */
class CustomerRepositoryImpl(
    private val dataSource: CustomerDataSource = FakeCustomerDataSource()
) : CustomerRepository {

    override fun getCustomers(): Flow<List<Customer>> =
        dataSource.observeCustomers()

    override fun getCustomerById(customerId: String): Flow<Customer?> =
        dataSource.observeCustomers().map { list ->
            list.find { it.customerId == customerId }
        }

    override suspend fun findCustomerById(customerId: String): DomainResult<Customer> =
        dataSource.fetchCustomerById(customerId)

    override suspend fun addCustomer(customer: Customer): DomainResult<Customer> =
        dataSource.insertCustomer(customer)

    override suspend fun updateCustomer(customer: Customer): DomainResult<Customer> =
        dataSource.updateCustomer(customer)

    override suspend fun setCustomerStatus(customerId: String, status: CustomerStatusType): DomainResult<Customer> =
        dataSource.setCustomerStatus(customerId, status)

    override suspend fun deactivateCustomer(customerId: String): DomainResult<Customer> =
        dataSource.setCustomerStatus(customerId, CustomerStatusType.INACTIVE)

    override suspend fun reactivateCustomer(customerId: String): DomainResult<Customer> =
        dataSource.setCustomerStatus(customerId, CustomerStatusType.ACTIVE)

    override suspend fun archiveCustomer(customerId: String): DomainResult<Customer> =
        dataSource.setCustomerStatus(customerId, CustomerStatusType.ARCHIVED)

    override suspend fun restoreCustomer(customerId: String): DomainResult<Customer> =
        dataSource.setCustomerStatus(customerId, CustomerStatusType.ACTIVE)

    override suspend fun findDuplicateCustomer(
        phone: String,
        email: String?,
        excludeCustomerId: String?
    ): Customer? {
        val normPhone = com.sucharu.sucharupro.core.validation.CustomerValidation.normalizePhoneNumber(phone)
        val normEmail = email?.trim()?.takeIf { it.isNotBlank() }?.let {
            com.sucharu.sucharupro.core.validation.CustomerValidation.normalizeEmail(it)
        }

        val allCustomers = when (val result = dataSource.fetchCustomers()) {
            is DomainResult.Success -> result.data
            else -> emptyList()
        }

        return allCustomers.firstOrNull { customer ->
            if (excludeCustomerId != null && customer.customerId == excludeCustomerId) {
                false
            } else {
                val cPhone = com.sucharu.sucharupro.core.validation.CustomerValidation.normalizePhoneNumber(customer.primaryPhone)
                val cAltPhone = customer.alternatePhone?.let {
                    com.sucharu.sucharupro.core.validation.CustomerValidation.normalizePhoneNumber(it)
                }
                val cEmail = customer.email?.let {
                    com.sucharu.sucharupro.core.validation.CustomerValidation.normalizeEmail(it)
                }

                (normPhone.isNotBlank() && (normPhone == cPhone || normPhone == cAltPhone)) ||
                    (normEmail != null && normEmail.isNotBlank() && normEmail == cEmail)
            }
        }
    }

    override fun searchCustomers(query: String): Flow<List<Customer>> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return getCustomers()
        }

        return dataSource.observeCustomers().map { list ->
            list.filter { customer ->
                customer.displayName.contains(trimmed, ignoreCase = true) ||
                    customer.customerCode.contains(trimmed, ignoreCase = true) ||
                    customer.customerId.contains(trimmed, ignoreCase = true) ||
                    customer.primaryPhone.contains(trimmed, ignoreCase = true) ||
                    (customer.alternatePhone != null && customer.alternatePhone.contains(trimmed, ignoreCase = true)) ||
                    (customer.email != null && customer.email.contains(trimmed, ignoreCase = true)) ||
                    (customer.contactPersonName != null && customer.contactPersonName.contains(trimmed, ignoreCase = true))
            }
        }
    }

    override fun filterCustomers(
        type: CustomerType?,
        status: CustomerStatusType?
    ): Flow<List<Customer>> =
        dataSource.observeCustomers().map { list ->
            list.filter { customer ->
                (type == null || customer.customerType == type) &&
                    (status == null || customer.status == status)
            }
        }

    override suspend fun refreshCustomers(): Result<Unit> {
        return when (val result = dataSource.fetchCustomers()) {
            is DomainResult.Success -> Result.success(Unit)
            is DomainResult.Error -> Result.failure(result.exception ?: Exception(result.message))
            is DomainResult.Loading -> Result.success(Unit)
        }
    }

    override fun observeCustomerNotes(customerId: String): Flow<List<CustomerNote>> =
        dataSource.observeCustomerNotes(customerId)

    override suspend fun addCustomerNote(note: CustomerNote): DomainResult<CustomerNote> =
        dataSource.insertCustomerNote(note)

    override suspend fun updateCustomerNote(note: CustomerNote): DomainResult<CustomerNote> =
        dataSource.updateCustomerNote(note)

    override suspend fun deleteCustomerNote(noteId: String, customerId: String): DomainResult<Unit> =
        dataSource.deleteCustomerNote(noteId, customerId)

    override suspend fun toggleImportantNote(noteId: String, customerId: String): DomainResult<CustomerNote> =
        dataSource.toggleImportantNote(noteId, customerId)

    override fun observeCustomerActivities(customerId: String): Flow<List<CustomerActivity>> =
        dataSource.observeCustomerActivities(customerId)

    override suspend fun setFollowUpDate(customerId: String, followUpAt: String?): DomainResult<Customer> =
        dataSource.setCustomerFollowUp(customerId, followUpAt)
}
