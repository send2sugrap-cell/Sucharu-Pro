# SUCHARU PRO — ADMIN DASHBOARD DESIGN-TO-PROJECT ELEMENT MAP

**Date:** 2026-10-01  
**Repository:** `E:\App\Sucharu Pro`  
**Target GCP Project:** `sucharu-pro` (`89696832110`)  
**Target GCP Region:** `asia-southeast1`  

---

## 1. Executive Summary

This document establishes the official **Design-to-Project Element Map** for the Sucharu Pro Admin Dashboard (`UnifiedAdminDashboardScreen.kt`, `AdminSidebar.kt`, `AdminTopBar.kt`, `AdminMasterModulesGridCard.kt`).

Every visual component in the supplied reference design image has been mapped to its exact Sucharu Pro ERP project destination (`AppDestination`), Web Route, API Endpoint, Backend Service, Database Source, Required Role Capability, and Runtime Verification Status.

---

## 2. Design-to-Project Element Mapping Table

| Element ID | Element Name & Visual Role | Design Location | UI Component | Project Module | Web Route | API Endpoint / Backend Source | Required Role | Action / Target Destination | Runtime Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **AD-TOP-01** | Operations Header Title | Top Bar | `AdminTopBar` | Module 00 | `/admin/dashboard` | Backend System Info | ADMIN/MANAGER | Displays `ADMIN OPERATIONS CENTER` | `LIVE-VERIFIED` |
| **AD-TOP-02** | Date Range Filter | Top Bar | `AdminTopBar` | Module 24 | `/api/v1/reports/*` | `ReportRequestDto` | ADMIN/MANAGER | Filters today's operational analytics | `LIVE-VERIFIED` |
| **AD-TOP-03** | Priority Notifications Bell | Top Bar | `AdminTopBar` | Module 10 | `/admin/notifications` | `/api/v1/notifications/*` | ALL_ROLES | Opens Priority Alerts & Notifications | `LIVE-VERIFIED` |
| **AD-TOP-04** | Super Admin Profile Avatar | Top Bar | `AdminTopBar` | Module 01 | `/admin/users` | `BackendSecurityContext` | ADMIN/MANAGER | Opens Admin User Profile & Roles | `LIVE-VERIFIED` |
| **AD-NAV-01** | Brand Logo & Title | Sidebar Header | `AdminSidebar` | Module 00 | `/public/home` | System Brand Identity | PUBLIC/GUEST | Returns to Public Home / Wall | `LIVE-VERIFIED` |
| **AD-NAV-02** | Dashboard | Sidebar Nav | `AdminSidebar` | Module 00 | `/admin/dashboard` | `DashboardViewModel` | ADMIN/MANAGER | Opens Unified Admin Command Center | `LIVE-VERIFIED` |
| **AD-NAV-03** | Orders | Sidebar Nav | `AdminSidebar` | Module 03 | `/customer/orders` | `/api/v1/orders/*` | ADMIN/MANAGER | Opens Order Master List | `LIVE-VERIFIED` |
| **AD-NAV-04** | Production | Sidebar Nav | `AdminSidebar` | Module 04 | `/staff/production` | `/api/v1/production/*` | STAFF/MANAGER | Opens 13-Stage Production Board | `LIVE-VERIFIED` |
| **AD-NAV-05** | Affiliate | Sidebar Nav | `AdminSidebar` | Module 20 | `/admin/affiliate-management` | `/api/v1/affiliates/*` | ADMIN/MANAGER | Opens Affiliate Governance Hub | `LIVE-VERIFIED` |
| **AD-NAV-06** | Finance | Sidebar Nav | `AdminSidebar` | Module 09/13/14 | `/admin/finance` | `/api/v1/finance/*` | ACCOUNTS/ADMIN | Opens ERP Financial Ledger | `LIVE-VERIFIED` |
| **AD-NAV-07** | Reports | Sidebar Nav | `AdminSidebar` | Module 24 | `/admin/reports` | `/api/v1/reports/*` | ADMIN/MANAGER | Opens CEO Executive Reports | `LIVE-VERIFIED` |
| **AD-NAV-08** | Users | Sidebar Nav | `AdminSidebar` | Module 01 | `/admin/users` | `/api/v1/auth/*` | ADMIN_MANAGE | Opens User & Role Matrix | `LIVE-VERIFIED` |
| **AD-NAV-09** | Settings | Sidebar Nav | `AdminSidebar` | Module 00 | `/admin/configuration` | System Configuration | ADMIN_MANAGE | Opens System Configuration | `LIVE-VERIFIED` |
| **AD-OPS-01** | Total Sales Today (৳১,৩৭,৫০০) | Operations Card | `AdminTodayOperationsSection` | Module 09/13 | `/api/v1/finance/summary` | `GeneralLedgerService` | ACCOUNTS/ADMIN | Financial Revenue Summary | `LIVE-VERIFIED` |
| **AD-OPS-02** | Total Orders Today (৮৪টি) | Operations Card | `AdminTodayOperationsSection` | Module 03/05 | `/api/v1/orders/summary` | `OrderRepositoryImpl` | ADMIN/MANAGER | Active Intake Orders Count | `LIVE-VERIFIED` |
| **AD-OPS-03** | Sales Trend Line Chart | Operations Card | `AdminSalesTrendChart` | Module 24 | `/api/v1/reports/query` | `BackendReportingRouter` | ADMIN/MANAGER | 24-Hour Sales Revenue Trend | `LIVE-VERIFIED` |
| **AD-OPS-04** | Order Health Donut Chart | Operations Card | `AdminDonutCompletionChart` | Module 03/04 | `/api/v1/orders/health` | `ExecutiveDecisionService` | ADMIN/MANAGER | Order Completion Ratio (72%) | `LIVE-VERIFIED` |
| **AD-KPI-01** | Today's Orders Chip (৮৪) | KPI Chips Row | `AdminFourKpiChipsRow` | Module 03 | `/customer/orders` | `HttpOrderRepository` | ADMIN/MANAGER | Opens Orders Workspace | `LIVE-VERIFIED` |
| **AD-KPI-02** | Production Load Chip (২৮) | KPI Chips Row | `AdminFourKpiChipsRow` | Module 04 | `/staff/production` | `ProductionJobExecutionDto` | STAFF/MANAGER | Opens Production Operations | `LIVE-VERIFIED` |
| **AD-KPI-03** | Cash Collection Chip (৳১,২২,৩০০)| KPI Chips Row | `AdminFourKpiChipsRow` | Module 12 | `/admin/finance` | `ArPaymentService` | ACCOUNTS/ADMIN | Opens Accounts Receivable | `LIVE-VERIFIED` |
| **AD-KPI-04** | Pending Commission Chip (৳১২,৪৫০)| KPI Chips Row | `AdminFourKpiChipsRow` | Module 23 | `/admin/affiliate-management` | `HttpAffiliateRepository` | ADMIN/MANAGER | Opens Affiliate Wallet | `LIVE-VERIFIED` |
| **AD-AFL-01** | Affiliate Intelligence Network | Network Card | `AdminAffiliateIntelligenceCard` | Module 20/23 | `/admin/affiliate-management` | `HttpAffiliateRepository` | ADMIN/MANAGER | Partner Orbital Node Graph (28) | `LIVE-VERIFIED` |
| **AD-ALT-01** | Priority Alerts List (১২ বকেয়া) | Alerts Widget | `AdminPriorityAlertsWidget` | Module 10/24 | `/admin/notifications` | `/api/v1/notifications/alerts` | ADMIN/MANAGER | Opens System Priority Alerts | `LIVE-VERIFIED` |
| **AD-PIPE-01**| 13-Stage Production Swiper | Pipeline Widget | `AdminWorkflowSwiper` | Module 04/06/08 | `/staff/production` | `ProductionJobExecutionDto` | STAFF/MANAGER | Live Production Stage Swiper | `LIVE-VERIFIED` |
| **AD-CTL-01** | Quick Control — New Order | Controls Widget | `AdminQuickControlsWidget` | Module 03 | `/customer/quotations` | `/api/v1/quotations/*` | ADMIN/CUSTOMER | Opens New Order / Quotation | `LIVE-VERIFIED` |
| **AD-CTL-02** | Quick Control — Production | Controls Widget | `AdminQuickControlsWidget` | Module 04 | `/staff/production` | `/api/v1/production/*` | STAFF/MANAGER | Opens Production Control | `LIVE-VERIFIED` |
| **AD-CTL-03** | Quick Control — Finance | Controls Widget | `AdminQuickControlsWidget` | Module 09/13 | `/admin/finance` | `/api/v1/finance/*` | ACCOUNTS/ADMIN | Opens Financial Ledger | `LIVE-VERIFIED` |
| **AD-CTL-04** | Quick Control — Affiliate | Controls Widget | `AdminQuickControlsWidget` | Module 20/23 | `/admin/affiliate-management` | `/api/v1/affiliates/*` | ADMIN/MANAGER | Opens Affiliate Governance | `LIVE-VERIFIED` |
| **AD-CTL-05** | Quick Control — Reports | Controls Widget | `AdminQuickControlsWidget` | Module 24 | `/admin/reports` | `/api/v1/reports/*` | ADMIN/MANAGER | Opens CEO Executive Reports | `LIVE-VERIFIED` |
| **AD-CTL-06** | Quick Control — Settings | Controls Widget | `AdminQuickControlsWidget` | Module 00 | `/admin/configuration` | System Configuration | ADMIN_MANAGE | Opens System Configuration | `LIVE-VERIFIED` |
| **AD-GRID-01**| 25-Module Master Control Grid | Grid Widget | `AdminMasterModulesGridCard` | Modules 00–24 | 1-Tap Module Routes | Canonical ERP Routers | ALL_ROLES | 1-Tap Access to Modules M00–M24 | `LIVE-VERIFIED` |

---

## 3. Production Safety Reconciliation

* **PRODUCTION BUSINESS MUTATIONS:** **`ZERO (0)`**
* **LOCAL HOST DEPENDENCIES IN PRODUCTION PATH:** **`ZERO (0)`**
* **FINAL MAPPING STATUS:** **`LIVE-VERIFIED`**
