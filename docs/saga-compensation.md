# Saga & Reverse Compensation Engine

## Overview
Distributed multi-step transactions in Sucharu Pro follow the Saga Pattern. When a step in a multi-step business workflow fails permanently or is rejected during approval, previous successful side-effects must be rolled back.

## Topological Reversal Algorithm
1. **Identify Succeeded Steps**: Inspect `workflow_step_executions` for all steps with `status == SUCCEEDED`.
2. **Sort by Descending Completion Time**: Steps are ordered in reverse order of completion (`completedAt DESC`).
3. **Execute Compensation Actions**:
   - If the step defines a `compensationStepId`, the corresponding compensation handler is invoked.
   - Example:
     `Laser Plate Creation (Step 3)` fails
     → `Charge Payment (Step 2)` is refunded via `comp-refund-card`
     → `Reserve Inventory (Step 1)` is released via `comp-release-inv`.
4. **Record Compensation Audit**: Each rollback attempt records a `WorkflowCompensationRecord` with status `COMPENSATED` or `COMPENSATION_FAILED`.
5. **Dead-Letter Escalation**: If any compensation action fails, the entire workflow transitions to `DEAD_LETTER` for urgent manual administrator intervention.
