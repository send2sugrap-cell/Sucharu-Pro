package com.sucharu.sucharupro.data.event.serialization

import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.domain.event.model.DomainEvent
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.*
import java.math.BigDecimal

/**
 * Production-grade, deterministic, zero-dependency JSON serialization and deserialization
 * for Sucharu Pro Domain Events and Envelopes (INFRA-04 Step 02).
 */
object EventSerializationHelper {

    // --- JSON Encoding Helpers ---

    private fun escapeJson(value: String): String {
        val sb = StringBuilder()
        for (c in value) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (c.code < 0x20) {
                        sb.append(String.format("\\u%04x", c.code))
                    } else {
                        sb.append(c)
                    }
                }
            }
        }
        return sb.toString()
    }

    fun serializeMap(map: Map<String, Any?>): String {
        val entries = map.entries.mapNotNull { (k, v) ->
            if (v == null) {
                "\"${escapeJson(k)}\":null"
            } else {
                val encodedVal = when (v) {
                    is Number -> v.toString()
                    is Boolean -> v.toString()
                    is String -> "\"${escapeJson(v)}\""
                    is BigDecimal -> "\"${v.toPlainString()}\""
                    is List<*> -> serializeList(v)
                    is Map<*, *> -> @Suppress("UNCHECKED_CAST") serializeMap(v as Map<String, Any?>)
                    else -> "\"${escapeJson(v.toString())}\""
                }
                "\"${escapeJson(k)}\":$encodedVal"
            }
        }
        return "{${entries.joinToString(",")}}"
    }

    private fun serializeList(list: List<*>): String {
        val items = list.mapNotNull { v ->
            if (v == null) {
                "null"
            } else {
                when (v) {
                    is Number -> v.toString()
                    is Boolean -> v.toString()
                    is String -> "\"${escapeJson(v)}\""
                    is BigDecimal -> "\"${v.toPlainString()}\""
                    is Map<*, *> -> @Suppress("UNCHECKED_CAST") serializeMap(v as Map<String, Any?>)
                    else -> "\"${escapeJson(v.toString())}\""
                }
            }
        }
        return "[${items.joinToString(",")}]"
    }

    // --- JSON Simple Parser Helper ---

    fun parseJsonObject(json: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val trimmed = json.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return result
        val content = trimmed.substring(1, trimmed.length - 1).trim()
        if (content.isEmpty()) return result

        var index = 0
        while (index < content.length) {
            val keyStart = content.indexOf('"', index)
            if (keyStart == -1) break
            val keyEnd = content.indexOf('"', keyStart + 1)
            if (keyEnd == -1) break
            val key = content.substring(keyStart + 1, keyEnd)

            val colonIdx = content.indexOf(':', keyEnd + 1)
            if (colonIdx == -1) break

            var valStart = colonIdx + 1
            while (valStart < content.length && content[valStart].isWhitespace()) {
                valStart++
            }
            if (valStart >= content.length) break

            if (content[valStart] == '"') {
                val sb = StringBuilder()
                var i = valStart + 1
                var escaped = false
                while (i < content.length) {
                    val c = content[i]
                    if (escaped) {
                        when (c) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            else -> sb.append(c)
                        }
                        escaped = false
                    } else if (c == '\\') {
                        escaped = true
                    } else if (c == '"') {
                        i++
                        break
                    } else {
                        sb.append(c)
                    }
                    i++
                }
                result[key] = sb.toString()
                index = i
            } else if (content[valStart] == '{' || content[valStart] == '[') {
                var depth = 0
                val openChar = content[valStart]
                val closeChar = if (openChar == '{') '}' else ']'
                var i = valStart
                while (i < content.length) {
                    if (content[i] == openChar) depth++
                    if (content[i] == closeChar) depth--
                    i++
                    if (depth == 0) break
                }
                result[key] = content.substring(valStart, i)
                index = i
            } else {
                var i = valStart
                while (i < content.length && content[i] != ',' && content[i] != '}') {
                    i++
                }
                val rawVal = content.substring(valStart, i).trim()
                if (rawVal != "null") {
                    result[key] = rawVal
                }
                index = i
            }

            while (index < content.length && (content[index] == ',' || content[index].isWhitespace())) {
                index++
            }
        }
        return result
    }

    // --- Domain Event Payload Serialization ---

    fun serializePayload(event: DomainEvent): String {
        val map = mutableMapOf<String, Any?>()
        map["_type"] = event.eventType.name
        map["aggregateId"] = event.aggregateId
        map["aggregateType"] = event.aggregateType
        map["aggregateVersion"] = event.aggregateVersion

        when (event) {
            is OrderCreatedEvent -> {
                map["orderId"] = event.orderId
                map["customerId"] = event.customerId
                map["totalAmount"] = event.totalAmount.toPlainString()
                map["itemCount"] = event.itemCount
                map["currency"] = event.currency
            }
            is OrderUpdatedEvent -> {
                map["orderId"] = event.orderId
                map["customerId"] = event.customerId
                map["updatedTotalAmount"] = event.updatedTotalAmount.toPlainString()
                map["updateReason"] = event.updateReason
                map["currency"] = event.currency
            }
            is OrderCancelledEvent -> {
                map["orderId"] = event.orderId
                map["customerId"] = event.customerId
                map["cancellationReason"] = event.cancellationReason
            }
            is ProductionStartedEvent -> {
                map["jobId"] = event.jobId
                map["orderId"] = event.orderId
                map["workstationId"] = event.workstationId
                map["operatorId"] = event.operatorId
            }
            is ProductionCompletedEvent -> {
                map["jobId"] = event.jobId
                map["orderId"] = event.orderId
                map["completedQuantity"] = event.completedQuantity
                map["wastageQuantity"] = event.wastageQuantity
            }
            is QcPassedEvent -> {
                map["inspectionId"] = event.inspectionId
                map["jobId"] = event.jobId
                map["inspectedBy"] = event.inspectedBy
                map["sampleSize"] = event.sampleSize
                map["passRatePercentage"] = event.passRatePercentage
            }
            is QcFailedEvent -> {
                map["inspectionId"] = event.inspectionId
                map["jobId"] = event.jobId
                map["inspectedBy"] = event.inspectedBy
                map["defectReason"] = event.defectReason
                map["rejectedQuantity"] = event.rejectedQuantity
            }
            is StockReceivedEvent -> {
                map["movementId"] = event.movementId
                map["productId"] = event.productId
                map["sku"] = event.sku
                map["quantity"] = event.quantity.toPlainString()
                map["warehouseId"] = event.warehouseId
                map["purchaseOrderId"] = event.purchaseOrderId
            }
            is StockIssuedEvent -> {
                map["movementId"] = event.movementId
                map["productId"] = event.productId
                map["sku"] = event.sku
                map["quantity"] = event.quantity.toPlainString()
                map["warehouseId"] = event.warehouseId
                map["destinationType"] = event.destinationType
                map["destinationRefId"] = event.destinationRefId
            }
            is StockAdjustedEvent -> {
                map["adjustmentId"] = event.adjustmentId
                map["productId"] = event.productId
                map["warehouseId"] = event.warehouseId
                map["varianceQuantity"] = event.varianceQuantity.toPlainString()
                map["reason"] = event.reason
                map["authorizedBy"] = event.authorizedBy
            }
            is DeliveryCreatedEvent -> {
                map["challanId"] = event.challanId
                map["orderId"] = event.orderId
                map["customerId"] = event.customerId
                map["deliveryAddress"] = event.deliveryAddress
                map["totalPackages"] = event.totalPackages
            }
            is DeliveryDispatchedEvent -> {
                map["challanId"] = event.challanId
                map["orderId"] = event.orderId
                map["carrierName"] = event.carrierName
                map["trackingNumber"] = event.trackingNumber
            }
            is DeliveryDeliveredEvent -> {
                map["challanId"] = event.challanId
                map["orderId"] = event.orderId
                map["deliveredToPerson"] = event.deliveredToPerson
                map["deliveredTimestamp"] = event.deliveredTimestamp
            }
            is ReturnRequestedEvent -> {
                map["returnRequestId"] = event.returnRequestId
                map["orderId"] = event.orderId
                map["customerId"] = event.customerId
                map["reason"] = event.reason
                map["requestedItemCount"] = event.requestedItemCount
            }
            is ReturnInspectedEvent -> {
                map["returnRequestId"] = event.returnRequestId
                map["inspectedBy"] = event.inspectedBy
                map["inspectionNotes"] = event.inspectionNotes
                map["restockableCount"] = event.restockableCount
                map["damagedCount"] = event.damagedCount
            }
            is ReturnApprovedEvent -> {
                map["returnRequestId"] = event.returnRequestId
                map["orderId"] = event.orderId
                map["approvedBy"] = event.approvedBy
                map["refundActionRequired"] = event.refundActionRequired
            }
            is ReturnRejectedEvent -> {
                map["returnRequestId"] = event.returnRequestId
                map["orderId"] = event.orderId
                map["rejectedBy"] = event.rejectedBy
                map["rejectionReason"] = event.rejectionReason
            }
            is InvoiceCreatedEvent -> {
                map["invoiceId"] = event.invoiceId
                map["orderId"] = event.orderId
                map["customerId"] = event.customerId
                map["invoiceNumber"] = event.invoiceNumber
                map["totalAmount"] = event.totalAmount.toPlainString()
                map["currency"] = event.currency
                map["dueTimestamp"] = event.dueTimestamp
            }
            is PaymentReceivedEvent -> {
                map["paymentId"] = event.paymentId
                map["invoiceId"] = event.invoiceId
                map["orderId"] = event.orderId
                map["customerId"] = event.customerId
                map["amount"] = event.amount.toPlainString()
                map["currency"] = event.currency
                map["paymentMethod"] = event.paymentMethod
                map["transactionRef"] = event.transactionRef
            }
            is PaymentRefundedEvent -> {
                map["refundId"] = event.refundId
                map["originalPaymentId"] = event.originalPaymentId
                map["customerId"] = event.customerId
                map["refundedAmount"] = event.refundedAmount.toPlainString()
                map["currency"] = event.currency
                map["reason"] = event.reason
            }
            is CustomerRegisteredEvent -> {
                map["customerId"] = event.customerId
                map["customerCode"] = event.customerCode
                map["displayName"] = event.displayName
                map["primaryPhone"] = event.primaryPhone
                map["email"] = event.email
                map["referringAffiliateId"] = event.referringAffiliateId
            }
            is CustomerVerifiedEvent -> {
                map["customerId"] = event.customerId
                map["verificationMethod"] = event.verificationMethod
                map["verifiedTimestamp"] = event.verifiedTimestamp
            }
            is AffiliateReferralCreatedEvent -> {
                map["referralId"] = event.referralId
                map["affiliateId"] = event.affiliateId
                map["referredCustomerId"] = event.referredCustomerId
                map["referralCode"] = event.referralCode
            }
            is AffiliateCommissionGeneratedEvent -> {
                map["commissionId"] = event.commissionId
                map["affiliateId"] = event.affiliateId
                map["orderId"] = event.orderId
                map["commissionAmount"] = event.commissionAmount.toPlainString()
                map["currency"] = event.currency
                map["commissionRatePercentage"] = event.commissionRatePercentage
            }
            is AuthenticationSucceededEvent -> {
                map["userId"] = event.userId
                map["username"] = event.username
                map["clientIpMasked"] = event.clientIpMasked
                map["userAgentSummary"] = event.userAgentSummary
            }
            is AuthenticationFailedEvent -> {
                map["attemptedIdentifierMasked"] = event.attemptedIdentifierMasked
                map["failureReason"] = event.failureReason
                map["clientIpMasked"] = event.clientIpMasked
            }
            is SessionCreatedEvent -> {
                map["sessionId"] = event.sessionId
                map["userId"] = event.userId
                map["expiresAt"] = event.expiresAt
                map["deviceInfo"] = event.deviceInfo
            }
            is SessionRevokedEvent -> {
                map["sessionId"] = event.sessionId
                map["userId"] = event.userId
                map["revocationReason"] = event.revocationReason
                map["revokedByActorId"] = event.revokedByActorId
            }
            is AuthorizationDeniedEvent -> {
                map["userId"] = event.userId
                map["attemptedAction"] = event.attemptedAction
                map["resourceType"] = event.resourceType
                map["denialReasonCode"] = event.denialReasonCode
            }
            is AccountLockedEvent -> {
                map["userId"] = event.userId
                map["lockReason"] = event.lockReason
                map["unlockTimestamp"] = event.unlockTimestamp
            }
            is PasswordChangedEvent -> {
                map["userId"] = event.userId
                map["changeMethod"] = event.changeMethod
                map["allSessionsRevoked"] = event.allSessionsRevoked
            }
            is SystemMaintenanceScheduledEvent -> {
                map["maintenanceId"] = event.maintenanceId
                map["startTimestamp"] = event.startTimestamp
                map["endTimestamp"] = event.endTimestamp
                map["description"] = event.description
            }
            is SystemAlertEvent -> {
                map["alertId"] = event.alertId
                map["severity"] = event.severity
                map["alertMessage"] = event.alertMessage
                map["affectedSubsystem"] = event.affectedSubsystem
            }
            is NotificationAuthorizationDeniedEvent -> {
                map["notificationId"] = event.notificationId
                map["recipientId"] = event.recipientId
                map["denialReason"] = event.denialReason
                map["attemptedChannel"] = event.attemptedChannel
                map["actorId"] = event.actorId
            }
            is NotificationSuppressedEvent -> {
                map["recipientId"] = event.recipientId
                map["channel"] = event.channel
                map["suppressionReason"] = event.suppressionReason
                map["suppressionType"] = event.suppressionType
            }
            is NotificationRateLimitTriggeredEvent -> {
                map["dimensionKey"] = event.dimensionKey
                map["recipientId"] = event.recipientId
                map["channel"] = event.channel
                map["retryAfterMs"] = event.retryAfterMs
            }
            is NotificationAbuseDetectedEvent -> {
                map["signalType"] = event.signalType
                map["description"] = event.description
                map["severity"] = event.severity
                map["recipientId"] = event.recipientId
            }
            is NotificationReplayDeniedEvent -> {
                map["originalEventId"] = event.originalEventId
                map["actorId"] = event.actorId
                map["denialReason"] = event.denialReason
            }
            is NotificationProviderSecurityFailureEvent -> {
                map["providerName"] = event.providerName
                map["failureType"] = event.failureType
                map["sanitizedDetails"] = event.sanitizedDetails
            }
        }
        return serializeMap(map)
    }

    // --- Domain Event Payload Deserialization ---

    fun deserializePayload(eventType: DomainEventType, json: String): DomainEvent {
        val map = parseJsonObject(json)
        val aggVersion = map["aggregateVersion"]?.toLongOrNull() ?: 1L

        return when (eventType) {
            DomainEventType.ORDER_CREATED -> OrderCreatedEvent(
                orderId = map["orderId"] ?: throw IllegalArgumentException("Missing orderId"),
                customerId = map["customerId"] ?: throw IllegalArgumentException("Missing customerId"),
                totalAmount = BigDecimal(map["totalAmount"] ?: "0"),
                itemCount = map["itemCount"]?.toIntOrNull() ?: 1,
                currency = map["currency"] ?: "BDT",
                aggregateVersion = aggVersion
            )
            DomainEventType.ORDER_UPDATED -> OrderUpdatedEvent(
                orderId = map["orderId"] ?: throw IllegalArgumentException("Missing orderId"),
                customerId = map["customerId"] ?: throw IllegalArgumentException("Missing customerId"),
                updatedTotalAmount = BigDecimal(map["updatedTotalAmount"] ?: "0"),
                updateReason = map["updateReason"] ?: "",
                aggregateVersion = aggVersion,
                currency = map["currency"] ?: "BDT"
            )
            DomainEventType.ORDER_CANCELLED -> OrderCancelledEvent(
                orderId = map["orderId"] ?: throw IllegalArgumentException("Missing orderId"),
                customerId = map["customerId"] ?: "",
                cancellationReason = map["cancellationReason"] ?: "",
                aggregateVersion = aggVersion
            )
            DomainEventType.PRODUCTION_STARTED -> ProductionStartedEvent(
                jobId = map["jobId"] ?: throw IllegalArgumentException("Missing jobId"),
                orderId = map["orderId"] ?: throw IllegalArgumentException("Missing orderId"),
                workstationId = map["workstationId"] ?: "",
                operatorId = map["operatorId"] ?: "",
                aggregateVersion = aggVersion
            )
            DomainEventType.PRODUCTION_COMPLETED -> ProductionCompletedEvent(
                jobId = map["jobId"] ?: throw IllegalArgumentException("Missing jobId"),
                orderId = map["orderId"] ?: throw IllegalArgumentException("Missing orderId"),
                completedQuantity = map["completedQuantity"]?.toIntOrNull() ?: 0,
                wastageQuantity = map["wastageQuantity"]?.toIntOrNull() ?: 0,
                aggregateVersion = aggVersion
            )
            DomainEventType.QC_PASSED -> QcPassedEvent(
                inspectionId = map["inspectionId"] ?: throw IllegalArgumentException("Missing inspectionId"),
                jobId = map["jobId"] ?: "",
                inspectedBy = map["inspectedBy"] ?: "",
                sampleSize = map["sampleSize"]?.toIntOrNull() ?: 1,
                passRatePercentage = map["passRatePercentage"]?.toDoubleOrNull() ?: 100.0,
                aggregateVersion = aggVersion
            )
            DomainEventType.QC_FAILED -> QcFailedEvent(
                inspectionId = map["inspectionId"] ?: throw IllegalArgumentException("Missing inspectionId"),
                jobId = map["jobId"] ?: "",
                inspectedBy = map["inspectedBy"] ?: "",
                defectReason = map["defectReason"] ?: "",
                rejectedQuantity = map["rejectedQuantity"]?.toIntOrNull() ?: 0,
                aggregateVersion = aggVersion
            )
            DomainEventType.STOCK_RECEIVED -> StockReceivedEvent(
                movementId = map["movementId"] ?: throw IllegalArgumentException("Missing movementId"),
                productId = map["productId"] ?: "",
                sku = map["sku"] ?: "",
                quantity = BigDecimal(map["quantity"] ?: "0"),
                warehouseId = map["warehouseId"] ?: "",
                purchaseOrderId = map["purchaseOrderId"],
                aggregateVersion = aggVersion
            )
            DomainEventType.STOCK_ISSUED -> StockIssuedEvent(
                movementId = map["movementId"] ?: throw IllegalArgumentException("Missing movementId"),
                productId = map["productId"] ?: "",
                sku = map["sku"] ?: "",
                quantity = BigDecimal(map["quantity"] ?: "0"),
                warehouseId = map["warehouseId"] ?: "",
                destinationType = map["destinationType"] ?: "",
                destinationRefId = map["destinationRefId"] ?: "",
                aggregateVersion = aggVersion
            )
            DomainEventType.STOCK_ADJUSTED -> StockAdjustedEvent(
                adjustmentId = map["adjustmentId"] ?: throw IllegalArgumentException("Missing adjustmentId"),
                productId = map["productId"] ?: "",
                warehouseId = map["warehouseId"] ?: "",
                varianceQuantity = BigDecimal(map["varianceQuantity"] ?: "0"),
                reason = map["reason"] ?: "",
                authorizedBy = map["authorizedBy"] ?: "",
                aggregateVersion = aggVersion
            )
            DomainEventType.DELIVERY_CREATED -> DeliveryCreatedEvent(
                challanId = map["challanId"] ?: throw IllegalArgumentException("Missing challanId"),
                orderId = map["orderId"] ?: "",
                customerId = map["customerId"] ?: "",
                deliveryAddress = map["deliveryAddress"] ?: "",
                totalPackages = map["totalPackages"]?.toIntOrNull() ?: 1,
                aggregateVersion = aggVersion
            )
            DomainEventType.DELIVERY_DISPATCHED -> DeliveryDispatchedEvent(
                challanId = map["challanId"] ?: throw IllegalArgumentException("Missing challanId"),
                orderId = map["orderId"] ?: "",
                carrierName = map["carrierName"] ?: "",
                trackingNumber = map["trackingNumber"],
                aggregateVersion = aggVersion
            )
            DomainEventType.DELIVERY_DELIVERED -> DeliveryDeliveredEvent(
                challanId = map["challanId"] ?: throw IllegalArgumentException("Missing challanId"),
                orderId = map["orderId"] ?: "",
                deliveredToPerson = map["deliveredToPerson"] ?: "",
                deliveredTimestamp = map["deliveredTimestamp"]?.toLongOrNull() ?: System.currentTimeMillis(),
                aggregateVersion = aggVersion
            )
            DomainEventType.RETURN_REQUESTED -> ReturnRequestedEvent(
                returnRequestId = map["returnRequestId"] ?: throw IllegalArgumentException("Missing returnRequestId"),
                orderId = map["orderId"] ?: "",
                customerId = map["customerId"] ?: "",
                reason = map["reason"] ?: "",
                requestedItemCount = map["requestedItemCount"]?.toIntOrNull() ?: 1,
                aggregateVersion = aggVersion
            )
            DomainEventType.RETURN_INSPECTED -> ReturnInspectedEvent(
                returnRequestId = map["returnRequestId"] ?: throw IllegalArgumentException("Missing returnRequestId"),
                inspectedBy = map["inspectedBy"] ?: "",
                inspectionNotes = map["inspectionNotes"] ?: "",
                restockableCount = map["restockableCount"]?.toIntOrNull() ?: 0,
                damagedCount = map["damagedCount"]?.toIntOrNull() ?: 0,
                aggregateVersion = aggVersion
            )
            DomainEventType.RETURN_APPROVED -> ReturnApprovedEvent(
                returnRequestId = map["returnRequestId"] ?: throw IllegalArgumentException("Missing returnRequestId"),
                orderId = map["orderId"] ?: "",
                approvedBy = map["approvedBy"] ?: "",
                refundActionRequired = map["refundActionRequired"]?.toBooleanStrictOrNull() ?: false,
                aggregateVersion = aggVersion
            )
            DomainEventType.RETURN_REJECTED -> ReturnRejectedEvent(
                returnRequestId = map["returnRequestId"] ?: throw IllegalArgumentException("Missing returnRequestId"),
                orderId = map["orderId"] ?: "",
                rejectedBy = map["rejectedBy"] ?: "",
                rejectionReason = map["rejectionReason"] ?: "",
                aggregateVersion = aggVersion
            )
            DomainEventType.INVOICE_CREATED -> InvoiceCreatedEvent(
                invoiceId = map["invoiceId"] ?: throw IllegalArgumentException("Missing invoiceId"),
                orderId = map["orderId"] ?: "",
                customerId = map["customerId"] ?: "",
                invoiceNumber = map["invoiceNumber"] ?: "",
                totalAmount = BigDecimal(map["totalAmount"] ?: "0"),
                currency = map["currency"] ?: "BDT",
                dueTimestamp = map["dueTimestamp"]?.toLongOrNull() ?: 0L,
                aggregateVersion = aggVersion
            )
            DomainEventType.PAYMENT_RECEIVED -> PaymentReceivedEvent(
                paymentId = map["paymentId"] ?: throw IllegalArgumentException("Missing paymentId"),
                invoiceId = map["invoiceId"],
                orderId = map["orderId"],
                customerId = map["customerId"] ?: "",
                amount = BigDecimal(map["amount"] ?: "0"),
                currency = map["currency"] ?: "BDT",
                paymentMethod = map["paymentMethod"] ?: "CASH",
                transactionRef = map["transactionRef"] ?: "",
                aggregateVersion = aggVersion
            )
            DomainEventType.PAYMENT_REFUNDED -> PaymentRefundedEvent(
                refundId = map["refundId"] ?: throw IllegalArgumentException("Missing refundId"),
                originalPaymentId = map["originalPaymentId"] ?: "",
                customerId = map["customerId"] ?: "",
                refundedAmount = BigDecimal(map["refundedAmount"] ?: "0"),
                currency = map["currency"] ?: "BDT",
                reason = map["reason"] ?: "",
                aggregateVersion = aggVersion
            )
            DomainEventType.CUSTOMER_REGISTERED -> CustomerRegisteredEvent(
                customerId = map["customerId"] ?: throw IllegalArgumentException("Missing customerId"),
                customerCode = map["customerCode"] ?: "",
                displayName = map["displayName"] ?: "",
                primaryPhone = map["primaryPhone"] ?: "",
                email = map["email"],
                referringAffiliateId = map["referringAffiliateId"],
                aggregateVersion = aggVersion
            )
            DomainEventType.CUSTOMER_VERIFIED -> CustomerVerifiedEvent(
                customerId = map["customerId"] ?: throw IllegalArgumentException("Missing customerId"),
                verificationMethod = map["verificationMethod"] ?: "EMAIL",
                verifiedTimestamp = map["verifiedTimestamp"]?.toLongOrNull() ?: System.currentTimeMillis(),
                aggregateVersion = aggVersion
            )
            DomainEventType.AFFILIATE_REFERRAL_CREATED -> AffiliateReferralCreatedEvent(
                referralId = map["referralId"] ?: throw IllegalArgumentException("Missing referralId"),
                affiliateId = map["affiliateId"] ?: "",
                referredCustomerId = map["referredCustomerId"] ?: "",
                referralCode = map["referralCode"] ?: "",
                aggregateVersion = aggVersion
            )
            DomainEventType.AFFILIATE_COMMISSION_GENERATED -> AffiliateCommissionGeneratedEvent(
                commissionId = map["commissionId"] ?: throw IllegalArgumentException("Missing commissionId"),
                affiliateId = map["affiliateId"] ?: "",
                orderId = map["orderId"] ?: "",
                commissionAmount = BigDecimal(map["commissionAmount"] ?: "0"),
                currency = map["currency"] ?: "BDT",
                commissionRatePercentage = map["commissionRatePercentage"]?.toDoubleOrNull() ?: 5.0,
                aggregateVersion = aggVersion
            )
            DomainEventType.AUTH_SUCCEEDED -> AuthenticationSucceededEvent(
                userId = map["userId"] ?: throw IllegalArgumentException("Missing userId"),
                username = map["username"] ?: "",
                clientIpMasked = map["clientIpMasked"],
                userAgentSummary = map["userAgentSummary"],
                aggregateVersion = aggVersion
            )
            DomainEventType.AUTH_FAILED -> AuthenticationFailedEvent(
                attemptedIdentifierMasked = map["attemptedIdentifierMasked"] ?: "***",
                failureReason = map["failureReason"] ?: "UNKNOWN",
                clientIpMasked = map["clientIpMasked"],
                aggregateVersion = aggVersion
            )
            DomainEventType.SESSION_CREATED -> SessionCreatedEvent(
                sessionId = map["sessionId"] ?: throw IllegalArgumentException("Missing sessionId"),
                userId = map["userId"] ?: "",
                expiresAt = map["expiresAt"]?.toLongOrNull() ?: 0L,
                deviceInfo = map["deviceInfo"],
                aggregateVersion = aggVersion
            )
            DomainEventType.SESSION_REVOKED -> SessionRevokedEvent(
                sessionId = map["sessionId"] ?: throw IllegalArgumentException("Missing sessionId"),
                userId = map["userId"] ?: "",
                revocationReason = map["revocationReason"] ?: "MANUAL",
                revokedByActorId = map["revokedByActorId"] ?: "SYSTEM",
                aggregateVersion = aggVersion
            )
            DomainEventType.AUTHZ_DENIED -> AuthorizationDeniedEvent(
                userId = map["userId"],
                attemptedAction = map["attemptedAction"] ?: "",
                resourceType = map["resourceType"] ?: "",
                denialReasonCode = map["denialReasonCode"] ?: "",
                aggregateVersion = aggVersion
            )
            DomainEventType.ACCOUNT_LOCKED -> AccountLockedEvent(
                userId = map["userId"] ?: throw IllegalArgumentException("Missing userId"),
                lockReason = map["lockReason"] ?: "SECURITY_LOCKOUT",
                unlockTimestamp = map["unlockTimestamp"]?.toLongOrNull(),
                aggregateVersion = aggVersion
            )
            DomainEventType.PASSWORD_CHANGED -> PasswordChangedEvent(
                userId = map["userId"] ?: throw IllegalArgumentException("Missing userId"),
                changeMethod = map["changeMethod"] ?: "USER_INITIATED",
                allSessionsRevoked = map["allSessionsRevoked"]?.toBooleanStrictOrNull() ?: true,
                aggregateVersion = aggVersion
            )
            DomainEventType.SYSTEM_MAINTENANCE_SCHEDULED -> SystemMaintenanceScheduledEvent(
                maintenanceId = map["maintenanceId"] ?: throw IllegalArgumentException("Missing maintenanceId"),
                startTimestamp = map["startTimestamp"]?.toLongOrNull() ?: 0L,
                endTimestamp = map["endTimestamp"]?.toLongOrNull() ?: 0L,
                description = map["description"] ?: "",
                aggregateVersion = aggVersion
            )
            DomainEventType.SYSTEM_ALERT -> SystemAlertEvent(
                alertId = map["alertId"] ?: throw IllegalArgumentException("Missing alertId"),
                severity = map["severity"] ?: "INFO",
                alertMessage = map["alertMessage"] ?: "",
                affectedSubsystem = map["affectedSubsystem"] ?: "CORE",
                aggregateVersion = aggVersion
            )
            DomainEventType.NOTIFICATION_AUTHORIZATION_DENIED -> NotificationAuthorizationDeniedEvent(
                notificationId = map["notificationId"],
                recipientId = map["recipientId"] ?: throw IllegalArgumentException("Missing recipientId"),
                denialReason = map["denialReason"] ?: throw IllegalArgumentException("Missing denialReason"),
                attemptedChannel = map["attemptedChannel"],
                actorId = map["actorId"],
                aggregateVersion = aggVersion
            )
            DomainEventType.NOTIFICATION_SUPPRESSED -> NotificationSuppressedEvent(
                recipientId = map["recipientId"] ?: throw IllegalArgumentException("Missing recipientId"),
                channel = map["channel"],
                suppressionReason = map["suppressionReason"] ?: throw IllegalArgumentException("Missing suppressionReason"),
                suppressionType = map["suppressionType"] ?: "RECIPIENT",
                aggregateVersion = aggVersion
            )
            DomainEventType.NOTIFICATION_RATE_LIMIT_TRIGGERED -> NotificationRateLimitTriggeredEvent(
                dimensionKey = map["dimensionKey"] ?: throw IllegalArgumentException("Missing dimensionKey"),
                recipientId = map["recipientId"],
                channel = map["channel"],
                retryAfterMs = map["retryAfterMs"]?.toLongOrNull() ?: 0L,
                aggregateVersion = aggVersion
            )
            DomainEventType.NOTIFICATION_ABUSE_DETECTED -> NotificationAbuseDetectedEvent(
                signalType = map["signalType"] ?: throw IllegalArgumentException("Missing signalType"),
                description = map["description"] ?: "",
                severity = map["severity"] ?: "MEDIUM",
                recipientId = map["recipientId"],
                aggregateVersion = aggVersion
            )
            DomainEventType.NOTIFICATION_REPLAY_DENIED -> NotificationReplayDeniedEvent(
                originalEventId = map["originalEventId"] ?: throw IllegalArgumentException("Missing originalEventId"),
                actorId = map["actorId"],
                denialReason = map["denialReason"] ?: throw IllegalArgumentException("Missing denialReason"),
                aggregateVersion = aggVersion
            )
            DomainEventType.NOTIFICATION_PROVIDER_SECURITY_FAILURE -> NotificationProviderSecurityFailureEvent(
                providerName = map["providerName"] ?: throw IllegalArgumentException("Missing providerName"),
                failureType = map["failureType"] ?: "SECURITY_ERROR",
                sanitizedDetails = map["sanitizedDetails"] ?: "",
                aggregateVersion = aggVersion
            )
        }
    }

    // --- Envelope Serialization / Deserialization ---

    fun serializeEnvelope(envelope: EventEnvelope<*>): String {
        val map = mutableMapOf<String, Any?>()
        map["eventId"] = envelope.eventId
        map["eventType"] = envelope.eventType.name
        map["eventVersion"] = envelope.eventVersion
        map["occurredAt"] = envelope.occurredAt
        map["publishedAt"] = envelope.publishedAt
        map["projectId"] = envelope.projectId
        map["aggregateType"] = envelope.aggregateType
        map["aggregateId"] = envelope.aggregateId
        map["aggregateVersion"] = envelope.aggregateVersion
        map["actorType"] = envelope.actorType.name
        map["actorId"] = envelope.actorId
        map["principalType"] = envelope.principalType.name
        map["correlationId"] = envelope.correlationId
        map["causationId"] = envelope.causationId
        map["requestId"] = envelope.requestId
        map["source"] = envelope.source
        map["payload"] = serializePayload(envelope.payload)
        map["metadata"] = serializeMap(envelope.metadata)
        return serializeMap(map)
    }

    fun deserializeEnvelope(json: String): EventEnvelope<DomainEvent> {
        val map = parseJsonObject(json)
        val eventTypeStr = map["eventType"] ?: throw IllegalArgumentException("Missing eventType")
        val eventType = DomainEventType.valueOf(eventTypeStr)
        val payloadJson = map["payload"] ?: throw IllegalArgumentException("Missing payload")
        val payload = deserializePayload(eventType, payloadJson)

        val metadataJson = map["metadata"]
        val metadataMap = if (!metadataJson.isNullOrBlank()) parseJsonObject(metadataJson) else emptyMap()

        val actorType = PrincipalType.valueOf(map["actorType"] ?: "HUMAN")
        val principalType = PrincipalType.valueOf(map["principalType"] ?: "HUMAN")

        return EventEnvelope(
            eventId = map["eventId"] ?: throw IllegalArgumentException("Missing eventId"),
            eventType = eventType,
            eventVersion = map["eventVersion"] ?: "v1",
            occurredAt = map["occurredAt"]?.toLongOrNull() ?: System.currentTimeMillis(),
            publishedAt = map["publishedAt"]?.toLongOrNull() ?: System.currentTimeMillis(),
            projectId = map["projectId"] ?: throw IllegalArgumentException("Missing projectId"),
            aggregateType = map["aggregateType"] ?: "",
            aggregateId = map["aggregateId"] ?: "",
            aggregateVersion = map["aggregateVersion"]?.toLongOrNull() ?: 1L,
            actorType = actorType,
            actorId = map["actorId"] ?: "unknown",
            principalType = principalType,
            correlationId = map["correlationId"] ?: "",
            causationId = map["causationId"],
            requestId = map["requestId"],
            source = map["source"] ?: "sucharu-pro-backend",
            payload = payload,
            metadata = metadataMap
        )
    }
}
