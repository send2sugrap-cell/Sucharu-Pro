package com.sucharu.sucharupro.domain.model.erp

enum class CustomerActionType {
    VIEW_PRODUCT,
    SELECT_QUANTITY,
    APPLY_OFFER,
    GET_QUOTE,
    ORDER_NOW,
    REQUEST_CUSTOM_QUOTE
}

enum class ErpWorkflowStatus {
    INITIATED,
    QUOTATION_CREATED,
    CUSTOMER_APPROVED,
    ORDER_CONFIRMED,
    PRODUCTION_HANDOFF,
    IN_PRODUCTION,
    QC_PASSED,
    STOCK_ALLOCATED,
    DISPATCHED,
    INVOICED,
    COMPLETED,
    FAILED
}
