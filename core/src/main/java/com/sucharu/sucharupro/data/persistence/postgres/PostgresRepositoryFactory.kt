package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.CustomerDataSource
import com.sucharu.sucharupro.data.datasource.FinancialTransactionDataSource
import com.sucharu.sucharupro.data.datasource.OrderDataSource
import com.sucharu.sucharupro.data.datasource.VendorDataSource
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.data.repository.OrderRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.repository.CustomerRepository
import com.sucharu.sucharupro.domain.repository.FinancialTransactionRepository
import com.sucharu.sucharupro.domain.repository.OrderRepository
import com.sucharu.sucharupro.domain.repository.VendorRepository
import com.sucharu.sucharupro.domain.service.vendor.VendorService
import com.sucharu.sucharupro.domain.service.vendor.VendorServiceImpl

/**
 * Production-grade PostgreSQL Repository Factory (INFRA-01 Step 04).
 *
 * Provides a clean composition root / dependency injection bridge that wires
 * domain Repository interfaces to PostgreSQL-backed DataSources while preserving
 * FakeDataSources for in-memory unit tests.
 */
open class PostgresRepositoryFactory(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) {

    // --- PostgreSQL DataSources ---

    val customerDataSource: CustomerDataSource by lazy {
        PostgresCustomerDataSource(transactionManager, defaultTenantId)
    }

    val orderDataSource: OrderDataSource by lazy {
        PostgresOrderDataSource(transactionManager, defaultTenantId)
    }

    val vendorDataSource: VendorDataSource by lazy {
        PostgresVendorDataSource(transactionManager, defaultTenantId)
    }

    val vendorProfileDataSource: com.sucharu.sucharupro.data.datasource.VendorProfileDataSource by lazy {
        PostgresVendorProfileDataSource(transactionManager, defaultTenantId)
    }

    val vendorContactDataSource: com.sucharu.sucharupro.data.datasource.VendorContactDataSource by lazy {
        PostgresVendorContactDataSource(transactionManager, defaultTenantId)
    }

    val vendorAddressDataSource: com.sucharu.sucharupro.data.datasource.VendorAddressDataSource by lazy {
        PostgresVendorAddressDataSource(transactionManager, defaultTenantId)
    }

    val vendorCapabilityDataSource: com.sucharu.sucharupro.data.datasource.VendorCapabilityDataSource by lazy {
        PostgresVendorCapabilityDataSource(transactionManager, defaultTenantId)
    }

    val vendorServiceRateDataSource: com.sucharu.sucharupro.data.datasource.VendorServiceRateDataSource by lazy {
        PostgresVendorServiceRateDataSource(transactionManager, defaultTenantId)
    }

    val vendorWorkOrderDataSource: com.sucharu.sucharupro.data.datasource.VendorWorkOrderDataSource by lazy {
        PostgresVendorWorkOrderDataSource(transactionManager, defaultTenantId)
    }

    val vendorPurchaseOrderDataSource: com.sucharu.sucharupro.data.datasource.VendorPurchaseOrderDataSource by lazy {
        PostgresVendorPurchaseOrderDataSource(transactionManager, defaultTenantId)
    }

    val financialTransactionDataSource: FinancialTransactionDataSource by lazy {
        PostgresFinancialTransactionDataSource(transactionManager)
    }

    val productionQcDataSource: PostgresProductionQcDataSource by lazy {
        PostgresProductionQcDataSource(transactionManager, defaultTenantId)
    }

    val inventoryProductDataSource: PostgresInventoryProductDataSource by lazy {
        PostgresInventoryProductDataSource(transactionManager, defaultTenantId)
    }

    val deliveryChallanDataSource: PostgresDeliveryChallanDataSource by lazy {
        PostgresDeliveryChallanDataSource(transactionManager, defaultTenantId)
    }

    val returnDataSource: PostgresReturnDataSource by lazy {
        PostgresReturnDataSource(transactionManager, defaultTenantId)
    }

    // --- PostgreSQL Event Persistence (INFRA-04 Step 02) ---

    val eventStore: com.sucharu.sucharupro.data.event.postgres.PostgresEventStore by lazy {
        com.sucharu.sucharupro.data.event.postgres.PostgresEventStore(transactionManager)
    }

    val outboxStore: com.sucharu.sucharupro.data.event.postgres.PostgresTransactionalOutboxStore by lazy {
        com.sucharu.sucharupro.data.event.postgres.PostgresTransactionalOutboxStore(transactionManager)
    }

    val idempotencyStore: com.sucharu.sucharupro.data.event.postgres.PostgresEventIdempotencyStore by lazy {
        com.sucharu.sucharupro.data.event.postgres.PostgresEventIdempotencyStore(transactionManager)
    }

    val deadLetterRepository: com.sucharu.sucharupro.data.event.postgres.DeadLetterRepository by lazy {
        com.sucharu.sucharupro.data.event.postgres.PostgresDeadLetterRepository(transactionManager)
    }

    // --- Persistent Integration Delivery (INFRA-04 Step 03) ---

    val integrationDeliveryRepository: com.sucharu.sucharupro.data.event.postgres.IntegrationDeliveryRepository by lazy {
        com.sucharu.sucharupro.data.event.postgres.PostgresIntegrationDeliveryRepository(transactionManager)
    }

    fun createConsumerRegistry(): com.sucharu.sucharupro.domain.event.consumer.orchestration.EventConsumerRegistry {
        return com.sucharu.sucharupro.domain.event.consumer.orchestration.EventConsumerRegistry()
    }

    fun createConsumerExecutionEngine(): com.sucharu.sucharupro.domain.event.consumer.orchestration.EventConsumerExecutionEngine {
        return com.sucharu.sucharupro.domain.event.consumer.orchestration.EventConsumerExecutionEngine(
            idempotencyStore = idempotencyStore,
            deliveryRepository = integrationDeliveryRepository
        )
    }

    fun createConsumerRouter(
        registry: com.sucharu.sucharupro.domain.event.consumer.orchestration.EventConsumerRegistry
    ): com.sucharu.sucharupro.domain.event.consumer.orchestration.EventConsumerRouter {
        return com.sucharu.sucharupro.domain.event.consumer.orchestration.EventConsumerRouter(
            registry = registry,
            executionEngine = createConsumerExecutionEngine()
        )
    }

    fun createOutboxDispatcher(
        domainEventDispatcher: com.sucharu.sucharupro.domain.event.dispatcher.DomainEventDispatcher,
        retryConfig: com.sucharu.sucharupro.data.event.model.RetryConfig = com.sucharu.sucharupro.data.event.model.RetryConfig()
    ): com.sucharu.sucharupro.data.event.dispatcher.OutboxDispatcher {
        return com.sucharu.sucharupro.data.event.dispatcher.OutboxDispatcher(
            outboxStore = outboxStore,
            domainEventDispatcher = domainEventDispatcher,
            retryConfig = retryConfig
        )
    }

    // --- PostgreSQL-Backed Repositories ---

    open fun createCustomerRepository(
        tenantId: String = defaultTenantId
    ): CustomerRepository {
        val ds = PostgresCustomerDataSource(transactionManager, tenantId)
        return CustomerRepositoryImpl(ds)
    }

    fun createOrderRepository(
        tenantId: String = defaultTenantId
    ): OrderRepository {
        val ds = PostgresOrderDataSource(transactionManager, tenantId)
        return OrderRepositoryImpl(ds)
    }

    open fun createVendorRepository(
        tenantId: String = defaultTenantId
    ): VendorRepository {
        val ds = PostgresVendorDataSource(transactionManager, tenantId)
        return VendorRepositoryImpl(ds)
    }

    open fun createVendorService(
        tenantId: String = defaultTenantId
    ): VendorService {
        return VendorServiceImpl(createVendorRepository(tenantId))
    }

    open fun createVendorProfileRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.VendorProfileRepository {
        val ds = PostgresVendorProfileDataSource(transactionManager, tenantId)
        return com.sucharu.sucharupro.data.repository.VendorProfileRepositoryImpl(ds)
    }

    open fun createVendorProfileService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.vendor.VendorProfileService {
        return com.sucharu.sucharupro.domain.service.vendor.VendorProfileServiceImpl(
            vendorRepository = createVendorRepository(tenantId),
            profileRepository = createVendorProfileRepository(tenantId)
        )
    }

    open fun createVendorContactRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.VendorContactRepository {
        val ds = PostgresVendorContactDataSource(transactionManager, tenantId)
        return com.sucharu.sucharupro.data.repository.VendorContactRepositoryImpl(ds)
    }

    open fun createVendorContactService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.vendor.VendorContactService {
        return com.sucharu.sucharupro.domain.service.vendor.VendorContactServiceImpl(
            vendorRepository = createVendorRepository(tenantId),
            contactRepository = createVendorContactRepository(tenantId)
        )
    }

    open fun createVendorAddressRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.VendorAddressRepository {
        val ds = PostgresVendorAddressDataSource(transactionManager, tenantId)
        return com.sucharu.sucharupro.data.repository.VendorAddressRepositoryImpl(ds)
    }

    open fun createVendorAddressService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.vendor.VendorAddressService {
        return com.sucharu.sucharupro.domain.service.vendor.VendorAddressServiceImpl(
            vendorRepository = createVendorRepository(tenantId),
            addressRepository = createVendorAddressRepository(tenantId)
        )
    }

    open fun createVendorCapabilityRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.VendorCapabilityRepository {
        val ds = PostgresVendorCapabilityDataSource(transactionManager, tenantId)
        return com.sucharu.sucharupro.data.repository.VendorCapabilityRepositoryImpl(ds)
    }

    open fun createVendorCapabilityService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.vendor.VendorCapabilityService {
        return com.sucharu.sucharupro.domain.service.vendor.VendorCapabilityServiceImpl(
            vendorRepository = createVendorRepository(tenantId),
            capabilityRepository = createVendorCapabilityRepository(tenantId)
        )
    }

    open fun createVendorServiceRateRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.VendorServiceRateRepository {
        val ds = PostgresVendorServiceRateDataSource(transactionManager, tenantId)
        return com.sucharu.sucharupro.data.repository.VendorServiceRateRepositoryImpl(ds)
    }

    open fun createVendorServiceRateService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.vendor.VendorServiceRateService {
        return com.sucharu.sucharupro.domain.service.vendor.VendorServiceRateServiceImpl(
            vendorRepository = createVendorRepository(tenantId),
            capabilityRepository = createVendorCapabilityRepository(tenantId),
            rateRepository = createVendorServiceRateRepository(tenantId)
        )
    }

    open fun createVendorWorkOrderRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.VendorWorkOrderRepository {
        val ds = PostgresVendorWorkOrderDataSource(transactionManager, tenantId)
        return com.sucharu.sucharupro.data.repository.VendorWorkOrderRepositoryImpl(ds)
    }

    open fun createVendorWorkOrderService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.vendor.VendorWorkOrderService {
        return com.sucharu.sucharupro.domain.service.vendor.VendorWorkOrderServiceImpl(
            vendorRepository = createVendorRepository(tenantId),
            capabilityRepository = createVendorCapabilityRepository(tenantId),
            rateService = createVendorServiceRateService(tenantId),
            workOrderRepository = createVendorWorkOrderRepository(tenantId)
        )
    }

    open fun createVendorPurchaseOrderRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.VendorPurchaseOrderRepository {
        val ds = PostgresVendorPurchaseOrderDataSource(transactionManager, tenantId)
        return com.sucharu.sucharupro.data.repository.VendorPurchaseOrderRepositoryImpl(ds)
    }

