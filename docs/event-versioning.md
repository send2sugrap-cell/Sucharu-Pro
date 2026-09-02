# Domain Event Versioning Strategy

## 1. Schema Versioning Rules
Every event payload is bound to an explicit version string (e.g. `"v1"`).

- **Backward Compatibility**: Non-breaking additions (e.g. new optional fields) may be accommodated within the existing version if consumers remain functional.
- **Breaking Schema Changes**: Any structural modification, removal of fields, or semantic shift in an event requires a new version (e.g., `OrderCreated:v2`).
- **Consumer Version Binding**: `DomainEventConsumer` implementations declare `supportedVersion`. Consumers automatically ignore envelopes whose `eventVersion` does not match their contract.
