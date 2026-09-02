# MODULE 17 → STEP 06: PRODUCTION SCHEDULING, CAPACITY PLANNING & DISPATCH ORCHESTRATION ENGINE
## SCOPE MATRIX & ARCHITECTURAL AUDIT

---

### 1. Architectural Authority & Reuse Classification

| Capability | Existing Authority | Reuse Strategy | Extend / New Components | UI Exists | API Exists | Production Ready |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Production Job** | `ProductionJobExecution` (Step 05) | **Reuse** canonical authority without duplicating entity or table. | Link `ProductionSchedule` to canonical `executionJobId`. | Yes (Step 05 Command Center) | Yes (`/api/v1/production-jobs`) | Yes |
| **Work Order** | `ProductionWorkOrder` (Step 05) | **Reuse** canonical `workOrderId` and sequence. | Bridge into `ProductionScheduleSlot` and `ProductionDispatchQueueItem`. | Yes (Step 05 WO details) | Yes (`/api/v1/production-jobs/.../work-orders`) | Yes |
| **Production Stage** | `ProductionStageType` (13 stages) | **Reuse** canonical enum (`DESIGN`, `PREPRESS`, `OFFSET_PRINTING`, `DIGITAL_PRINTING`, etc.). | Use in stage compatibility & routing. | Yes | Yes | Yes |
| **Machine** | `job_stages.machine_id`, `MachineCompatibilityResult` (Step 04) | **Reuse** machine definitions & compatibility rules. | `ProductionMachineAvailability`, dynamic shift/working window models. | Partial | Partial | Extend with availability & capacity |
| **Machine Capacity** | Concept in Step 04 | **Extend** with deterministic `BigDecimal(scale = 4)` capacity window calculation. | `ProductionCapacityWindow`, working hours, setup/run time calculations. | No | No | New (Step 06) |
| **Operator** | `job_stages.operator_id`, `ProductionOperator` | **Reuse** operator competency & assignment from Step 05. | `ProductionOperatorAvailability`, workload conflict detector. | Partial | Partial | Extend with availability & shift |
| **Scheduling** | None | **New** deterministic scheduling engine. | `ProductionSchedule`, `ProductionScheduleSlot`, `ProductionSchedulingEngine`. | No | No | New (Step 06) |
| **Dispatch Queue** | None (ad-hoc in Step 05) | **New** canonical dispatch orchestration queue. | `ProductionDispatchQueueItem`, `DispatchStatus`, dispatch transition workflow. | No | No | New (Step 06) |
| **Priority** | `OrderPriority` (`URGENT`, `HIGH`, `NORMAL`, `LOW`) | **Reuse** canonical order priority + Step 05 execution priority. | `ProductionSchedulingPriorityCalculator` (deterministic scoring). | Yes | Yes | Extend with smart priority scoring |
| **Maintenance Block** | None | **New** planned machine maintenance tracking. | `ProductionMaintenanceBlock` model & conflict detection. | No | No | New (Step 06) |
| **Shift Calendar** | Default 8h/16h/24h shifts | **New** deterministic shift/window generator. | `ProductionShiftWindow` calculation. | No | No | New (Step 06) |
| **Capacity Conflict** | None | **New** deterministic multi-point conflict detector. | `ProductionScheduleConflict`, machine double-booking, missing operator, dependency violation. | No | No | New (Step 06) |
| **Notifications** | `NotificationService` / Event outbox | **Reuse** existing event / notification architecture. | Dispatch events, conflict alerts. | Yes | Yes | Extend |
| **Audit** | `ProductionExecutionEvent` | **Reuse** append-only audit patterns. | `ProductionScheduleEvent` for immutable schedule versioning & transitions. | Yes | Yes | New (Step 06) |
| **AI Handoff** | `Module17Step04/05...HandoffContract` | **Reuse** read-only explainable handoff contract pattern. | `Module17Step06ProductionSchedulingHandoffContract`. | Yes | Yes | New (Step 06) |

---

### 2. Upstream Provenance Chain
```
Module 17 Step 01: PrintingCalculationResult (spec, dimensions, paper, imposition, waste)
  ↓
Module 17 Step 02: PrintingQuote / PrintingQuoteVersion (pricing, terms, quantity breaks)
  ↓
Module 17 Step 03: CommercialCommitment / Order / OrderItem (approved order, customer commitment)
  ↓
Module 17 Step 04: ProductionPlanningSnapshot (readiness score, 13-stage routing, material requirements)
  ↓
Module 17 Step 05: ProductionJobExecution + ProductionWorkOrder (execution job, work orders, actuals, hold, rework)
  ↓
Module 17 Step 06: ProductionSchedule + ProductionDispatchQueue (capacity windows, machine/operator slot, dispatch queue)
```

---

### 3. Absolute Architectural Non-Negotiables
1. **Zero Duplication**: No duplicate Orders, Production Jobs, or Work Orders.
2. **Zero Floating Point Arithmetic**: All capacity, duration, and quantity computations use `BigDecimal(scale = 4, RoundingMode.HALF_UP)`.
3. **Immutable Versions**: Schedules use version incrementing (`version = 1`, `version = 2`, superseded history preserved).
4. **Deterministic Decisions**: Given the same state and inputs, scheduling and conflict engines always output identical slots, priority scores, and diagnostics.
5. **Multi-Tenant Security**: `tenant_id` on all tables, `FORCE ROW LEVEL SECURITY`, RLS policies, RBAC enforced on server.
6. **No Fake Data in Production Android UI**: Android UI connects to real endpoints and repository/service stack.
