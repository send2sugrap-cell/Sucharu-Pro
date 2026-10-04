# SUCHARU PRO — AI CONVERSATION ORCHESTRATION ARCHITECTURE

**Date:** 2026-10-01  
**Repository:** `E:\App\Sucharu Pro`  
**Classification:** `Enterprise Architecture Specification`  

---

## 1. Executive Architecture Overview

Sucharu Pro integrates a controlled, production-oriented **AI Conversation Orchestration Pipeline** that unifies Google Gemini AI, RAG Knowledge Base, Persistent AI Memory, ERP Context, Model Context Protocol (MCP) Tools, Role-Based Access Control (RBAC), and n8n Workflow Automation.

```text
Customer / Staff / Affiliate
        │
        ▼
   [Public Wall / AI Assistant UI]
        │
        ▼
   [Sucharu AI Gateway / Client Transport]
        │
        ▼
   [SucharuAiContextOrchestrator]
   ├── 1. Identity, Tenant Scope & RBAC (`BackendSecurityContext`, `TenantContext`)
   ├── 2. Persistent User Memory (`PostgresAiUserMemoryRepository`)
   ├── 3. RAG Knowledge Base (`SucharuKnowledgeRAGProvider` - 25 Domains)
   ├── 4. ERP Context & Commercial Secrecy Protection (`KNOW-CONF-004`)
   └── 5. MCP Tool Discovery & Risk Policy (`McpToolRegistry` - R0 to R3)
        │
        ▼
   [Gemini AI Reasoning Engine]
   (Natural Bengali understanding & intent classification)
        │
        ├── Is Tool Call Required?
        │     │
        │     ├── NO ──► Natural Conversational Bengali Response
        │     │
        │     └── YES ──► [McpToolRegistry / CopilotToolRiskLevel]
        │                     │
        │                     ├── R0 (READ_ONLY) ──► Direct Staging Backend API Call
        │                     │                          │
        │                     │                          ▼
        │                     │                   [PostgreSQL + RLS]
        │                     │                          │
        │                     │                          ▼
        │                     │                   Verified Business Fact
        │                     │
        │                     └── R2 (CONFIRM_REQUIRED) ──► [CopilotActionProposal]
        │                                                       │
        │                                                       ▼
        │                                                Human Confirmation Gate
        │
        ▼
   [n8n Automation Boundary] (`N8nIntegrationBoundary`)
        │
        ▼
   [Natural Bengali Response Composition]
```

---

## 2. Core Architectural Principles & Invariants

1. **Zero Shadow ERP Databases:** The AI orchestration pipeline never maintains duplicate customer, product, order, or financial ledgers. PostgreSQL (`sucharu_pro` / `sucharu_pro_staging`) is the single system of record.
2. **Commercial Secrecy Protection (`KNOW-CONF-004`):** Internal vendor purchase rates, paper substrate supplier discounts, and gross margin calculations are strictly protected and never disclosed to external customers or public users.
3. **Human Confirmation Gate (R2 / R3 Actions):** High-impact business actions (recording payments, locking quotations, dispatching communications) generate a `CopilotActionProposal` requiring explicit human approval before backend execution.
4. **Server-Authoritative Security:** Client applications never hold direct database connections, Gemini API keys, or raw SQL access. All requests pass through `BackendSecurityContext`, `TenantContext`, and PostgreSQL Row-Level Security (`app.current_tenant_id`).

---

## 3. Layered Component Responsibilities

| Layer | Primary Class / Component | Responsibilities |
| :--- | :--- | :--- |
| **AI Gateway / Client** | `HttpBackendApiClient`, `FirebaseAiLogicProvider` | Manages HTTPS transport, JWT Bearer tokens, and request correlation IDs. |
| **Context Orchestrator** | `SucharuAiContextOrchestrator` | Assembles RAG knowledge, persistent memories, MCP tools, and safety policies. |
| **Agent Boundary** | `BusinessCopilotService` | Manages conversation state, intent classification, and action proposals. |
| **Risk Policy** | `CopilotToolRiskLevel` | Classifies tools into R0 (Auto Read), R1 (Draft), R2 (Confirm), and R3 (Restricted). |
| **RAG Knowledge Base** | `SucharuKnowledgeRAGProvider` | Retrieves domain SOPs and printing advice across 25 locked knowledge categories. |
| **MCP Tool Interface** | `McpToolRegistry` | Exposes 9 typed tools for BI-01 to BI-12 capabilities. |
| **n8n Automation** | `N8nIntegrationBoundary` | Sanitizes outgoing event payloads for n8n automations while blocking security events. |
| **Database & RLS** | PostgreSQL (`sucharu_pro_staging`) | Enforces multi-tenant isolation via `app.current_tenant_id` RLS policies. |
