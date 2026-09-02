package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDesignArtworkDataSource
import com.sucharu.sucharupro.data.datasource.FakeDesignProjectDataSource
import com.sucharu.sucharupro.data.datasource.FakeDesignProofDataSource
import com.sucharu.sucharupro.data.repository.DesignArtworkRepositoryImpl
import com.sucharu.sucharupro.data.repository.DesignProjectRepositoryImpl
import com.sucharu.sucharupro.data.repository.DesignProofRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.model.design.DesignActivityType
import com.sucharu.sucharupro.domain.model.design.DesignArtwork
import com.sucharu.sucharupro.domain.model.design.DesignProject
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.model.design.ProofStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests verifying activity type extensions for Proof Management and Revision Workflows (Module 05 Step 03).
 */
class ProofAuditTest {

    @Test
    fun designActivityTypes_includeStep03Activities() {
        val expectedTypes = listOf(
            DesignActivityType.PROOF_CREATED,
            DesignActivityType.PROOF_VERSION_CREATED,
            DesignActivityType.PROOF_SUBMITTED_FOR_REVIEW,
            DesignActivityType.REVISION_REQUESTED,
            DesignActivityType.REVISION_STARTED,
            DesignActivityType.PROOF_RESUBMITTED,
            DesignActivityType.REVISION_RESOLVED,
            DesignActivityType.PROOF_ARCHIVED
        )

        expectedTypes.forEach { type ->
            assertTrue(type.name.isNotBlank())
            assertTrue(type.defaultLabel.isNotBlank())
        }
    }
}
