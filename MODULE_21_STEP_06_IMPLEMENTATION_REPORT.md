# MODULE 21 → STEP 06: DOWNTIME & FAULT EVENT MANAGEMENT — FINAL VERIFICATION REPORT

## A. Scope
**Module 21 → Step 06 — Downtime & Fault Event Management**  
Establishes the canonical foundation for capturing, tracking, acknowledging, and resolving machine Fault Events and Downtime Events integrated with Machine Registry (Step 01), Telemetry Ingestion (Step 02), Health Monitoring (Step 03), Production Integration (Step 04), and Maintenance (Step 05).

---

## B. Repository Forensic Audit
- Forensic repository search confirmed no dedicated machine fault or downtime event tables previously existed.
- Created `machine_fault_events` and `machine_downtime_events` tables linked to `machine_registry.machine_id`.

---

## C. Existing Event Architecture Found
- Reused `ProductionExecutionEvent` for production execution history and `TenantContext` RLS event boundaries.

---

## D. Existing Machine Architecture Reused
- **Step 01**: `machine_registry` table, `MachineEquipment`, `MachineType`, `MachineStatus`, `MachineRegistryRepository`.
- **Step 02**: `machine_telemetry_records` table, `MachineTelemetryRecord`, `TelemetryMetricType`, `MachineTelemetryRepository`.
- **Step 03**: `MachineStatusMonitoringService`, `MachineOperationalState`, `MachineHealthCondition`, `MachineHealthSnapshot`.
- **Step 04**: `ProductionExecutionService`, `ProductionWorkOrder.assignedMachineId` validation.
- **Step 05**: `MachineMaintenanceService`, `MaintenanceRecord`.
- **Security & Authorization**: `BackendAuthorizationPolicy`, `RoleCapabilityMatrix`, `AuthorizationCapability`, `TenantContext`.

---

## E. Fault Event Domain
- `MachineFaultEvent` entity (`faultEventId`, `tenantId`, `machineId`, `faultCode`, `faultType`, `severity`, `status`, `description`, `source`, `occurredAt`, `detectedAt`, `acknowledgedAt`, `acknowledgedBy`, `resolvedAt`, `resolvedBy`, `resolutionNotes`, `maintenanceRecordId`).

---

## F. Fault Lifecycle
`OPEN` → `ACKNOWLEDGED` → `RESOLVED` (or `CANCELLED`)
- Validated via `MachineEventValidator.validateFaultStatusTransition`.
- Terminal states (`RESOLVED`, `CANCELLED`) cannot be modified or re-opened.

---

## G. Fault Severity
Enum `FaultSeverity`:
- `WARNING`
- `FAULT`
- `CRITICAL`

---

## H. Fault Acknowledgement & Resolution
- `acknowledgeFault(...)`: Transitions status to `ACKNOWLEDGED`, captures `acknowledgedAt` timestamp & `acknowledgedBy` actor ID.
- `resolveFault(...)`: Transitions status to `RESOLVED`, captures `resolvedAt` timestamp, `resolvedBy` actor ID, `resolutionNotes`, and optional `maintenanceRecordId`.

---

## I. Downtime Event Domain
- `MachineDowntimeEvent` entity (`downtimeId`, `tenantId`, `machineId`, `faultEventId`, `executionJobId`, `workOrderId`, `reasonCategory`, `reasonDetails`, `status`, `startedAt`, `endedAt`, `durationSeconds`).

---

## J. Downtime Lifecycle
`STARTED` → `ENDED` (or `CANCELLED`)
- Terminal states (`ENDED`, `CANCELLED`) cannot be restarted.

---

## K. Downtime Reason
Enum `DowntimeReasonCategory`:
- `MACHINE_FAULT`
- `MAINTENANCE`
- `POWER`
- `MATERIAL_WAIT`
- `OPERATOR`
- `SETUP`
- `OTHER`

---

## L. Duration Logic
- `durationSeconds = (endedAt - startedAt) / 1000L`.
- Validated: `endedAt` cannot be earlier than `startedAt`.

