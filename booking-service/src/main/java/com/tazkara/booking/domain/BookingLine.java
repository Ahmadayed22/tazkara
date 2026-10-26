package com.tazkara.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;


@Embeddable
public record BookingLine(
        @Column(name = "ticket_type_id", nullable = false) Long ticketTypeId,
        @Column(name = "ticket_type_name", nullable = false, length = 50) String ticketTypeName,
        @Column(name = "unit_price", nullable = false, precision = 10, scale = 2) BigDecimal unitPrice,
        @Column(nullable = false) int quantity) {

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
