package io.fiap.fastfood.driven.core.domain.model;

import java.time.LocalDateTime;

public record Billing(
    String id,
    LocalDateTime openAt,
    LocalDateTime closedAt) {
}
