package com.sucharu.sucharupro.ui.features.orders

import com.sucharu.sucharupro.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailNavigationIntegrationTest {

    @Test
    fun inquiryDetails_routeCreation_isCorrect() {
        assertEquals("inquiry/{inquiryId}", Screen.InquiryDetails.route)
        assertEquals("inquiry/inq-12345", Screen.InquiryDetails.createRoute("inq-12345"))
        assertEquals("inquiryId", Screen.InquiryDetails.ARG_INQUIRY_ID)
    }

    @Test
    fun quotationDetails_routeCreation_isCorrect() {
        assertEquals("quotation/{quotationId}", Screen.QuotationDetails.route)
        assertEquals("quotation/quo-67890", Screen.QuotationDetails.createRoute("quo-67890"))
        assertEquals("quotationId", Screen.QuotationDetails.ARG_QUOTATION_ID)
    }

    @Test
    fun orderDetails_routeCreation_isCorrect() {
        assertEquals("order/{orderId}", Screen.OrderDetails.route)
        assertEquals("order/ord-54321", Screen.OrderDetails.createRoute("ord-54321"))
        assertEquals("orderId", Screen.OrderDetails.ARG_ORDER_ID)
    }

    @Test
    fun customerDetails_routeCreation_isCorrect() {
        assertEquals("customer/{customerId}", Screen.CustomerDetails.route)
        assertEquals("customer/cus-101", Screen.CustomerDetails.createRoute("cus-101"))
        assertEquals("customerId", Screen.CustomerDetails.ARG_CUSTOMER_ID)
    }
}