    open fun createVendorPurchaseOrderService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.vendor.VendorPurchaseOrderService {
        return com.sucharu.sucharupro.domain.service.vendor.VendorPurchaseOrderServiceImpl(
            vendorRepository = createVendorRepository(tenantId),
            capabilityRepository = createVendorCapabilityRepository(tenantId),
            rateService = createVendorServiceRateService(tenantId),
            purchaseOrderRepository = createVendorPurchaseOrderRepository(tenantId)
        )
    }

    open fun createVendorDeliveryReceiptRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.VendorDeliveryReceiptRepository {
        val ds = PostgresVendorDeliveryReceiptDataSource(transactionManager, tenantId)
        return com.sucharu.sucharupro.data.repository.VendorDeliveryReceiptRepositoryImpl(ds)
    }

    open fun createVendorDeliveryReceiptService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.vendor.VendorDeliveryReceiptService {
        return com.sucharu.sucharupro.domain.service.vendor.VendorDeliveryReceiptServiceImpl(
            vendorRepository = createVendorRepository(tenantId),
            purchaseOrderRepository = createVendorPurchaseOrderRepository(tenantId),
            receiptRepository = createVendorDeliveryReceiptRepository(tenantId)
        )
    }

    open fun createVendorInvoiceRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.VendorInvoiceRepository {
        val ds = PostgresVendorInvoiceDataSource(transactionManager, tenantId)
        return com.sucharu.sucharupro.data.repository.VendorInvoiceRepositoryImpl(ds)
    }

    open fun createVendorInvoiceService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.vendor.VendorInvoiceService {
        return com.sucharu.sucharupro.domain.service.vendor.VendorInvoiceServiceImpl(
            vendorRepository = createVendorRepository(tenantId),
            purchaseOrderRepository = createVendorPurchaseOrderRepository(tenantId),
            receiptRepository = createVendorDeliveryReceiptRepository(tenantId),
            invoiceRepository = createVendorInvoiceRepository(tenantId)
        )
    }

    open fun createVendorQualityRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.VendorQualityRepository {
        val ds = PostgresVendorQualityDataSource(transactionManager, tenantId)
        return com.sucharu.sucharupro.data.repository.VendorQualityRepositoryImpl(ds)
    }

    open fun createVendorQualityService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.vendor.VendorQualityService {
        return com.sucharu.sucharupro.domain.service.vendor.VendorQualityServiceImpl(
            vendorRepository = createVendorRepository(tenantId),
            purchaseOrderRepository = createVendorPurchaseOrderRepository(tenantId),
            receiptRepository = createVendorDeliveryReceiptRepository(tenantId),
            qualityRepository = createVendorQualityRepository(tenantId)
        )
    }

    open fun createVendorPerformanceRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.VendorPerformanceRepository {
        val ds = PostgresVendorPerformanceDataSource(transactionManager, tenantId)
        return com.sucharu.sucharupro.data.repository.VendorPerformanceRepositoryImpl(ds)
    }

    open fun createVendorPerformanceService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.vendor.VendorPerformanceService {
        return com.sucharu.sucharupro.domain.service.vendor.VendorPerformanceServiceImpl(
            performanceRepository = createVendorPerformanceRepository(tenantId),
            vendorRepository = createVendorRepository(tenantId),
            purchaseOrderRepository = createVendorPurchaseOrderRepository(tenantId),
            receiptRepository = createVendorDeliveryReceiptRepository(tenantId),
            qualityRepository = createVendorQualityRepository(tenantId),
            invoiceRepository = createVendorInvoiceRepository(tenantId)
        )
    }

    open fun createVendorAnalyticsRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.VendorAnalyticsRepository {
        return com.sucharu.sucharupro.data.repository.VendorAnalyticsRepositoryImpl(
            vendorRepository = createVendorRepository(tenantId),
            poRepository = createVendorPurchaseOrderRepository(tenantId),
            deliveryRepository = createVendorDeliveryReceiptRepository(tenantId),
            invoiceRepository = createVendorInvoiceRepository(tenantId),
            qualityRepository = createVendorQualityRepository(tenantId),
            performanceRepository = createVendorPerformanceRepository(tenantId),
            settlementRepository = createVendorSettlementRepository(tenantId)
        )
    }

    open fun createVendorSettlementService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.vendor.VendorSettlementService {
        return com.sucharu.sucharupro.domain.service.vendor.VendorSettlementServiceImpl(
            settlementRepository = createVendorSettlementRepository(tenantId),
            analyticsRepository = createVendorAnalyticsRepository(tenantId),
            vendorRepository = createVendorRepository(tenantId),
            invoiceRepository = createVendorInvoiceRepository(tenantId)
        )
    }

    fun createFinancialTransactionRepository(): FinancialTransactionRepository {
        return FinancialTransactionRepositoryImpl(financialTransactionDataSource)
    }

    fun createProductionQcDataSource(tenantId: String = defaultTenantId): PostgresProductionQcDataSource {
        return PostgresProductionQcDataSource(transactionManager, tenantId)
    }

    fun createInventoryProductDataSource(tenantId: String = defaultTenantId): PostgresInventoryProductDataSource {
        return PostgresInventoryProductDataSource(transactionManager, tenantId)
    }

    fun createDeliveryChallanDataSource(tenantId: String = defaultTenantId): PostgresDeliveryChallanDataSource {
        return PostgresDeliveryChallanDataSource(transactionManager, tenantId)
    }

    // --- INFRA-04 Step 04: Background Job Execution Repositories & Engine ---

    val jobRepository: com.sucharu.sucharupro.data.job.postgres.JobRepository by lazy {
        com.sucharu.sucharupro.data.job.postgres.PostgresJobRepository(transactionManager)
    }

    val jobExecutionRepository: com.sucharu.sucharupro.data.job.postgres.JobExecutionRepository by lazy {
        com.sucharu.sucharupro.data.job.postgres.PostgresJobExecutionRepository(transactionManager)
    }

    val jobScheduleRepository: com.sucharu.sucharupro.data.job.postgres.JobScheduleRepository by lazy {
        com.sucharu.sucharupro.data.job.postgres.PostgresJobScheduleRepository(transactionManager)
    }

    val jobDependencyRepository: com.sucharu.sucharupro.data.job.postgres.JobDependencyRepository by lazy {
        com.sucharu.sucharupro.data.job.postgres.PostgresJobDependencyRepository(transactionManager)
    }

    val jobDeadLetterRepository: com.sucharu.sucharupro.data.job.postgres.JobDeadLetterRepository by lazy {
        com.sucharu.sucharupro.data.job.postgres.PostgresJobDeadLetterRepository(transactionManager)
    }

    val jobIdempotencyStore: com.sucharu.sucharupro.domain.job.idempotency.JobIdempotencyStore by lazy {
        com.sucharu.sucharupro.data.job.postgres.PostgresJobIdempotencyStore(transactionManager)
    }

    fun createJobExecutionEngine(
        handlerRegistry: com.sucharu.sucharupro.domain.job.worker.JobHandlerRegistry,
        retryEngine: com.sucharu.sucharupro.data.job.retry.JobRetryEngine = com.sucharu.sucharupro.data.job.retry.JobRetryEngine()
    ): com.sucharu.sucharupro.data.job.worker.JobExecutionEngine {
        return com.sucharu.sucharupro.data.job.worker.JobExecutionEngine(
            handlerRegistry = handlerRegistry,
            jobRepository = jobRepository,
            executionRepository = jobExecutionRepository,
            deadLetterRepository = jobDeadLetterRepository,
            dependencyRepository = jobDependencyRepository,
            retryEngine = retryEngine
        )
    }

    fun createJobScheduler(): com.sucharu.sucharupro.domain.job.scheduler.JobScheduler {
        return com.sucharu.sucharupro.domain.job.scheduler.JobScheduler(
            scheduleRepository = jobScheduleRepository,
            jobRepository = jobRepository
        )
    }

    fun createJobClaimService(): com.sucharu.sucharupro.data.job.worker.JobClaimService {
        return com.sucharu.sucharupro.data.job.worker.JobClaimService(jobRepository)
    }

    fun createJobLeaseRecoveryService(): com.sucharu.sucharupro.data.job.lease.JobLeaseRecoveryService {
        return com.sucharu.sucharupro.data.job.lease.JobLeaseRecoveryService(jobRepository)
    }

    fun createJobOperationsService(
        auditLogger: com.sucharu.sucharupro.data.job.observability.JobAuditLogger = com.sucharu.sucharupro.data.job.observability.JobAuditLogger()
    ): com.sucharu.sucharupro.domain.job.operations.JobOperationsService {
        return com.sucharu.sucharupro.data.job.operations.DefaultJobOperationsService(
            jobRepository = jobRepository,
            scheduleRepository = jobScheduleRepository,
            deadLetterRepository = jobDeadLetterRepository,
            auditLogger = auditLogger
        )
    }

    // --- PostgreSQL Workflow Persistence (INFRA-04 Step 05) ---

    val workflowDefinitionRepository: com.sucharu.sucharupro.data.workflow.postgres.PostgresWorkflowDefinitionRepository by lazy {
        com.sucharu.sucharupro.data.workflow.postgres.PostgresWorkflowDefinitionRepository(transactionManager)
    }

    val workflowInstanceRepository: com.sucharu.sucharupro.data.workflow.postgres.PostgresWorkflowInstanceRepository by lazy {
        com.sucharu.sucharupro.data.workflow.postgres.PostgresWorkflowInstanceRepository(transactionManager)
    }

    val workflowStepExecutionRepository: com.sucharu.sucharupro.data.workflow.postgres.PostgresWorkflowStepExecutionRepository by lazy {
        com.sucharu.sucharupro.data.workflow.postgres.PostgresWorkflowStepExecutionRepository(transactionManager)
    }

    val workflowCompensationRepository: com.sucharu.sucharupro.data.workflow.postgres.PostgresWorkflowCompensationRepository by lazy {
        com.sucharu.sucharupro.data.workflow.postgres.PostgresWorkflowCompensationRepository(transactionManager)
    }

    val workflowApprovalRepository: com.sucharu.sucharupro.data.workflow.postgres.PostgresWorkflowApprovalRepository by lazy {
        com.sucharu.sucharupro.data.workflow.postgres.PostgresWorkflowApprovalRepository(transactionManager)
    }

    val workflowIdempotencyStore: com.sucharu.sucharupro.data.workflow.postgres.PostgresWorkflowIdempotencyStore by lazy {
        com.sucharu.sucharupro.data.workflow.postgres.PostgresWorkflowIdempotencyStore(transactionManager)
    }

    fun createWorkflowOperationsService(): com.sucharu.sucharupro.domain.workflow.operations.WorkflowOperationsService {
        return com.sucharu.sucharupro.data.workflow.operations.DefaultWorkflowOperationsService(
            definitionRepository = workflowDefinitionRepository,
            instanceRepository = workflowInstanceRepository,
            approvalRepository = workflowApprovalRepository
        )
    }

    fun createWorkflowControlPlaneService(): com.sucharu.sucharupro.data.workflow.control.WorkflowControlPlaneService {
        return com.sucharu.sucharupro.data.workflow.control.WorkflowControlPlaneService(
            definitionRepository = workflowDefinitionRepository,
            instanceRepository = workflowInstanceRepository,
            stepExecutionRepository = workflowStepExecutionRepository,
            compensationRepository = workflowCompensationRepository,
            approvalRepository = workflowApprovalRepository,
            idempotencyStore = workflowIdempotencyStore
        )
    }

