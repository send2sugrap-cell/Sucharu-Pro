# SUCHARU PRO — EVIDENCE LEDGER
### Authoritative Verification & Test Evidence Index

---

## 1. MASTER EVIDENCE INDEX

| Record ID | Scope | Commit SHA | Primary Files & Schema | Test Results | Build Status | Runtime Status | Evidence Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **EV-MOD-00-24** | Modules 00–24 | `9ba295c` | `docs/audit/MASTER_AUDIT_MODULE_00_24_FINAL.md` | 3,900+ Unit/ViewModel Tests Passed | `assembleDebug` PASSED | **PASS WITH GAPS** (L1–L6 Passed, L7 Hardware Pending) | **LOCKED** |
| **EV-FORM-01** | Form 01 | `f60c234` | `ContentFoundation.kt`, `V20261209` SQL | `ContentFoundationServiceTest.kt` Passed | `assembleDebug` PASSED | **RUNTIME VERIFICATION BLOCKED** (Docker Engine Unavailable) | **SOURCE VERIFIED** |
| **EV-FORM-02** | Form 02 | `30098f3` / `a1e4dd3` / `30098f3` | `VisualDesignConfiguration.kt`, `V20261210`, `V20261211`, `V20261212` SQL | `VisualDesignServiceTest.kt` Passed | `assembleDebug` PASSED | **RUNTIME VERIFICATION BLOCKED** (Docker Engine Unavailable) | **SOURCE VERIFIED** |
| **EV-FORM-03** | Form 03 | `155eaa0` | `PromotionalOffer.kt`, `V20261213` SQL | `PromotionalOfferServiceTest.kt` Passed (Eligibility $\neq$ Wall Visibility) | `assembleDebug` PASSED | **RUNTIME VERIFICATION BLOCKED** (Docker Engine Unavailable) | **SOURCE VERIFIED** |
| **EV-FORM-04** | Form 04 | `13dcc27` | `ProductPriceConfiguration.kt`, `OrderPriceSnapshot.kt`, `V20261214` SQL | `CommercialPricingServiceTest.kt` Passed (Historical Price Invariant ৳350 Unchanged) | `assembleDebug` PASSED | **RUNTIME VERIFICATION BLOCKED** (Docker Engine Unavailable) | **SOURCE VERIFIED** |
| **EV-FORM-05** | Form 05 | `ed8a249` | `ErpWorkflowOrchestration.kt`, `V20261215` SQL | `ErpWorkflowOrchestrationServiceTest.kt` Passed (E2E Order Orchestration) | `assembleDebug` PASSED | **RUNTIME VERIFICATION BLOCKED** (Docker Engine Unavailable) | **SOURCE VERIFIED** |
| **EV-FORM-06** | Form 06 | `e1334e0` | `ServerDrivenWallConfig.kt`, `V20261216` SQL | `ServerDrivenWallServiceTest.kt` Passed (Server-Driven Resolver) | `assembleDebug` PASSED | **RUNTIME VERIFICATION BLOCKED** (Docker Engine Unavailable) | **SOURCE VERIFIED** |
| **EV-DOCS-AUDIT** | Docs Audit | `e1334e0` | `docs/` (108 files inspected) | Read-Only Inspection | Clean Tree | N/A | **VERIFIED** |

---

## 2. DETAILED TEST EVIDENCE LOGS

### A. Form 01 Test Evidence
- **Test File**: `core/src/test/java/com/sucharu/sucharupro/domain/service/content/ContentFoundationServiceTest.kt`
- **Tests Executed**:
  1. `createContentRecord_persistsValidContentFoundationEntity` — **PASSED**
  2. `publishContent_changesStatusToPublished` — **PASSED**
  3. `scheduleContent_setsScheduledTimestampsAndPublicationState` — **PASSED**

### B. Form 02 Test Evidence
- **Test File**: `core/src/test/java/com/sucharu/sucharupro/domain/service/design/VisualDesignServiceTest.kt`
- **Tests Executed**:
  1. `createDesignStudioConfig_persistsValidVisualDesignConfiguration` — **PASSED**
  2. `publishDesignStudioVersion_createsImmutableVersionSnapshot` — **PASSED**
  3. `duplicateDesignStudioConfig_createsIsolatedDraftConfiguration` — **PASSED**

### C. Form 03 Test Evidence
- **Test File**: `core/src/test/java/com/sucharu/sucharupro/domain/service/offer/PromotionalOfferServiceTest.kt`
- **Tests Executed**:
  1. `criticalInvariant_offerEligibilityIsNotEqualWallVisibility` — **PASSED** (Proves `Affiliate Wall = ON` while `Affiliate Eligibility = OFF` allows teaser visibility while blocking redemption!)
  2. `criticalInvariant_eligibleCustomerCanRedeemEvenIfHiddenFromCustomerWall` — **PASSED** (Proves direct deal link allows customer redemption even when hidden from wall!)
  3. `evaluateEligibility_failsWhenOrderQuantityBelowThreshold` — **PASSED**

### D. Form 04 Test Evidence
- **Test File**: `core/src/test/java/com/sucharu/sucharupro/domain/service/pricing/CommercialPricingServiceTest.kt`
- **Tests Executed**:
  1. `mandatoryHistoricalPriceInvariant_orderPriceSnapshotRemainsUnchangedWhenMasterPriceIncreases` — **PASSED** (Proves Order `#ORD-1001` grand total remained **৳410 UNCHANGED** after Master Price increased from ৳350 to ৳500 in Version 2!)
  2. `evaluateCommercialPrice_matchesHigherQuantityTierPriceCorrectly` — **PASSED**

### E. Form 05 Test Evidence
- **Test File**: `core/src/test/java/com/sucharu/sucharupro/domain/service/erp/ErpWorkflowOrchestrationServiceTest.kt`
- **Tests Executed**:
  1. `orchestrateCustomerOrderAction_createsOrderAndPreservesPriceSnapshot` — **PASSED** (Proves E2E customer order action $\rightarrow$ commercial price evaluation $\rightarrow$ order creation $\rightarrow$ price snapshot preservation!)

### F. Form 06 Test Evidence
- **Test File**: `core/src/test/java/com/sucharu/sucharupro/domain/service/wall/ServerDrivenWallServiceTest.kt`
- **Tests Executed**:
  1. `resolveServerDrivenWall_returnsActiveSectionsForGuestAudience` — **PASSED** (Proves server-driven wall resolver returns active sections for Guest audience!)
  2. `publishWallVersion_createsImmutablePublishingSnapshot` — **PASSED**
