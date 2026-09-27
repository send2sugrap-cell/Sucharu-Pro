package com.sucharu.sucharupro.domain.service.knowledge

import com.sucharu.sucharupro.domain.model.knowledge.ProfessionContext
import com.sucharu.sucharupro.domain.model.knowledge.ProfessionType
import com.sucharu.sucharupro.domain.model.knowledge.ProfessionWorkflowType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProfessionAdaptiveIntelligenceServiceTest {

    private lateinit var service: ProfessionAdaptiveIntelligenceService

    @Before
    fun setUp() {
        service = ProfessionAdaptiveIntelligenceService()
    }

    @Test
    fun `getRegisteredProfiles_returnsAll9CanonicalProfessions`() {
        val profiles = service.getRegisteredProfiles()

        assertEquals(9, profiles.size)

        val types = profiles.map { it.professionType }.toSet()
        assertTrue(types.contains(ProfessionType.TEACHER))
        assertTrue(types.contains(ProfessionType.DOCTOR_HEALTHCARE_PROFESSIONAL))
        assertTrue(types.contains(ProfessionType.FARMER))
        assertTrue(types.contains(ProfessionType.SHOPKEEPER_RETAILER))
        assertTrue(types.contains(ProfessionType.FREELANCER))
        assertTrue(types.contains(ProfessionType.MANUFACTURER))
        assertTrue(types.contains(ProfessionType.ACCOUNTANT))
        assertTrue(types.contains(ProfessionType.DESIGNER))
        assertTrue(types.contains(ProfessionType.BUSINESS_OWNER_ENTREPRENEUR))
    }

    @Test
    fun `generateAdaptiveGuidance_adaptsWorkflowAndEnforcesHealthcareSafetyBound`() {
        // 1. Teacher Context
        val teacherContext = ProfessionContext(
            professionType = ProfessionType.TEACHER,
            primaryGoal = "Lesson Planning"
        )
        val teacherGuidance = service.generateAdaptiveGuidance(teacherContext, "How to plan workbook printing?")

        assertNotNull(teacherGuidance)
        assertEquals(ProfessionType.TEACHER, teacherGuidance.professionType)
        assertEquals(ProfessionWorkflowType.TEACHING_WORKFLOW, teacherGuidance.primaryWorkflow)
        assertTrue(!teacherGuidance.isHealthcareSafetyBoundActive)

        // 2. Doctor / Healthcare Context
        val doctorContext = ProfessionContext(
            professionType = ProfessionType.DOCTOR_HEALTHCARE_PROFESSIONAL,
            primaryGoal = "Appointment Scheduling"
        )
        val doctorGuidance = service.generateAdaptiveGuidance(doctorContext, "How to optimize office workflow?")

        assertNotNull(doctorGuidance)
        assertEquals(ProfessionType.DOCTOR_HEALTHCARE_PROFESSIONAL, doctorGuidance.professionType)
        assertEquals(ProfessionWorkflowType.PRACTICE_OFFICE_WORKFLOW, doctorGuidance.primaryWorkflow)

        // CRITICAL SAFETY BOUNDARY PROOF:
        // Healthcare guidance MUST activate safety bound and disclaim medical treatment/diagnosis instructions!
        assertTrue("Healthcare guidance MUST activate safety bound!", doctorGuidance.isHealthcareSafetyBoundActive)
        assertTrue(doctorGuidance.guidanceText.contains("not clinical diagnosis or medical treatment instructions"))
    }

    @Test
    fun `generateAdaptiveGuidance_safelyFallsBackForUnknownProfession`() {
        val unknownContext = ProfessionContext(
            professionType = ProfessionType.OTHER,
            primaryGoal = "General Task"
        )
        val fallbackGuidance = service.generateAdaptiveGuidance(unknownContext, "How to organize tasks?")

        assertNotNull(fallbackGuidance)
        assertEquals(ProfessionType.OTHER, fallbackGuidance.professionType)
        assertEquals(ProfessionWorkflowType.GENERAL_WORKFLOW, fallbackGuidance.primaryWorkflow)
    }
}
