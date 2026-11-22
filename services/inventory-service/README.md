# Inventory service

Inventory owns stock and reservations in its own PostgreSQL database. Register stock using the UUID of
a catalog product. Order and payment integration can use the reservation operations once those modules
are available.

Run the service using the [local infrastructure guide](../../devops/README.md). Import
[the OpenAPI contract](src/main/openapi/inventory-api.yaml) into Postman and use
`http://localhost:8081/api/v1` as the base URL.

| Operation | Method and path | Body |
| --- | --- | --- |
| Register stock | `POST /stocks` | `{"productId":"<uuid>","quantity":10}` |
| Read stock | `GET /stocks/{productId}` | |
| Adjust available stock | `POST /stocks/{productId}/adjustments` | `{"delta":-2}` |
| Reserve stock for an order | `PUT /reservations/{orderId}` | `{"lines":[{"productId":"<uuid>","quantity":2}]}` |
| Read a reservation | `GET /reservations/{orderId}` | |
| Release reserved stock | `PUT /reservations/{orderId}/release` | |
| Confirm stock consumption | `PUT /reservations/{orderId}/confirm` | |

An adjustment affects only available units. A reservation accepts up to 100 distinct products. A reservation moves units from available to reserved
for every line in one transaction. A release returns those units to available stock. A confirmation
consumes them from reserved stock. Use release after payment rejection and confirmation after
payment approval.

A retry with the same order ID and product quantities returns the existing reservation, regardless
of line order. Repeating a release or confirmation returns the current state. A different set of
lines or a conflicting transition returns `409`. If any line is missing stock or requests more than
is available, the entire reservation rolls back.
