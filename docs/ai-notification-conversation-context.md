# Conversational Notification Context

## Scoping Rules
- Conversational context is strictly scoped by `conversationId`, `projectId`, and `entityReference`.
- No global history retrieval is permitted.
- History requests are capped at 20 items per call.
- All titles and body snippets are sanitized before conversational presentation.
