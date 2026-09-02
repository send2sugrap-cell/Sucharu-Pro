# Background Processing Architecture

## Overview
Sucharu Pro utilizes a server-authoritative, PostgreSQL-backed asynchronous background processing platform. Background tasks are completely decoupled from mobile clients and operate inside dedicated server worker processes.

## Architecture Diagram
```
+-------------------------------------------------------------------------------+
|                             Sucharu Pro Core                                  |
|                                                                               |
|  +------------------+     +--------------------+     +---------------------+  |
|  | Domain Event     |---->| EventToJob         |---->| PostgresJobQueue    |  |
|  | Envelope (Pub)   |     | Dispatcher         |     | (background_jobs)   |  |
|  +------------------+     +--------------------+     +----------+----------+  |
|                                                                 |             |
|  +------------------+                                           |             |
|  | JobScheduler     |-------------------------------------------+             |
|  | (Cron/Interval)  |                                           |             |
|  +------------------+                                           |             |
|                                                                 |             |
|  +------------------+     +--------------------+                |             |
|  | n8n Webhook /    |---->| Security Boundary  |----------------+             |
|  | AI Agent Trigger |     | (HMAC/Permissions) |                              |
|  +------------------+     +--------------------+                              |
+-------------------------------------------------------------------------------+
                                                                  |
                                                                  v
+-------------------------------------------------------------------------------+
|                         Background Worker Cluster                             |
|                                                                               |
|  +-------------------------------------------------------------------------+  |
|  | Worker Loop (SELECT ... FOR UPDATE SKIP LOCKED)                         |  |
|  +-------------------------------------------------------------------------+  |
|         |                                  |                      |           |
|         v                                  v                      v           |
|  [Email/SMS/Push Job]            [DAG Workflow Step]     [ERP Report Job]     |
|         |                                  |                      |           |
|         +----------------------------------+----------------------+           |
|                                            |                                  |
|                                            v                                  |
|                              +---------------------------+                    |
|                              | JobExecutionEngine        |                    |
|                              | (Retry / Dead Letter / DB)|                    |
|                              +---------------------------+                    |
+-------------------------------------------------------------------------------+
```

## Guarantees
1. **Multi-Tenant Isolation**: RLS enforced across all background job queries.
2. **At-Least-Once Execution**: Managed via lease heartbeat and lease recovery services.
3. **Idempotency**: Handlers execute with idempotency keys; duplicate triggers are skipped.
4. **Resilience**: Exponential backoff with jitter prevents thundering herd against downstream APIs.
