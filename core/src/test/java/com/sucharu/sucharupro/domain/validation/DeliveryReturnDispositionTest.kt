package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnDisposition
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLineCondition
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryReturnDispositionTest {

    @Test
    fun `RESTOCK disposition with GOOD condition succeeds`() {
        val res = DeliveryReturnDispositionValidator.validateDispositionCompatibility(
            condition = DeliveryReturnLineCondition.GOOD,
            disposition = DeliveryReturnDisposition.RESTOCK
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun `RESTOCK disposition with DAMAGED or DEFECTIVE condition is rejected`() {
        val resDamaged = DeliveryReturnDispositionValidator.validateDispositionCompatibility(
            condition = DeliveryReturnLineCondition.DAMAGED,
            disposition = DeliveryReturnDisposition.RESTOCK
        )
        assertTrue(resDamaged is DomainResult.Error)

        val resDefective = DeliveryReturnDispositionValidator.validateDispositionCompatibility(
            condition = DeliveryReturnLineCondition.DEFECTIVE,
            disposition = DeliveryReturnDisposition.RESTOCK
        )
        assertTrue(resDefective is DomainResult.Error)
    }

    @Test
    fun `QUARANTINE or SCRAP disposition with DAMAGED condition succeeds`() {
        val resQ = DeliveryReturnDispositionValidator.validateDispositionCompatibility(
            condition = DeliveryReturnLineCondition.DAMAGED,
            disposition = DeliveryReturnDisposition.QUARANTINE
        )
        assertTrue(resQ is DomainResult.Success)

        val resS = DeliveryReturnDispositionValidator.validateDispositionCompatibility(
            condition = DeliveryReturnLineCondition.DAMAGED,
            disposition = DeliveryReturnDisposition.SCRAP
        )
        assertTrue(resS is DomainResult.Success)
    }
}
