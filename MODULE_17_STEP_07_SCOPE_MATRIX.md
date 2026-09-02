# MODULE 17 → STEP 07: SHOP-FLOOR LIVE EXECUTION TRACKING, MATERIAL CONSUMPTION, MACHINE TELEMETRY & OUTPUT RECORDING ENGINE

## SCOPE MATRIX & ARCHITECTURAL AUDIT

---

### 1. Canonical Step 07 Identification & Roadmap Alignment

- **Module**: `MODULE 17 — SMART PRINTING CALCULATOR & END-TO-END COMMERCIAL PRODUCTION ENGINE`
- **Step**: `STEP 07 — Shop-Floor Live Execution Tracking, Material Consumption, Machine Telemetry & Output Recording Engine`
- **Canonical Upstream Dependencies**:
  - `Step 01`: Smart Printing Calculator (`PrintingCalculationResult`)
  - `Step 02`: Commercial Costing & Quotation (`PrintingQuote`)
  - `Step 03`: Commercial Commitment & Order (`CommercialCommitment` / `Order`)
  - `Step 04`: Production Planning & Manufacturing Readiness (`ProductionPlanningSnapshot`, Planned BOM)
  - `Step 05`: Production Job Creation & Execution (`ProductionJobExecution`, `ProductionWorkOrder`, `ProductionHold`, `ProductionWastageRecord`, `ProductionReworkRecord`)
  - `Step 06`: Production Scheduling, Capacity Planning & Dispatch Orchestration (`ProductionSchedule`, `ProductionDispatchQueueItem`, `ProductionCapacityWindow`)
- **Downstream Bridge**:
  - Feeds actual shop-floor execution metrics, real material depletion, machine run logs, and yield/variance calculations into Module 16 (Actual Cost & Variance Analytics) and Module 06/07 (Final QC & Inventory Depletion).

---

### 2. Architectural Authority & Reuse Classification

| Capability | Existing Authority | Reuse Strategy | Extend / New Components | UI Exists | API Exists | Production Ready |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Production Job** | `ProductionJobExecution` (Step 05) | **Reuse** canonical authority without duplicating entity. | Update live execution status & progress % in real-time. | Yes (Step 05) | Yes | Yes |
| **Work Order** | `ProductionWorkOrder` (Step 05) | **Reuse** canonical `workOrderId` and sequence. | Transition work orders through live tracking (`IN_PROGRESS`, `PAUSED`, `COMPLETED`). | Yes (Step 05) | Yes | Yes |
| **Dispatch Queue** | `ProductionDispatchQueueItem` (Step 06) | **Reuse** canonical queue records. | Acknowledge & start work order from dispatch queue. | Yes (Step 06) | Yes | Yes |
| **Material Consumption** | `MaterialRequirement` (Step 04) | **Reuse** planned BOM as baseline; record actual consumption. | `ProductionMaterialConsumptionRecord` (actual vs planned variance, scrap, batch/lot reference). | No | No | New (Step 07) |
| **Machine Telemetry & Run Logs** | Step 06 Machine assignments | **Extend** with real-time operational logs. | `MachineTelemetryLog` (speed sheets/hr, impressions, downtime duration & categorization). | No | No | New (Step 07) |
| **Operator Time & Shift Output** | Step 05 / Step 06 Operator assignments | **Extend** with live timer / shift output records. | `OperatorTimeTrackingRecord` (setup mins, run mins, idle/downtime mins, good output count, rejected count). | No | No | New (Step 07) |
| **Stage Output Handover** | `ProductionStageType` (13 stages) | **Reuse** canonical 13 stages. | `StageOutputHandoverRecord` (handover quantity, physical sign-off, scrap quantity, next stage readiness). | No | No | New (Step 07) |
| **Execution Variance & Yield** | Step 01 / Step 04 estimation | **Reuse** baseline rates & standard times. | `ProductionExecutionVarianceSummary` (time variance, speed efficiency %, material scrap rate %, yield %). | No | No | New (Step 07) |
| **Audit & Event Trail** | `ProductionExecutionEvent` / Outbox | **Reuse** append-only audit architecture. | `ShopFloorTrackingEvent` capturing all telemetry, pause/resume, and material consumption actions. | Yes | Yes | Extend |
| **AI Handoff Contract** | Step 01-06 Handoff pattern | **Reuse** read-only explainable JSON contract. | `Module17Step07ShopFloorTrackingHandoffContract`. | Yes | Yes | New (Step 07) |

---

### 3. Absolute Architectural Non-Negotiables

1. **Zero Duplication**: Do NOT duplicate Orders, Production Jobs, Work Orders, or Inventory authorities.
2. **Zero Floating Point Math**: All consumption quantities, run speeds, time durations, yields, and variance percentages must use `BigDecimal(scale = 4, RoundingMode.HALF_UP)`.
3. **Multi-Tenant Security**: Every table must have `tenant_id`, `FORCE ROW LEVEL SECURITY`, RLS policies, and server-side tenant resolution.
4. **Idempotency & Concurrency**: Start, pause, resume, output record, and handover actions must be guarded by server-side idempotency keys and optimistic concurrency checks.
5. **No Fake Data in Production Path**: Real Android UI connecting through ViewModels, REST APIs, repositories, and PostgreSQL database.
