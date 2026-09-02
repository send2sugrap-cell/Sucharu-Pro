# Event Envelope Specification

## 1. Specification
The canonical container for all events emitted in Sucharu Pro is `EventEnvelope<out T : DomainEvent>`.

### Required Properties
- `eventId`: UUIDv4 string, globally unique and collision resistant.
- `eventType`: Canonical `DomainEventType` enum.
- `eventVersion`: Schema version string (default `"v1"`).
- `occurredAt`: Long timestamp (epoch ms) when the domain fact occurred.
- `publishedAt`: Long timestamp (epoch ms) when the envelope was dispatched.
- `projectId`: Non-blank tenant project identifier derived from server authority.
- `aggregateType`: String classification of the root aggregate (e.g. `"ORDER"`).
- `aggregateId`: Unique identifier of the aggregate (e.g. `"ORD-1001"`).
- `aggregateVersion`: Monotonic long version number for concurrency/ordering checks.
- `actorType`: `PrincipalType` (`HUMAN`, `AI_AGENT`, `SYSTEM`, `PUBLIC`).
- `actorId`: Authoritative actor identity.
- `principalType`: Principal classification.
- `correlationId`: Distributed workflow tracing identifier.
- `causationId`: Nullable identifier of the preceding event or command.
- `requestId`: Nullable originating API request identifier.
- `source`: Subsystem identifier (e.g. `"sucharu-pro-backend"`).
- `payload`: Strongly typed generic payload `T : DomainEvent`.
- `metadata`: Immutable map of non-domain tracing attributes.

## 2. Invariants
- `EventEnvelope` instances are immutable.
- `projectId` is never accepted blindly from client request parameters.
- Direct JSON serializations of `EventEnvelope` omit passwords, JWT tokens, and database credentials.
