# ADR-007 Transactional outbox

## Status: Accepted

## Decision

We will implement a transactional outbox pattern to ensure reliable message delivery in our system.
This pattern will allow us to decouple the message production from the message consumption, ensuring
that messages are not lost in case of failures.

## Alternatives Considered

1. **Direct Message Sending**: Sending messages directly from the service that generates them. This
   approach is simple but can lead to message loss if the service crashes before the message is
   sent.

## Consequences

- **Pros:**
    - Ensures reliable message delivery.
    - Decouples message production from consumption.
    - Allows for easier scaling of message consumers.
    - Needs an outbox publisher to read messages from the outbox and send them to the message
      broker.
- **Cons:**
    - Adds complexity to the system.
    - Requires additional infrastructure for message storage and processing.