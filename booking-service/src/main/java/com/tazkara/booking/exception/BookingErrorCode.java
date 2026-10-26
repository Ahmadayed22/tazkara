package com.tazkara.booking.exception;

import com.tazkara.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum BookingErrorCode implements ErrorCode {

    BOOKING_NOT_FOUND(HttpStatus.NOT_FOUND),
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND),
    TICKET_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_CONTENT),
    BOOKING_NOT_PENDING(HttpStatus.CONFLICT),
    HOLD_EXPIRED(HttpStatus.UNPROCESSABLE_CONTENT),
    EVENT_CANCELLED(HttpStatus.UNPROCESSABLE_CONTENT),
    EVENT_ALREADY_STARTED(HttpStatus.UNPROCESSABLE_CONTENT),
    BOOKING_NOT_CANCELLABLE(HttpStatus.CONFLICT),
    CANCELLATION_NOT_ALLOWED(HttpStatus.UNPROCESSABLE_CONTENT),
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT),
    EVENT_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE);

    private final HttpStatus status;

    BookingErrorCode(HttpStatus status) {
        this.status = status;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public HttpStatus status() {
        return status;
    }
}
