package com.sucharu.sucharupro.data.observability.slo

import com.sucharu.sucharupro.domain.observability.OperationalHealthStatus
import com.sucharu.sucharupro.domain.observability.SloDefinition
import com.sucharu.sucharupro.domain.observability.SloMeasurement
import java.util.concurrent.ConcurrentHashMap

/**
 * Service Level Objective & SLA Evaluation Engine (INFRA-04 Step 09).
 */
class SloEngine {

    private val definitions = ConcurrentHashMap<String, SloDefinition>()

    init {
        // Register canonical default platform SLOs
        registerSlo(SloDefinition("slo-evt-pub", "Event Publication SLO", "EVENT_INFRA", 99.9, warningThreshold = 99.0, criticalThreshold = 95.0))
        registerSlo(SloDefinition("slo-notif-deliv", "Notification Delivery SLO", "NOTIFICATION", 99.0, warningThreshold = 97.0, criticalThreshold = 90.0))
        registerSlo(SloDefinition("slo-job-comp", "Background Job Completion SLO", "BACKGROUND_JOBS", 98.0, warningThreshold = 95.0, criticalThreshold = 90.0))
        registerSlo(SloDefinition("slo-wf-comp", "Workflow Completion SLO", "WORKFLOW", 99.0, warningThreshold = 97.0, criticalThreshold = 92.0))
        registerSlo(SloDefinition("slo-prov-avail", "Provider Availability SLO", "NOTIFICATION_PROVIDERS", 99.5, warningThreshold = 98.0, criticalThreshold = 95.0))
        registerSlo(SloDefinition("slo-ai-act", "AI Notification Action SLO", "AI_AGENT", 95.0, warningThreshold = 90.0, criticalThreshold = 80.0))
    }

    fun registerSlo(definition: SloDefinition) {
        definitions[definition.sloId] = definition
    }

    fun getSloDefinitions(): List<SloDefinition> = definitions.values.toList()

    fun evaluateSlo(sloId: String, currentPercentage: Double, sampleCount: Long = 100): SloMeasurement? {
        val def = definitions[sloId] ?: return null

        val isMeeting = currentPercentage >= def.targetPercentage
        val status = when {
            currentPercentage >= def.targetPercentage -> OperationalHealthStatus.HEALTHY
            currentPercentage >= def.warningThreshold -> OperationalHealthStatus.DEGRADED
            else -> OperationalHealthStatus.CRITICAL
        }

        return SloMeasurement(
            sloId = def.sloId,
            name = def.name,
            subsystem = def.subsystem,
            currentPercentage = currentPercentage,
            targetPercentage = def.targetPercentage,
            isMeetingSlo = isMeeting,
            status = status,
            measurementWindowSeconds = def.measurementWindowSeconds,
            sampleCount = sampleCount
        )
    }

    fun evaluateAll(measurementsMap: Map<String, Double>): List<SloMeasurement> {
        return definitions.keys.mapNotNull { sloId ->
            val value = measurementsMap[sloId] ?: 100.0
            evaluateSlo(sloId, value)
        }
    }
}
