package com.tazkara.event.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value object describing a ticket type to create: name, price (0 = free) and quantity.
 */
public record TicketTypeSpec(String name, BigDecimal price, int quantity) {

    public TicketTypeSpec {
        name = name.trim();
        price = price.setScale(2, RoundingMode.HALF_UP);
    }
}
