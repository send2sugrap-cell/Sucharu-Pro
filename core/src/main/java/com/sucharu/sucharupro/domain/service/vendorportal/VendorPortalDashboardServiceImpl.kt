package com.sucharu.sucharupro.domain.service.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.CapabilityStatus
import com.sucharu.sucharupro.domain.model.vendor.RateStatus
import com.sucharu.sucharupro.domain.model.vendor.VendorCapability
import com.sucharu.sucharupro.domain.model.vendor.VendorServiceRate
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.VendorCapabilityRepository
import com.sucharu.sucharupro.domain.repository.VendorPortalDashboardRepository
import com.sucharu.sucharupro.domain.repository.VendorServiceRateRepository
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorPortalDashboardCalculator

/**
 * Production implementation of [VendorPortalDashboardService] (Module 13 Step 02).
 */
class VendorPortalDashboardServiceImpl(
    private val portalService: VendorPortalService,
    private val dashboardRepository: VendorPortalDashboardRepository,
    private val capabilityRepository: VendorCapabilityRepository? = null,
    private val rateRepository: VendorServiceRateRepository? = null
) : VendorPortalDashboardService {

    private suspend fun resolveEffectiveProjectId(vendorId: String, context: VendorPortalAccessContext, tenantId: String): String {
        if (context.projectScope.isNotBlank() && context.projectScope != "*") {
            return context.projectScope
        }
        val accRes = portalService.listAccounts(projectId = null, status = null, tenantId = tenantId)
        val account = (accRes as? DomainResult.Success)?.data?.firstOrNull { it.vendorId == vendorId }
        return account?.projectId ?: tenantId
    }

    override suspend fun getDashboard(
        userId: String,
        vendorId: String,
        tenantId: String,
        clientIp: String?
    ): DomainResult<VendorPortalDashboard> {
        return try {
            // 1. Authorize and resolve access context
            val ctxRes = portalService.getAccessContext(userId, vendorId, tenantId, clientIp)
            val context = when (ctxRes) {
                is DomainResult.Success -> ctxRes.data
                is DomainResult.Error -> return DomainResult.Error(ctxRes.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            val projectId = resolveEffectiveProjectId(vendorId, context, tenantId)

            // 2. Resolve feature visibility
            val visibility = VendorPortalDashboardCalculator.resolveFeatureVisibility(context.role, context.policy)

            // 3. Query section summaries based on visibility
            val profileRes = dashboardRepository.getProfileSummary(vendorId, tenantId, projectId)
            val profile = when (profileRes) {
                is DomainResult.Success -> profileRes.data.copy(portalRole = context.role.name)
                is DomainResult.Error -> return DomainResult.Error(profileRes.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            val operations = if (visibility.canViewPurchaseOrders || visibility.canViewWorkOrders || visibility.canViewDeliveries) {
                (dashboardRepository.getOperationalSummary(vendorId, tenantId, projectId) as? DomainResult.Success)?.data
            } else null

            val financials = if (visibility.canViewFinancials || visibility.canViewInvoices || visibility.canViewSettlements) {
                (dashboardRepository.getFinancialSummary(vendorId, tenantId, projectId) as? DomainResult.Success)?.data
            } else null

            val quality = if (visibility.canViewQuality || visibility.canViewDisputes) {
                (dashboardRepository.getQualitySummary(vendorId, tenantId, projectId) as? DomainResult.Success)?.data
            } else null

            val performance = if (visibility.canViewPerformance) {
                (dashboardRepository.getPerformanceSummary(vendorId, tenantId, projectId) as? DomainResult.Success)?.data
            } else null

            val compliance = if (visibility.canViewCompliance) {
                (dashboardRepository.getComplianceSummary(vendorId, tenantId, projectId) as? DomainResult.Success)?.data
            } else null

            val activities = (dashboardRepository.getRecentActivities(vendorId, tenantId, 5) as? DomainResult.Success)?.data ?: emptyList()

            // 4. Build role-aware KPIs
            val kpis = VendorPortalDashboardCalculator.buildDashboardKpis(
                visibility = visibility,
                operations = operations,
                financials = financials,
                quality = quality,
                performance = performance
            )

            // 5. Build role-aware navigation
            val navigation = VendorPortalDashboardCalculator.buildNavigationItems(
                visibility = visibility,
                openPoCount = operations?.activePurchaseOrders ?: 0,
                openWoCount = operations?.openWorkOrders ?: 0,
                pendingDeliveryCount = operations?.pendingDeliveries ?: 0,
                pendingInvoiceCount = financials?.pendingInvoices ?: 0,
                openDisputeCount = quality?.openDisputes ?: 0
            )

            val dashboard = VendorPortalDashboard(
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                vendorCode = context.vendorCode,
                vendorName = context.vendorName,
                portalRole = context.role,
                membershipStatus = context.membershipStatus,
                accountStatus = context.accountStatus,
                kpis = kpis,
                profile = profile,
                operations = operations,
                financials = financials,
                quality = quality,
                performance = performance,
                compliance = compliance,
                recentActivities = activities,
                featureVisibility = visibility,
                navigationItems = navigation
            )

            DomainResult.Success(dashboard)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getProfile(
        userId: String,
        vendorId: String,
        tenantId: String
    ): DomainResult<VendorPortalProfileSummary> {
        return try {
            val ctxRes = portalService.getAccessContext(userId, vendorId, tenantId)
            val context = when (ctxRes) {
                is DomainResult.Success -> ctxRes.data
                is DomainResult.Error -> return DomainResult.Error(ctxRes.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            val projectId = resolveEffectiveProjectId(vendorId, context, tenantId)
            val res = dashboardRepository.getProfileSummary(vendorId, tenantId, projectId)
            if (res is DomainResult.Success) {
                DomainResult.Success(res.data.copy(portalRole = context.role.name))
            } else res
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getCapabilities(
        userId: String,
        vendorId: String,
        tenantId: String
    ): DomainResult<List<VendorCapability>> {
        return try {
            val ctxRes = portalService.getAccessContext(userId, vendorId, tenantId)
            val context = when (ctxRes) {
                is DomainResult.Success -> ctxRes.data
                is DomainResult.Error -> return DomainResult.Error(ctxRes.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            val projectId = resolveEffectiveProjectId(vendorId, context, tenantId)
            val caps = capabilityRepository?.listByVendor(projectId, vendorId)
            if (caps is DomainResult.Success) {
                DomainResult.Success(caps.data.filter { it.status == CapabilityStatus.ACTIVE })
            } else {
                DomainResult.Success(emptyList())
            }
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getRates(
        userId: String,
        vendorId: String,
        tenantId: String
    ): DomainResult<List<VendorServiceRate>> {
        return try {
            val ctxRes = portalService.getAccessContext(userId, vendorId, tenantId)
            val context = when (ctxRes) {
                is DomainResult.Success -> ctxRes.data
                is DomainResult.Error -> return DomainResult.Error(ctxRes.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            val visibility = VendorPortalDashboardCalculator.resolveFeatureVisibility(context.role, context.policy)
            if (!visibility.canViewRates) {
                return DomainResult.Error(SecurityException("Role '${context.role}' is not authorized to view commercial rates"))
            }

            val rates = rateRepository?.listByVendor(vendorId, tenantId)
            if (rates is DomainResult.Success) {
                DomainResult.Success(rates.data.filter { it.status == RateStatus.ACTIVE })
            } else {
                DomainResult.Success(emptyList())
            }
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getOperationalSummary(
        userId: String,
        vendorId: String,
        tenantId: String
    ): DomainResult<VendorPortalOperationalSummary> {
        return try {
            val ctxRes = portalService.getAccessContext(userId, vendorId, tenantId)
            val context = when (ctxRes) {
                is DomainResult.Success -> ctxRes.data
                is DomainResult.Error -> return DomainResult.Error(ctxRes.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            val visibility = VendorPortalDashboardCalculator.resolveFeatureVisibility(context.role, context.policy)
            if (!visibility.canViewPurchaseOrders && !visibility.canViewWorkOrders && !visibility.canViewDeliveries) {
                return DomainResult.Error(SecurityException("Role '${context.role}' is not authorized to view operational summary"))
            }

            val projectId = resolveEffectiveProjectId(vendorId, context, tenantId)
            dashboardRepository.getOperationalSummary(vendorId, tenantId, projectId)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getFinancialSummary(
        userId: String,
        vendorId: String,
        tenantId: String
    ): DomainResult<VendorPortalFinancialSummary> {
        return try {
            val ctxRes = portalService.getAccessContext(userId, vendorId, tenantId)
            val context = when (ctxRes) {
                is DomainResult.Success -> ctxRes.data
                is DomainResult.Error -> return DomainResult.Error(ctxRes.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            val visibility = VendorPortalDashboardCalculator.resolveFeatureVisibility(context.role, context.policy)
            if (!visibility.canViewFinancials && !visibility.canViewInvoices && !visibility.canViewSettlements) {
                return DomainResult.Error(SecurityException("Role '${context.role}' is not authorized to view financial summary"))
            }

            val projectId = resolveEffectiveProjectId(vendorId, context, tenantId)
            dashboardRepository.getFinancialSummary(vendorId, tenantId, projectId)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getQualitySummary(
        userId: String,
        vendorId: String,
        tenantId: String
    ): DomainResult<VendorPortalQualitySummary> {
        return try {
            val ctxRes = portalService.getAccessContext(userId, vendorId, tenantId)
            val context = when (ctxRes) {
                is DomainResult.Success -> ctxRes.data
                is DomainResult.Error -> return DomainResult.Error(ctxRes.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            val visibility = VendorPortalDashboardCalculator.resolveFeatureVisibility(context.role, context.policy)
            if (!visibility.canViewQuality && !visibility.canViewDisputes) {
                return DomainResult.Error(SecurityException("Role '${context.role}' is not authorized to view quality summary"))
            }

            val projectId = resolveEffectiveProjectId(vendorId, context, tenantId)
            dashboardRepository.getQualitySummary(vendorId, tenantId, projectId)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getPerformanceSummary(
        userId: String,
        vendorId: String,
        tenantId: String
    ): DomainResult<VendorPortalPerformanceSummary> {
        return try {
            val ctxRes = portalService.getAccessContext(userId, vendorId, tenantId)
            val context = when (ctxRes) {
                is DomainResult.Success -> ctxRes.data
                is DomainResult.Error -> return DomainResult.Error(ctxRes.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            val visibility = VendorPortalDashboardCalculator.resolveFeatureVisibility(context.role, context.policy)
            if (!visibility.canViewPerformance) {
                return DomainResult.Error(SecurityException("Role '${context.role}' is not authorized to view performance summary"))
            }

            val projectId = resolveEffectiveProjectId(vendorId, context, tenantId)
            dashboardRepository.getPerformanceSummary(vendorId, tenantId, projectId)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getComplianceSummary(
        userId: String,
        vendorId: String,
        tenantId: String
    ): DomainResult<VendorPortalComplianceSummary> {
        return try {
            val ctxRes = portalService.getAccessContext(userId, vendorId, tenantId)
            val context = when (ctxRes) {
                is DomainResult.Success -> ctxRes.data
                is DomainResult.Error -> return DomainResult.Error(ctxRes.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            val visibility = VendorPortalDashboardCalculator.resolveFeatureVisibility(context.role, context.policy)
            if (!visibility.canViewCompliance) {
                return DomainResult.Error(SecurityException("Role '${context.role}' is not authorized to view compliance summary"))
            }

            val projectId = resolveEffectiveProjectId(vendorId, context, tenantId)
            dashboardRepository.getComplianceSummary(vendorId, tenantId, projectId)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getRecentActivities(
        userId: String,
        vendorId: String,
        tenantId: String,
        limit: Int
    ): DomainResult<List<VendorPortalActivitySummary>> {
        return try {
            val ctxRes = portalService.getAccessContext(userId, vendorId, tenantId)
            when (ctxRes) {
                is DomainResult.Success -> dashboardRepository.getRecentActivities(vendorId, tenantId, limit)
                is DomainResult.Error -> DomainResult.Error(ctxRes.exception)
                DomainResult.Loading -> DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getWorkspace(
        userId: String,
        vendorId: String,
        tenantId: String,
        clientIp: String?
    ): DomainResult<VendorPortalWorkspace> {
        return try {
            val ctxRes = portalService.getAccessContext(userId, vendorId, tenantId, clientIp)
            val context = when (ctxRes) {
                is DomainResult.Success -> ctxRes.data
                is DomainResult.Error -> return DomainResult.Error(ctxRes.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            val projectId = resolveEffectiveProjectId(vendorId, context, tenantId)
            val visibility = VendorPortalDashboardCalculator.resolveFeatureVisibility(context.role, context.policy)
            val profileRes = dashboardRepository.getProfileSummary(vendorId, tenantId, projectId)
            val profile = when (profileRes) {
                is DomainResult.Success -> profileRes.data.copy(portalRole = context.role.name)
                is DomainResult.Error -> return DomainResult.Error(profileRes.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            val navigation = VendorPortalDashboardCalculator.buildNavigationItems(visibility)

            val workspace = VendorPortalWorkspace(
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                userId = userId,
                portalRole = context.role,
                profile = profile,
                featureVisibility = visibility,
                navigationItems = navigation
            )
            DomainResult.Success(workspace)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }
}
