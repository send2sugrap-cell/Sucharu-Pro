package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeQcGovernanceDataSource
import com.sucharu.sucharupro.data.repository.QcGovernanceRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceKpi
import com.sucharu.sucharupro.domain.model.qc.governance.QcKpiTarget
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcGovernanceConcurrencyTest {

    private lateinit var dataSource: FakeQcGovernanceDataSource
    private lateinit var repository: QcGovernanceRepository

    @Before
    fun setup() {
        dataSource = FakeQcGovernanceDataSource()
        repository = QcGovernanceRepositoryImpl(governanceDataSource = dataSource)
    }

    @Test
    fun `concurrent target updates execute safely with mutex protection`() = runBlocking {
        val totalCoroutines = 25
        val results = (1..totalCoroutines).map { index ->
            async(Dispatchers.Default) {
                repository.setTarget(
                    QcKpiTarget(
                        targetId = "TGT-$index",
                        projectId = "PRJ-CONC",
                        kpiType = QcGovernanceKpi.FIRST_PASS_RATE,
                        targetValue = 90.0 + (index % 10),
                        effectiveFrom = "2026-08-01T00:00:00Z",
                        configuredBy = "admin-1",
                        createdAt = "2026-08-01T00:00:00Z",
                        updatedAt = "2026-08-01T00:00:00Z"
                    ),
                    callerRole = UserRole.ADMIN
                )
            }
        }.awaitAll()

        assertEquals(totalCoroutines, results.size)
        assertTrue(results.all { it is DomainResult.Success })

        val targetsRes = repository.getTargets("PRJ-CONC")
        assertTrue(targetsRes is DomainResult.Success)
        assertEquals(totalCoroutines, (targetsRes as DomainResult.Success).data.size)
    }
}
