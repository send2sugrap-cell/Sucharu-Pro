package com.sucharu.sucharupro.data.api.client

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.auth.model.*
import com.sucharu.sucharupro.data.composition.DemoRole
import com.sucharu.sucharupro.data.datasource.DemoOrderFixtures
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthStatus
import java.math.BigDecimal
import java.util.UUID

/**
 * Enhanced In-Memory Demo Backend API Client supporting All ERP Showcase Roles (INFRA-06).
 *
 * Supports Customer, Affiliate, Staff, Manager, and Admin demo roles with realistic synthetic data.
 *
 * Absolute Invariants:
 * 1. MUST NOT connect to PostgreSQL or hold DB credentials.
 * 2. MUST NOT invoke production API Gateway or live SMS services.
 * 3. MUST NOT affect or mutate production authentication accounts.
 * 4. Deterministic demo OTP: '123456' accepted ONLY inside this isolated demo runtime.
 */
class DemoBackendApiClient(
    initialRole: DemoRole = DemoRole.CUSTOMER,
    val demoTenantId: String = "TENANT-DEMO-001",
    val demoProjectId: String = "PROJECT-DEMO-001",
    val demoOtp: String = "123456"
) : BackendApiClient {

    var activeDemoRole: DemoRole = initialRole
        private set

    private var isAuthenticated = false
    private var isVerified = false
    private var isPendingVerification = false

    fun switchRole(role: DemoRole): AuthenticatedPrincipal {
        activeDemoRole = role
        isAuthenticated = true
        isVerified = true
        return buildPrincipal(role)
    }

    private fun buildPrincipal(role: DemoRole): AuthenticatedPrincipal =
        AuthenticatedPrincipal(
            userId = role.demoUserId,
            projectId = demoProjectId,
            username = role.demoUsername,
            role = role.userRole,
            permissions = if (role == DemoRole.ADMIN) setOf(UserPermission.ADMIN_ALL) else emptySet(),
            email = role.demoEmail,
            accountStatus = if (isVerified) AccountStatus.ACTIVE else AccountStatus.PENDING
        )

    private fun buildUserProfile(role: DemoRole): UserProfileDto =
        UserProfileDto(
            userId = role.demoUserId,
            projectId = demoProjectId,
            username = role.demoUsername,
            displayName = role.displayName,
            email = role.demoEmail,
            phone = role.demoPhone,
            emailVerified = true,
            phoneVerified = true,
            role = role.userRole,
            permissions = if (role == DemoRole.ADMIN) setOf(UserPermission.ADMIN_ALL) else emptySet(),
            accountStatus = if (isVerified) AccountStatus.ACTIVE else AccountStatus.PENDING
        )

    override suspend fun register(request: RegisterRequestDto): ApiResult<RegisterResponseDto> {
        val targetRole = request.requestedRole?.let { DemoRole.fromUserRole(it) } ?: DemoRole.CUSTOMER
        activeDemoRole = targetRole
        isPendingVerification = true
        isVerified = false
        return ApiResult.Success(
            RegisterResponseDto(
                userId = targetRole.demoUserId,
                username = request.username?.ifBlank { targetRole.demoUsername } ?: targetRole.demoUsername,
                email = request.email,
                phone = request.phone,
                accountStatus = AccountStatus.PENDING,
                role = targetRole.userRole,
                verificationRequired = true,
                message = "Demo registration initiated for ${targetRole.displayName}. Use Demo OTP: $demoOtp",
                deliveryAccepted = true,
                deliveryStatus = "DELIVERY_ACCEPTED"
            )
        )
    }

    override suspend fun login(request: LoginRequestDto): ApiResult<AuthResponseDto> {
        val targetRole = DemoRole.fromIdentifier(request.identifier)
        activeDemoRole = targetRole
        isAuthenticated = true
        isVerified = true
        return ApiResult.Success(
            AuthResponseDto(
                accessToken = "demo-jwt-${targetRole.name.lowercase()}-${UUID.randomUUID()}",
                refreshToken = "demo-refresh-${targetRole.name.lowercase()}-${UUID.randomUUID()}",
                tokenType = "Bearer",
                expiresInSeconds = 86400L,
                sessionId = "demo-session-${targetRole.name.lowercase()}-001",
                user = buildUserProfile(targetRole).copy(accountStatus = AccountStatus.ACTIVE)
            )
        )
    }

    override suspend fun refreshToken(refreshToken: String): ApiResult<AuthResponseDto> {
        if (!isAuthenticated) {
            return ApiResult.Error(ApiErrorResponse(errorCode = ErrorCode.UNAUTHENTICATED, message = "Demo session expired."))
        }
        return login(LoginRequestDto(identifier = activeDemoRole.demoUsername, password = "demoPassword123!"))
    }

    override suspend fun logout(allDevices: Boolean): ApiResult<Map<String, String>> {
        isAuthenticated = false
        isVerified = false
        isPendingVerification = false
        return ApiResult.Success(mapOf("status" to "LOGGED_OUT", "message" to "Demo session cleared successfully."))
    }

    override suspend fun logoutAll(): ApiResult<Map<String, String>> {
        return logout(allDevices = true)
    }

    override suspend fun requestPasswordRecovery(request: PasswordRecoveryRequestDto): ApiResult<PasswordRecoveryResponseDto> {
        return ApiResult.Success(
            PasswordRecoveryResponseDto(
                message = "If an account exists, a recovery code has been sent. Use Demo OTP: $demoOtp"
            )
        )
    }

    override suspend fun confirmPasswordReset(request: PasswordRecoveryConfirmDto): ApiResult<Map<String, Any>> {
        return if (request.token == demoOtp) {
            ApiResult.Success(mapOf("success" to true, "message" to "Demo password reset successfully."))
        } else {
            ApiResult.Error(ApiErrorResponse(errorCode = ErrorCode.VALIDATION_ERROR, message = "Invalid demo reset code. Please enter $demoOtp."))
        }
    }

    override suspend fun requestVerificationToken(request: RequestVerificationRequestDto): ApiResult<Map<String, Any>> {
        isPendingVerification = true
        return ApiResult.Success(mapOf("success" to true, "message" to "Demo verification challenge sent for ${activeDemoRole.displayName}. Demo OTP: $demoOtp"))
    }

    override suspend fun confirmVerificationToken(request: ConfirmVerificationRequestDto): ApiResult<Map<String, Any>> {
        return if (request.token == demoOtp) {
            isVerified = true
            isAuthenticated = true
            ApiResult.Success(mapOf("success" to true, "message" to "Demo account verified and activated for ${activeDemoRole.displayName}!"))
        } else {
            ApiResult.Error(ApiErrorResponse(errorCode = ErrorCode.VALIDATION_ERROR, message = "Invalid demo verification code. Please enter $demoOtp."))
        }
    }

    override suspend fun resendVerificationToken(identifier: String): ApiResult<Map<String, Any>> {
        return ApiResult.Success(mapOf("success" to true, "message" to "Demo verification token resent. Demo OTP: $demoOtp"))
    }

    override suspend fun getPublicCompanyInfo(): ApiResult<CompanyInfoDto> {
        return ApiResult.Success(
            CompanyInfoDto(
                companyName = "Sucharu Graphics",
                tagline = "Commercial Printing & Creative Packaging Ecosystem",
                email = "support@sucharugraphics.com",
                phone = "+8801700000000",
                address = "Dhaka, Bangladesh"
            )
        )
    }

    override suspend fun getPublicProducts(): ApiResult<List<PublicProductDto>> {
        return ApiResult.Success(
            listOf(
                PublicProductDto(
                    productId = "prod-01",
                    name = "Commercial Catalogues & Brochures",
                    category = "Offset Printing",
                    description = "High-volume offset printing with vivid CMYK reproduction.",
                    startingPrice = BigDecimal("1500.00")
                ),
                PublicProductDto(
                    productId = "prod-02",
                    name = "Custom Rigid & Folding Boxes",
                    category = "Packaging",
                    description = "Premium packaging with lamination, foil stamping, and embossing.",
                    startingPrice = BigDecimal("2500.00")
                )
            )
        )
    }

    override suspend fun getMyProfile(): ApiResult<AuthenticatedPrincipal> {
        return if (isAuthenticated) {
            ApiResult.Success(buildPrincipal(activeDemoRole))
        } else {
            ApiResult.Error(ApiErrorResponse(errorCode = ErrorCode.UNAUTHENTICATED, message = "Demo user is not authenticated."))
        }
    }

    override suspend fun getCustomerProfile(): ApiResult<CustomerProfileDto> {
        return ApiResult.Success(
            CustomerProfileDto(
                customerId = DemoRole.CUSTOMER.demoUserId,
                customerCode = "CUST-DEMO-001",
                name = "Sucharu Demo Client",
                companyName = "Sucharu Graphics Showcase Ltd.",
                email = DemoRole.CUSTOMER.demoEmail,
                phone = DemoRole.CUSTOMER.demoPhone,
                creditLimit = BigDecimal("50000.00"),
                currentBalance = BigDecimal("1200.00"),
                status = "ACTIVE"
            )
        )
    }

    override suspend fun getCustomerOrders(): ApiResult<List<CustomerOrderSummaryDto>> {
        val summaries = DemoOrderFixtures.demoOrders().map { ord ->
            CustomerOrderSummaryDto(
                orderId = ord.orderId,
                orderNumber = ord.orderNumber,
                status = ord.status.name,
                totalAmount = BigDecimal(ord.items.sumOf { (it.quantity * it.unitPrice.amount.toDouble()).toLong() }),
                createdAt = System.currentTimeMillis()
            )
        }
        return ApiResult.Success(summaries)
    }

    override suspend fun getCustomerOrderDetail(orderId: String): ApiResult<CustomerOrderDetailDto> {
        val ord = DemoOrderFixtures.demoOrders().find { it.orderId == orderId }
            ?: DemoOrderFixtures.demoOrders().first()
        return ApiResult.Success(
            CustomerOrderDetailDto(
                orderId = ord.orderId,
                orderNumber = ord.orderNumber,
                customerId = DemoRole.CUSTOMER.demoUserId,
                status = ord.status.name,
                items = ord.items.map { item ->
                    OrderItemDto(
                        itemId = item.itemId,
                        description = item.description,
                        quantity = item.quantity,
                        unitPrice = item.unitPrice.amount,
                        totalPrice = item.unitPrice.amount.multiply(BigDecimal(item.quantity))
                    )
                },
                subtotal = BigDecimal("1200.00"),
                discount = BigDecimal("100.00"),
                totalAmount = BigDecimal("1100.00"),
                notes = ord.notes,
                version = 1L,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun createCustomerOrder(request: CreateOrderRequestDto, idempotencyKey: String?): ApiResult<CustomerOrderDetailDto> {
        return ApiResult.Success(
            CustomerOrderDetailDto(
                orderId = "ord-demo-${UUID.randomUUID().toString().take(8)}",
                orderNumber = "ORD-DEMO-NEW",
                customerId = DemoRole.CUSTOMER.demoUserId,
                status = "SUBMITTED",
                items = request.items.mapIndexed { idx, item ->
                    OrderItemDto(
                        itemId = "item-new-$idx",
                        description = item.description,
                        quantity = item.quantity,
                        unitPrice = item.unitPrice,
                        totalPrice = item.unitPrice.multiply(BigDecimal(item.quantity))
                    )
                },
                subtotal = BigDecimal("500.00"),
                discount = BigDecimal("0.00"),
                totalAmount = BigDecimal("500.00"),
                notes = request.notes,
                version = 1L,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun getAffiliateProfile(): ApiResult<AffiliateProfileDto> {
        return ApiResult.Success(
            AffiliateProfileDto(
                affiliateId = DemoRole.AFFILIATE.demoUserId,
                affiliateCode = "DEMO-AFF-2026",
                name = "Sucharu Demo Affiliate Partner",
                email = DemoRole.AFFILIATE.demoEmail,
                tier = "GOLD",
                commissionRatePercent = BigDecimal("7.50"),
                lifetimeEarnings = BigDecimal("4500.00"),
                unpaidEarnings = BigDecimal("750.00"),
                status = "ACTIVE"
            )
        )
    }

    override suspend fun getAffiliateCommission(): ApiResult<AffiliateCommissionDto> {
        return ApiResult.Success(
            AffiliateCommissionDto(
                affiliateId = DemoRole.AFFILIATE.demoUserId,
                totalReferralsCount = 18,
                totalSalesVolume = BigDecimal("60000.00"),
                totalCommissionEarned = BigDecimal("4500.00"),
                pendingPayoutAmount = BigDecimal("750.00"),
                lastPayoutDate = System.currentTimeMillis() - 604800000L
            )
        )
    }

    override suspend fun checkHealthLive(): ApiResult<Map<String, String>> {
        return ApiResult.Success(mapOf("status" to "UP", "mode" to "DEVELOPMENT_DEMO", "activeRole" to activeDemoRole.name))
    }

    override suspend fun checkHealthReady(): ApiResult<DatabaseHealthStatus> {
        return ApiResult.Success(
            DatabaseHealthStatus(
                isLive = true,
                isReady = true,
                databaseName = "in-memory-demo",
                latencyMs = 1L
            )
        )
    }
}
