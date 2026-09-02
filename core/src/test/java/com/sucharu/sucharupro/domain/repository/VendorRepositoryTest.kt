package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorCategory
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.model.vendor.VendorType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorRepositoryTest {

    private lateinit var fakeDataSource: FakeVendorDataSource
    private lateinit var repository: VendorRepository

    @Before
    fun setUp() {
        fakeDataSource = FakeVendorDataSource()
        repository = VendorRepositoryImpl(fakeDataSource)
    }

    private fun sampleVendor(id: String = "vnd_01", code: String = "VND-001", name: String = "Delta Packaging"): Vendor {
        return Vendor(
            vendorId = id,
            projectId = "PRJ-01",
            vendorCode = code,
            vendorName = name,
            vendorType = VendorType.PRODUCTION_VENDOR,
            vendorCategory = VendorCategory.PACKAGING,
            status = VendorStatus.ACTIVE
        )
    }

    @Test
    fun testCreateAndFindById() = runBlocking {
        val v = sampleVendor()
        val createRes = repository.createVendor(v)
        assertTrue(createRes is DomainResult.Success)

        val findRes = repository.findById("PRJ-01", "vnd_01")
        assertTrue(findRes is DomainResult.Success)
        val loaded = (findRes as DomainResult.Success).data
        assertEquals("Delta Packaging", loaded.vendorName)
        assertEquals("VND-001", loaded.vendorCode)
    }

    @Test
    fun testFindByCode() = runBlocking {
        repository.createVendor(sampleVendor("vnd_01", "VND-001", "Delta"))
        val findRes = repository.findByCode("PRJ-01", "VND-001")
        assertTrue(findRes is DomainResult.Success)
        assertEquals("vnd_01", (findRes as DomainResult.Success).data.vendorId)
    }

    @Test
    fun testExistsByCode() = runBlocking {
        repository.createVendor(sampleVendor("vnd_01", "VND-001", "Delta"))
        assertTrue(repository.existsByCode("PRJ-01", "VND-001"))
        assertFalse(repository.existsByCode("PRJ-01", "VND-999"))
        assertFalse(repository.existsByCode("PRJ-01", "VND-001", excludeVendorId = "vnd_01"))
    }

    @Test
    fun testDuplicateCodeRejection() = runBlocking {
        val v1 = sampleVendor("vnd_01", "VND-001", "Delta 1")
        val v2 = sampleVendor("vnd_02", "VND-001", "Delta 2")

        assertTrue(repository.createVendor(v1) is DomainResult.Success)
        val duplicateRes = repository.createVendor(v2)
        assertTrue(duplicateRes is DomainResult.Error)
    }

    @Test
    fun testUpdateVendor() = runBlocking {
        val v = sampleVendor()
        repository.createVendor(v)

        val updateRes = repository.updateVendor(v.copy(vendorName = "Delta Packaging Solutions"))
        assertTrue(updateRes is DomainResult.Success)
        assertEquals(2L, (updateRes as DomainResult.Success).data.version)

        val loaded = (repository.findById("PRJ-01", "vnd_01") as DomainResult.Success).data
        assertEquals("Delta Packaging Solutions", loaded.vendorName)
        assertEquals(2L, loaded.version)
    }

    @Test
    fun testUpdateStatus() = runBlocking {
        val v = sampleVendor()
        repository.createVendor(v)

        val statusRes = repository.updateStatus("PRJ-01", "vnd_01", VendorStatus.SUSPENDED, "admin_user")
        assertTrue(statusRes is DomainResult.Success)
        assertEquals(VendorStatus.SUSPENDED, (statusRes as DomainResult.Success).data.status)
    }

    @Test
    fun testObserveVendors() = runBlocking {
        repository.createVendor(sampleVendor("vnd_01", "VND-001", "Alpha"))
        repository.createVendor(sampleVendor("vnd_02", "VND-002", "Beta"))

        val list = repository.observeVendors("PRJ-01").first()
        assertEquals(2, list.size)
    }

    @Test
    fun testListVendorsWithFilter() = runBlocking {
        repository.createVendor(sampleVendor("vnd_01", "VND-001", "Alpha").copy(vendorType = VendorType.SERVICE_PROVIDER))
        repository.createVendor(sampleVendor("vnd_02", "VND-002", "Beta").copy(vendorType = VendorType.MATERIAL_SUPPLIER))

        val serviceList = repository.listVendors("PRJ-01", type = VendorType.SERVICE_PROVIDER)
        assertTrue(serviceList is DomainResult.Success)
        assertEquals(1, (serviceList as DomainResult.Success).data.size)
        assertEquals("Alpha", serviceList.data[0].vendorName)
    }
}
