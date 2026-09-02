package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallan
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine
import kotlinx.coroutines.flow.Flow

/**
 * Data source contract for Delivery Challans (Module 08 Step 02).
 */
interface DeliveryChallanDataSource {

    // Challan Headers
    fun observeChallans(projectId: String): Flow<List<DeliveryChallan>>
    fun observeChallansForDeliveryOrder(deliveryOrderId: String): Flow<List<DeliveryChallan>>
    fun observeChallan(challanId: String): Flow<DeliveryChallan?>
    suspend fun getChallan(challanId: String): DeliveryChallan?
    suspend fun getChallanByNo(projectId: String, challanNo: String): DeliveryChallan?
    suspend fun getChallansForDeliveryOrder(deliveryOrderId: String): List<DeliveryChallan>
    suspend fun insertChallan(challan: DeliveryChallan, lines: List<DeliveryChallanLine>)
    suspend fun updateChallan(challan: DeliveryChallan)
    suspend fun updateChallanWithLines(challan: DeliveryChallan, lines: List<DeliveryChallanLine>)

    // Challan Lines
    fun observeChallanLines(challanId: String): Flow<List<DeliveryChallanLine>>
    suspend fun getChallanLines(challanId: String): List<DeliveryChallanLine>
    suspend fun getChallanLine(lineId: String): DeliveryChallanLine?
    suspend fun getLinesForChallans(challanIds: List<String>): List<DeliveryChallanLine>

    // Audit Events
    fun observeActivityEvents(challanId: String): Flow<List<DeliveryChallanActivityEvent>>
    suspend fun getActivityEvents(challanId: String): List<DeliveryChallanActivityEvent>
    suspend fun insertActivityEvent(event: DeliveryChallanActivityEvent)
}
