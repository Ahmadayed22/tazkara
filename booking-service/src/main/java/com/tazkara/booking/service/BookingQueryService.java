package com.tazkara.booking.service;

import com.tazkara.booking.domain.Booking;
import com.tazkara.booking.domain.BookingStatus;
import com.tazkara.booking.repository.BookingRepository;
import com.tazkara.booking.dto.BookingResponse;
import com.tazkara.common.security.AuthenticatedUser;
import com.tazkara.common.web.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US-13: an attendee sees only their own bookings; anybody else's reference answers 404. */
@Service
public class BookingQueryService {

    private final BookingRepository bookingRepository;

    public BookingQueryService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> listMine(AuthenticatedUser attendee, BookingStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<Booking> bookings = status == null
                ? bookingRepository.findByAttendeeId(attendee.id(), pageable)
                : bookingRepository.findByAttendeeIdAndStatus(attendee.id(), status, pageable);
        return PageResponse.of(bookings, BookingResponse::from);
    }

    @Transactional(readOnly = true)
    public BookingResponse getMine(AuthenticatedUser attendee, String reference) {
        return bookingRepository.findByReference(reference)
                .filter(booking -> booking.isOwnedBy(attendee.id()))
                .map(BookingResponse::from)
                .orElseThrow(() -> BookingService.notFound(reference));
    }
}
