package com.tazkara.common.error;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Base class of the per-service @RestControllerAdvice. Translates every exception into the common
 * 
 */
public abstract class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomain(DomainException ex, HttpServletRequest request) {
        return respond(ApiError.builder().errorCode(ex.getErrorCode()).message(ex.getMessage()), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ApiError.Builder builder = ApiError.builder()
                .errorCode(CommonErrorCode.VALIDATION_FAILED)
                .message("Request validation failed");
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> builder.fieldError(fe.getField(), fe.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(ge -> builder.fieldError(ge.getObjectName(), ge.getDefaultMessage()));
        return respond(builder, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleParameterValidation(HandlerMethodValidationException ex, HttpServletRequest request) {
        ApiError.Builder builder = ApiError.builder()
                .errorCode(CommonErrorCode.VALIDATION_FAILED)
                .message("Request validation failed");
        ex.getParameterValidationResults().forEach(result -> result.getResolvableErrors().forEach(error ->
                builder.fieldError(result.getMethodParameter().getParameterName(), error.getDefaultMessage())));
        return respond(builder, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        ApiError.Builder builder = ApiError.builder()
                .errorCode(CommonErrorCode.VALIDATION_FAILED)
                .message("Request validation failed");
        ex.getConstraintViolations().forEach(v -> {
            String path = v.getPropertyPath().toString();
            builder.fieldError(path.substring(path.lastIndexOf('.') + 1), v.getMessage());
        });
        return respond(builder, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ApiError.Builder builder = ApiError.builder()
                .errorCode(CommonErrorCode.MALFORMED_REQUEST)
                .message("Request body is missing or malformed");
        Throwable cause = ex.getCause();
        if (cause instanceof UnrecognizedPropertyException upe) {
            builder.message("Unknown field in request body").fieldError(upe.getPropertyName(), "field is not allowed");
        } else if (cause instanceof InvalidFormatException ife && ife.getTargetType() != null && ife.getTargetType().isEnum()) {
            String allowed = Arrays.stream(ife.getTargetType().getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            builder.message("Invalid value in request body").fieldError(fieldName(ife), "must be one of: " + allowed);
        } else if (cause instanceof MismatchedInputException mie && !mie.getPath().isEmpty()) {
            builder.message("Invalid value in request body").fieldError(fieldName(mie), "has an invalid format");
        }
        return respond(builder, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return respond(ApiError.builder()
                .errorCode(CommonErrorCode.VALIDATION_FAILED)
                .message("Invalid value for parameter '" + ex.getName() + "'")
                .fieldError(ex.getName(), "has an invalid value"), request);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingRequestHeaderException.class})
    public ResponseEntity<ApiError> handleMissingParameter(Exception ex, HttpServletRequest request) {
        return respond(ApiError.builder().errorCode(CommonErrorCode.VALIDATION_FAILED).message(ex.getMessage()), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return respond(ApiError.builder().errorCode(CommonErrorCode.UNAUTHORIZED).message("Authentication is required"), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return respond(ApiError.builder()
                .errorCode(CommonErrorCode.ACCESS_DENIED)
                .message("You do not have permission to perform this action"), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return respond(ApiError.builder().errorCode(CommonErrorCode.NOT_FOUND).message("Resource not found"), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return respond(ApiError.builder().errorCode(CommonErrorCode.METHOD_NOT_ALLOWED).message(ex.getMessage()), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return respond(ApiError.builder().errorCode(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE).message(ex.getMessage()), request);
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    public ResponseEntity<ApiError> handleLocking(Exception ex, HttpServletRequest request) {
        log.info("Concurrent modification detected on {}: {}", request.getRequestURI(), ex.getMessage());
        return respond(ApiError.builder()
                .errorCode(CommonErrorCode.CONCURRENT_MODIFICATION)
                .message("The resource was modified concurrently, please retry"), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation on {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return respond(ApiError.builder()
                .errorCode(CommonErrorCode.CONCURRENT_MODIFICATION)
                .message("The request conflicts with the current state of the resource"), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(ApiError.builder()
                .errorCode(CommonErrorCode.INTERNAL_ERROR)
                .message("An unexpected error occurred"), request);
    }

    protected ResponseEntity<ApiError> respond(ApiError.Builder builder, HttpServletRequest request) {
        ApiError body = builder.path(request.getRequestURI()).build();
        return ResponseEntity.status(body.status()).body(body);
    }

    private static String fieldName(MismatchedInputException ex) {
        return ex.getPath().stream()
                .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                .collect(Collectors.joining("."));
    }
}