    fun createReturnDataSource(tenantId: String = defaultTenantId): PostgresReturnDataSource {
        return PostgresReturnDataSource(transactionManager, tenantId)
    }

    fun createVendorSettlementDataSource(tenantId: String = defaultTenantId): PostgresVendorSettlementDataSource {
        return PostgresVendorSettlementDataSource(transactionManager, tenantId)
    }

    fun createVendorSettlementRepository(tenantId: String = defaultTenantId): com.sucharu.sucharupro.domain.repository.VendorSettlementRepository {
        return com.sucharu.sucharupro.data.repository.VendorSettlementRepositoryImpl(
            createVendorSettlementDataSource(tenantId)
        )
    }

    fun createVendorPortalDataSource(tenantId: String = defaultTenantId): PostgresVendorPortalDataSource {
        return PostgresVendorPortalDataSource(transactionManager, tenantId)
    }

    open fun createVendorPortalRepository(tenantId: String = defaultTenantId): com.sucharu.sucharupro.domain.repository.VendorPortalRepository {
        return com.sucharu.sucharupro.data.repository.VendorPortalRepositoryImpl(
            createVendorPortalDataSource(tenantId)
        )
    }

    open fun createVendorPortalService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalService {
        return com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalServiceImpl(
            portalRepository = createVendorPortalRepository(tenantId),
            vendorRepository = createVendorRepository(tenantId)
        )
    }

    open fun createVendorPortalDashboardRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.VendorPortalDashboardRepository {
        return com.sucharu.sucharupro.data.repository.VendorPortalDashboardRepositoryImpl(
            vendorRepository = createVendorRepository(tenantId),
            portalRepository = createVendorPortalRepository(tenantId),
            settlementRepository = createVendorSettlementRepository(tenantId)
        )
    }

    open fun createVendorPortalDashboardService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalDashboardService {
        return com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalDashboardServiceImpl(
            portalService = createVendorPortalService(tenantId),
            dashboardRepository = createVendorPortalDashboardRepository(tenantId)
        )
    }

    private val rfqDataSource = com.sucharu.sucharupro.data.datasource.FakeVendorRfqDataSource()
    private val quotationDataSource = com.sucharu.sucharupro.data.datasource.FakeVendorQuotationDataSource()

    open fun createVendorRfqRepository(tenantId: String = defaultTenantId): com.sucharu.sucharupro.domain.repository.VendorRfqRepository {
        return com.sucharu.sucharupro.data.repository.VendorRfqRepositoryImpl(rfqDataSource)
    }

    open fun createVendorRfqService(tenantId: String = defaultTenantId): com.sucharu.sucharupro.domain.service.vendorportal.VendorRfqService {
        return com.sucharu.sucharupro.domain.service.vendorportal.VendorRfqServiceImpl(
            rfqRepository = createVendorRfqRepository(tenantId),
            vendorRepository = createVendorRepository(tenantId)
        )
    }

    open fun createVendorQuotationRepository(tenantId: String = defaultTenantId): com.sucharu.sucharupro.domain.repository.VendorQuotationRepository {
        return com.sucharu.sucharupro.data.repository.VendorQuotationRepositoryImpl(quotationDataSource)
    }

    open fun createVendorQuotationService(tenantId: String = defaultTenantId): com.sucharu.sucharupro.domain.service.vendorportal.VendorQuotationService {
        return com.sucharu.sucharupro.domain.service.vendorportal.VendorQuotationServiceImpl(
            quotationRepository = createVendorQuotationRepository(tenantId),
            rfqRepository = createVendorRfqRepository(tenantId)
        )
    }

    open fun createVendorRfqEvaluationService(tenantId: String = defaultTenantId): com.sucharu.sucharupro.domain.service.vendorportal.VendorRfqEvaluationService {
        return com.sucharu.sucharupro.domain.service.vendorportal.VendorRfqEvaluationServiceImpl(
            rfqRepository = createVendorRfqRepository(tenantId),
            quotationRepository = createVendorQuotationRepository(tenantId),
            vendorRepository = createVendorRepository(tenantId)
        )
    }

    private val collaborationDataSource = com.sucharu.sucharupro.data.datasource.FakeVendorCollaborationDataSource()

    open fun createVendorCollaborationRepository(tenantId: String = defaultTenantId): com.sucharu.sucharupro.domain.repository.VendorCollaborationRepository {
        return com.sucharu.sucharupro.data.repository.VendorCollaborationRepositoryImpl(collaborationDataSource)
    }

    open fun createVendorPortalCollaborationService(tenantId: String = defaultTenantId): com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalCollaborationService {
        return com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalCollaborationServiceImpl(
            collaborationRepository = createVendorCollaborationRepository(tenantId),
            vendorPurchaseOrderService = createVendorPurchaseOrderService(tenantId),
            vendorWorkOrderService = createVendorWorkOrderService(tenantId),
            vendorRepository = createVendorRepository(tenantId)
        )
    }

    private val deliveryDataSource = com.sucharu.sucharupro.data.datasource.FakeVendorPortalDeliveryDataSource()

    open fun createVendorPortalDeliveryRepository(tenantId: String = defaultTenantId): com.sucharu.sucharupro.domain.repository.VendorPortalDeliveryRepository {
        return com.sucharu.sucharupro.data.repository.VendorPortalDeliveryRepositoryImpl(deliveryDataSource)
    }

    open fun createVendorPortalDeliveryService(tenantId: String = defaultTenantId): com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalDeliveryService {
        return com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalDeliveryServiceImpl(
            deliveryRepository = createVendorPortalDeliveryRepository(tenantId),
            vendorPurchaseOrderService = createVendorPurchaseOrderService(tenantId),
            vendorDeliveryReceiptService = createVendorDeliveryReceiptService(tenantId),
            vendorQualityService = createVendorQualityService(tenantId),
            vendorRepository = createVendorRepository(tenantId)
        )
    }

    private val invoiceDataSource = com.sucharu.sucharupro.data.datasource.FakeVendorPortalInvoiceDataSource()

    open fun createVendorPortalInvoiceRepository(tenantId: String = defaultTenantId): com.sucharu.sucharupro.domain.repository.VendorPortalInvoiceRepository {
        return com.sucharu.sucharupro.data.repository.VendorPortalInvoiceRepositoryImpl(invoiceDataSource)
    }

    open fun createVendorPortalInvoiceService(tenantId: String = defaultTenantId): com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalInvoiceService {
        return com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalInvoiceServiceImpl(
            invoiceRepository = createVendorPortalInvoiceRepository(tenantId),
            vendorInvoiceService = createVendorInvoiceService(tenantId),
            vendorPurchaseOrderService = createVendorPurchaseOrderService(tenantId),
            vendorSettlementService = createVendorSettlementService(tenantId),
            vendorRepository = createVendorRepository(tenantId)
        )
    }

    private val qualityWorkspaceDataSource = com.sucharu.sucharupro.data.datasource.FakeVendorPortalQualityDataSource()

    open fun createVendorPortalQualityRepository(tenantId: String = defaultTenantId): com.sucharu.sucharupro.domain.repository.VendorPortalQualityRepository {
        return com.sucharu.sucharupro.data.repository.VendorPortalQualityRepositoryImpl(qualityWorkspaceDataSource)
    }

    open fun createVendorPortalQualityService(tenantId: String = defaultTenantId): com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalQualityService {
        return com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalQualityServiceImpl(
            qualityRepository = createVendorPortalQualityRepository(tenantId),
            canonicalQualityService = createVendorQualityService(tenantId),
            vendorPurchaseOrderService = createVendorPurchaseOrderService(tenantId),
            vendorDeliveryReceiptService = createVendorDeliveryReceiptService(tenantId),
            vendorRepository = createVendorRepository(tenantId)
        )
    }

    private val performanceComplianceDataSource = com.sucharu.sucharupro.data.datasource.FakeVendorPortalPerformanceComplianceDataSource()

    open fun createVendorPortalPerformanceComplianceRepository(tenantId: String = defaultTenantId): com.sucharu.sucharupro.domain.repository.VendorPortalPerformanceComplianceRepository {
        return com.sucharu.sucharupro.data.repository.VendorPortalPerformanceComplianceRepositoryImpl(performanceComplianceDataSource)
    }

    open fun createVendorPortalPerformanceComplianceService(tenantId: String = defaultTenantId): com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalPerformanceComplianceService {
        return com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalPerformanceComplianceServiceImpl(
            portalRepository = createVendorPortalPerformanceComplianceRepository(tenantId),
            canonicalPerformanceService = createVendorPerformanceService(tenantId),
            vendorRepository = createVendorRepository(tenantId)
        )
    }

    private val settlementWorkspaceDataSource = com.sucharu.sucharupro.data.datasource.FakeVendorPortalSettlementDataSource()

    open fun createVendorPortalSettlementRepository(tenantId: String = defaultTenantId): com.sucharu.sucharupro.domain.repository.VendorPortalSettlementRepository {
        return com.sucharu.sucharupro.data.repository.VendorPortalSettlementRepositoryImpl(settlementWorkspaceDataSource)
    }

    open fun createVendorPortalSettlementService(tenantId: String = defaultTenantId): com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalSettlementService {
        return com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalSettlementServiceImpl(
            portalRepository = createVendorPortalSettlementRepository(tenantId),
            canonicalSettlementService = createVendorSettlementService(tenantId),
            canonicalInvoiceService = createVendorInvoiceService(tenantId),
            vendorRepository = createVendorRepository(tenantId)
        )
    }

    private val analyticsNotificationSearchDataSource = com.sucharu.sucharupro.data.datasource.FakeVendorPortalAnalyticsNotificationSearchDataSource()

    open fun createVendorPortalAnalyticsNotificationSearchRepository(tenantId: String = defaultTenantId): com.sucharu.sucharupro.domain.repository.VendorPortalAnalyticsNotificationSearchRepository {
        return com.sucharu.sucharupro.data.repository.VendorPortalAnalyticsNotificationSearchRepositoryImpl(analyticsNotificationSearchDataSource)
    }

    open fun createVendorPortalAnalyticsNotificationSearchService(tenantId: String = defaultTenantId): com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalAnalyticsNotificationSearchService {
        return com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalAnalyticsNotificationSearchServiceImpl(
            repository = createVendorPortalAnalyticsNotificationSearchRepository(tenantId),
            vendorRepository = createVendorRepository(tenantId),
            purchaseOrderRepository = createVendorPurchaseOrderRepository(tenantId),
            workOrderRepository = createVendorWorkOrderRepository(tenantId),
            deliveryRepository = createVendorPortalDeliveryRepository(tenantId),
            invoiceRepository = createVendorInvoiceRepository(tenantId),
            qualityRepository = createVendorQualityRepository(tenantId),
            portalQualityRepository = createVendorPortalQualityRepository(tenantId),
            settlementRepository = createVendorSettlementRepository(tenantId),
            portalSettlementRepository = createVendorPortalSettlementRepository(tenantId),
            performanceComplianceRepository = createVendorPortalPerformanceComplianceRepository(tenantId),
            canonicalSettlementService = createVendorSettlementService(tenantId),
            canonicalInvoiceService = createVendorInvoiceService(tenantId),
            canonicalPerformanceService = createVendorPerformanceService(tenantId),
            dashboardRepository = createVendorPortalDashboardRepository(tenantId)
        )
    }

