# Multi-Channel Notification Integration

## Overview
The Sucharu Pro notification integration layer bridges domain events to customer, staff, and manager notifications across four distinct channels:
- `IN_APP`: Persistent in-app activity notifications.
- `PUSH`: Mobile device push notifications.
- `EMAIL`: Transactional confirmation and invoice emails.
- `SMS`: Urgent delivery, order, and payment SMS alerts.

## Workflow
1. **Resolution**: `NotificationIntentResolver` extracts domain payload and produces a `NotificationIntent` with title, body, recipient ID, channels, and deep link URL.
2. **Preference Check**: `NotificationDispatchService` verifies user-configured preferences (`NotificationPreferences`) and enforces quiet hours constraints.
3. **Provider Dispatch**: Invokes channel-specific `NotificationProvider` implementations.
4. **Idempotency**: Keyed by `(projectId, eventId, recipientId, channel)` to guarantee customers never receive duplicate SMS or push alerts.
5. **Zero Secrets**: Financial tokens, passwords, and API secrets are never included in notification messages.
