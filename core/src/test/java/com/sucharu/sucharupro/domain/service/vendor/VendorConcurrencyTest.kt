package com.sucharu.sucharupro.domain.service.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class VendorConcurrencyTest {

    @Test
    fun testConcurrentDuplicateCodeCreationYieldsExactlyOneSuccess() = runBlocking {
        val fakeDataSource = FakeVendorDataSource()
        val repository = VendorRepositoryImpl(fakeDataSource)
        val service = VendorServiceImpl(repository)

        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)
        val concurrencyLevel = 10

        val deferreds = (1..concurrencyLevel).map { index ->
            async(Dispatchers.Default) {
                val res = service.createVendor(
                    projectId = "PRJ-CONCUR",
                    vendorName = "Vendor Concurrent $index",
                    vendorCode = "VND-CONCUR-FIXED"
                )
                if (res is DomainResult.Success) {
                    successCount.incrementAndGet()
                } else {
                    failCount.incrementAndGet()
                }
            }
        }

        deferreds.awaitAll()

        assertEquals(1, successCount.get())
        assertEquals(concurrencyLevel - 1, failCount.get())

        // Verify only 1 vendor exists in repository
        val list = (service.listVendors("PRJ-CONCUR") as DomainResult.Success).data
        assertEquals(1, list.size)
        assertEquals("VND-CONCUR-FIXED", list[0].vendorCode)
    }
}
