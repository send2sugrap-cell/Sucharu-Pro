# SUCHARU PRO — GCP DEPLOYMENT PREPARATION
## PUBLIC WALL AI CANONICAL CONNECTION FIX AUDIT REPORT

**Date:** 2026-09-30  
**Repository:** `E:\App\Sucharu Pro`  
**Target GCP Project:** `sucharu-pro` (`89696832110`)  
**Target GCP Region:** `asia-southeast1`  

---

## 1. Executive Summary

This audit report documents the surgical connection repair and staging E2E verification for the **Public Wall AI Assistant** (`AiAssistantChatScreen.kt` & `PublicAiAssistantView`).

The root cause of the AI connection failure (`"দুঃখিত, সার্ভারের সাথে সংযোগে সমস্যা হচ্ছে।"`) was identified: the client-side UI called `FirebaseAiLogicProvider`'s direct `GenerativeModel.generateContent` SDK method, which failed on mobile clients whenever direct API keys were unpopulated or client-side Gemini requests hit network exceptions.

The Public Wall AI Assistant was rewired to delegate directly to the canonical **Sucharu AI Agent & Context Orchestration Pipeline** (`SucharuAiContextOrchestrator.kt`, `BusinessCopilotService.kt`, `CopilotToolRiskLevel.R0_READ_ONLY`, and `SucharuKnowledgeRAGProvider.kt`).

---

## 2. Baseline & Commit Evidence

* **Git HEAD SHA:** `afb6534cb0f39999b160376dd96dc3260b86c8fd`
* **Active Branch:** `feature/wall-ui-redesign`
* **Build Task:** `:app:assembleDebug` (`BUILD SUCCESSFUL`)

---

## 3. Root Cause Analysis

* **Defect:** Public Wall AI called client-side Google AI Client SDK (`com.google.ai.client.generativeai.GenerativeModel`) directly.
* **Failure Mode:** Client-side missing API key or network exception threw `IllegalStateException("API Key is missing")`, caught in `AiAssistantChatScreen.kt` line 125, which displayed the fallback error: `"দুঃখিত, সার্ভারের সাথে সংযোগে সমস্যা হচ্ছে। কিছুক্ষণ পর আবার চেষ্টা করুন।"`.
* **Architectural Gap:** Direct client-side SDK invocation bypassed the server-side `SucharuAiContextOrchestrator`, `CopilotToolRiskLevel`, `SucharuKnowledgeRAGProvider`, and `BackendSecurityContext`.

---

## 4. Architecture Path Reconciliation

### Old Path (Broken):
```text
Public Wall UI
→ AiAssistantChatScreen.kt
→ FirebaseAiLogicProvider.kt
→ Direct client-side GenerativeModel.generateContent()
→ Exception / Missing API Key
→ Generic Fallback ("দুঃখিত, সার্ভারের সাথে সংযোগে সমস্যা হচ্ছে।")
```

### New Path (Canonical Verified):
```text
Public Wall UI
→ AiAssistantChatScreen.kt
→ FirebaseAiLogicProvider.kt (Delegates to Canonical Orchestrator)
→ SucharuAiContextOrchestrator.orchestrateQuery()
→ R0 Risk Policy (CopilotToolRiskLevel.R0_READ_ONLY)
→ SucharuKnowledgeRAGProvider (25 Locked Knowledge Categories)
→ McpToolRegistry (9 Typed MCP Tools)
→ Backend Security / TenantContext
→ Natural Bengali AI Printing Advice Response
```

---

## 5. Security & Commercial Secrecy Invariants

1. **Zero Secret Leakage:** No Gemini API keys or credentials embedded or exposed in Android client bundles or log outputs.
2. **Pricing Secrecy Protected:** `SucharuKnowledgeRAGProvider` enforces `isPricingSecretsProtected = true` (`KNOW-CONF-004`), prohibiting disclosure of internal vendor purchase rates, paper substrate supplier discounts, or gross margins.
3. **Public Access Safety:** Public/Guest queries receive general printing advice without accessing customer-private financial statements or order details.

---

## 6. Staging Prompt Verification

* **Exact Test Prompt (Bengali):** `"অফসেট বনাম ডিজিটাল প্রিন্টিং খরচ"`
* **Target Environment:** Staging Backend (`https://sucharu-backend-staging-89696832110.asia-southeast1.run.app`) & `sucharu_pro_staging`
* **Correlation ID:** `req-4102ff4439322d11`
* **AI Orchestration Outcome:** `AiContextOutcomeType.ANSWER`
* **Risk Policy Level:** `CopilotToolRiskLevel.R0_READ_ONLY`
* **Natural AI Response Returned:**
  > *"কম পরিমাণে (১-৫০০ কপি) দ্রুত প্রিন্টিংয়ের জন্য ডিজিটাল প্রিন্টিং সাশ্রয়ী। তবে ৫০০ কপির বেশি হলে অফসেট প্রিন্টিংয়ে প্লেট ও প্রডাকশন সেটআপ খরচ ভাগ হয়ে প্রতি কপির ইউনিট কস্ট অনেক কমে যায়। আপনার প্রয়োজনীয় কপির সংখ্যা জানালে আমি সঠিক হিসাব ও গাইডলাইন দিতে পারি।"*

---

## 7. Security Negative Test

* **Unauthenticated Protected API Probe:** `GET https://sucharu-backend-staging-89696832110.asia-southeast1.run.app/api/v1/business-cost-centers`
* **Response Status:** `HTTP/1.1 401 Unauthorized`
* **Correlation ID:** `req-9ee964f84abc58d4`
* **Payload:** `{"success":false,"errorCode":"UNAUTHENTICATED","message":"Authorization header is missing.","correlationId":"req-9ee964f84abc58d4"}`

---

## 8. Production Non-Interference Safety Reconciliation

* **Production Cloud Run Service:** `sucharu-backend-server` (`sucharu-backend-server-00003-rt6`, `100%` traffic, `HTTP 200 OK`)
* **Production Cloud SQL Instance:** `sucharu-postgres-db` (`POSTGRES_16_15`, `10.20.0.3`, `deletionProtectionEnabled: true`)
* **Production Business Mutations:** **`ZERO (0)`**

---

## 9. Modified Files

1. `app/src/main/java/com/sucharu/sucharupro/data/ai/FirebaseAiLogicProvider.kt`
2. `core/src/main/java/com/sucharu/sucharupro/domain/service/ai/SucharuAiContextOrchestrator.kt`
3. `docs/audit/SUCHARU_PRO_PUBLIC_WALL_AI_CONNECTION_FIX.md`

---

## 10. Final Status

* **PUBLIC WALL AI FIX STATUS:** **`VERIFIED`**
* **PRODUCTION DATA MUTATION:** **`ZERO`**
* **PRODUCTION DEPLOYMENT READINESS:** **`YES`**
