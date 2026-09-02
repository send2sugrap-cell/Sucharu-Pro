# Human-in-the-Loop & AI Agent Boundaries

## Overview
Sucharu Pro balances AI-driven automation with human oversight to ensure high-stakes commercial printing and financial actions remain safely governed.

## Security Boundary Matrix
| Initiator Principal | Target Workflow Category | Requirements | Status |
|---|---|---|---|
| `HUMAN` (Staff/Manager) | Standard Printing Operations | Authenticated Session | `AUTHORIZED` |
| `HUMAN` (Staff/Manager) | Financial Payout / Bulk Refund | Role Check + Approval Policy | `AUTHORIZED` / `PENDING_APPROVAL` |
| `AI_AGENT` (Machine) | Read / Analyze Print Specs | Registered Capability Check | `AUTHORIZED` |
| `AI_AGENT` (Machine) | Automated Preflight Check | Registered Capability Check | `AUTHORIZED` |
| `AI_AGENT` (Machine) | High-Impact (Refund / Cancellation) | Explicit Human Approval Metadata (`approvedByHumanId`) | `REQUIRES_HUMAN_APPROVAL` |
| `AI_AGENT` (Machine) | Approval Decision / Self-Approval | N/A | `STRICTLY_DENIED` |
| `AI_AGENT` (Machine) | Cross-Tenant Operation | N/A | `STRICTLY_DENIED` |

## Human Confirmation Metadata Contract
```json
{
  "requiresConfirmation": "true",
  "confirmationId": "CONF-FIN-2026-9812",
  "approvedByHumanId": "usr_mgr_881",
  "approvedAt": 1756024800000,
  "signature": "hmac_sha256_verified"
}
```
If an AI Agent initiates a high-impact workflow without this verified metadata, the security boundary returns `AiAgentWorkflowAuthResult.RequiresHumanApproval` and halts execution.
