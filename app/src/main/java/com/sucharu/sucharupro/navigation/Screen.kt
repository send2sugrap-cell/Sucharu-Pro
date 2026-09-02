package com.sucharu.sucharupro.navigation

/**
 * Type-safe navigation screen destinations for Sucharu Pro.
 */
sealed class Screen(val route: String, val title: String) {
    data object Dashboard : Screen(route = "dashboard", title = "Dashboard")
    data object Customers : Screen(route = "customers", title = "Customers")
    data object Orders : Screen(route = "orders", title = "Orders")
    data object Printing : Screen(route = "printing", title = "Printing & Rates")
    data object Reports : Screen(route = "reports", title = "Reports & Analytics")
    data object Settings : Screen(route = "settings", title = "Settings")
    data object CustomerDetails : Screen(route = "customer/{customerId}", title = "Customer Details") {
        const val ARG_CUSTOMER_ID = "customerId"
        fun createRoute(customerId: String): String = "customer/$customerId"
    }
    data object CustomerCreate : Screen(route = "customer/create", title = "New Customer")
    data object CustomerEdit : Screen(route = "customer/edit/{customerId}", title = "Edit Customer") {
        const val ARG_CUSTOMER_ID = "customerId"
        fun createRoute(customerId: String): String = "customer/edit/$customerId"
    }
    data object InquiryDetails : Screen(route = "inquiry/{inquiryId}", title = "Inquiry Details") {
        const val ARG_INQUIRY_ID = "inquiryId"
        fun createRoute(inquiryId: String): String = "inquiry/$inquiryId"
    }
    data object InquiryCreate : Screen(route = "inquiry/create", title = "New Inquiry")
    data object InquiryEdit : Screen(route = "inquiry/edit/{inquiryId}", title = "Edit Inquiry") {
        const val ARG_INQUIRY_ID = "inquiryId"
        fun createRoute(inquiryId: String): String = "inquiry/edit/$inquiryId"
    }
    data object QuotationDetails : Screen(route = "quotation/{quotationId}", title = "Quotation Details") {
        const val ARG_QUOTATION_ID = "quotationId"
        fun createRoute(quotationId: String): String = "quotation/$quotationId"
    }
    data object QuotationCreate : Screen(
        route = "quotation/create?inquiryId={inquiryId}&customerId={customerId}",
        title = "New Quotation"
    ) {
        const val ARG_INQUIRY_ID = "inquiryId"
        const val ARG_CUSTOMER_ID = "customerId"
        fun createRoute(inquiryId: String? = null, customerId: String? = null): String {
            val inq = inquiryId ?: ""
            val cus = customerId ?: ""
            return "quotation/create?inquiryId=$inq&customerId=$cus"
        }
    }
    data object QuotationEdit : Screen(route = "quotation/edit/{quotationId}", title = "Edit Quotation") {
        const val ARG_QUOTATION_ID = "quotationId"
        fun createRoute(quotationId: String): String = "quotation/edit/$quotationId"
    }
    data object OrderDetails : Screen(route = "order/{orderId}", title = "Order Details") {
        const val ARG_ORDER_ID = "orderId"
        fun createRoute(orderId: String): String = "order/$orderId"
    }

    data object FinancialReconciliationDashboard : Screen(route = "finance/reconciliation", title = "Financial Reconciliation")
    data object FinancialReconciliationExecution : Screen(route = "finance/reconciliation/execute/{periodId}", title = "Execute Reconciliation") {
        const val ARG_PERIOD_ID = "periodId"
        fun createRoute(periodId: String): String = "finance/reconciliation/execute/$periodId"
    }
    data object FinancialDiscrepancies : Screen(route = "finance/discrepancies", title = "Financial Discrepancies")
    data object AccountingPeriods : Screen(route = "finance/periods", title = "Accounting Periods")
    data object ClosingChecklist : Screen(route = "finance/period/{periodId}/checklist", title = "Closing Review") {
        const val ARG_PERIOD_ID = "periodId"
        fun createRoute(periodId: String): String = "finance/period/$periodId/checklist"
    }
    data object ClosingSnapshot : Screen(route = "finance/period/{periodId}/snapshot", title = "Closing Snapshot") {
        const val ARG_PERIOD_ID = "periodId"
        fun createRoute(periodId: String): String = "finance/period/$periodId/snapshot"
    }
    data object PeriodReopenRequests : Screen(route = "finance/period/{periodId}/reopen-requests", title = "Reopen Requests") {
        const val ARG_PERIOD_ID = "periodId"
        fun createRoute(periodId: String): String = "finance/period/$periodId/reopen-requests"
    }

