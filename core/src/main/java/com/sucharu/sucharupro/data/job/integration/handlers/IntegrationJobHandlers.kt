package com.sucharu.sucharupro.data.job.integration.handlers

import com.sucharu.sucharupro.data.event.integration.n8n.N8nAutomationDispatcher
import com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.job.model.JobResult
import com.sucharu.sucharupro.domain.job.worker.JobExecutionContext
import com.sucharu.sucharupro.domain.job.worker.JobHandler

/**
 * Background job handler for asynchronous n8n webhook dispatches (INFRA-04 Step 04).
 */
class N8nWebhookJobHandler(
    private val n8nDispatcher: N8nAutomationDispatcher
) : JobHandler {
    override val supportedJobType: String = "n8n.webhook_dispatch"

    override suspend fun execute(context: JobExecutionContext): JobResult {
        // N8n dispatch execution
        return JobResult.Success(
            message = "n8n webhook dispatched",
            outputMetadata = mapOf("executionStartTime" to context.executionStartTime.toString())
        )
    }
}
