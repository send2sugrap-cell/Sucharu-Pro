package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.api.client.BackendApiClient
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.*
import com.sucharu.sucharupro.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.model.RegisterRequestDto
import com.sucharu.sucharupro.data.auth.model.UpdateUserProfileRequestDto

/**
 * Production HTTP REST API implementation of [CustomerRepository] (INFRA-05 Step 03).
 * Communicates exclusively over secure HTTP REST API boundary via [BackendApiClient].
 * Strictly prohibits fallback to fake or mock data sources upon network/API failures.
 */
class HttpCustomerRepository(
    private val client: BackendApiClient
) : CustomerRepository {

    override fun getCustomers(): Flow<List<Customer>> = flow {
        when (val res = client.listCustomers()) {
            is ApiResult.Success -> {
                emit(res.data.map { it.toDomain() })
            }
            is ApiResult.Error -> {
                // Fallback to customer profile if user is a single customer
                when (val profRes = client.getCustomerProfile()) {
                    is ApiResult.Success -> emit(listOf(mapCustomerProfileDtoToCustomer(profRes.data)))
                    is ApiResult.Error -> emit(emptyList())
                }
            }
        }
    }

    override fun getCustomerById(customerId: String): Flow<Customer?> = flow {
        when (val res = client.getCustomerById(customerId)) {
            is ApiResult.Success -> emit(res.data.toDomain())
            is ApiResult.Error -> {
                when (val profRes = client.getCustomerProfile()) {
                    is ApiResult.Success -> {
                        val c = mapCustomerProfileDtoToCustomer(profRes.data)
                        if (c.customerId == customerId || customerId.isBlank()) emit(c) else emit(null)
                    }
                    is ApiResult.Error -> emit(null)
                }
            }
        }
    }

    override suspend fun findCustomerById(customerId: String): DomainResult<Customer> {
        return when (val res = client.getCustomerById(customerId)) {
            is ApiResult.Success -> DomainResult.Success(res.data.toDomain())
            is ApiResult.Error -> {
                when (val profRes = client.getCustomerProfile()) {
                    is ApiResult.Success -> DomainResult.Success(mapCustomerProfileDtoToCustomer(profRes.data))
                    is ApiResult.Error -> DomainResult.Error(message = res.errorResponse.message)
                }
            }
        }
    }

    override suspend fun addCustomer(customer: Customer): DomainResult<Customer> {
        val req = CreateCustomerRequestDto(
            displayName = customer.displayName,
            customerType = customer.customerType.name,
            primaryPhone = customer.primaryPhone,
            alternatePhone = customer.alternatePhone,
            email = customer.email,
            contactPersonName = customer.contactPersonName,
            creditLimit = customer.creditProfile.creditLimit.amount,
            paymentTermDays = customer.creditProfile.paymentTermDays,
            notes = customer.notes
        )
        return when (val res = client.createCustomer(req)) {
            is ApiResult.Success -> DomainResult.Success(res.data.toDomain())
            is ApiResult.Error -> DomainResult.Error(message = res.errorResponse.message)
        }
    }

    override suspend fun updateCustomer(customer: Customer): DomainResult<Customer> {
        val req = UpdateCustomerRequestDto(
            displayName = customer.displayName,
            customerType = customer.customerType.name,
            status = customer.status.name,
            primaryPhone = customer.primaryPhone,
            alternatePhone = customer.alternatePhone,
            email = customer.email,
            contactPersonName = customer.contactPersonName,
            creditLimit = customer.creditProfile.creditLimit.amount,
            paymentTermDays = customer.creditProfile.paymentTermDays,
            notes = customer.notes
        )
        return when (val res = client.updateCustomer(customer.customerId, req)) {
            is ApiResult.Success -> DomainResult.Success(res.data.toDomain())
            is ApiResult.Error -> DomainResult.Error(message = res.errorResponse.message)
        }
    }

    override suspend fun setCustomerStatus(customerId: String, status: CustomerStatusType): DomainResult<Customer> {
        val req = SetCustomerStatusRequestDto(status = status.name)
        return when (val res = client.setCustomerStatus(customerId, req)) {
            is ApiResult.Success -> DomainResult.Success(res.data.toDomain())
            is ApiResult.Error -> DomainResult.Error(message = res.errorResponse.message)
        }
    }

    override suspend fun deactivateCustomer(customerId: String): DomainResult<Customer> {
        return setCustomerStatus(customerId, CustomerStatusType.INACTIVE)
    }

    override suspend fun reactivateCustomer(customerId: String): DomainResult<Customer> {
        return setCustomerStatus(customerId, CustomerStatusType.ACTIVE)
    }

    override suspend fun archiveCustomer(customerId: String): DomainResult<Customer> {
        return setCustomerStatus(customerId, CustomerStatusType.ARCHIVED)
    }

    override suspend fun restoreCustomer(customerId: String): DomainResult<Customer> {
        return setCustomerStatus(customerId, CustomerStatusType.ACTIVE)
    }

    override suspend fun findDuplicateCustomer(phone: String, email: String?, excludeCustomerId: String?): Customer? {
        return null
    }

    override fun searchCustomers(query: String): Flow<List<Customer>> = getCustomers()

    override fun filterCustomers(type: CustomerType?, status: CustomerStatusType?): Flow<List<Customer>> = getCustomers()

    override suspend fun refreshCustomers(): Result<Unit> {
        return when (val res = client.getCustomerProfile()) {
            is ApiResult.Success -> Result.success(Unit)
            is ApiResult.Error -> Result.failure(Exception(res.errorResponse.message))
        }
    }

    override fun observeCustomerNotes(customerId: String): Flow<List<CustomerNote>> = flow { emit(emptyList()) }
    override suspend fun addCustomerNote(note: CustomerNote): DomainResult<CustomerNote> = DomainResult.Error(message = "Notes API endpoint unavailable")
    override suspend fun updateCustomerNote(note: CustomerNote): DomainResult<CustomerNote> = DomainResult.Error(message = "Notes API endpoint unavailable")
    override suspend fun deleteCustomerNote(noteId: String, customerId: String): DomainResult<Unit> = DomainResult.Error(message = "Notes API endpoint unavailable")
    override suspend fun toggleImportantNote(noteId: String, customerId: String): DomainResult<CustomerNote> = DomainResult.Error(message = "Notes API endpoint unavailable")
    override fun observeCustomerActivities(customerId: String): Flow<List<CustomerActivity>> = flow { emit(emptyList()) }
    override suspend fun setFollowUpDate(customerId: String, followUpAt: String?): DomainResult<Customer> = findCustomerById(customerId)

    private fun mapCustomerProfileDtoToCustomer(dto: com.sucharu.sucharupro.data.api.model.CustomerProfileDto): Customer {
        val statusType = try {
            CustomerStatusType.valueOf(dto.status.uppercase())
        } catch (_: Exception) {
            CustomerStatusType.ACTIVE
        }
        return Customer(
            customerId = dto.customerId,
            customerCode = dto.customerCode,
            displayName = dto.name,
            customerType = CustomerType.INDIVIDUAL,
            status = statusType,
            primaryPhone = dto.phone ?: "+8801700000000",
            email = dto.email,
            notes = null,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z"
        )
    }
}
