# AI Human-in-the-Loop Confirmation Lifecycle

## Workflow
1. AI Agent requests high-impact action (e.g. `REQUEST_SEND`).
2. Action Gateway detects unconfirmed request and creates an `AiNotificationConfirmationRequest` (`status: PENDING`, TTL: 30 min).
3. Gateway returns `RequiresConfirmation(confirmationId, ...)`.
4. Authorized Human (`MANAGER` or `ADMIN`) reviews and approves via `AiNotificationConfirmationService.approveConfirmation`.
5. AI Agent resubmits action with `confirmationId`.
6. Gateway verifies approval, role, non-expiration, tenant match, and executes.
