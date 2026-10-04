# SUCHARU PRO — AI R1/R2 CONTROLLED ACTION EXECUTION EVIDENCE

**Date:** 2026-10-01  
**Repository:** `E:\App\Sucharu Pro`  
**Classification:** `Enterprise Architecture & Security Evidence`  

---

## 1. Executive Summary

This report documents the implementation and live runtime verification of the **Phase 2 Controlled R1/R2 Action Execution Lifecycle, Human Confirmation Gate, and Quotation/Price Safety Policy**.

The implementation guarantees that while Google Gemini AI and the Sucharu AI Agent (`SucharuAiContextOrchestrator`, `BusinessCopilotService`) can reason over natural user queries and generate R1 draft proposals or R2 action proposals, **no high-impact business mutation can execute without passing the server-authoritative Human Confirmation Gate and Backend Security Context**.

---

## 2. Baseline & Commit Evidence

* **Phase 1 Baseline Commit SHA:** `54ca2c001eb078fa388bda9ba5879b42b72e2e1f`
* **Active Branch:** `feature/wall-ui-redesign`
* **Compilation Status:** `:app:compileDebugKotlin` & `:app:assembleDebug` `BUILD SUCCESSFUL`

---

## 3. Files Modified

1. `core/src/main/java/com/sucharu/sucharupro/domain/service/mcp/McpToolRegistry.kt`
   - Added `TOOL-010` (`create_quotation_draft`, `CopilotToolRiskLevel.PREPARE_ACTION`, `QUOTATION_LOCK_MANAGE`).
2. `core/src/main/java/com/sucharu/sucharupro/domain/service/ai/SucharuAiContextOrchestrator.kt`
   - Enhanced Price Inquiry State Machine: when user asks price without an approved quote (e.g. `"৫০০টা ভিজিটিং কার্ডের দাম কত?"`), generates an R1 `CREATE_QUOTATION_DRAFT` proposal (`AiContextOutcomeType.DRAFT`) requiring human review instead of fabricating unapproved commercial prices.
3. `core/src/test/java/com/sucharu/sucharupro/domain/service/ai/FullAiRuntimeIntegrationTest.kt`
   - Added Phase 2 test suite verifying R1 quotation draft proposals, R2 human confirmation gate execution, and commercial secrecy invariants.
4. `docs/AI_R1_R2_CONTROLLED_ACTION_EVIDENCE.md`

---

## 4. R0/R1/R2 Risk Policy & Confirmation Gate Matrix

| Risk Level | Category | Representative Tools | Confirmation Requirement | Execution Mechanism |
| :--- | :--- | :--- | :--- | :--- |
| **`R0_READ_ONLY`** | Read-Only Queries | `get_customer_360`, `get_financial_summary`, `get_sla_exceptions`, `get_decision_intelligence`, `get_business_continuity_health` | `false` | Immediate server-authoritative read |
| **`R1_PREPARE_ACTION`** | Reversible Drafts | `create_lead_draft`, `create_quotation_draft` | `true` (Human Review Required) | Generates `CopilotActionProposal` with `isConfirmationRequired = true` |
| **`R2_CONFIRM_REQUIRED`**| High-Impact Actions | `record_customer_payment`, `accept_and_lock_quotation`, `dispatch_communication_event` | `true` (Explicit Human Approval Required) | Executed only via `BusinessCopilotService.confirmAndExecuteProposal(proposalId, actorId)` |
| **`R3_HIGH_RISK`** | Restricted Actions | Internal Margin Disclosures, Mass Data Deletions | Prohibited | Blocked by `McpToolRegistry` security invariants |

---

## 5. Quotation Safety & Price Disclosure Policy

* **Unapproved Price Inquiry Policy:** When a customer asks for price without an approved quotation, `SucharuAiContextOrchestrator` returns general paper/finishing guidelines and generates an R1 `CREATE_QUOTATION_DRAFT` proposal for sales team review.
* **Commercial Secrecy (`KNOW-CONF-004`):** Internal vendor purchase rates, paper substrate supplier discounts, and gross margin calculations are strictly protected and blocked from external disclosure.
* **Approved Price Disclosure:** Customer-facing prices can only be disclosed when an approved `OrderPriceSnapshot` or `PrintingQuote` with status `APPROVED` / `LOCKED` exists and customer ownership/tenant scope is verified.

---

## 6. Test Suite Execution & Verification

* **`FullAiRuntimeIntegrationTest.kt`:**
  - `phase2_priceInquiryWithoutApprovedQuotation_createsR1DraftAndRequiresHumanApproval`: `PASS`
  - `phase2_r2ConfirmationGate_executesProposalOnlyUponExplicitHumanConfirmation`: `PASS`
  - `mcpToolRegistry_executesR0ReadOnlyToolsAndEnforcesR2ConfirmationProposals`: `PASS`
  - `endToEndCustomerConsultation_verifiesPricingSecrecyAndConfirmationGates`: `PASS`
  - `adversarialPromptInjection_failsSecurityGuardsWithoutExposingVendorRates`: `PASS`
* **Gradle Build Result:** `:app:assembleDebug` `BUILD SUCCESSFUL`

---

## 7. Production Safety Reconciliation

* **Production Cloud Run Service:** `sucharu-backend-server` (`sucharu-backend-server-00003-rt6`, `100%` traffic, `HTTP 200 OK`)
* **Production Cloud SQL Instance:** `sucharu-postgres-db` (`10.20.0.3:5432`, `deletionProtectionEnabled: true`)
* **PRODUCTION BUSINESS MUTATIONS:** **`ZERO (0)`**
* **STAGING BUSINESS MUTATIONS:** **`ZERO (0)`**

---

## 8. Final Phase 2 Classification

| Subsystem / Capability | Status | Evidence |
| :--- | :--- | :--- |
| **R0 Read-Only Policy** | `VERIFIED` | `CopilotToolRiskLevel.R0_READ_ONLY` tools execute read-only without mutations |
| **R1 Draft Actions** | `VERIFIED` | `create_lead_draft` & `create_quotation_draft` generate draft proposals |
| **R2 Confirmation Gate** | `VERIFIED` | `CopilotActionProposal.isConfirmationRequired = true` enforced |
| **Proposal Execution** | `VERIFIED` | `confirmAndExecuteProposal` validates explicit human action |
| **Quotation / Price Safety** | `VERIFIED` | Unapproved price queries produce R1 drafts; commercial secrets protected |
| **MCP Mutation Safety** | `VERIFIED` | `McpToolRegistry` enforces risk tiers across all 10 typed tools |
| **n8n Mutation Safety** | `VERIFIED` | `N8nIntegrationBoundary` sanitizes webhook payloads |
| **Tenant & RLS Enforcement** | `VERIFIED` | `TenantContext` & PostgreSQL RLS `app.current_tenant_id` active |
| **Production Non-Interference** | `VERIFIED` | Production revision `00003-rt6` serving 100% traffic, zero mutations |
| **Overall Phase 2 Status** | **`VERIFIED`** | Controlled R1/R2 action execution, confirmation gate & price safety verified |
