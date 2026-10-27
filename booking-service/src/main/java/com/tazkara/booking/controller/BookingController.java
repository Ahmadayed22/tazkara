package com.tazkara.booking.controller;

import com.tazkara.booking.domain.BookingStatus;
import com.tazkara.booking.service.BookingQueryService;
import com.tazkara.booking.service.BookingService;
import com.tazkara.booking.dto.BookingResponse;
import com.tazkara.booking.dto.PayBookingRequest;
import com.tazkara.booking.dto.PlaceBookingRequest;
import com.tazkara.common.security.AuthenticatedUser;
import com.tazkara.common.web.PageResponse;
import com.tazkara.common.web.Paging;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/bookings")
@PreAuthorize("hasRole('ATTENDEE')")
@Tag(name = "Bookings", description = "US-10/11/13/14 book, pay, follow and cancel (ATTENDEE only)")
public class BookingController {

    private final BookingService bookingService;
    private final BookingQueryService bookingQueryService;

    public BookingController(BookingService bookingService, BookingQueryService bookingQueryService) {
        this.bookingService = bookingService;
        this.bookingQueryService = bookingQueryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Book tickets: PENDING_PAYMENT with a 10 minute hold, or CONFIRMED when free")
    public BookingResponse place(@AuthenticationPrincipal AuthenticatedUser user,
                                 @Valid @RequestBody PlaceBookingRequest request) {
        return bookingService.place(user, request);
    }

    @PostMapping("/{reference}/payment")
    @Operation(summary = "Pay a pending booking (simulated, always succeeds)")
    public BookingResponse pay(@AuthenticationPrincipal AuthenticatedUser user,
                               @PathVariable String reference,
                               @Valid @RequestBody PayBookingRequest request) {
        return bookingService.pay(user, reference, request.paymentMethod());
    }

    @PostMapping("/{reference}/cancel")
    @Operation(summary = "Cancel my booking (refund policy applies to confirmed bookings)")
    public BookingResponse cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String reference) {
        return bookingService.cancel(user, reference);
    }

    @GetMapping
    @Operation(summary = "List my bookings, newest first, optionally filtered by status")
    public PageResponse<BookingResponse> listMine(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = Paging.DEFAULT_SIZE_TEXT) @Min(1) @Max(Paging.MAX_SIZE) int size) {
        return bookingQueryService.listMine(user, status, page, size);
    }

    @GetMapping("/{reference}")
    @Operation(summary = "One of my bookings by reference")
    public BookingResponse getMine(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String reference) {
        return bookingQueryService.getMine(user, reference);
    }
}
