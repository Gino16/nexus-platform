# ADR-006 Saga pattern

## Status: Accepted

## Decision

We will implement the Saga pattern for order service in Nexus to coordinate inventory and payment
services. The Saga pattern will help manage distributed transactions across multiple services,
ensuring data consistency and reliability.

## Alternatives Considered

1. **Choreography**: In this approach, services communicate directly with each other to manage the
   transaction. While it reduces the need for a central coordinator, it can lead to complex
   inter-service dependencies and make it harder to manage failures and retries.
2. **Orchestration**: This approach involves a central coordinator that manages the transaction
   across
   services. While it simplifies the management of distributed transactions, it can introduce a
   single point of failure and may require additional infrastructure to support the coordinator.
3. **Distributed Transactions**: Using distributed transactions (e.g., two-phase commit) can ensure
   data consistency across services. However, it can introduce significant complexity, performance
   overhead, and potential for deadlocks, making it less suitable for a microservices' architecture.

## Consequences

- **Pros**:
    - The Saga pattern provides a structured approach to managing distributed transactions,
      ensuring data consistency and reliability across services.
    - It allows for better fault tolerance and recovery, as each service can handle its own
      compensating actions in case of failures.
    - The Saga pattern promotes loose coupling between services, enabling independent development,
      deployment, and scaling.
- **Cons**:
    - Implementing the Saga pattern can introduce additional complexity in terms of managing
      compensating actions, error handling, and monitoring.
    - It may result in increased latency and reduced performance due to the coordination required
      between services.
    - Debugging and troubleshooting can be more challenging due to the distributed nature of the
      transactions.