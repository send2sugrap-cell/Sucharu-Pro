package com.sucharu.sucharupro.data.job.integration.handlers

import com.sucharu.sucharupro.data.event.integration.notification.NotificationProvider
import com.sucharu.sucharupro.data.event.integration.notification.NotificationRecipient
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.job.model.JobResult
import com.sucharu.sucharupro.domain.job.worker.JobExecutionContext
import com.sucharu.sucharupro.domain.job.worker.JobHandler

/**
 * Background job handler for asynchronous email delivery (INFRA-04 Step 04).
 */
class EmailDispatchJobHandler(
    private val emailProvider: NotificationProvider
) : JobHandler {
    override val supportedJobType: String = "notification.email_dispatch"

    override suspend fun execute(context: JobExecutionContext): JobResult {
        val recipient = NotificationRecipient(
            recipientId = context.metadata["recipientId"] ?: context.actorId,
            projectId = context.projectId,
            displayName = context.metadata["recipientName"] ?: "Customer",
            email = context.metadata["recipientEmail"]
        )

        val result = emailProvider.deliver(
            recipient = recipient,
            title = context.metadata["title"] ?: "Notification",
            body = context.payloadJson,
            metadata = context.metadata,
            idempotencyKey = context.jobId
        )

        return if (result.isSuccess) {
            JobResult.Success(
                message = "Email delivered via ${result.channel}",
                outputMetadata = mapOf("providerRef" to (result.providerRef ?: ""))
            )
        } else {
            JobResult.Failure(
                reason = result.errorMessage ?: "Email delivery failed",
                classification = result.failureClassification ?: EventFailureClassification.TRANSIENT
            )
        }
    }
}

/**
 * Background job handler for asynchronous SMS delivery.
 */
class SmsDispatchJobHandler(
    private val smsProvider: NotificationProvider
) : JobHandler {
    override val supportedJobType: String = "notification.sms_dispatch"

    override suspend fun execute(context: JobExecutionContext): JobResult {
        val recipient = NotificationRecipient(
            recipientId = context.metadata["recipientId"] ?: context.actorId,
            projectId = context.projectId,
            displayName = context.metadata["recipientName"] ?: "Customer",
            phone = context.metadata["recipientPhone"]
        )

        val result = smsProvider.deliver(
            recipient = recipient,
            title = context.metadata["title"] ?: "SMS Alert",
            body = context.payloadJson,
            metadata = context.metadata,
            idempotencyKey = context.jobId
        )

        return if (result.isSuccess) {
            JobResult.Success(
                message = "SMS delivered via ${result.channel}",
                outputMetadata = mapOf("providerRef" to (result.providerRef ?: ""))
            )
        } else {
            JobResult.Failure(
                reason = result.errorMessage ?: "SMS delivery failed",
                classification = result.failureClassification ?: EventFailureClassification.TRANSIENT
            )
        }
    }
}

/**
 * Background job handler for asynchronous Push notification delivery.
 */
class PushDispatchJobHandler(
    private val pushProvider: NotificationProvider
) : JobHandler {
    override val supportedJobType: String = "notification.push_dispatch"

    override suspend fun execute(context: JobExecutionContext): JobResult {
        val recipient = NotificationRecipient(
            recipientId = context.metadata["recipientId"] ?: context.actorId,
            projectId = context.projectId,
            displayName = context.metadata["recipientName"] ?: "Customer",
            pushToken = context.metadata["pushToken"]
        )

        val result = pushProvider.deliver(
            recipient = recipient,
            title = context.metadata["title"] ?: "Push Alert",
            body = context.payloadJson,
            metadata = context.metadata,
            idempotencyKey = context.jobId
        )

        return if (result.isSuccess) {
            JobResult.Success(
                message = "Push notification delivered via ${result.channel}",
                outputMetadata = mapOf("providerRef" to (result.providerRef ?: ""))
            )
        } else {
            JobResult.Failure(
                reason = result.errorMessage ?: "Push notification delivery failed",
                classification = result.failureClassification ?: EventFailureClassification.TRANSIENT
            )
        }
    }
}
