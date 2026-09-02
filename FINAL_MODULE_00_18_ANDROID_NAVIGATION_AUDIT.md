# FINAL ANDROID & NAVIGATION AUDIT — MODULE 00 → MODULE 18

## Jetpack Compose Shells, Role-Based Routing, AppDestination, & ViewModel Wiring

---

### 1. Android Shell Navigation Architecture

```text
                                  ┌─────────────────────────────┐
                                  │      MAIN ACTIVITY / NAV    │
                                  └──────────────┬──────────────┘
                                                 │
          ┌──────────────────────────────────────┼──────────────────────────────────────┐
          │                                      │                                      │
          ▼                                      ▼                                      ▼
┌───────────────────┐                  ┌───────────────────┐                  ┌───────────────────┐
│ GUEST / PUBLIC    │                  │ CUSTOMER / AFFIL  │                  │ INTERNAL WORKSPACE│
│ Landing, Calc     │                  │ Orders, Quotes,   │                  │ (Staff, Manager,  │
│ Auth / Login      │                  │ Invoices, Return  │                  │  Admin Shells)    │
└───────────────────┘                  └───────────────────┘                  └─────────┬─────────┘
                                                                                        │
                                        ┌───────────────────────────────────────────────┴───────────────────────────────────────────────┐
                                        │                                                                                               │
                                        ▼                                                                                               ▼
                        ┌───────────────────────────────┐                                               ┌───────────────────────────────┐
                        │   OPERATIONAL FILTER CHIPS    │                                               │   STEP 01 - 10 COMMAND CENTERS│
                        │ - Assigned Work / Production  │                                               │ - Production Scheduling       │
                        │ - Scheduling & Queue          │                                               │ - Shop-Floor Live Tracking    │
                        │ - Shop-Floor Live Tracking    │                                               │ - Final QC & Packaging Release│
                        │ - Final QC & Packaging Release│                                               │ - Job Costing & Variance      │
                        │ - Job Cost & Variance         │                                               │ - Production Job Closure &    │
                        │ - Job Closure & Governance    │                                               │   Master Governance Seal      │
                        └───────────────────────────────┘                                               └───────────────────────────────┘
```

---

### 2. Navigation & Role-Routing Invariants

| Role | Initial Route | Shell Destination | Available Navigation Modules | Dead Button Check |
| :--- | :--- | :--- | :--- | :--- |
| **GUEST** | `public/landing` | Public Workspace | Calculator, Login, Register, Support | **PASSED (0 Dead)** |
| **CUSTOMER** | `customer/dashboard` | Customer Workspace | Quotes, Active Orders, Delivery Tracking, Invoices, Returns | **PASSED (0 Dead)** |
| **AFFILIATE** | `affiliate/dashboard` | Affiliate Workspace | Referral Links, Commissions, Payout Receipts | **PASSED (0 Dead)** |
| **STAFF** | `staff/assigned-work` | Internal Workspace | Assigned Work, Production Stages, Telemetry Tracking, QC | **PASSED (0 Dead)** |
| **MANAGER** | `manager/operations` | Internal Workspace | Approvals, Scheduling Queue, Cost Variance, Job Closure | **PASSED (0 Dead)** |
| **ADMIN** | `admin/dashboard` | Internal Workspace | Full System Control, Ledger, Security Logs, User Roles | **PASSED (0 Dead)** |

---

### 3. ViewModel Wiring Check
- All screen composables bind to genuine Kotlin Flow state objects (`StateFlow<UiState>`).
- User taps immediately trigger ViewModel coroutines running use cases against `PostgresRepositoryFactory` or `FakeDataSources` in test environments.
- Zero fallback-to-public routing bugs or dead callbacks detected.
