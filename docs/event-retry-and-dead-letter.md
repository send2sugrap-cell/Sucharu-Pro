# Sucharu Pro Event Retry & Dead-Letter Quarantine

## 1. Retry Strategy & Mathematical Formulation
For transient downstream failures (network timeouts, transient database deadlocks), Sucharu Pro uses **Exponential Backoff with Full Jitter**:

$$\text{Calculated Delay} = \min\left(\text{maxDelayMs}, \text{baseDelayMs} \times \text{multiplier}^{\text{attempt}-1}\right)$$
$$\text{Applied Delay} = \text{Calculated Delay} \times \left(1.0 - \text{jitterFactor} + 2.0 \times \text{jitterFactor} \times \text{rand}()\right)$$

### Default Parameters
- `maxAttempts`: 5
- `baseDelayMs`: 1,000 ms
- `multiplier`: 2.0
- `maxDelayMs`: 300,000 ms (5 minutes)
- `jitterFactor`: 0.20 (±20% randomized spread to avoid thundering herd)

## 2. Failure Classification Matrix
| Classification | Description | Action |
|---|---|---|
| `TRANSIENT` | Network timeout, database connection blip, rate limit | Schedule retry with backoff up to `maxAttempts` |
| `NON_RETRYABLE` | Validation violation, illegal business state | Quarantine immediately to Dead Letter |
| `CORRUPT_PAYLOAD` | Malformed JSON or unparseable payload | Quarantine immediately to Dead Letter |
| `UNSUPPORTED_VERSION`| Event version not supported by consumer | Quarantine immediately to Dead Letter |
| `DOWNSTREAM_UNAVAILABLE` | Downstream service circuit open | Schedule retry with extended backoff |

## 3. Dead-Letter Quarantine Management (`PostgresDeadLetterRepository`)
- `listDeadLetters(tenantContext, limit)`: Returns unresolved quarantined records.
- `getByDeadLetterId(tenantContext, deadLetterId)`: Retrieves full diagnostics, payload, and root cause error.
- `markReplayed(tenantContext, deadLetterId, replayedBy)`: Marks dead letter as replayed and resolves quarantine.
- `resolve(tenantContext, deadLetterId)`: Manually resolves non-actionable dead letters.
