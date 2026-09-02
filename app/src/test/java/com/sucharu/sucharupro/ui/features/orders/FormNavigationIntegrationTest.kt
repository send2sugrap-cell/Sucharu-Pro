package com.sucharu.sucharupro.ui.features.orders

import com.sucharu.sucharupro.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Test

class FormNavigationIntegrationTest {

    @Test
    fun inquiryCreateRoute_generatesCorrectPath() {
        assertEquals("inquiry/create", Screen.InquiryCreate.route)
    }

    @Test
    fun inquiryEditRoute_generatesCorrectPathWithInquiryId() {
        val route = Screen.InquiryEdit.createRoute("inq-789")
        assertEquals("inquiry/edit/inq-789", route)
    }

    @Test
    fun quotationCreateRoute_generatesCorrectPathWithOptionalParams() {
        val routeWithParams = Screen.QuotationCreate.createRoute(inquiryId = "inq-101", customerId = "cus-001")
        assertEquals("quotation/create?inquiryId=inq-101&customerId=cus-001", routeWithParams)

        val defaultRoute = Screen.QuotationCreate.createRoute()
        assertEquals("quotation/create?inquiryId=&customerId=", defaultRoute)
    }

    @Test
    fun quotationEditRoute_generatesCorrectPathWithQuotationId() {
        val route = Screen.QuotationEdit.createRoute("quo-555")
        assertEquals("quotation/edit/quo-555", route)
    }
}
