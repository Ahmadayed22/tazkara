package com.tazkara.booking.repository;

import com.tazkara.booking.domain.BookingStatus;

import java.math.BigDecimal;

public record TicketSalesRow(Long ticketTypeId, BookingStatus status, Long tickets, BigDecimal amount) {
}
