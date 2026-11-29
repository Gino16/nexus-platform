# Order flow

Orders snapshots catalog prices over HTTP. Inventory commands also use HTTP, while payment
authorization and terminal notifications use Kafka. Pending outbox records are retried after a
broker outage. The payment service stores one simulated authorization per order in its own database.

```mermaid
sequenceDiagram
    autonumber
    actor Customer
    participant Catalog
    participant Orders
    participant Inventory
    participant Kafka
    participant Payment
    participant Notifications

    Customer->>Orders: PUT /api/v1/orders/{orderId}
    Orders->>Catalog: Read active products and prices
    Orders-->>Customer: 202 Accepted
    Orders->>Inventory: Reserve every order line

    alt Inventory unavailable
        Orders->>Orders: Save REJECTED and terminal outbox event
    else Inventory reserved
        Orders->>Orders: Save payment request in outbox
        Orders->>Kafka: PaymentRequested
        Kafka->>Payment: Payment request
        Payment->>Payment: Persist simulated authorization
        Payment->>Kafka: Authorization result
        Kafka->>Orders: Payment result
        alt Approved
            Orders->>Inventory: Confirm reservation
            Orders->>Orders: Save CONFIRMED and terminal outbox event
        else Declined
            Orders->>Inventory: Release reservation
            Orders->>Orders: Save REJECTED and terminal outbox event
        end
    end

    Orders->>Kafka: OrderConfirmed or OrderRejected
    Kafka->>Notifications: Record terminal event in application log
    Customer->>Orders: GET /api/v1/orders/{orderId}
```
