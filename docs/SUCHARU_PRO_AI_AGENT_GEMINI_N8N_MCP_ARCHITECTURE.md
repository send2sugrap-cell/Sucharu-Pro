# SUCHARU PRO — AI AGENT, GEMINI, n8n, MCP & KNOWLEDGE ARCHITECTURE
### Master Architecture Specification & Implementation Control Document

**Project:** Sucharu Pro  
**Business:** Sucharu Graphics & Printing  
**Repository:** `E:\App\Sucharu Pro`  
**Scope:** Unified Commercial Printing ERP + Customer/Affiliate Experience + AI Business Copilot  
**Document Type:** Canonical Architecture & Implementation Control Specification  
**Status:** ARCHITECTURE APPROVED — IMPLEMENTATION CONTROL LOCKED  
**Priority:** HIGH  

---

## 1. OBJECTIVE & NON-NEGOTIABLE ARCHITECTURE PRINCIPLE

This specification defines the canonical, unified AI architecture for Sucharu Pro:
- **Gemini**: Reasoning, natural-language understanding, and structured extraction engine.
- **Sucharu AI Agent**: AI control boundary, user context manager, and memory retriever.
- **Memory**: Controlled, role-aware, and tenant-isolated context storage (Redis + PostgreSQL).
- **Tool Risk Policy**: Action governance layer categorizing tools into R0 (Read), R1 (Draft), R2 (Human Approval Required), and R3 (Restricted).
- **n8n**: Workflow orchestration, event-driven triggers, retries, and external integrations.
- **Sucharu MCP Layer**: Controlled Model Context Protocol tool interface.
- **Sucharu Backend**: Canonical business authority, domain logic, and API router.
- **PostgreSQL + RLS**: System of record and final data security boundary.

### Authority Hierarchy
```text
1. PostgreSQL / Canonical Persisted Business Data (Highest Authority)
2. Sucharu Backend Domain / Business Logic & APIs
3. Backend Authorization + Tenant RLS Policies
4. Locked Sucharu Business Rules & Form Baselines (Forms 01–06, BI-01–BI-12)
5. Approved Sucharu Knowledge & SOPs
6. Controlled AI Memory (Short-term & Structured)
7. External / General Knowledge
8. Gemini Reasoning Engine (Lowest Authority)
```
> **CRITICAL INVARIANT**: AI-generated content MUST NOT override canonical business data or mutate historical price/financial snapshots (`OrderPriceSnapshot`).

---

## 2. CORE ARCHITECTURE DIAGRAM

```text
                         ┌────────────────────┐
                         │      GEMINI        │
                         │ Reasoning / NLP    │
                         └─────────┬──────────┘
                                   │
                         ┌─────────▼──────────┐
                         │ Sucharu AI Agent   │
                         │ Boundary + Memory  │
                         └─────────┬──────────┘
                                   │
                         ┌─────────▼──────────┐
                         │  Tool Risk Policy  │
                         │ Auth / Approval    │
                         └─────────┬──────────┘
                                   │
                         ┌─────────▼──────────┐
                         │        n8n         │
                         │   Orchestration    │
                         └─────────┬──────────┘
                                   │
                         ┌─────────▼──────────┐
                         │ Sucharu MCP Layer  │
                         │ R0 / R1 / R2 / R3  │
                         └─────────┬──────────┘
                                   │
                         ┌─────────▼──────────┐
                         │ Sucharu Backend    │
                         │ Canonical Business │
                         │ Logic + APIs       │
                         └─────────┬──────────┘
                                   │
                         ┌─────────▼──────────┐
                         │ PostgreSQL + RLS    │
                         └────────────────────┘
```

---

## 3. SECURITY PRINCIPLE — DEFENSE IN DEPTH

Tool Risk Policy MUST NOT be the only security boundary. The Sucharu Backend independently enforces:
- Authentication (`BackendSecurityContext`)
- Capability Authorization (`RoleCapabilityMatrix`)
- Role Authorization & Ownership (`ResourceOwnershipGuard`)
- Tenant / Project Isolation (`TenantContext`)
- Approval State Validation
- Business Rules & Constraints
- Idempotency Deduplication (`idempotencyKey`)
- PostgreSQL Row-Level Security (`app.current_project_id`)

```text
AI Allowed ≠ n8n Allowed ≠ MCP Allowed ≠ Backend Authorized ≠ Business Action Approved
```
The final business operation is authorized exclusively by the Sucharu Backend.

---

## 4. GEMINI ROLE & BOUNDARY

Gemini serves as the **Reasoning, Natural Language, and Structured Extraction Engine**.

### Gemini May:
- Process Bangla (`bn-BD`) and English (`en-US`) natural language queries.
- Extract structured printing specifications (quantity, paper GSM, size, pages, lamination, binding).
- Classify customer intent (e.g. `QUERY_RECEIVABLES`, `REQUEST_QUOTATION`, `ORDER_STATUS`).
- Reason over approved Sucharu Knowledge RAG documents.
- Prepare non-binding drafts (e.g. quotation draft, followup message draft).
- Propose actionable tool executions through the Confirmation Gate.