---

## M. Telemetry Integration
- Ingested telemetry with fault/warning operational codes (e.g. `metricType = OPERATIONAL_STATE`, `value >= 50`) can trigger or correlate with `MachineFaultEvent` records.

---

## N. Machine Health Integration
- Integrated with Step 03 `MachineStatusMonitoringService`. Machine health snapshot reflects `MachineOperationalState.FAULT` when active OPEN/ACKNOWLEDGED fault events or FAULT telemetry exist.

---

## O. Production Integration
- Production execution work orders (`executionJobId`, `workOrderId`) can be optionally referenced in `MachineDowntimeEvent` without modifying the canonical 13-stage `ProductionStageType` workflow.

---

## P. Maintenance Integration
- `MachineFaultEvent.maintenanceRecordId` optionally links resolved faults to Step 05 `MaintenanceRecord` instances.

---

## Q. Database Changes
- **Migration**: `V20261134__create_machine_downtime_and_fault_event_tables.sql`
- **Tables**: `machine_fault_events`, `machine_downtime_events`
- **Foreign Keys**: `machine_id REFERENCES machine_registry(machine_id) ON DELETE CASCADE`
- **Indexes**: `idx_fault_events_tenant_machine`, `idx_fault_events_tenant_status`, `idx_downtime_tenant_machine`, `idx_downtime_tenant_status`
- **RLS**: Enabled and forced (`FORCE ROW LEVEL SECURITY`) via tenant isolation policies on both tables.

---

## R. API Changes
- REST Endpoints:
  - `POST /api/v1/machines/{machineId}/faults`
  - `GET /api/v1/machines/{machineId}/faults`
  - `POST /api/v1/machines/{machineId}/faults/{faultId}/acknowledge`
  - `POST /api/v1/machines/{machineId}/faults/{faultId}/resolve`
  - `POST /api/v1/machines/{machineId}/downtime`
  - `GET /api/v1/machines/{machineId}/downtime`
  - `POST /api/v1/machines/{machineId}/downtime/{downtimeId}/end`

---

## S. Security / Capability
- Added `READ_MACHINE_EVENTS`, `MANAGE_MACHINE_EVENTS`, `READ_MACHINE_DOWNTIME`, and `MANAGE_MACHINE_DOWNTIME` capabilities to `RoleCapabilityMatrix`.
- Granted to `STAFF`, `MANAGER`, and `ADMIN` roles.

---

## T. RLS / Tenant Isolation
- Enforced via `TenantContext` & PostgreSQL RLS policies.
- Cross-tenant fault or downtime requests (`tenantId != machine.tenantId`) rejected with explicit error message.

---

## U. Idempotency
- Handled via `FakeMachineEventDataSource` and PostgreSQL transaction boundaries.

---

## V. Audit
- Fault and downtime lifecycle transitions record actor ID, timestamps, and details.

---

## W. Inventory Boundary
- Verified: Machine fault/downtime events do **NOT** mutate finished product inventory or raw material stock in Step 06.

---

## X. Finance Boundary
- Verified: Machine fault/downtime events do **NOT** create invoices, GL entries, or accounting transactions in Step 06.

---

## Y. Android / UI Changes
- **UI CHANGE**: NONE (Backend/Domain event foundation step; no UI changes required in Step 06).

---

## Z. Real Device Verification
**DEVICE VERIFICATION**: NOT APPLICABLE — NO UI CHANGE

---

## AA. Test Results
- **Domain & Integration Tests (`MachineFaultAndDowntimeDomainTest`)**: 6 / 6 **PASSED**
  - Test 1: Fault event recording & status initialization (**PASS**)
  - Test 2: Fault lifecycle acknowledge & resolve flow (**PASS**)
  - Test 3: Downtime start & end duration calculation (**PASS**)
  - Test 4: Downtime end prior to start timestamp fails validation (**PASS**)
  - Test 5: Decommissioned machine event rejection (**PASS**)
  - Test 6: Canonical 13-stage `ProductionStageType` workflow regression check (**PASS**)
