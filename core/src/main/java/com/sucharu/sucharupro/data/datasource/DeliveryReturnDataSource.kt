package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturn
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnShipment
import kotlinx.coroutines.flow.Flow

/**
 * Data Source contract for Delivery Returns (Module 08 Step 07).
 */
interface DeliveryReturnDataSource {

    fun observeReturns(projectId: String): Flow<List<DeliveryReturn>>

    fun observeReturn(returnId: String): Flow<DeliveryReturn?>

    suspend fun getReturn(returnId: String): DeliveryReturn?

    suspend fun getReturnByNumber(projectId: String, returnNo: String): DeliveryReturn?

    suspend fun getReturnsByDeliveryOrder(deliveryOrderId: String): List<DeliveryReturn>

    suspend fun getReturnsByShipment(shipmentId: String): List<DeliveryReturn>

    suspend fun getReturnsByCustomer(projectId: String, customerId: String): List<DeliveryReturn>

    suspend fun getReturnLines(returnId: String): List<DeliveryReturnLine>

    suspend fun getReturnLine(returnLineId: String): DeliveryReturnLine?

    suspend fun insertReturn(ret: DeliveryReturn, lines: List<DeliveryReturnLine>)

    suspend fun updateReturn(ret: DeliveryReturn)

    suspend fun updateReturnLine(line: DeliveryReturnLine)

    suspend fun removeReturnLine(returnLineId: String)

    fun observeEvents(returnId: String): Flow<List<DeliveryReturnActivityEvent>>

    suspend fun insertEvent(event: DeliveryReturnActivityEvent)

    fun observeReverseShipment(returnId: String): Flow<DeliveryReturnShipment?>

    suspend fun getReverseShipment(returnId: String): DeliveryReturnShipment?

    suspend fun getReverseShipmentById(reverseShipmentId: String): DeliveryReturnShipment?

    suspend fun insertReverseShipment(shipment: DeliveryReturnShipment)

    suspend fun updateReverseShipment(shipment: DeliveryReturnShipment)
}