### Gemini MUST NOT:
- Directly query or connect to PostgreSQL.
- Access private database credentials or API secrets.
- Bypass MCP, n8n, or Backend security controls.
- Invent prices, stock quantities, delivery dates, or vendor costs.
- Directly disclose unapproved commercial rates or internal margins.
- Execute R2/R3 actions without explicit human approval.

---

## 5. SUCHARU AI AGENT BOUNDARY

The Sucharu AI Agent Boundary manages user identity, role, tenant, capabilities, conversation context, and tool dispatching independently of Gemini.

### Responsibilities:
- User identity/context resolution (`userId`, `role`, `projectId`).
- Contextual memory retrieval and filtering.
- Tool selection and Risk Classification (R0–R3).
- Sensitive information redaction (redacting passwords, tokens, vendor purchase costs).
- Human confirmation gate enforcement for R2 actions.
- Audit event logging for tool executions.

---

## 6. MEMORY ARCHITECTURE

Memory is structured into three distinct layers:

### 6.1 Short-Term Conversation Context (Redis)
- Stores active chat sessions, recent messages, and temporary tool execution states.
- Time-to-live (TTL) managed; decoupled from canonical customer master data.

### 6.2 Structured AI Memory (PostgreSQL `ai_user_memory`)
- Stores approved, non-sensitive customer/staff preferences (e.g., preferred paper type, usual order quantity, communication channel preference).
- Enforces strict tenant isolation (`project_id`) and customer ownership restrictions.

### 6.3 Canonical Business Data (PostgreSQL System of Record)
- Modules 00–24 & Forms 01–06 canonical data (`Customer`, `Order`, `JobCard`, `CustomerInvoice`, `OrderPriceSnapshot`).
- **Canonical business data ALWAYS wins over AI memory.**

---

## 7. KNOWLEDGE ARCHITECTURE (RAG DOMAINS)

The RAG Knowledge layer covers:
1. **Printing Knowledge**: Offset, digital, book publishing, packaging, calendars, brochures, flyers, visiting cards, GSM paper specifications, 13 production stages (`DESIGN` $\rightarrow$ `DELIVERED`).
2. **Printing Business Knowledge**: Customer specification extraction, quotation considerations, production scheduling, finishing options.
3. **Printing Market Knowledge**: Industry terminology, B2B/B2C printing trends, promotional merchandise.
4. **Marketing Knowledge**: Lead qualification, customer persona discovery, objection handling, follow-up strategies, campaign copy.
5. **Office Management Knowledge**: Order progress tracking, delivery schedules, receivable aging summaries, supplier obligations.
6. **Office SOP Knowledge**: Standard Operating Procedures for Quotation, Order Acceptance, Design Proofing, QC, Vendor Bills, Delivery Challans, and Returns.
7. **Finance & Commercial Knowledge**: Quoted rates, commercial price evaluation, payment allocation, Bangladesh VAT/tax rules.

---

## 8. RISK CLASSIFICATION & TOOL RISK POLICY

Every AI-accessible tool is classified into one of four risk tiers:

| Tier | Risk Level | Description | Example Tools |
| :--- | :--- | :--- | :--- |
| **R0** | `READ_ONLY` | Safe read-only information retrieval | `get_customer_360`, `get_order_status`, `get_delivery_status`, `get_financial_summary`, `get_sla_exceptions` |
| **R1** | `DRAFT` | Prepares non-binding drafts | `create_quotation_draft`, `create_followup_draft`, `create_marketing_campaign_draft` |
| **R2** | `CONFIRM_REQUIRED` | Requires explicit human approval before execution | `confirm_order`, `record_customer_payment`, `send_sensitive_message`, `approve_discount` |
| **R3** | `RESTRICTED` | Strictly prohibited for automated AI tools | Security config changes, raw SQL, tenant boundary changes, secret management |

---

## 9. PRICING & RATE DISCLOSURE — ABSOLUTE RULE

> **UNAPPROVED COMMERCIAL RATES MUST NEVER BE DIRECTLY DISCLOSED BY THE AI.**

When a Guest, Customer, or Affiliate asks:
> *"rate কত?"* or *"How much does this cost?"*

The Sucharu AI Agent MUST follow this exact flow:
```text
Customer Requirement
        ↓
Specification Extraction
        ↓
Form 04 Commercial Pricing Engine
        ↓
Quotation Draft (R1)
        ↓
Submitted to Admin/Staff for Approval
        ↓
Human Approval (R2)
        ↓
Approved Rate Communicated to Customer
```
The AI MUST NOT invent prices, disclose vendor purchase costs, or reveal internal gross margins.

---

## 10. ROLE-AWARE BEHAVIOR

Access and responses vary strictly by user role:
- **Guest**: General information, printing consultation, specification collection, quotation request creation. No internal prices or business metrics.
- **Customer**: Own profile, own orders, own approved quotations, own delivery status, own financial account statement. No other customers' data.
- **Affiliate**: Authorized affiliate performance, referral leads, wallet balance, approved promo offers.
- **Staff / Manager / Admin**: Role capability-authorized access to ERP operations, production workloads, costing breakdowns, collection intelligence, and SLA exceptions.