    private val workflowDataSource = com.sucharu.sucharupro.data.datasource.FakeVendorPortalWorkflowDataSource()

    open fun createVendorPortalWorkflowRepository(tenantId: String = defaultTenantId): com.sucharu.sucharupro.domain.repository.VendorPortalWorkflowRepository {
        return com.sucharu.sucharupro.data.repository.VendorPortalWorkflowRepositoryImpl(workflowDataSource)
    }

    open fun createVendorPortalWorkflowService(tenantId: String = defaultTenantId): com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalWorkflowService {
        return com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalWorkflowServiceImpl(
            repository = createVendorPortalWorkflowRepository(tenantId),
            vendorRepository = createVendorRepository(tenantId),
            purchaseOrderRepository = createVendorPurchaseOrderRepository(tenantId),
            workOrderRepository = createVendorWorkOrderRepository(tenantId),
            deliveryRepository = createVendorPortalDeliveryRepository(tenantId),
            invoiceRepository = createVendorInvoiceRepository(tenantId),
            qualityRepository = createVendorQualityRepository(tenantId),
            portalQualityRepository = createVendorPortalQualityRepository(tenantId),
            settlementRepository = createVendorSettlementRepository(tenantId),
            portalSettlementRepository = createVendorPortalSettlementRepository(tenantId),
            notificationService = createVendorPortalAnalyticsNotificationSearchService(tenantId)
        )
    }

    open fun createCustomerFinancialAccountRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.customerfinancial.CustomerFinancialAccountRepository {
        val ds = PostgresCustomerFinancialAccountDataSource(transactionManager, tenantId)
        return com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl(ds)
    }

    open fun createCustomerFinancialAccountService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.customerfinancial.CustomerFinancialAccountService {
        return com.sucharu.sucharupro.domain.service.customerfinancial.CustomerFinancialAccountServiceImpl(
            repository = createCustomerFinancialAccountRepository(tenantId),
            customerRepository = createCustomerRepository(tenantId)
        )
    }

    open fun createCustomerInvoiceRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.customerinvoice.CustomerInvoiceRepository {
        val ds = PostgresCustomerInvoiceDataSource(transactionManager, tenantId)
        return com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl(ds)
    }

    open fun createCustomerInvoiceService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.customerinvoice.CustomerInvoiceService {
        return com.sucharu.sucharupro.domain.service.customerinvoice.CustomerInvoiceServiceImpl(
            repository = createCustomerInvoiceRepository(tenantId),
            customerRepository = createCustomerRepository(tenantId),
            accountRepository = createCustomerFinancialAccountRepository(tenantId)
        )
    }

    open fun createCustomerPaymentRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.customerpayment.CustomerPaymentRepository {
        val ds = PostgresCustomerPaymentDataSource(transactionManager, tenantId)
        return com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl(ds)
    }

    open fun createCustomerPaymentService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.customerpayment.CustomerPaymentService {
        return com.sucharu.sucharupro.domain.service.customerpayment.CustomerPaymentServiceImpl(
            paymentRepository = createCustomerPaymentRepository(tenantId),
            invoiceRepository = createCustomerInvoiceRepository(tenantId),
            customerRepository = createCustomerRepository(tenantId),
            accountRepository = createCustomerFinancialAccountRepository(tenantId)
        )
    }

    open fun createCustomerCreditRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.customercredit.CustomerCreditRepository {
        val ds = PostgresCustomerCreditDataSource(transactionManager, tenantId)
        return com.sucharu.sucharupro.data.repository.customercredit.CustomerCreditRepositoryImpl(ds)
    }

    open fun createCustomerCreditService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.customercredit.CustomerCreditService {
        return com.sucharu.sucharupro.domain.service.customercredit.CustomerCreditServiceImpl(
            creditRepository = createCustomerCreditRepository(tenantId),
            accountRepository = createCustomerFinancialAccountRepository(tenantId),
            invoiceRepository = createCustomerInvoiceRepository(tenantId),
            customerRepository = createCustomerRepository(tenantId),
            paymentRepository = createCustomerPaymentRepository(tenantId)
        )
    }

    open fun createCustomerLedgerRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.customerledger.CustomerLedgerRepository {
        val ds = PostgresCustomerLedgerDataSource(transactionManager, tenantId)
        return com.sucharu.sucharupro.data.repository.customerledger.CustomerLedgerRepositoryImpl(ds)
    }

    open fun createCustomerLedgerService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.customerledger.CustomerLedgerService {
        return com.sucharu.sucharupro.domain.service.customerledger.CustomerLedgerServiceImpl(
            ledgerRepository = createCustomerLedgerRepository(tenantId),
            accountRepository = createCustomerFinancialAccountRepository(tenantId),
            invoiceRepository = createCustomerInvoiceRepository(tenantId),
            paymentRepository = createCustomerPaymentRepository(tenantId),
            creditRepository = createCustomerCreditRepository(tenantId),
            customerRepository = createCustomerRepository(tenantId)
        )
    }

    open fun createCustomerPaymentAllocationRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.customersettlement.CustomerPaymentAllocationRepository {
        val ds = PostgresCustomerPaymentAllocationDataSource(transactionManager, tenantId)
        return com.sucharu.sucharupro.data.repository.customersettlement.CustomerPaymentAllocationRepositoryImpl(ds)
    }

    open fun createCustomerSettlementService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.customersettlement.CustomerSettlementService {
        return com.sucharu.sucharupro.domain.service.customersettlement.CustomerSettlementServiceImpl(
            allocationRepository = createCustomerPaymentAllocationRepository(tenantId),
            paymentRepository = createCustomerPaymentRepository(tenantId),
            invoiceRepository = createCustomerInvoiceRepository(tenantId),
            accountRepository = createCustomerFinancialAccountRepository(tenantId),
            creditRepository = createCustomerCreditRepository(tenantId)
        )
    }

    open fun createCustomerCreditControlRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.customercreditcontrol.CustomerCreditControlRepository {
        val ds = PostgresCustomerCreditControlDataSource(transactionManager, tenantId)
        return com.sucharu.sucharupro.data.repository.customercreditcontrol.CustomerCreditControlRepositoryImpl(ds)
    }

    open fun createCustomerCreditControlService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.customercreditcontrol.CustomerCreditControlService {
        return com.sucharu.sucharupro.domain.service.customercreditcontrol.CustomerCreditControlServiceImpl(
            creditControlRepository = createCustomerCreditControlRepository(tenantId),
            customerRepository = createCustomerRepository(tenantId),
            accountRepository = createCustomerFinancialAccountRepository(tenantId),
            settlementService = createCustomerSettlementService(tenantId),
            invoiceRepository = createCustomerInvoiceRepository(tenantId)
        )
    }

    open fun createCustomerCollectionRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.customercollection.CustomerCollectionRepository {
        val ds = PostgresCustomerCollectionDataSource(transactionManager, tenantId)
        return com.sucharu.sucharupro.data.repository.customercollection.CustomerCollectionRepositoryImpl(ds)
    }

    open fun createCustomerCollectionService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.customercollection.CustomerCollectionService {
        return com.sucharu.sucharupro.domain.service.customercollection.CustomerCollectionServiceImpl(
            collectionRepository = createCustomerCollectionRepository(tenantId),
            customerRepository = createCustomerRepository(tenantId),
            accountRepository = createCustomerFinancialAccountRepository(tenantId),
            invoiceRepository = createCustomerInvoiceRepository(tenantId),
            settlementService = createCustomerSettlementService(tenantId),
            creditControlService = createCustomerCreditControlService(tenantId)
        )
    }

    open fun createCustomerFinancialDashboardService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.customerfinancialdashboard.CustomerFinancialDashboardService {
        return com.sucharu.sucharupro.domain.service.customerfinancialdashboard.CustomerFinancialDashboardServiceImpl(
            customerRepository = createCustomerRepository(tenantId),
            accountRepository = createCustomerFinancialAccountRepository(tenantId),
            invoiceRepository = createCustomerInvoiceRepository(tenantId),
            paymentRepository = createCustomerPaymentRepository(tenantId),
            creditRepository = createCustomerCreditRepository(tenantId),
            collectionRepository = createCustomerCollectionRepository(tenantId),
            settlementService = createCustomerSettlementService(tenantId),
            creditControlService = createCustomerCreditControlService(tenantId),
            collectionService = createCustomerCollectionService(tenantId),
            ledgerService = createCustomerLedgerService(tenantId)
        )
    }

    open fun createCustomerFinancialReportingService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.customerfinancialreporting.CustomerFinancialReportingService {
        return com.sucharu.sucharupro.domain.service.customerfinancialreporting.CustomerFinancialReportingServiceImpl(
            customerRepository = createCustomerRepository(tenantId),
            accountRepository = createCustomerFinancialAccountRepository(tenantId),
            invoiceRepository = createCustomerInvoiceRepository(tenantId),
            paymentRepository = createCustomerPaymentRepository(tenantId),
            creditRepository = createCustomerCreditRepository(tenantId),
            ledgerService = createCustomerLedgerService(tenantId),
            settlementService = createCustomerSettlementService(tenantId),
            creditControlService = createCustomerCreditControlService(tenantId),
            collectionService = createCustomerCollectionService(tenantId),
            dashboardService = createCustomerFinancialDashboardService(tenantId)
        )
    }

    open fun createCustomerFinancialDocumentDeliveryRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.customerfinancialreporting.CustomerFinancialDocumentDeliveryRepository {
        val ds = PostgresCustomerFinancialDocumentDeliveryDataSource(transactionManager, tenantId)
        return com.sucharu.sucharupro.data.repository.customerfinancialreporting.CustomerFinancialDocumentDeliveryRepositoryImpl(ds)
    }

    open fun createCustomerFinancialDocumentDeliveryService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.customerfinancialreporting.CustomerFinancialDocumentDeliveryService {
        return com.sucharu.sucharupro.domain.service.customerfinancialreporting.CustomerFinancialDocumentDeliveryServiceImpl(
            deliveryRepository = createCustomerFinancialDocumentDeliveryRepository(tenantId),
            reportingService = createCustomerFinancialReportingService(tenantId),
            customerRepository = createCustomerRepository(tenantId),
            notificationRepository = null
        )
    }

    open fun createCustomerFinancialAlertRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.customerfinancialreporting.CustomerFinancialAlertRepository {
        val ds = PostgresCustomerFinancialAlertDataSource(transactionManager)
        return com.sucharu.sucharupro.data.repository.customerfinancialreporting.CustomerFinancialAlertRepositoryImpl(ds)
    }

    open fun createCustomerFinancialAlertService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.customerfinancialreporting.CustomerFinancialAlertService {
        return com.sucharu.sucharupro.domain.service.customerfinancialreporting.CustomerFinancialAlertServiceImpl(
            alertRepository = createCustomerFinancialAlertRepository(tenantId),
            customerRepository = createCustomerRepository(tenantId),
            accountRepository = createCustomerFinancialAccountRepository(tenantId),
            invoiceRepository = createCustomerInvoiceRepository(tenantId),
            paymentRepository = createCustomerPaymentRepository(tenantId),
            creditControlService = createCustomerCreditControlService(tenantId),
            collectionService = createCustomerCollectionService(tenantId),
            dashboardService = createCustomerFinancialDashboardService(tenantId),
            notificationRepository = null
        )
    }

    open fun createCustomerFinancialReportScheduleRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.customerfinancialreporting.CustomerFinancialReportScheduleRepository {
        val ds = PostgresCustomerFinancialReportScheduleDataSource(transactionManager)
        return com.sucharu.sucharupro.data.repository.customerfinancialreporting.CustomerFinancialReportScheduleRepositoryImpl(ds)
    }

    open fun createCustomerFinancialScheduleService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.customerfinancialreporting.CustomerFinancialScheduleService {
        return com.sucharu.sucharupro.domain.service.customerfinancialreporting.CustomerFinancialScheduleServiceImpl(
            scheduleRepository = createCustomerFinancialReportScheduleRepository(tenantId),
            customerRepository = createCustomerRepository(tenantId),
            documentDeliveryService = createCustomerFinancialDocumentDeliveryService(tenantId),
            alertRepository = createCustomerFinancialAlertRepository(tenantId)
        )
    }

    // --- Module 15 Step 01: Business Expense Management ---

    val businessExpenseDataSource: com.sucharu.sucharupro.data.datasource.businessexpense.BusinessExpenseDataSource by lazy {
        PostgresBusinessExpenseDataSource(transactionManager, defaultTenantId)
    }

    open fun createBusinessExpenseRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.businessexpense.BusinessExpenseRepository {
        val ds = PostgresBusinessExpenseDataSource(transactionManager, tenantId)
        return com.sucharu.sucharupro.data.repository.businessexpense.BusinessExpenseRepositoryImpl(ds)
    }

    open fun createBusinessExpenseService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.businessexpense.BusinessExpenseService {
        return com.sucharu.sucharupro.domain.service.businessexpense.BusinessExpenseServiceImpl(
            repository = createBusinessExpenseRepository(tenantId),
            defaultTenantId = tenantId
        )
    }

    // --- Module 15 Step 02: Vendor Payable & Supplier Liability Management ---

    val vendorPayableDataSource: com.sucharu.sucharupro.data.datasource.vendorpayable.VendorPayableDataSource by lazy {
        PostgresVendorPayableDataSource(transactionManager)
    }

    open fun createVendorPayableRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.vendorpayable.VendorPayableRepository {
        val ds = PostgresVendorPayableDataSource(transactionManager)
        return com.sucharu.sucharupro.data.repository.vendorpayable.VendorPayableRepositoryImpl(ds)
    }

    open fun createVendorPayableService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.vendorpayable.VendorPayableService {
        return com.sucharu.sucharupro.domain.service.vendorpayable.VendorPayableServiceImpl(
            repository = createVendorPayableRepository(tenantId),
            defaultTenantId = tenantId
        )
    }

    // --- Module 15 Step 03: Business Ledger, Financial Posting & Cost Allocation Foundation ---

    val businessLedgerDataSource: com.sucharu.sucharupro.data.datasource.businessledger.BusinessLedgerDataSource by lazy {
        PostgresBusinessLedgerDataSource(transactionManager, defaultTenantId)
    }

    open fun createBusinessLedgerRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.businessledger.BusinessLedgerRepository {
        val ds = PostgresBusinessLedgerDataSource(transactionManager, tenantId)
        return com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl(ds)
    }

    open fun createBusinessLedgerService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.businessledger.BusinessLedgerService {
        return com.sucharu.sucharupro.domain.service.businessledger.BusinessLedgerServiceImpl(
            repository = createBusinessLedgerRepository(tenantId),
            expenseRepository = createBusinessExpenseRepository(tenantId),
            payableRepository = createVendorPayableRepository(tenantId),
            defaultTenantId = tenantId
        )
    }

    // --- Module 15 Step 04: Business Cost Center, Cost Category & Job Cost Tracking ---

    val businessCostManagementDataSource: com.sucharu.sucharupro.data.datasource.businesscost.BusinessCostManagementDataSource by lazy {
        PostgresBusinessCostManagementDataSource(transactionManager)
    }

    open fun createBusinessCostManagementRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.businesscost.BusinessCostManagementRepository {
        val ds = PostgresBusinessCostManagementDataSource(transactionManager)
        return com.sucharu.sucharupro.data.repository.businesscost.BusinessCostManagementRepositoryImpl(ds)
    }

    open fun createBusinessCostManagementService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.businesscost.BusinessCostManagementService {
        return com.sucharu.sucharupro.domain.service.businesscost.BusinessCostManagementServiceImpl(
            repository = createBusinessCostManagementRepository(tenantId),
            expenseRepository = createBusinessExpenseRepository(tenantId),
            payableRepository = createVendorPayableRepository(tenantId),
            ledgerRepository = createBusinessLedgerRepository(tenantId),
            defaultTenantId = tenantId
        )
    }

    // --- Module 15 Step 05: Business Cost Commitment, Accrual & Period-End Control Foundation ---

    val businessCostControlDataSource: com.sucharu.sucharupro.data.datasource.businesscostcontrol.BusinessCostControlDataSource by lazy {
        PostgresBusinessCostControlDataSource(transactionManager)
    }

    open fun createBusinessCostControlRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.repository.businesscostcontrol.BusinessCostControlRepository {
        val ds = PostgresBusinessCostControlDataSource(transactionManager)
        return com.sucharu.sucharupro.data.repository.businesscostcontrol.BusinessCostControlRepositoryImpl(ds)
    }

    open fun createBusinessCostControlService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.businesscostcontrol.BusinessCostControlService {
        return com.sucharu.sucharupro.domain.service.businesscostcontrol.BusinessCostControlServiceImpl(
            repository = createBusinessCostControlRepository(tenantId),
            costManagementRepository = createBusinessCostManagementRepository(tenantId),
            ledgerService = createBusinessLedgerService(tenantId),
            payableRepository = createVendorPayableRepository(tenantId),
            expenseRepository = createBusinessExpenseRepository(tenantId),
            defaultTenantId = tenantId
        )
    }

    // --- Module 15 Step 06: Business Financial Reconciliation, Settlement Control & Period-End Closing Foundation ---

    val businessFinancialReconciliationDataSource: com.sucharu.sucharupro.data.datasource.businessreconciliation.BusinessFinancialReconciliationDataSource by lazy {
        PostgresBusinessFinancialReconciliationDataSource(transactionManager)
    }

    open fun createBusinessFinancialReconciliationRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.repository.businessreconciliation.BusinessFinancialReconciliationRepository {
        val ds = PostgresBusinessFinancialReconciliationDataSource(transactionManager)
        return com.sucharu.sucharupro.data.repository.businessreconciliation.BusinessFinancialReconciliationRepositoryImpl(ds)
    }

    open fun createBusinessFinancialReconciliationService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.businessreconciliation.BusinessFinancialReconciliationService {
        return com.sucharu.sucharupro.domain.service.businessreconciliation.BusinessFinancialReconciliationServiceImpl(
            repository = createBusinessFinancialReconciliationRepository(tenantId),
            expenseRepository = createBusinessExpenseRepository(tenantId),
            payableRepository = createVendorPayableRepository(tenantId),
            ledgerRepository = createBusinessLedgerRepository(tenantId),
            costManagementRepository = createBusinessCostManagementRepository(tenantId),
            costControlRepository = createBusinessCostControlRepository(tenantId),
            defaultTenantId = tenantId
        )
    }

    // --- Module 15 Step 07: Business Financial Adjustment, Refund, Write-Off & Correction Control Foundation ---

    val businessFinancialAdjustmentDataSource: com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.BusinessFinancialAdjustmentDataSource by lazy {
        PostgresBusinessFinancialAdjustmentDataSource(transactionManager)
    }

    open fun createBusinessFinancialAdjustmentRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.repository.businessfinancialadjustment.BusinessFinancialAdjustmentRepository {
        val ds = PostgresBusinessFinancialAdjustmentDataSource(transactionManager)
        return com.sucharu.sucharupro.data.repository.businessfinancialadjustment.BusinessFinancialAdjustmentRepositoryImpl(ds)
    }

    open fun createBusinessFinancialAdjustmentService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.businessfinancialadjustment.BusinessFinancialAdjustmentService {
        return com.sucharu.sucharupro.domain.service.businessfinancialadjustment.BusinessFinancialAdjustmentServiceImpl(
            repository = createBusinessFinancialAdjustmentRepository(tenantId),
            ledgerService = createBusinessLedgerService(tenantId),
            costControlService = createBusinessCostControlService(tenantId),
            expenseRepository = createBusinessExpenseRepository(tenantId),
            payableRepository = createVendorPayableRepository(tenantId),
            reconciliationRepository = createBusinessFinancialReconciliationRepository(tenantId),
            defaultTenantId = tenantId
        )
    }

    // --- Module 15 Step 08: Business Financial Reporting, Analytics & Management Intelligence Foundation ---

    val businessFinancialReportingDataSource: com.sucharu.sucharupro.data.datasource.businessfinancialreporting.BusinessFinancialReportingDataSource by lazy {
        PostgresBusinessFinancialReportingDataSource(transactionManager)
    }

    open fun createBusinessFinancialReportingRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.businessfinancialreporting.BusinessFinancialReportingRepository {
        val ds = PostgresBusinessFinancialReportingDataSource(transactionManager)
        return com.sucharu.sucharupro.data.repository.businessfinancialreporting.BusinessFinancialReportingRepositoryImpl(ds)
    }

    open fun createBusinessFinancialReportingService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.businessfinancialreporting.BusinessFinancialReportingService {
        return com.sucharu.sucharupro.domain.service.businessfinancialreporting.BusinessFinancialReportingServiceImpl(
            reportingRepository = createBusinessFinancialReportingRepository(tenantId),
            expenseRepository = createBusinessExpenseRepository(tenantId),
            payableRepository = createVendorPayableRepository(tenantId),
            ledgerRepository = createBusinessLedgerRepository(tenantId),
            costManagementRepository = createBusinessCostManagementRepository(tenantId),
            costControlRepository = createBusinessCostControlRepository(tenantId),
            reconciliationRepository = createBusinessFinancialReconciliationRepository(tenantId),
            adjustmentRepository = createBusinessFinancialAdjustmentRepository(tenantId),
            orderRepository = createOrderRepository(tenantId),
            defaultTenantId = tenantId
        )
    }

    // --- Module 15 Step 09: Business Financial Governance, Budget Control & Forecast ---

    val businessFinancialGovernanceDataSource: com.sucharu.sucharupro.data.datasource.businessfinancialgovernance.BusinessFinancialGovernanceDataSource by lazy {
        PostgresBusinessFinancialGovernanceDataSource(transactionManager)
    }

    open fun createBusinessFinancialGovernanceRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.businessfinancialgovernance.BusinessFinancialGovernanceRepository {
        val ds = PostgresBusinessFinancialGovernanceDataSource(transactionManager)
        return com.sucharu.sucharupro.data.repository.businessfinancialgovernance.BusinessFinancialGovernanceRepositoryImpl(ds)
    }

    open fun createBusinessFinancialGovernanceService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.businessfinancialgovernance.BusinessFinancialGovernanceService {
        return com.sucharu.sucharupro.domain.service.businessfinancialgovernance.BusinessFinancialGovernanceServiceImpl(
            governanceRepository = createBusinessFinancialGovernanceRepository(tenantId),
            expenseRepository = createBusinessExpenseRepository(tenantId),
            payableRepository = createVendorPayableRepository(tenantId),
            ledgerRepository = createBusinessLedgerRepository(tenantId),
            costManagementRepository = createBusinessCostManagementRepository(tenantId),
            costControlRepository = createBusinessCostControlRepository(tenantId),
            reconciliationRepository = createBusinessFinancialReconciliationRepository(tenantId),
            adjustmentRepository = createBusinessFinancialAdjustmentRepository(tenantId),
            defaultTenantId = tenantId
        )
    }