- **Security & RLS Tests (`PostgresMachineEventSecurityTest`)**: 1 / 1 **PASSED**
- **API Tests (`MachineEventApiTest`)**: 3 / 3 **PASSED**

---

## AB. Build Results
- **Core Compile (`:core:jar`)**: **PASS**
- **Backend Compile (`:backend:jar`)**: **PASS**
- **App Compile (`:app:assembleDebug`)**: **PASS**

---

## AC. Regression Verification
- Modules 00–20 preserved 100%.
- Module 21 Steps 01–05 preserved 100%.
- Canonical `ProductionStageType` workflow preserved.

---

## AD. Out-of-Scope Leakage Check
- No OEE calculations, MTBF/MTTR analytics, predictive ML algorithms, PLC/MQTT/SCADA hardware connections, push notification engines, or finance modifications introduced.

---

## AE. Files Changed
1. `core/.../db/migration/V20261134__create_machine_downtime_and_fault_event_tables.sql`
2. `core/.../data/auth/authorization/AuthorizationModels.kt`
3. `core/.../data/auth/authorization/RoleCapabilityMatrix.kt`
4. `core/.../domain/machine/events/MachineEventModels.kt`
5. `core/.../domain/machine/events/MachineEventValidator.kt`
6. `core/.../data/datasource/machine/events/MachineEventDataSource.kt`
7. `core/.../data/datasource/machine/events/FakeMachineEventDataSource.kt`
8. `core/.../data/persistence/postgres/PostgresMachineEventDataSource.kt`
9. `core/.../domain/repository/machine/events/MachineEventRepository.kt`
10. `core/.../data/repository/machine/events/MachineEventRepositoryImpl.kt`
11. `core/.../domain/service/machine/events/MachineFaultEventService.kt`
12. `core/.../domain/service/machine/events/MachineFaultEventServiceImpl.kt`
13. `core/.../domain/service/machine/events/MachineDowntimeService.kt`
14. `core/.../domain/service/machine/events/MachineDowntimeServiceImpl.kt`
15. `core/.../data/api/model/machine/events/MachineEventDtos.kt`
16. `core/.../data/api/server/BackendFaultAndDowntimeUseCases.kt`
17. `core/.../data/api/server/BackendRouter.kt`
18. `core/.../data/persistence/postgres/PostgresRepositoryFactory.kt`
19. `core/.../data/composition/RuntimeComposition.kt`
20. `core/.../domain/machine/events/MachineFaultAndDowntimeDomainTest.kt`
21. `core/.../data/machine/events/PostgresMachineEventSecurityTest.kt`
22. `backend/.../backend/machine/events/MachineEventApiTest.kt`
23. `MODULE_21_STEP_06_IMPLEMENTATION_REPORT.md`

---

## AF. Git Commit
- **Branch**: `main`
- **HEAD Commit**: `43c53c9` (plus Step 06 staged additions)

---

## AG. Remaining Gaps
- None. Step 06 scope is 100% complete and verified.

---

## AH. Final Verdict
**PASS**

---

------------------------------------------------------------
MODULE 21 STATUS
------------------------------------------------------------

Step 01 — Machine Registry & Equipment Foundation  
Status: VERIFIED (PASS)

Step 02 — Machine Telemetry Ingestion Foundation  
Status: VERIFIED (PASS)

Step 03 — Machine Status & Health Monitoring  
Status: VERIFIED (PASS)

Step 04 — Production Machine Integration  
Status: VERIFIED (PASS)

Step 05 — Maintenance Management  
Status: VERIFIED (PASS)

Step 06 — Downtime & Fault Event Management  
Status: VERIFIED (PASS)

Next allowed step:  
MODULE 21 → STEP 07 — Alerts & Maintenance Notifications

Do NOT start Step 07 automatically.

------------------------------------------------------------
