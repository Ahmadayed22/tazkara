package com.tazkara.identity.exception;
import com.tazkara.common.error.ErrorCode;
import org.springframework.http.HttpStatus;
public enum IdentityErrorCode implements ErrorCode {

    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    ORGANIZATION_NAME_REQUIRED(HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND);

    private final HttpStatus status;

    IdentityErrorCode(HttpStatus status) {
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
