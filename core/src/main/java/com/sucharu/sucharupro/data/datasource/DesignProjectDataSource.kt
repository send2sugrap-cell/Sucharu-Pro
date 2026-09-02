package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignActivityEvent
import com.sucharu.sucharupro.domain.model.design.DesignAssignment
import com.sucharu.sucharupro.domain.model.design.DesignProject
import kotlinx.coroutines.flow.Flow

/**
 * Data source contract for Design Project persistence in Sucharu Pro ERP.
 */
interface DesignProjectDataSource {
    fun observeProjects(): Flow<List<DesignProject>>
    suspend fun fetchProjectById(projectId: String): DomainResult<DesignProject>
    suspend fun insertProject(project: DesignProject): DomainResult<DesignProject>
    suspend fun updateProject(project: DesignProject): DomainResult<DesignProject>

    fun observeAssignments(): Flow<List<DesignAssignment>>
    suspend fun insertAssignment(assignment: DesignAssignment): DomainResult<DesignAssignment>
    suspend fun updateAssignment(assignment: DesignAssignment): DomainResult<DesignAssignment>

    fun observeActivityEvents(): Flow<List<DesignActivityEvent>>
    suspend fun insertActivityEvent(event: DesignActivityEvent): DomainResult<DesignActivityEvent>
}