    // --- Module 15 Step 10: Business Financial Governance, Audit, Reconciliation & Final Integrity Control ---

    val businessFinancialIntegrityDataSource: com.sucharu.sucharupro.data.datasource.businessintegrity.BusinessFinancialIntegrityDataSource by lazy {
        PostgresBusinessFinancialIntegrityDataSource(transactionManager)
    }

    open fun createBusinessFinancialIntegrityRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.businessintegrity.BusinessFinancialIntegrityRepository {
        val ds = PostgresBusinessFinancialIntegrityDataSource(transactionManager)
        return com.sucharu.sucharupro.data.repository.businessintegrity.BusinessFinancialIntegrityRepositoryImpl(ds)
    }

    open fun createBusinessFinancialIntegrityService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.businessintegrity.BusinessFinancialIntegrityService {
        return com.sucharu.sucharupro.domain.service.businessintegrity.BusinessFinancialIntegrityServiceImpl(
            integrityRepository = createBusinessFinancialIntegrityRepository(tenantId),
            expenseRepository = createBusinessExpenseRepository(tenantId),
            payableRepository = createVendorPayableRepository(tenantId),
            ledgerRepository = createBusinessLedgerRepository(tenantId),
            costManagementRepository = createBusinessCostManagementRepository(tenantId),
            costControlRepository = createBusinessCostControlRepository(tenantId),
            reconciliationRepository = createBusinessFinancialReconciliationRepository(tenantId),
            adjustmentRepository = createBusinessFinancialAdjustmentRepository(tenantId),
            governanceRepository = createBusinessFinancialGovernanceRepository(tenantId),
            defaultTenantId = tenantId
        )
    }

    open fun createProfitabilityRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.repository.profitability.ProfitabilityRepository {
        val ds = PostgresProfitabilityDataSource(transactionManager)
        return com.sucharu.sucharupro.data.repository.profitability.ProfitabilityRepositoryImpl(ds)
    }

