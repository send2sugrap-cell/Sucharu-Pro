package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorCategory
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.model.vendor.VendorType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory thread-safe Fake Vendor DataSource for unit tests and local mocks (Module 12 Step 01).
 */
class FakeVendorDataSource(
    initialVendors: List<Vendor> = emptyList()
) : VendorDataSource {

    private val vendorsMap = ConcurrentHashMap<String, MutableMap<String, Vendor>>()
    private val vendorFlow = MutableStateFlow<Map<String, Map<String, Vendor>>>(emptyMap())

    init {
        initialVendors.forEach { v ->
            val projectMap = vendorsMap.computeIfAbsent(v.projectId) { ConcurrentHashMap() }
            projectMap[v.vendorId] = v
        }
        publish()
    }

    private fun publish() {
        val snapshot = vendorsMap.mapValues { (_, vMap) -> vMap.toMap() }
        vendorFlow.value = snapshot
    }

    override fun observeVendors(projectId: String): Flow<List<Vendor>> {
        return vendorFlow.asStateFlow().map { all ->
            all[projectId]?.values?.sortedByDescending { it.createdAt } ?: emptyList()
        }
    }

    override suspend fun fetchVendors(
        projectId: String,
        type: VendorType?,
        category: VendorCategory?,
        status: VendorStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<Vendor>> {
        val projectVendors = vendorsMap[projectId]?.values?.toList() ?: emptyList()
        val filtered = projectVendors
            .filter { type == null || it.vendorType == type }
            .filter { category == null || it.vendorCategory == category }
            .filter { status == null || it.status == status }
            .sortedByDescending { it.createdAt }
            .drop(offset)
            .take(limit)
        return DomainResult.Success(filtered)
    }

    override suspend fun fetchVendorById(projectId: String, vendorId: String): DomainResult<Vendor> {
        val v = vendorsMap[projectId]?.get(vendorId)
        return if (v != null) {
            DomainResult.Success(v)
        } else {
            DomainResult.Error(NoSuchElementException("Vendor not found: '$vendorId' in project '$projectId'."))
        }
    }

    override suspend fun fetchVendorByCode(projectId: String, vendorCode: String): DomainResult<Vendor> {
        val v = vendorsMap[projectId]?.values?.find { it.vendorCode.equals(vendorCode, ignoreCase = true) }
        return if (v != null) {
            DomainResult.Success(v)
        } else {
            DomainResult.Error(NoSuchElementException("Vendor not found with code: '$vendorCode' in project '$projectId'."))
        }
    }

    override suspend fun existsByCode(projectId: String, vendorCode: String, excludeVendorId: String?): Boolean {
        return vendorsMap[projectId]?.values?.any {
            it.vendorCode.equals(vendorCode, ignoreCase = true) && (excludeVendorId == null || it.vendorId != excludeVendorId)
        } ?: false
    }

    override suspend fun insertVendor(vendor: Vendor): DomainResult<Vendor> = synchronized(this) {
        val projectMap = vendorsMap.computeIfAbsent(vendor.projectId) { ConcurrentHashMap() }
        if (projectMap.values.any { it.vendorCode.equals(vendor.vendorCode, ignoreCase = true) }) {
            return@synchronized DomainResult.Error(IllegalStateException("Duplicate vendorCode '${vendor.vendorCode}' in project '${vendor.projectId}'."))
        }
        projectMap[vendor.vendorId] = vendor
        publish()
        DomainResult.Success(vendor)
    }

    override suspend fun updateVendor(vendor: Vendor): DomainResult<Vendor> {
        val projectMap = vendorsMap[vendor.projectId]
            ?: return DomainResult.Error(NoSuchElementException("Project '${vendor.projectId}' not found."))
        val existing = projectMap[vendor.vendorId]
            ?: return DomainResult.Error(NoSuchElementException("Vendor '${vendor.vendorId}' not found."))

        if (existing.version != vendor.version) {
            return DomainResult.Error(IllegalStateException("Optimistic lock failure: expected version ${existing.version} but got ${vendor.version}."))
        }

        val updated = vendor.copy(version = existing.version + 1L, updatedAt = System.currentTimeMillis())
        projectMap[vendor.vendorId] = updated
        publish()
        return DomainResult.Success(updated)
    }

    override suspend fun updateVendorStatus(
        projectId: String,
        vendorId: String,
        status: VendorStatus,
        updatedBy: String
    ): DomainResult<Vendor> {
        val projectMap = vendorsMap[projectId]
            ?: return DomainResult.Error(NoSuchElementException("Project '$projectId' not found."))
        val existing = projectMap[vendorId]
            ?: return DomainResult.Error(NoSuchElementException("Vendor '$vendorId' not found."))

        val updated = existing.copy(
            status = status,
            updatedBy = updatedBy,
            updatedAt = System.currentTimeMillis(),
            version = existing.version + 1L
        )
        projectMap[vendorId] = updated
        publish()
        return DomainResult.Success(updated)
    }
}
