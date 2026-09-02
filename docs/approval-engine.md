# Approval Engine & Separation of Duties

## Overview
The Sucharu Pro Approval Engine governs sensitive financial, operational, and QC decisions across printing workflows.

## Security Policies & Controls
1. **Separation of Duties (SoD)**:
   - When `allowSelfApproval == false` on an `ApprovalPolicy`, the creator/requester of the request (`requesterId`) is strictly denied from approving their own request.
   - Any attempt to bypass SoD throws a security violation and is logged to `WorkflowAuditLogger`.

2. **Role & Capability Check**:
   - Approvers must hold the required role (e.g., `MANAGER`, `ADMIN`) and possess required capabilities.
   - External roles (`CUSTOMER`, `AFFILIATE`) cannot access or approve internal requests.

3. **Machine Principal Fence**:
   - `PrincipalType.AI_AGENT` and `UserRole.AI_AGENT` are strictly barred from deciding or approving workflow approval requests.

4. **Multi-Approver Quorums**:
   - Supports M-of-N thresholds (`minimumApprovals`). The approval status transitions to `APPROVED` only when the threshold of independent approver decisions is met.

5. **Escalation Mechanism**:
   - Rejection by lower roles or timeout expiration triggers automatic escalation to higher tier roles defined by `escalationRole`.