    // Module 10 Step 03 Internal Communication Screens
    data object InternalCommunicationDashboard : Screen(route = "internal-communication/dashboard", title = "Internal Communication")
    data object InternalCommunicationInbox : Screen(route = "internal-communication/inbox", title = "Communication Inbox")
    data object InternalCommunicationDetails : Screen(route = "internal-communication/details/{communicationId}", title = "Communication Details") {
        const val ARG_COMMUNICATION_ID = "communicationId"
        fun createRoute(communicationId: String): String = "internal-communication/details/$communicationId"
    }
    data object InternalCommunicationCompose : Screen(route = "internal-communication/compose", title = "New Communication")
    data object InternalCommunicationThread : Screen(route = "internal-communication/thread/{threadId}", title = "Communication Thread") {
        const val ARG_THREAD_ID = "threadId"
        fun createRoute(threadId: String): String = "internal-communication/thread/$threadId"
    }
    data object InternalCommunicationTeam : Screen(route = "internal-communication/team", title = "Team Communication")
    data object InternalCommunicationDepartment : Screen(route = "internal-communication/department", title = "Department Communication")
    data object InternalCommunicationBroadcast : Screen(route = "internal-communication/broadcast", title = "Broadcast Communication")

    data object TaskDashboard : Screen(route = "task/dashboard", title = "Task Dashboard")
    data object TaskList : Screen(route = "task/list", title = "Task List")
    data object TaskDetails : Screen(route = "task/details/{taskId}", title = "Task Details") {
        const val ARG_TASK_ID = "taskId"
        fun createRoute(taskId: String): String = "task/details/$taskId"
    }
    data object TaskCreate : Screen(route = "task/create", title = "New Task")
    data object TaskAssignment : Screen(route = "task/assignment/{taskId}", title = "Assign Task") {
        const val ARG_TASK_ID = "taskId"
        fun createRoute(taskId: String): String = "task/assignment/$taskId"
    }
    data object TaskProgress : Screen(route = "task/progress/{taskId}", title = "Task Progress") {
        const val ARG_TASK_ID = "taskId"
        fun createRoute(taskId: String): String = "task/progress/$taskId"
    }
    data object TaskBoard : Screen(route = "task/board", title = "Task Board")

    // Module 10 Step 05 Vendor & Supplier Communication Screens
    data object VendorCommunicationDashboard : Screen(route = "vendor-communication/dashboard", title = "Vendor Communications")
    data object VendorCommunicationCenter : Screen(route = "vendor-communication/center", title = "Communications Center")
    data object VendorCommunicationDetails : Screen(route = "vendor-communication/details/{communicationId}", title = "Communication Details") {
        const val ARG_COMMUNICATION_ID = "communicationId"
        fun createRoute(communicationId: String): String = "vendor-communication/details/$communicationId"
    }
    data object VendorCommunicationCompose : Screen(route = "vendor-communication/compose", title = "New Vendor Communication")
    data object VendorCommunicationThread : Screen(route = "vendor-communication/thread/{communicationId}", title = "Communication Thread") {
        const val ARG_COMMUNICATION_ID = "communicationId"
        fun createRoute(communicationId: String): String = "vendor-communication/thread/$communicationId"
    }
    data object VendorCommunicationHistory : Screen(route = "vendor-communication/history/{communicationId}", title = "Communication History") {
        const val ARG_COMMUNICATION_ID = "communicationId"
        fun createRoute(communicationId: String): String = "vendor-communication/history/$communicationId"
    }
    data object VendorCommunicationEngagement : Screen(route = "vendor-communication/engagement/{vendorId}", title = "Vendor Engagement") {
        const val ARG_VENDOR_ID = "vendorId"
        fun createRoute(vendorId: String): String = "vendor-communication/engagement/$vendorId"
    }
    data object VendorCommunicationAdmin : Screen(route = "vendor-communication/admin", title = "Vendor Comm Admin")
    data object VendorCommunicationSchedule : Screen(route = "vendor-communication/schedule", title = "Scheduled Communications")
    data object VendorCommunicationAcknowledgement : Screen(route = "vendor-communication/acknowledge/{communicationId}", title = "Acknowledge Communication") {
        const val ARG_COMMUNICATION_ID = "communicationId"
        fun createRoute(communicationId: String): String = "vendor-communication/acknowledge/$communicationId"
    }

    // Module 11 Step 06 Return Analytics & Governance Screens
    data object ReturnAnalyticsDashboard : Screen(route = "returns/analytics?projectId={projectId}", title = "Return Analytics") {
        const val ARG_PROJECT_ID = "projectId"
        fun createRoute(projectId: String): String = "returns/analytics?projectId=$projectId"
    }
    data object ReturnGovernanceCenter : Screen(route = "returns/governance?projectId={projectId}", title = "Return Governance") {
        const val ARG_PROJECT_ID = "projectId"
        fun createRoute(projectId: String): String = "returns/governance?projectId=$projectId"
    }

    // Module 17 Printing Calculator & Quotation Screens
    data object PrintingCalculatorWorkspace : Screen(route = "printing/calculator", title = "Printing Calculator")
    data object PrintingQuotationWorkspace : Screen(route = "printing/quotations", title = "Printing Quotations")

    // Module 19 Substrate Stock Auto-Reservation Screen
    data object SubstrateReservationCommandCenter : Screen(route = "inventory/substrate-reservation", title = "Substrate Stock Auto-Reservation")


    companion object {
        val topLevelDestinations: List<Screen> = listOf(
            Dashboard,
            Orders,
            Printing,
            Customers,
            Reports,
            Settings
        )
    }
}
