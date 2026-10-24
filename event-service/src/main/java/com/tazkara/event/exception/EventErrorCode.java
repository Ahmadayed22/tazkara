package com.tazkara.event.exception;

import com.tazkara.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum EventErrorCode implements ErrorCode {

    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND),
    TICKET_TYPE_NOT_FOUND(HttpStatus.UNPROCESSABLE_CONTENT),
    EVENT_START_TOO_SOON(HttpStatus.UNPROCESSABLE_CONTENT),
    INVALID_TICKET_TYPES(HttpStatus.UNPROCESSABLE_CONTENT),
    DUPLICATE_TICKET_TYPE_NAME(HttpStatus.UNPROCESSABLE_CONTENT),
    EVENT_NOT_EDITABLE(HttpStatus.CONFLICT),
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT),
    EVENT_NOT_BOOKABLE(HttpStatus.UNPROCESSABLE_CONTENT),
    TICKETS_UNAVAILABLE(HttpStatus.CONFLICT),
    RESERVATION_ALREADY_RELEASED(HttpStatus.CONFLICT),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST);

    private final HttpStatus status;

    EventErrorCode(HttpStatus status) {
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
