package com.tazkara.booking.dto;

import java.math.BigDecimal;
import java.util.List;

/** US-15 sales summary: per ticket type total, sold (CONFIRMED), held (PENDING_PAYMENT), remaining and revenue. */
public record SalesSummaryResponse(
        Long eventId,
        String eventTitle,
        String eventStatus,
        String currency,
        List<TicketTypeSales> ticketTypes,
        long totalSold,
        long totalHeld,
        BigDecimal totalRevenue) {

    public record TicketTypeSales(Long ticketTypeId, String name, BigDecimal price, int total, long sold, long held,
                                  long remaining, BigDecimal revenue) {
    }
}
