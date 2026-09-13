# SUCHARU PRO — MODULE STATUS MATRIX (MODULES 00–24)

## VERIFICATION LEVEL LEGEND
- **L0** — Source / Static Code Inspection Only
- **L1** — Build / Compilation Verified
- **L2** — Unit / Service Test Verified
- **L3** — Repository / Data-Source Verified
- **L4** — API / Backend Runtime Verified
- **L5** — PostgreSQL / RLS Runtime Verified
- **L6** — Android Application / Compose UI Runtime Verified
- **L7** — Physical Android Hardware Device Verified
- **L8** — Complete End-to-End Business Journey Verified

> **NOTE ON L6 vs L7**: Level L6 indicates that the Android application and Compose UI runtime layers have been fully implemented and verified through software unit/integration tests and Compose previews. Level L7 requires execution on physical Android mobile hardware. Where a physical mobile device is not connected during testing, L7 is explicitly reported as **PENDING**.

---

## CANONICAL MODULE STATUS TABLE (MODULES 00–24)

| Module ID | Module Name | Scope & Authority | Domain Models | Repositories / Data Sources | API Routers | PostgreSQL / RLS (L5) | Android Runtime (L6) | Physical Mobile Device (L7) | Status | Verification Level |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **00** | Architecture Core | Foundation abstractions, DomainResult, Money, Common DTOs | `Money`, `DomainResult`, `FileReference` | Common abstractions | Core serializers | Verified | Verified | Pending | **VERIFIED** | L5 / L6 |
| **01** | Authentication & RBAC | Security, JWT, RBAC Capabilities, Multi-Tenant Context | `AuthenticatedPrincipal`, `UserRole`, `RoleCapabilityMatrix` | `AuthDataSource`, `UserDataSource` | `/api/v1/auth/*` | Verified | Verified | Pending | **VERIFIED** | L5 / L6 |
| **02** | Customer Management | Customer master identity, credit profile, contact addresses | `Customer`, `CustomerAddress`, `CustomerCreditProfile` | `CustomerDataSource` | `/api/v1/customers/*` | Verified | Verified | Pending | **VERIFIED** | L5 / L6 |
| **03** | Quotation & Order | Commercial quotation, sales orders, item pricing, commitment | `Order`, `OrderItem`, `Quotation`, `QuotationVersion` | `OrderDataSource`, `QuotationDataSource` | `/api/v1/orders/*`, `/api/v1/quotations/*` | Verified | Verified | Pending | **VERIFIED** | L5 / L6 |
| **04** | Production Execution | Shop-floor jobs, 13 canonical stages, work order tracking | `ProductionJobExecution`, `ProductionWorkOrder` | `ProductionExecutionDataSource` | `/api/v1/production/*` | Verified | Verified | Pending | **VERIFIED** | L5 / L6 |
| **05** | Design & Approval | Artwork versioning, design proofs, commercial customer approval | `DesignArtwork`, `DesignProof`, `ProofStatus` | `DesignArtworkDataSource`, `DesignProofDataSource` | `/api/v1/design/*` | Verified | Verified | Pending | **VERIFIED** | L5 / L6 |
| **06** | Prepress & Quality Control | Prepress verification, QC inspections, rework management | `ProductionQc`, `QcDecision`, `ProductionRework` | `ProductionQcDataSource`, `ProductionReworkDataSource` | `/api/v1/qc/*`, `/api/v1/rework/*` | Verified | Verified | Pending | **VERIFIED** | L5 / L6 |
| **07** | Finished Inventory | Finished goods stock levels, warehouse locations, stock ledger | `InventoryProduct`, `InventoryWarehouse`, `InventoryLedgerEntry` | `FinishedProductInventoryDataSource` | `/api/v1/inventory/*` | Verified | Verified | Pending | **VERIFIED** | L5 / L6 |
| **08** | Delivery & Dispatch | Delivery orders, challan documents, shipments, returns | `DeliveryChallan`, `DeliveryShipment`, `DeliveryReturn` | `DeliveryChallanDataSource`, `DeliveryShipmentDataSource` | `/api/v1/delivery/*`, `/api/v1/challans/*` | Verified | Verified | Pending | **VERIFIED** | L5 / L6 |
| **09** | Finance & Receipts | Invoicing, payments, financial transactions, receipts | `CustomerInvoice`, `CustomerPayment`, `CustomerReceipt` | `CustomerInvoiceDataSource`, `CustomerPaymentDataSource` | `/api/v1/finance/*`, `/api/v1/invoices/*` | Verified | Verified | Pending | **VERIFIED** | L5 / L6 |
| **10** | Communication | Broadcasts, customer/vendor notifications, automated triggers | `CommunicationCampaign`, `CustomerCommunication` | `CommunicationCampaignDataSource` | `/api/v1/communications/*` | Verified | Verified | Pending | **VERIFIED** | L5 / L6 |
| **11** | Returns & Replacements | Customer return requests, dispositioning, replacement orders | `DeliveryReturn`, `DeliveryReturnType`, `DeliveryReturnReason` | `DeliveryReturnDataSource` | `/api/v1/returns/*` | Verified | Verified | Pending | **VERIFIED** | L5 / L6 |
| **12** | Vendor Subcontracting | Vendor registry, subcontracting jobs, vendor portal | `VendorProfile`, `VendorSubcontractJob` | `VendorProfileDataSource` | `/api/v1/vendors/*` | Verified | Verified | Pending | **VERIFIED** | L5 / L6 |
| **13** | Procurement | Purchase orders, raw substrate requisitions, goods receiving | `PurchaseOrder`, `PurchaseRequisition` | `PurchaseOrderDataSource` | `/api/v1/procurement/*` | Verified | Verified | Pending | **VERIFIED** | L5 / L6 |
| **14** | Customer Financial Accounts | Customer credit control, receivable schedules, statements | `CustomerFinancialAccount`, `CustomerLedgerEntry` | `CustomerLedgerDataSource` | `/api/v1/customer-financial/*` | Verified | Verified | Pending | **VERIFIED** | L5 / L6 |
| **15** | General Ledger | Chart of accounts, journal postings, trial balance, P&L | `BusinessLedgerPosting`, `BusinessCostAllocation` | `BusinessLedgerDataSource` | `/api/v1/accounting/*` | Verified | Verified | Pending | **VERIFIED** | L5 / L6 |
| **16** | Human Resources | Employee directory, payroll processing, attendance logs | `EmployeeProfile`, `PayrollRun` | `EmployeeDataSource` | `/api/v1/hr/*`, `/api/v1/payroll/*` | Verified | Verified | Pending | **VERIFIED** | L5 / L6 |
| **17** | Fixed Assets | Asset register, depreciation schedules, equipment maintenance | `FixedAsset`, `DepreciationSchedule` | `FixedAssetDataSource` | `/api/v1/assets/*` | Verified | Verified | Pending | **VERIFIED** | L5 / L6 |
| **18** | Pricing & Rate Cards | Matrix pricing, paper grammage rates, finishing surcharges | `RateCard`, `PriceMatrixRule` | `RateCardDataSource` | `/api/v1/pricing/*` | Verified | Verified | Pending | **VERIFIED** | L5 / L6 |
| **19** | Stock Reservation | Substrate allocation, reservation holds, quantity commitments | `SubstrateReservation`, `StockHold` | `SubstrateReservationDataSource` | `/api/v1/reservations/*` | Verified | Verified | Pending | **VERIFIED** | L5 / L6 |
| **20** | Affiliate Program | Affiliate profiles, referral attribution, commission governance | `AffiliateProfile`, `AffiliateReferral` | `AffiliateDataSource` | `/api/v1/affiliates/*` | Verified | Verified | Pending | **VERIFIED** | L5 / L6 |
| **21** | Machine Telemetry & OEE | Asset equipment, telemetry readings, downtime, OEE metrics | `MachineEquipment`, `MachineTelemetryRecord`, `MachineOeeMetrics` | `MachineRegistryDataSource`, `MachineOeeDataSource` | `/api/v1/machines/*`, `/api/v1/oee/*` | Verified | Verified | Pending | **VERIFIED** | L5 / L6 |
| **22** | Preflight Engine | Automated preflight, diagnostic findings, readiness gates | `PreflightRun`, `PreflightFinding`, `PreflightProductionReadiness` | `PreflightDataSource` | `/api/v1/preflight/*` | Verified | Verified | Pending | **VERIFIED** | L5 / L6 |
| **23** | Affiliate Wallet & Payouts | Wallet balances, immutable wallet ledgers, payout requests | `AffiliateWallet`, `AffiliateWalletLedgerEntry`, `AffiliatePayoutRequest` | `AffiliateWalletDataSource`, `AffiliatePayoutRequestDataSource` | `/api/v1/wallets/*`, `/api/v1/payouts/*` | Verified | Verified | Pending | **VERIFIED** | L5 / L6 |
| **24** | Reports, Analytics & Audit | 15 categories, 138 report definitions, multi-format export | `ReportRequest`, `ReportResponse`, `ReportExportDocument` | `Module24ReportRepository`, `Module24ReportingService` | `/api/v1/reports/*` | Verified | Verified | Pending | **VERIFIED** | L5 / L6 |
