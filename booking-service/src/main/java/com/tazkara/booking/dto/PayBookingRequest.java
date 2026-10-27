package com.tazkara.booking.dto;

import com.tazkara.booking.domain.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** US-11 simulated payment: only the method, never card data. */
public record PayBookingRequest(@NotNull @Schema(example = "CARD") PaymentMethod paymentMethod) {
}