    open fun createProfitabilityHandoffAdapter(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.Module16FinancialHandoffAdapter {
        val integrityService = createBusinessFinancialIntegrityService(tenantId)
        return com.sucharu.sucharupro.domain.service.profitability.Module16FinancialHandoffAdapterImpl(integrityService)
    }

    open fun createProfitabilitySourceRegistry(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.ProfitabilitySourceRegistry {
        val handoffAdapter = createProfitabilityHandoffAdapter(tenantId)
        return com.sucharu.sucharupro.domain.service.profitability.ProfitabilitySourceRegistryImpl(handoffAdapter)
    }

    open fun createProfitabilityReconciliationService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.ProfitabilityReconciliationService {
        val handoffAdapter = createProfitabilityHandoffAdapter(tenantId)
        val sourceRegistry = createProfitabilitySourceRegistry(tenantId)
        return com.sucharu.sucharupro.domain.service.profitability.ProfitabilityReconciliationServiceImpl(handoffAdapter, sourceRegistry)
    }

    open fun createProfitabilityFoundationService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.ProfitabilityFoundationService {
        val repo = createProfitabilityRepository(tenantId)
        val handoffAdapter = createProfitabilityHandoffAdapter(tenantId)
        val sourceRegistry = createProfitabilitySourceRegistry(tenantId)
        val reconService = createProfitabilityReconciliationService(tenantId)
        return com.sucharu.sucharupro.domain.service.profitability.ProfitabilityFoundationServiceImpl(
            repository = repo,
            handoffAdapter = handoffAdapter,
            sourceRegistry = sourceRegistry,
            reconciliationService = reconService
        )
    }

    open fun createJobCostRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.repository.profitability.JobCostRepository {
        val ds = PostgresJobCostDataSource(transactionManager)
        return com.sucharu.sucharupro.data.repository.profitability.JobCostRepositoryImpl(ds)
    }

    open fun createJobCostSourceCollector(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.JobCostSourceCollector {
        return com.sucharu.sucharupro.domain.service.profitability.JobCostSourceCollectorImpl()
    }

    open fun createJobCostReconciliationService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.JobCostReconciliationService {
        return com.sucharu.sucharupro.domain.service.profitability.JobCostReconciliationServiceImpl()
    }

    open fun createJobCostCalculationService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.JobCostCalculationService {
        val repo = createJobCostRepository(tenantId)
        val collector = createJobCostSourceCollector(tenantId)
        val reconService = createJobCostReconciliationService(tenantId)
        return com.sucharu.sucharupro.domain.service.profitability.JobCostCalculationServiceImpl(
            repository = repo,
            sourceCollector = collector,
            reconciliationService = reconService
        )
    }

    open fun createProductProfitabilityRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.repository.profitability.ProductProfitabilityRepository {
        val ds = PostgresProductProfitabilityDataSource(transactionManager)
        return com.sucharu.sucharupro.data.repository.profitability.ProductProfitabilityRepositoryImpl(ds)
    }

    open fun createProductProfitabilitySourceCollector(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.ProductProfitabilitySourceCollector {
        return com.sucharu.sucharupro.domain.service.profitability.ProductProfitabilitySourceCollectorImpl()
    }

    open fun createProductProfitabilityReconciliationService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.ProductProfitabilityReconciliationService {
        return com.sucharu.sucharupro.domain.service.profitability.ProductProfitabilityReconciliationServiceImpl()
    }

    open fun createProductProfitabilityService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.ProductProfitabilityService {
        val repo = createProductProfitabilityRepository(tenantId)
        val collector = createProductProfitabilitySourceCollector(tenantId)
        val reconService = createProductProfitabilityReconciliationService(tenantId)
        return com.sucharu.sucharupro.domain.service.profitability.ProductProfitabilityServiceImpl(
            repository = repo,
            sourceCollector = collector,
            reconciliationService = reconService
        )
    }

    open fun createCustomerProfitabilityRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.repository.profitability.CustomerProfitabilityRepository {
        val ds = PostgresCustomerProfitabilityDataSource(transactionManager)
        return com.sucharu.sucharupro.data.repository.profitability.CustomerProfitabilityRepositoryImpl(ds)
    }

    open fun createCustomerProfitabilitySourceCollector(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.CustomerProfitabilitySourceCollector {
        return com.sucharu.sucharupro.domain.service.profitability.CustomerProfitabilitySourceCollectorImpl()
    }

    open fun createCustomerProfitabilityReconciliationService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.CustomerProfitabilityReconciliationService {
        return com.sucharu.sucharupro.domain.service.profitability.CustomerProfitabilityReconciliationServiceImpl()
    }

    open fun createCustomerProfitabilityRankingService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.CustomerProfitabilityRankingService {
        return com.sucharu.sucharupro.domain.service.profitability.CustomerProfitabilityRankingServiceImpl()
    }

    open fun createCustomerProfitabilityService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.CustomerProfitabilityService {
        val repo = createCustomerProfitabilityRepository(tenantId)
        val collector = createCustomerProfitabilitySourceCollector(tenantId)
        val reconService = createCustomerProfitabilityReconciliationService(tenantId)
        val rankingService = createCustomerProfitabilityRankingService(tenantId)
        return com.sucharu.sucharupro.domain.service.profitability.CustomerProfitabilityServiceImpl(
            repository = repo,
            sourceCollector = collector,
            reconciliationService = reconService,
            rankingService = rankingService
        )
    }

    open fun createVendorProfitabilityRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.repository.profitability.VendorProfitabilityRepository {
        val ds = PostgresVendorProfitabilityDataSource(transactionManager)
        return com.sucharu.sucharupro.data.repository.profitability.VendorProfitabilityRepositoryImpl(ds)
    }

    open fun createVendorProfitabilitySourceCollector(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.VendorProfitabilitySourceCollector {
        return com.sucharu.sucharupro.domain.service.profitability.VendorProfitabilitySourceCollectorImpl()
    }

    open fun createVendorProfitabilityReconciliationService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.VendorProfitabilityReconciliationService {
        return com.sucharu.sucharupro.domain.service.profitability.VendorProfitabilityReconciliationServiceImpl()
    }

    open fun createVendorProfitabilityRankingService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.VendorProfitabilityRankingService {
        return com.sucharu.sucharupro.domain.service.profitability.VendorProfitabilityRankingServiceImpl()
    }

    open fun createVendorProfitabilityService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.VendorProfitabilityService {
        val repo = createVendorProfitabilityRepository(tenantId)
        val collector = createVendorProfitabilitySourceCollector(tenantId)
        val reconService = createVendorProfitabilityReconciliationService(tenantId)
        val rankingService = createVendorProfitabilityRankingService(tenantId)
        return com.sucharu.sucharupro.domain.service.profitability.VendorProfitabilityServiceImpl(
            repository = repo,
            sourceCollector = collector,
            reconciliationService = reconService,
            rankingService = rankingService
        )
    }

    open fun createPeriodProfitabilityRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.repository.profitability.PeriodProfitabilityRepository {
        val ds = PostgresPeriodProfitabilityDataSource(transactionManager)
        return com.sucharu.sucharupro.data.repository.profitability.PeriodProfitabilityRepositoryImpl(ds)
    }

    open fun createPeriodProfitabilitySourceCollector(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.PeriodProfitabilitySourceCollector {
        return com.sucharu.sucharupro.domain.service.profitability.PeriodProfitabilitySourceCollectorImpl()
    }

    open fun createPeriodProfitabilityReconciliationService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.PeriodProfitabilityReconciliationService {
        return com.sucharu.sucharupro.domain.service.profitability.PeriodProfitabilityReconciliationServiceImpl()
    }

    open fun createPeriodProfitabilityTrendService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.PeriodProfitabilityTrendService {
        return com.sucharu.sucharupro.domain.service.profitability.PeriodProfitabilityTrendServiceImpl()
    }

    open fun createPeriodProfitabilityRankingService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.PeriodProfitabilityRankingService {
        return com.sucharu.sucharupro.domain.service.profitability.PeriodProfitabilityRankingServiceImpl()
    }

    open fun createPeriodProfitabilityService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.PeriodProfitabilityService {
        val repo = createPeriodProfitabilityRepository(tenantId)
        val collector = createPeriodProfitabilitySourceCollector(tenantId)
        val reconService = createPeriodProfitabilityReconciliationService(tenantId)
        val trendService = createPeriodProfitabilityTrendService(tenantId)
        val rankingService = createPeriodProfitabilityRankingService(tenantId)
        return com.sucharu.sucharupro.domain.service.profitability.PeriodProfitabilityServiceImpl(
            repository = repo,
            sourceCollector = collector,
            reconciliationService = reconService,
            trendService = trendService,
            rankingService = rankingService
        )
    }

    // =========================================================================
    // MODULE 16 STEP 07: CROSS-DIMENSIONAL PROFITABILITY INTELLIGENCE FACTORIES
    // =========================================================================

    open fun createProfitabilityIntelligenceDataSource(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.datasource.profitability.ProfitabilityIntelligenceDataSource {
        return com.sucharu.sucharupro.data.persistence.postgres.PostgresProfitabilityIntelligenceDataSource(transactionManager)
    }


    open fun createProfitabilityIntelligenceRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.repository.profitability.ProfitabilityIntelligenceRepository {
        val ds = createProfitabilityIntelligenceDataSource(tenantId)
        return com.sucharu.sucharupro.data.repository.profitability.ProfitabilityIntelligenceRepositoryImpl(ds)
    }

    open fun createProfitabilityIntelligenceSourceCollector(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.ProfitabilityIntelligenceSourceCollector {
        val periodRepo = createPeriodProfitabilityRepository(tenantId)
        val custRepo = createCustomerProfitabilityRepository(tenantId)
        val prodRepo = createProductProfitabilityRepository(tenantId)
        val venRepo = createVendorProfitabilityRepository(tenantId)
        val jobRepo = createJobCostRepository(tenantId)
        return com.sucharu.sucharupro.domain.service.profitability.ProfitabilityIntelligenceSourceCollectorImpl(
            periodRepo = periodRepo,
            customerRepo = custRepo,
            productRepo = prodRepo,
            vendorRepo = venRepo,
            jobCostRepo = jobRepo
        )
    }

    open fun createProfitabilityIntelligenceService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.ProfitabilityIntelligenceService {
        val repo = createProfitabilityIntelligenceRepository(tenantId)
        val collector = createProfitabilityIntelligenceSourceCollector(tenantId)
        return com.sucharu.sucharupro.domain.service.profitability.ProfitabilityIntelligenceServiceImpl(
            repository = repo,
            sourceCollector = collector
        )
    }

    // =========================================================================
    // MODULE 16 STEP 08: PROFITABILITY FORECASTING & SCENARIO FACTORIES
    // =========================================================================

    open fun createProfitabilityForecastDataSource(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.datasource.profitability.ProfitabilityForecastDataSource {
        return com.sucharu.sucharupro.data.persistence.postgres.PostgresProfitabilityForecastDataSource(transactionManager)
    }

    open fun createProfitabilityForecastRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.repository.profitability.ProfitabilityForecastRepository {
        val ds = createProfitabilityForecastDataSource(tenantId)
        return com.sucharu.sucharupro.data.repository.profitability.ProfitabilityForecastRepositoryImpl(ds)
    }

    open fun createProfitabilityForecastSourceCollector(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.ProfitabilityForecastSourceCollector {
        val periodRepo = createPeriodProfitabilityRepository(tenantId)
        val custRepo = createCustomerProfitabilityRepository(tenantId)
        val prodRepo = createProductProfitabilityRepository(tenantId)
        val venRepo = createVendorProfitabilityRepository(tenantId)
        val jobRepo = createJobCostRepository(tenantId)
        return com.sucharu.sucharupro.domain.service.profitability.ProfitabilityForecastSourceCollectorImpl(
            periodRepo = periodRepo,
            customerRepo = custRepo,
            productRepo = prodRepo,
            vendorRepo = venRepo,
            jobCostRepo = jobRepo
        )
    }

    open fun createProfitabilityForecastService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.ProfitabilityForecastService {
        val repo = createProfitabilityForecastRepository(tenantId)
        val collector = createProfitabilityForecastSourceCollector(tenantId)
        return com.sucharu.sucharupro.domain.service.profitability.ProfitabilityForecastServiceImpl(
            repository = repo,
            sourceCollector = collector
        )
    }

    open fun createProfitabilityAlertDataSource(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.datasource.profitability.ProfitabilityAlertDataSource {
        return PostgresProfitabilityAlertDataSource(transactionManager)
    }

    open fun createProfitabilityAlertRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.repository.profitability.ProfitabilityAlertRepository {
        val ds = createProfitabilityAlertDataSource(tenantId)
        return com.sucharu.sucharupro.data.repository.profitability.ProfitabilityAlertRepositoryImpl(ds)
    }

    open fun createProfitabilityAlertSourceCollector(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.ProfitabilityAlertSourceCollector {
        val fcService = createProfitabilityForecastService(tenantId)
        return com.sucharu.sucharupro.domain.service.profitability.ProfitabilityAlertSourceCollectorImpl(
            forecastService = fcService
        )
    }

    open fun createProfitabilityAlertService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.ProfitabilityAlertService {
        val repo = createProfitabilityAlertRepository(tenantId)
        val collector = createProfitabilityAlertSourceCollector(tenantId)
        return com.sucharu.sucharupro.domain.service.profitability.ProfitabilityAlertServiceImpl(
            repository = repo,
            sourceCollector = collector
        )
    }

    open fun createExecutiveProfitabilityDataSource(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.datasource.profitability.ExecutiveProfitabilityDataSource {
        return PostgresExecutiveProfitabilityDataSource(transactionManager)
    }

    open fun createExecutiveProfitabilityRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.repository.profitability.ExecutiveProfitabilityRepository {
        val ds = createExecutiveProfitabilityDataSource(tenantId)
        return com.sucharu.sucharupro.data.repository.profitability.ExecutiveProfitabilityRepositoryImpl(ds)
    }

    open fun createExecutiveProfitabilitySourceCollector(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.ExecutiveProfitabilitySourceCollector {
        val alertService = createProfitabilityAlertService(tenantId)
        val fcService = createProfitabilityForecastService(tenantId)
        return com.sucharu.sucharupro.domain.service.profitability.ExecutiveProfitabilitySourceCollectorImpl(
            alertService = alertService,
            forecastService = fcService
        )
    }

    open fun createExecutiveProfitabilityService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.profitability.ExecutiveProfitabilityService {
        val repo = createExecutiveProfitabilityRepository(tenantId)
        val collector = createExecutiveProfitabilitySourceCollector(tenantId)
        return com.sucharu.sucharupro.domain.service.profitability.ExecutiveProfitabilityServiceImpl(
            repository = repo,
            sourceCollector = collector
        )
    }

    open fun createPrintingCalculatorDataSource(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.datasource.printingcalculator.PrintingCalculatorDataSource {
        return PostgresPrintingCalculatorDataSource(transactionManager)
    }

    open fun createPrintingCalculatorRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.printingcalculator.PrintingCalculatorRepository {
        val ds = createPrintingCalculatorDataSource(tenantId)
        return com.sucharu.sucharupro.data.repository.printingcalculator.PrintingCalculatorRepositoryImpl(ds)
    }

    open fun createPrintingCalculatorService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.printingcalculator.PrintingCalculatorService {
        val repo = createPrintingCalculatorRepository(tenantId)
        return com.sucharu.sucharupro.domain.service.printingcalculator.PrintingCalculatorServiceImpl(
            repository = repo
        )
    }

    // ─────────────────────────────────────────────────────────────
    // MODULE 17 STEP 02 — SMART PRINTING CALCULATOR QUOTATION
    // ─────────────────────────────────────────────────────────────

    open fun createPrintingQuoteDataSource(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.datasource.printingquote.PrintingQuoteDataSource {
        return PostgresPrintingQuoteDataSource(transactionManager)
    }

    open fun createPrintingQuoteRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.printingquote.PrintingQuoteRepository {
        val ds = createPrintingQuoteDataSource(tenantId)
        return com.sucharu.sucharupro.data.repository.printingquote.PrintingQuoteRepositoryImpl(ds)
    }

    open fun createPrintingQuoteService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.printingquote.PrintingQuoteService {
        val repo = createPrintingQuoteRepository(tenantId)
        return com.sucharu.sucharupro.domain.service.printingquote.PrintingQuoteServiceImpl(
            repository = repo
        )
    }

    // ─────────────────────────────────────────────────────────────
    // MODULE 17 STEP 03 — COMMERCIAL COMMITMENT & ORDER CONVERSION
    // ─────────────────────────────────────────────────────────────

    open fun createCommercialCommitmentDataSource(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.datasource.commercialcommitment.CommercialCommitmentDataSource {
        return PostgresCommercialCommitmentDataSource(transactionManager)
    }

    open fun createCommercialCommitmentRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.commercialcommitment.CommercialCommitmentRepository {
        val ds = createCommercialCommitmentDataSource(tenantId)
        return com.sucharu.sucharupro.data.repository.commercialcommitment.CommercialCommitmentRepositoryImpl(ds)
    }

    open fun createCommercialCommitmentService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.commercialcommitment.CommercialCommitmentService {
        val commitmentRepo = createCommercialCommitmentRepository(tenantId)
        val quoteRepo = createPrintingQuoteRepository(tenantId)
        val orderRepo = createOrderRepository(tenantId)
        return com.sucharu.sucharupro.domain.service.commercialcommitment.CommercialCommitmentServiceImpl(
            commitmentRepository = commitmentRepo,
            quoteRepository = quoteRepo,
            orderRepository = orderRepo
        )
    }

    // ─────────────────────────────────────────────────────────────
    // MODULE 17 STEP 04 — PRODUCTION PLANNING & READINESS
    // ─────────────────────────────────────────────────────────────

    open fun createProductionPlanningDataSource(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.datasource.productionplanning.ProductionPlanningDataSource {
        return PostgresProductionPlanningDataSource(transactionManager)
    }

    open fun createProductionPlanningRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.productionplanning.ProductionPlanningRepository {
        val ds = createProductionPlanningDataSource(tenantId)
        return com.sucharu.sucharupro.data.repository.productionplanning.ProductionPlanningRepositoryImpl(ds)
    }

    open fun createProductionPlanningService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.productionplanning.ProductionPlanningService {
        val planningRepo = createProductionPlanningRepository(tenantId)
        val orderRepo = createOrderRepository(tenantId)
        val commitmentRepo = createCommercialCommitmentRepository(tenantId)
        val quoteRepo = createPrintingQuoteRepository(tenantId)
        return com.sucharu.sucharupro.domain.service.productionplanning.ProductionPlanningServiceImpl(
            planningRepository = planningRepo,
            orderRepository = orderRepo,
            commitmentRepository = commitmentRepo,
            quoteRepository = quoteRepo
        )
    }

    // ─────────────────────────────────────────────────────────────
    // MODULE 17 STEP 05 — PRODUCTION JOB EXECUTION & WORK ORDERS
    // ─────────────────────────────────────────────────────────────

    open fun createProductionExecutionDataSource(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.datasource.productionexecution.ProductionExecutionDataSource {
        return PostgresProductionExecutionDataSource(transactionManager)
    }

    open fun createProductionExecutionRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.productionexecution.ProductionExecutionRepository {
        val ds = createProductionExecutionDataSource(tenantId)
        return com.sucharu.sucharupro.data.repository.productionexecution.ProductionExecutionRepositoryImpl(ds)
    }

    open fun createProductionExecutionService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.productionexecution.ProductionExecutionService {
        val executionRepo = createProductionExecutionRepository(tenantId)
        val orderRepo = createOrderRepository(tenantId)
        val planningRepo = createProductionPlanningRepository(tenantId)
        val commitmentRepo = createCommercialCommitmentRepository(tenantId)
        val quoteRepo = createPrintingQuoteRepository(tenantId)
        return com.sucharu.sucharupro.domain.service.productionexecution.ProductionExecutionServiceImpl(
            executionRepository = executionRepo,
            orderRepository = orderRepo,
            planningRepository = planningRepo,
            commitmentRepository = commitmentRepo,
            quoteRepository = quoteRepo
        )
    }

    // ─────────────────────────────────────────────────────────────
    // MODULE 17 STEP 06 — PRODUCTION SCHEDULING & DISPATCH ENGINE
    // ─────────────────────────────────────────────────────────────

    open fun createProductionSchedulingDataSource(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.datasource.productionscheduling.ProductionSchedulingDataSource {
        return PostgresProductionSchedulingDataSource(transactionManager)
    }

    open fun createProductionSchedulingRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.productionscheduling.ProductionSchedulingRepository {
        val ds = createProductionSchedulingDataSource(tenantId)
        return com.sucharu.sucharupro.data.repository.productionscheduling.ProductionSchedulingRepositoryImpl(ds)
    }

    open fun createProductionSchedulingService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.productionscheduling.ProductionSchedulingService {
        val schedulingRepo = createProductionSchedulingRepository(tenantId)
        val executionRepo = createProductionExecutionRepository(tenantId)
        val orderRepo = createOrderRepository(tenantId)
        val planningRepo = createProductionPlanningRepository(tenantId)
        return com.sucharu.sucharupro.domain.service.productionscheduling.ProductionSchedulingServiceImpl(
            schedulingRepository = schedulingRepo,
            executionRepository = executionRepo,
            orderRepository = orderRepo,
            planningRepository = planningRepo
        )
    }

    // ─────────────────────────────────────────────────────────────
    // MODULE 17 STEP 07 — SHOP-FLOOR LIVE TRACKING & TELEMETRY ENGINE
    // ─────────────────────────────────────────────────────────────

    open fun createShopFloorTrackingDataSource(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.datasource.shopfloortracking.ShopFloorTrackingDataSource {
        return PostgresShopFloorTrackingDataSource(transactionManager)
    }

    open fun createShopFloorTrackingRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.shopfloortracking.ShopFloorTrackingRepository {
        val ds = createShopFloorTrackingDataSource(tenantId)
        return com.sucharu.sucharupro.data.repository.shopfloortracking.ShopFloorTrackingRepositoryImpl(ds)
    }

    open fun createShopFloorTrackingService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.shopfloortracking.ShopFloorTrackingService {
        val trackingRepo = createShopFloorTrackingRepository(tenantId)
        return com.sucharu.sucharupro.domain.service.shopfloortracking.ShopFloorTrackingServiceImpl(
            repository = trackingRepo
        )
    }

    // ─────────────────────────────────────────────────────────────
    // MODULE 17 STEP 08 — FINAL QUALITY CONTROL, DEFECT CONTAINMENT & PACKAGING RELEASE ENGINE
    // ─────────────────────────────────────────────────────────────

    open fun createFinalQcPackagingDataSource(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.datasource.finalqc.FinalQcPackagingDataSource {
        return PostgresFinalQcPackagingDataSource(transactionManager)
    }

    open fun createFinalQcPackagingRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.finalqc.FinalQcPackagingRepository {
        val ds = createFinalQcPackagingDataSource(tenantId)
        return com.sucharu.sucharupro.data.repository.finalqc.FinalQcPackagingRepositoryImpl(ds)
    }

    open fun createFinalQcPackagingService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.finalqc.FinalQcPackagingService {
        val repo = createFinalQcPackagingRepository(tenantId)
        return com.sucharu.sucharupro.domain.service.finalqc.FinalQcPackagingServiceImpl(
            repository = repo
        )
    }

    // ─────────────────────────────────────────────────────────────
    // MODULE 17 STEP 09 — PRODUCTION ACTUAL JOB COSTING, VARIANCE ANALYSIS & RECONCILIATION ENGINE
    // ─────────────────────────────────────────────────────────────

    open fun createProductionJobCostingDataSource(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.datasource.jobcosting.ProductionJobCostingDataSource {
        return PostgresProductionJobCostingDataSource(transactionManager)
    }

    open fun createProductionJobCostingRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.jobcosting.ProductionJobCostingRepository {
        val ds = createProductionJobCostingDataSource(tenantId)
        return com.sucharu.sucharupro.data.repository.jobcosting.ProductionJobCostingRepositoryImpl(ds)
    }

    open fun createProductionJobCostingService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.jobcosting.ProductionJobCostingService {
        val repo = createProductionJobCostingRepository(tenantId)
        return com.sucharu.sucharupro.domain.service.jobcosting.ProductionJobCostingServiceImpl(
            repository = repo
        )
    }

    // ─────────────────────────────────────────────────────────────
    // MODULE 17 STEP 10 — PRODUCTION JOB CLOSURE, ARCHIVAL & ENTERPRISE GOVERNANCE ENGINE
    // ─────────────────────────────────────────────────────────────

    open fun createProductionJobClosureDataSource(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.datasource.jobclosure.ProductionJobClosureDataSource {
        return PostgresProductionJobClosureDataSource(transactionManager)
    }

    open fun createProductionJobClosureRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.jobclosure.ProductionJobClosureRepository {
        val ds = createProductionJobClosureDataSource(tenantId)
        return com.sucharu.sucharupro.data.repository.jobclosure.ProductionJobClosureRepositoryImpl(ds)
    }

    open fun createProductionJobClosureService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.jobclosure.ProductionJobClosureService {
        val repo = createProductionJobClosureRepository(tenantId)
        return com.sucharu.sucharupro.domain.service.jobclosure.ProductionJobClosureServiceImpl(
            repository = repo
        )
    }

    // --- Module 19: Substrate Stock Auto-Reservation ---

    open fun createSubstrateReservationDataSource(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.datasource.substratereservation.SubstrateReservationDataSource {
        return PostgresSubstrateReservationDataSource(transactionManager)
    }

    open fun createSubstrateReservationRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.substratereservation.SubstrateReservationRepository {
        val dataSource = createSubstrateReservationDataSource(tenantId)
        return com.sucharu.sucharupro.data.repository.substratereservation.SubstrateReservationRepositoryImpl(dataSource)
    }

    open fun createSubstrateReservationService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.substratereservation.SubstrateReservationService {
        val repo = createSubstrateReservationRepository(tenantId)
        return com.sucharu.sucharupro.domain.service.substratereservation.SubstrateReservationServiceImpl(
            repository = repo
        )
    }

    open fun createSubstrateBatchSelectionDataSource(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.datasource.substratereservation.SubstrateBatchSelectionDataSource {
        return PostgresSubstrateBatchSelectionDataSource(transactionManager)
    }

    open fun createSubstrateBatchSelectionRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.substratereservation.SubstrateBatchSelectionRepository {
        val dataSource = createSubstrateBatchSelectionDataSource(tenantId)
        return com.sucharu.sucharupro.data.repository.substratereservation.SubstrateBatchSelectionRepositoryImpl(dataSource)
    }

    open fun createSubstrateBatchSelectionService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.substratereservation.SubstrateBatchSelectionService {
        val repo = createSubstrateBatchSelectionRepository(tenantId)
        return com.sucharu.sucharupro.domain.service.substratereservation.SubstrateBatchSelectionServiceImpl(
            repository = repo
        )
    }

    // --- Module 18: Advanced Dynamic Imposition & Gang-Run Optimizer Engine ---

    open fun createImpositionDataSource(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.datasource.imposition.ImpositionDataSource {
        return PostgresImpositionDataSource(transactionManager)
    }

    open fun createImpositionRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.imposition.ImpositionRepository {
        val dataSource = createImpositionDataSource(tenantId)
        return com.sucharu.sucharupro.data.repository.imposition.ImpositionRepositoryImpl(dataSource)
    }

    open fun createImpositionService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.imposition.ImpositionService {
        val repo = createImpositionRepository(tenantId)
        return com.sucharu.sucharupro.domain.service.imposition.ImpositionServiceImpl(
            repository = repo
        )
    }

    open fun createGangRunDataSource(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.datasource.imposition.GangRunDataSource {
        return PostgresGangRunDataSource(transactionManager)
    }

    open fun createGangRunRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.imposition.GangRunRepository {
        val dataSource = createGangRunDataSource(tenantId)
        return com.sucharu.sucharupro.data.repository.imposition.GangRunRepositoryImpl(dataSource)
    }

    open fun createGangRunService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.imposition.GangRunService {
        val repo = createGangRunRepository(tenantId)
        return com.sucharu.sucharupro.domain.service.imposition.GangRunServiceImpl(
            repository = repo
        )
    }

    open fun createDynamicNestingDataSource(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.datasource.imposition.DynamicNestingDataSource {
        return PostgresDynamicNestingDataSource(transactionManager)
    }

    open fun createDynamicNestingRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.imposition.DynamicNestingRepository {
        val dataSource = createDynamicNestingDataSource(tenantId)
        return com.sucharu.sucharupro.data.repository.imposition.DynamicNestingRepositoryImpl(dataSource)
    }

    open fun createDynamicNestingService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.imposition.DynamicNestingService {
        val repo = createDynamicNestingRepository(tenantId)
        return com.sucharu.sucharupro.domain.service.imposition.DynamicNestingServiceImpl(
            repository = repo
        )
    }

    open fun createSignatureImpositionDataSource(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.datasource.imposition.SignatureImpositionDataSource {
        return PostgresSignatureImpositionDataSource(transactionManager)
    }

    open fun createSignatureImpositionRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.imposition.SignatureImpositionRepository {
        val dataSource = createSignatureImpositionDataSource(tenantId)
        return com.sucharu.sucharupro.data.repository.imposition.SignatureImpositionRepositoryImpl(dataSource)
    }

    open fun createSignatureImpositionService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.imposition.SignatureImpositionService {
        val repo = createSignatureImpositionRepository(tenantId)
        return com.sucharu.sucharupro.domain.service.imposition.SignatureImpositionServiceImpl(
            repository = repo
        )
    }

    open fun createCtpOutputDataSource(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.datasource.imposition.CtpOutputDataSource {
        return PostgresCtpOutputDataSource(transactionManager)
    }

    open fun createCtpOutputRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.imposition.CtpOutputRepository {
        val dataSource = createCtpOutputDataSource(tenantId)
        return com.sucharu.sucharupro.data.repository.imposition.CtpOutputRepositoryImpl(dataSource)
    }

    open fun createCtpOutputService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.imposition.CtpOutputService {
        val repo = createCtpOutputRepository(tenantId)
        val sigRepo = createSignatureImpositionRepository(tenantId)
        val impRepo = createImpositionRepository(tenantId)
        return com.sucharu.sucharupro.domain.service.imposition.CtpOutputServiceImpl(
            ctpOutputRepository = repo,
            signatureImpositionRepository = sigRepo,
            impositionRepository = impRepo
        )
    }

    open fun createPrepressOrchestrationDataSource(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.data.datasource.imposition.PrepressOrchestrationDataSource {
        return PostgresPrepressOrchestrationDataSource(transactionManager, tenantId)
    }

    open fun createPrepressOrchestrationRepository(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.repository.imposition.PrepressOrchestrationRepository {
        val dataSource = createPrepressOrchestrationDataSource(tenantId)
        return com.sucharu.sucharupro.data.repository.imposition.PrepressOrchestrationRepositoryImpl(dataSource)
    }

    open fun createPrepressOrchestrationService(
        tenantId: String = defaultTenantId
    ): com.sucharu.sucharupro.domain.service.imposition.PrepressOrchestrationService {
        val planRepo = createPrepressOrchestrationRepository(tenantId)
        val impRepo = createImpositionRepository(tenantId)
        val gangRepo = createGangRunRepository(tenantId)
        val nestRepo = createDynamicNestingRepository(tenantId)
        val sigRepo = createSignatureImpositionRepository(tenantId)
        val ctpRepo = createCtpOutputRepository(tenantId)
        return com.sucharu.sucharupro.domain.service.imposition.PrepressOrchestrationServiceImpl(
            orchestrationRepository = planRepo,
            impositionRepository = impRepo,
            gangRunRepository = gangRepo,
            nestingRepository = nestRepo,
            signatureRepository = sigRepo,
            ctpOutputRepository = ctpRepo
        )
    }
}














