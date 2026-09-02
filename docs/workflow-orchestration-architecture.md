# Workflow Orchestration Architecture

## Overview
The Sucharu Pro Workflow Orchestration Engine coordinates long-running, multi-step business transactions across printing operations without tightly coupling individual domain components.

## Architectural Layers
```
┌─────────────────────────────────────────────────────────────┐
│                 Client Layer (Android / Web API)             │
└──────────────────────────────┬──────────────────────────────┘
                               │ Authenticated Principal / TenantContext
┌──────────────────────────────▼──────────────────────────────┐
│           Workflow Operations Service & API Gateway         │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                  Workflow Orchestrator                      │
│   ┌─────────────────────────────────────────────────────┐   │
│   │              Workflow State Machine                 │   │
│   └─────────────────────────────────────────────────────┘   │
│   ┌─────────────────────────────────────────────────────┐   │
│   │              Workflow Step Engine                   │   │
│   │  (ACTION, JOB, APPROVAL, EVENT_WAIT, CONDITION...)  │   │
│   └─────────────────────────────────────────────────────┘   │
│   ┌─────────────────────────────────────────────────────┐   │
│   │             Saga Compensation Engine                │   │
│   └─────────────────────────────────────────────────────┘   │
│   ┌─────────────────────────────────────────────────────┐   │
│   │                 Approval Engine                     │   │
│   └─────────────────────────────────────────────────────┘   │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│    PostgreSQL 16 Multi-Tenant RLS Tables (12 Schema Tables) │
└─────────────────────────────────────────────────────────────┘
```

## Core Guarantees
1. **Server-Authoritative**: Workflow state, transitions, decisions, and compensations are evaluated and recorded server-side.
2. **Tenant Isolation**: Every query and mutation executes within `TenantContext` enforcing PostgreSQL Row-Level Security.
3. **Auditability**: Every transition and decision creates an immutable chronological audit trail.
