package com.tazkara.common.error;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The single JSON error format returned by every service .
 * Built with (Builder pattern) because most fields are optional.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<FieldError> fieldErrors
) {

    public record FieldError(String field, String message) {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Instant timestamp = Instant.now();
        private ErrorCode errorCode = CommonErrorCode.INTERNAL_ERROR;
        private String message;
        private String path;
        private final List<FieldError> fieldErrors = new ArrayList<>();

        private Builder() {
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder errorCode(ErrorCode errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder fieldError(String field, String message) {
            this.fieldErrors.add(new FieldError(field, message));
            return this;
        }

        public ApiError build() {
            return new ApiError(
                    timestamp,
                    errorCode.status().value(),
                    errorCode.status().getReasonPhrase(),
                    errorCode.code(),
                    message,
                    path,
                    List.copyOf(fieldErrors));
        }
    }


} 
