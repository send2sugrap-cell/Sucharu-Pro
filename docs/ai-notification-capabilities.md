# AI Notification Capabilities Matrix

| Capability | Purpose | Default AI Role | Confirmation Required? |
|---|---|---|---|
| `AI_READ_NOTIFICATION_CONTEXT` | Read data-minimized event views | Denied (Explicit Grant) | No |
| `AI_READ_NOTIFICATION_STATUS` | Read notification status/state | Denied (Explicit Grant) | No |
| `AI_READ_NOTIFICATION_HISTORY` | Read scoped entity notification history | Denied (Explicit Grant) | No |
| `AI_CREATE_NOTIFICATION_DRAFT` | Generate notification proposal/draft | Denied (Explicit Grant) | No |
| `AI_REQUEST_NOTIFICATION_SEND` | Request notification dispatch | Denied (Explicit Grant) | **Yes (Human Manager/Admin)** |
| `AI_REQUEST_NOTIFICATION_REPLAY` | Request dead-letter replay | Denied (Explicit Grant) | **Yes (Human Manager/Admin)** |
| `AI_REQUEST_NOTIFICATION_SUPPRESSION` | Request recipient suppression | Denied (Explicit Grant) | **Yes (Human Manager/Admin)** |
| `AI_REQUEST_NOTIFICATION_PREFERENCE_UPDATE` | Propose preference updates | Denied (Explicit Grant) | **Yes (Human Manager/Admin)** |
