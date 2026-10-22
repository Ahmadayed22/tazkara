package com.tazkara.common.error;

import org.springframework.http.HttpStatus;

// Error codes shared by every service 

public enum CommonErrorCode implements ErrorCode {

    //400
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST),
    //401
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED),
    //403
    ACCESS_DENIED(HttpStatus.FORBIDDEN),
    //404
    NOT_FOUND(HttpStatus.NOT_FOUND),
    //405
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED),
    //409
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT),
    //415
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    //500
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    CommonErrorCode(HttpStatus status) {
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
