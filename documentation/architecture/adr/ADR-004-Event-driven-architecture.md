# ADR-004 Event Driven Architecture

## Status: Accepted

## Decision

We will adopt an event-driven architecture for the Nexus platform, where services communicate by
producing and consuming events. This approach promotes loose coupling between services, improves
scalability, and enables asynchronous communication.

## Alternatives Considered

1. **Synchronous Communication**: Using synchronous communication (e.g., REST APIs) between services
   could lead to tight coupling, reduced scalability, and potential performance bottlenecks. It also
   makes the system more fragile, as a failure in one service can directly impact others.
2. **Message Queues**: While message queues can facilitate asynchronous communication, they
   introduce additional complexity in terms of message ordering, delivery guarantees, and error
   handling. They also require additional infrastructure and maintenance.

## Consequences

- **Pros**:
    - Loose coupling between services allows for independent development, deployment, and scaling.
    - Asynchronous communication improves system responsiveness and resilience to failures.
    - Event-driven architecture enables better support for real-time data processing and analytics.
- **Cons**:
    - Increased complexity in managing event flows, message ordering, and error handling.
    - Potential for increased latency in processing events, as services may need to wait for events
      to be consumed and processed.