package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.DesignProductionHandoffDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignActivityType
import com.sucharu.sucharupro.domain.model.design.DesignProductionHandoff
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DesignApprovalRepository
import com.sucharu.sucharupro.domain.repository.DesignArtworkRepository
import com.sucharu.sucharupro.domain.repository.DesignProductionHandoffRepository
import com.sucharu.sucharupro.domain.repository.DesignProjectRepository
import com.sucharu.sucharupro.domain.repository.DesignProofRepository
import com.sucharu.sucharupro.domain.validation.DesignProductionHandoffValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Authoritative implementation of [DesignProductionHandoffRepository] enforcing 15-point checklist validation,
 * exact version integrity, RBAC checks, idempotency, and audit logging (Module 05 Step 05).
 */
class DesignProductionHandoffRepositoryImpl(
    private val dataSource: DesignProductionHandoffDataSource,
    private val projectRepository: DesignProjectRepository,
    private val artworkRepository: DesignArtworkRepository,
    private val proofRepository: DesignProofRepository,
    private val approvalRepository: DesignApprovalRepository
) : DesignProductionHandoffRepository {

    private val repositoryMutex = Mutex()

    override suspend fun canHandoffToProduction(
        approvalId: String,
        callerRole: UserRole?
    ): DomainResult<Boolean> = repositoryMutex.withLock {
        val approval = when (val res = approvalRepository.findApprovalById(approvalId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return DomainResult.Error(message = "Approval not found: ${res.message}")
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val project = when (val res = projectRepository.findDesignProjectById(approval.projectId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return DomainResult.Error(message = "Design project not found: ${res.message}")
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val artwork = when (val res = artworkRepository.findArtworkById(approval.artworkId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return DomainResult.Error(message = "Artwork not found: ${res.message}")
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val artworkVersion = artwork.versions.find { it.versionId == approval.artworkVersionId }
            ?: return DomainResult.Error(message = "Artwork Version '${approval.artworkVersionId}' not found on artwork '${artwork.name}'.")

        val proof = when (val res = proofRepository.findProofById(approval.proofId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return DomainResult.Error(message = "Proof not found: ${res.message}")
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val proofVersion = proof.versions.find { it.versionId == approval.proofVersionId }
            ?: return DomainResult.Error(message = "Proof Version '${approval.proofVersionId}' not found on proof '${proof.title}'.")

        val validation = DesignProductionHandoffValidator.validateProductionHandoff(
            project = project,
            artwork = artwork,
            artworkVersion = artworkVersion,
            proof = proof,
            proofVersion = proofVersion,
            approval = approval,
            callerRole = callerRole
        )

        return when (validation) {
            is DomainResult.Success -> DomainResult.Success(true)
            is DomainResult.Error -> DomainResult.Error(message = validation.message)
            is DomainResult.Loading -> DomainResult.Error(message = "Validation loading.")
        }
    }

    override suspend fun authorizeProductionHandoff(
        approvalId: String,
        authorizedBy: String,
        authorizedByName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<DesignProductionHandoff> = repositoryMutex.withLock {
        // Idempotency: If already authorized, return existing record
        when (val existing = dataSource.fetchHandoffByApprovalId(approvalId)) {
            is DomainResult.Success -> return DomainResult.Success(existing.data)
            is DomainResult.Error -> {} // Not authorized yet, proceed
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val approval = when (val res = approvalRepository.findApprovalById(approvalId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return DomainResult.Error(message = "Approval not found: ${res.message}")
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val project = when (val res = projectRepository.findDesignProjectById(approval.projectId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return DomainResult.Error(message = "Design project not found: ${res.message}")
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val artwork = when (val res = artworkRepository.findArtworkById(approval.artworkId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return DomainResult.Error(message = "Artwork not found: ${res.message}")
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val artworkVersion = artwork.versions.find { it.versionId == approval.artworkVersionId }
            ?: return DomainResult.Error(message = "Artwork Version '${approval.artworkVersionId}' not found on artwork '${artwork.name}'.")

        val proof = when (val res = proofRepository.findProofById(approval.proofId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return DomainResult.Error(message = "Proof not found: ${res.message}")
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val proofVersion = proof.versions.find { it.versionId == approval.proofVersionId }
            ?: return DomainResult.Error(message = "Proof Version '${approval.proofVersionId}' not found on proof '${proof.title}'.")

        val validation = DesignProductionHandoffValidator.validateProductionHandoff(
            project = project,
            artwork = artwork,
            artworkVersion = artworkVersion,
            proof = proof,
            proofVersion = proofVersion,
            approval = approval,
            callerRole = callerRole
        )

        if (validation is DomainResult.Error) {
            return validation
        }

        val handoffId = "handoff-" + UUID.randomUUID().toString()
        val handoff = DesignProductionHandoff(
            handoffId = handoffId,
            projectId = project.projectId,
            productionJobId = project.productionJobId,
            artworkId = artwork.artworkId,
            artworkVersionId = artworkVersion.versionId,
            proofId = proof.proofId,
            proofVersionId = proofVersion.versionId,
            approvalId = approval.approvalId,
            authorizedBy = authorizedBy,
            authorizedByName = authorizedByName,
            authorizedAt = timestamp,
            notes = notes
        )

        val insertResult = dataSource.insertHandoff(handoff)
        if (insertResult is DomainResult.Error) {
            return insertResult
        }

        // Advance project status if eligible
        if (project.status.canTransitionTo(DesignStatus.HANDED_OFF_TO_PRODUCTION)) {
            projectRepository.updateDesignStatus(
                projectId = project.projectId,
                targetStatus = DesignStatus.HANDED_OFF_TO_PRODUCTION,
                actorId = authorizedBy,
                actorName = authorizedByName,
                notes = "Production handoff authorized for Proof V${proofVersion.versionNumber} and Artwork V${artworkVersion.versionNumber} (Job: ${project.productionJobId}).",
                timestamp = timestamp
            )
        }

        return DomainResult.Success(handoff)
    }

    override fun getHandoffByApprovalId(approvalId: String): Flow<DesignProductionHandoff?> {
        return dataSource.observeHandoffs().map { list ->
            list.find { it.approvalId == approvalId }
        }
    }

    override fun getHandoffForProject(projectId: String): Flow<List<DesignProductionHandoff>> {
        return dataSource.observeHandoffs().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override fun getHandoffForJob(productionJobId: String): Flow<List<DesignProductionHandoff>> {
        return dataSource.observeHandoffs().map { list ->
            list.filter { it.productionJobId == productionJobId }
        }
    }

    override fun observeHandoffs(): Flow<List<DesignProductionHandoff>> = dataSource.observeHandoffs()
}
