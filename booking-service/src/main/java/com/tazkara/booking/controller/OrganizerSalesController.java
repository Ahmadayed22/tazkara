package com.tazkara.booking.controller;


import com.tazkara.booking.domain.BookingStatus;
import com.tazkara.booking.service.OrganizerSalesService;
import com.tazkara.booking.dto.OrganizerBookingResponse;
import com.tazkara.booking.dto.SalesSummaryResponse;
import com.tazkara.common.security.AuthenticatedUser;
import com.tazkara.common.web.PageResponse;
import com.tazkara.common.web.Paging;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/organizer/events/{eventId}")
@PreAuthorize("hasRole('ORGANIZER')")
@Tag(name = "Organizer sales", description = "US-15/16 bookings, sales summary and CSV export of my event (ORGANIZER only)")
public class OrganizerSalesController {

    private static final MediaType TEXT_CSV = new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8);

    private final OrganizerSalesService salesService;

    public OrganizerSalesController(OrganizerSalesService salesService) {
        this.salesService = salesService;
    }

    @GetMapping("/bookings")
    @Operation(summary = "Bookings of my event, newest first, optionally filtered by status")
    public PageResponse<OrganizerBookingResponse> bookings(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long eventId,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = Paging.DEFAULT_SIZE_TEXT) @Min(1) @Max(Paging.MAX_SIZE) int size) {
        return salesService.listBookings(user, eventId, status, page, size);
    }

    @GetMapping("/sales")
    @Operation(summary = "Sales summary per ticket type: total, sold, held, remaining, revenue")
    public SalesSummaryResponse sales(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long eventId) {
        return salesService.salesSummary(user, eventId);
    }

    @GetMapping(value = "/bookings/export", produces = "text/csv")
    @Operation(summary = "Download confirmed bookings as CSV")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long eventId) {
        OrganizerSalesService.CsvFile file = salesService.exportConfirmed(user, eventId);
        return ResponseEntity.ok()
                .contentType(TEXT_CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .body(file.content());
    }
}