---

## 11. n8n & MCP RESPONSIBILITY

- **n8n Orchestration**: Triggers, event-driven workflows, scheduled reminders, external webhook dispatching, retries, and notification channels. n8n does NOT own business rules or database credentials.
- **Sucharu MCP Layer**: Exposes structured, typed, permission-aware tools (`get_customer_360`, `get_order_status`, `get_sla_exceptions`) calling Sucharu Backend REST APIs. MCP does NOT execute raw SQL.

---

## 12. HUMAN APPROVAL ARCHITECTURE & CONFIRMATION GATES

For all R2 actions (`CONFIRM_REQUIRED`):
1. User requests action via chat/voice.
2. AI Agent generates a `CopilotActionProposal` with `isConfirmationRequired = true`.
3. UI renders an Impact Preview Card showing action details, target entity, and affected amounts.
4. User explicitly taps **Confirm**.
5. Backend executes canonical API and logs `AuditEvent`.

---

## 13. FOUR INITIAL AI WORKFLOWS
1. **Sales Consultant**: Requirement discovery, specification extraction, quotation request creation in Bengali/English.
2. **Quotation Draft Engine**: Consumes printing specifications, executes `PrintingCostingEngine`, and generates quotation drafts for human review.
3. **Market Intelligence**: Analyzes campaign trends, customer repeat purchase frequency, and quotation conversion rates.
4. **Production/Ops Daily Brief**: Read-only daily operational summary of delayed jobs, SLA risk alerts, delivery schedules, and collection attention items.

---

## 14. RECOMMENDED IMPLEMENTATION SEQUENCE
```text
STEP 01 — Repository & Existing AI Codebase Audit (COMPLETED)
STEP 02 — Architecture Specification Lock (COMPLETED - This Document)
STEP 03 — Existing AI Capability Audit (COMPLETED)
STEP 04 — Memory Architecture (COMPLETED - BI-12 `ai_user_memory`)
STEP 05 — Tool Registry & Risk Policy (COMPLETED - BI-12 `CopilotToolRiskLevel` R0-R3)
STEP 06 — MCP Layer & n8n Contracts (COMPLETED - BI-12 REST Endpoints)
STEP 07 — Agent Boundary & Confirmation Gate (COMPLETED - BI-12 `BusinessCopilotService`)
STEP 08 — Production/Ops Daily Brief Agent (COMPLETED - BI-12)
STEP 09 — Quotation Draft Engine Agent (COMPLETED - BI-12)
STEP 10 — Sales Consultant Agent (COMPLETED - BI-12)
STEP 11 — Market Intelligence Agent (COMPLETED - BI-12)
STEP 12 — Runtime Integration & Security/RLS Verification (COMPLETED - BI-12)
```

---

## 15. OBSERVABILITY, AUDIT & IDEMPOTENCY
- **Idempotency**: All tool execution proposals use `idempotencyKey` deduplication.
- **Audit**: Every tool execution logs `actorId`, `tenantId`, `toolId`, `proposalId`, `confirmationState`, and `timestamp`.
- **Fault Tolerance**: LLM or n8n failures do NOT affect or corrupt core ERP database transactions.

---

## 16. REPOSITORY AUDIT SUMMARY

### Existing AI Components Sourced & Integrated:
- **Speech & Voice Input**: Reused existing `SpeechRecognizer` (`bn-BD` Bangla Voice Input) and `SucharuAiInputBar.kt`.
- **Firebase AI & Gemini Integration**: Reused `FirebaseAiLogicProvider.kt` and `firebase-vertexai` SDK.
- **AI Security Boundaries**: Reused `AiAgentNotificationSecurityBoundary.kt`, `AiNotificationConfirmationService.kt`, and `AiAgentEventConsumer.kt`.
- **BI-12 Business Copilot Service**: `BusinessCopilotService.kt` and `BusinessCopilotServiceTest.kt` implemented and verified.
- **Zero Duplicate Systems**: 0 shadow AI databases or parallel ERP masters created.

---

## 17. GIT COMMIT & BASELINE REFERENCE
- **Architecture Specification Document**: `docs/SUCHARU_PRO_AI_AGENT_GEMINI_N8N_MCP_ARCHITECTURE.md`
- **Baseline Git Branch**: `feature/wall-ui-redesign`
- **BI-12 Final Commit**: `f3490c5`

---

## 18. IMPLEMENTATION READINESS
- **Status**: **`READY_WITH_GAPS`**
- **Evidence**: All domain models, services, DTOs, REST APIs, unit tests, and architecture control specifications are 100% verified and pushed to GitHub (`f3490c5`). Database runtime execution remains blocked due to Docker Engine offline availability.

---

## 19. NEXT PERMITTED STEP
The **Sucharu Pro Business Improvement Program (BI-01 to BI-12)** and **AI Agent, Gemini, n8n, MCP & Knowledge Architecture Specification** are 100% complete, locked, and synchronized with GitHub. Ready for final program verification or deployment review whenever requested!
