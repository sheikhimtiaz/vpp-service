package com.sheikhimtiaz.vpp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalWebFluxExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleResourceNotFoundException(
            ResourceNotFoundException ex, ServerWebExchange exchange) {
        return createErrorResponse(ex, HttpStatus.NOT_FOUND, exchange);
    }

    @ExceptionHandler(ValidationException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleValidationException(
            ValidationException ex, ServerWebExchange exchange) {
        return createErrorResponse(ex, HttpStatus.BAD_REQUEST, exchange);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleUnauthorizedException(
            UnauthorizedException ex, ServerWebExchange exchange) {
        return createErrorResponse(ex, HttpStatus.UNAUTHORIZED, exchange);
    }

    @ExceptionHandler(ForbiddenException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleForbiddenException(
            ForbiddenException ex, ServerWebExchange exchange) {
        return createErrorResponse(ex, HttpStatus.FORBIDDEN, exchange);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleResponseStatusException(
            ResponseStatusException ex, ServerWebExchange exchange) {
        return createErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, exchange);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleWebExchangeBindException(
            WebExchangeBindException ex, ServerWebExchange exchange) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return createErrorResponse(
                new ValidationException("Validation failed: " + errorMessage),
                HttpStatus.BAD_REQUEST,
                exchange);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleGenericException(
            Exception ex, ServerWebExchange exchange) {
        return createErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, exchange);
    }

    private Mono<ResponseEntity<Map<String, Object>>> createErrorResponse(
            Throwable ex, HttpStatus status, ServerWebExchange exchange) {

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("path", exchange.getRequest().getPath().value());
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());

        if (ex instanceof ResponseStatusException) {
            errorResponse.put("message", ((ResponseStatusException) ex).getReason());
        } else {
            errorResponse.put("message", ex.getMessage());
        }

        String requestId = exchange.getRequest().getId();
        if (requestId != null) {
            errorResponse.put("requestId", requestId);
        }

        return Mono.just(ResponseEntity.status(status).body(errorResponse));
    }

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }

    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) {
            super(message);
        }
    }
}