package io.nexus.order.infrastructure;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderEvent(UUID eventId, UUID orderId, String type, OffsetDateTime occurredAt,
    int schemaVersion) {}
