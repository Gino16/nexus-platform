# NEXUS — Software Requirements

**Version:** 1.0  
**Status:** MVP Baseline  
**Project:** NEXUS — Cloud Native Commerce Platform

## 1. Overview

NEXUS is a portfolio project that demonstrates the design, implementation, integration, security, and operation of a cloud-native retail commerce platform.

The platform allows administrators to manage products and inventory, while customers can browse products and create purchase orders.

The primary goal is to demonstrate software engineering capabilities through a working end-to-end system rather than to build a production-ready commercial application.

## 2. Scope

### In Scope

- Product catalog management.
- Inventory management and reservation.
- Purchase orders containing multiple products.
- Simulated payment processing.
- Authentication and role-based authorization.
- Event-driven communication between services.
- Basic order notifications.

### Out of Scope

- Real monetary transactions.
- Shipping and logistics.
- Refunds and customer-initiated cancellations.
- Discounts, promotions, and coupons.
- Tax calculation and invoicing.
- Marketplace and multiple sellers.
- Web or mobile frontend.
- Production-grade payment reconciliation.

The MVP will be accessible through APIs.

## 3. Actors

**CUSTOMER**

- Browse available products.
- Create purchase orders.
- Consult their own orders.

**ADMIN**

- Create and manage products.
- Manage product inventory.
- Inspect orders.

## 4. Functional Requirements

### RF-001 — Product Catalog

The system shall allow administrators to create and update products.

Each product shall contain:

- Unique identifier.
- Name.
- Description.
- Price.
- Active status.

Customers shall be able to list and retrieve active products.

### RF-002 — Inventory Management

Administrators shall be able to register and adjust product stock.

The system shall:

- Track available and reserved quantities.
- Prevent reservations exceeding available stock.
- Reserve all requested products in an order or none.
- Release reserved quantities when simulated payment is rejected.

### RF-003 — Purchase Orders

Authenticated customers shall be able to create orders containing one or more products.

Each order shall contain:

- Unique identifier.
- Customer identifier.
- Product lines with quantities.
- Unit-price snapshots.
- Total amount.
- Current status.
- Creation timestamp.

The system shall validate products, quantities, and inventory availability.

Customers shall be able to consult their own orders.

### RF-004 — Order Lifecycle

Orders shall support the following states:

| State | Description |
|---|---|
| PENDING | Order created |
| PROCESSING | Inventory reservation and payment processing |
| CONFIRMED | Inventory reserved and payment approved |
| REJECTED | Inventory unavailable or payment rejected |

Orders shall not be confirmed unless all requested inventory has been reserved and the simulated payment has been approved.

### RF-005 — Simulated Payments

The system shall simulate payment authorization.

The simulator shall support two outcomes:

- APPROVED.
- DECLINED.

An approved payment shall allow order confirmation.

A declined payment shall trigger release of the associated inventory reservation and order rejection.

No real financial transactions shall be performed.

### RF-006 — Authentication and Authorization

The system shall authenticate users through Keycloak using OAuth2/OIDC.

The system shall enforce CUSTOMER and ADMIN permissions.

Customers shall not access orders belonging to other customers.

Only administrators shall perform catalog and inventory management operations.

### RF-007 — Basic Notifications

When an order reaches CONFIRMED or REJECTED, the system shall publish an event.

A notification component shall consume the event and record a notification in application logs.

Sending real emails is outside the MVP.

## 5. Business Rules

- BR-001: An order must contain at least one product.
- BR-002: Product quantities must be positive integers.
- BR-003: Products must be active to be purchased.
- BR-004: A product can appear only once in an order.
- BR-005: Inventory reservations must cover every order line.
- BR-006: Inventory cannot become negative.
- BR-007: Payment authorization must succeed before confirming an order.
- BR-008: Product-price changes must not alter existing orders.
- BR-009: All product prices and order totals use PEN with two decimal places.
- BR-010: An order must not be processed twice due to duplicate delivery of the same event.

## 6. Non-Functional Requirements

### Security

- Protected operations require valid authentication.
- Authorization is enforced by the backend.
- Secrets must not be committed to the repository.

### Reliability

- Duplicate event deliveries must not duplicate inventory reservations or payments.
- Order processing must support safe retries for expected transient failures.
- Simulated payment outcomes are deterministic for the MVP.

### Maintainability

- Business logic must remain independent of infrastructure frameworks.
- Services must expose clear responsibilities.
- Critical business rules must have automated tests.

### Observability

- Services shall produce structured application logs.
- Requests and asynchronous processing shall support correlation.
- Distributed tracing will be incorporated during the observability phase.

### Deployment

- The system shall run locally using Docker Compose.
- Automated CI shall build and test the project.
- Kubernetes deployment will be added after the functional MVP is complete.

## 7. MVP Acceptance Criteria

The MVP is complete when:

1. An administrator can create a product and assign stock.
2. A customer can retrieve available products.
3. A customer can submit an order containing multiple products.
4. Sufficient inventory and approved payment produce a CONFIRMED order.
5. Insufficient inventory produces a REJECTED order without partial reservations.
6. Declined payment produces a REJECTED order and releases reserved stock.
7. A customer can consult their own orders but not those of another customer.
8. Administrative operations reject unauthorized access.
9. Order confirmation and rejection generate notification events.
10. The complete purchase flow can be demonstrated through automated integration tests.

## 8. Implementation Boundaries

The initial solution will use the following components:

- Java 21 and Quarkus.
- Maven multi-module monorepo.
- Hexagonal architecture within application services.
- PostgreSQL for persistence.
- Kafka for asynchronous integration.
- Keycloak for identity and access management.
- Docker Compose for local execution.

Redis, Kubernetes, Helm, CI/CD enhancements, and observability tooling will be incorporated incrementally.

These technologies support the portfolio's learning objectives and are not additional functional requirements.

---

**End of document.**