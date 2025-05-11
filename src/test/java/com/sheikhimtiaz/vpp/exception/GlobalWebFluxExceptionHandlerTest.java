package com.sheikhimtiaz.vpp.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalWebFluxExceptionHandlerTest {

    private GlobalWebFluxExceptionHandler exceptionHandler;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        exceptionHandler = new GlobalWebFluxExceptionHandler();

        when(exchange.getRequest()).thenReturn(request);

        org.springframework.http.server.RequestPath mockPath = mock(org.springframework.http.server.RequestPath.class);
        when(mockPath.value()).thenReturn("/api/batteries");
        when(request.getPath()).thenReturn(mockPath);

        when(request.getId()).thenReturn("test-request-id");
    }

    @Test
    void handleResourceNotFoundException_shouldReturnNotFoundStatus() {
        GlobalWebFluxExceptionHandler.ResourceNotFoundException exception =
                new GlobalWebFluxExceptionHandler.ResourceNotFoundException("Resource not found");

        Mono<ResponseEntity<Map<String, Object>>> responseMono =
                exceptionHandler.handleResourceNotFoundException(exception, exchange);

        StepVerifier.create(responseMono)
                .consumeNextWith(response -> {
                    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                    Map<String, Object> body = response.getBody();
                    assertNotNull(body);
                    assertEquals("Resource not found", body.get("message"));
                    assertEquals(HttpStatus.NOT_FOUND.value(), body.get("status"));
                    assertEquals(HttpStatus.NOT_FOUND.getReasonPhrase(), body.get("error"));
                    assertEquals("/api/batteries", body.get("path"));
                    assertEquals("test-request-id", body.get("requestId"));
                    assertNotNull(body.get("timestamp"));
                })
                .verifyComplete();
    }

    @Test
    void handleValidationException_shouldReturnBadRequestStatus() {
        GlobalWebFluxExceptionHandler.ValidationException exception =
                new GlobalWebFluxExceptionHandler.ValidationException("Validation failed");

        Mono<ResponseEntity<Map<String, Object>>> responseMono =
                exceptionHandler.handleValidationException(exception, exchange);

        StepVerifier.create(responseMono)
                .consumeNextWith(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    Map<String, Object> body = response.getBody();
                    assertNotNull(body);
                    assertEquals("Validation failed", body.get("message"));
                    assertEquals(HttpStatus.BAD_REQUEST.value(), body.get("status"));
                    assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), body.get("error"));
                })
                .verifyComplete();
    }

    @Test
    void handleUnauthorizedException_shouldReturnUnauthorizedStatus() {
        GlobalWebFluxExceptionHandler.UnauthorizedException exception =
                new GlobalWebFluxExceptionHandler.UnauthorizedException("Unauthorized access");

        Mono<ResponseEntity<Map<String, Object>>> responseMono =
                exceptionHandler.handleUnauthorizedException(exception, exchange);

        StepVerifier.create(responseMono)
                .consumeNextWith(response -> {
                    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
                    Map<String, Object> body = response.getBody();
                    assertNotNull(body);
                    assertEquals("Unauthorized access", body.get("message"));
                    assertEquals(HttpStatus.UNAUTHORIZED.value(), body.get("status"));
                    assertEquals(HttpStatus.UNAUTHORIZED.getReasonPhrase(), body.get("error"));
                })
                .verifyComplete();
    }

    @Test
    void handleForbiddenException_shouldReturnForbiddenStatus() {
        GlobalWebFluxExceptionHandler.ForbiddenException exception =
                new GlobalWebFluxExceptionHandler.ForbiddenException("Access forbidden");

        Mono<ResponseEntity<Map<String, Object>>> responseMono =
                exceptionHandler.handleForbiddenException(exception, exchange);

        StepVerifier.create(responseMono)
                .consumeNextWith(response -> {
                    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
                    Map<String, Object> body = response.getBody();
                    assertNotNull(body);
                    assertEquals("Access forbidden", body.get("message"));
                    assertEquals(HttpStatus.FORBIDDEN.value(), body.get("status"));
                    assertEquals(HttpStatus.FORBIDDEN.getReasonPhrase(), body.get("error"));
                })
                .verifyComplete();
    }

    @Test
    void handleResponseStatusException_shouldReturnCorrectStatus() {
        ResponseStatusException exception =
                new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service unavailable");

        Mono<ResponseEntity<Map<String, Object>>> responseMono =
                exceptionHandler.handleResponseStatusException(exception, exchange);

        StepVerifier.create(responseMono)
                .consumeNextWith(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                    Map<String, Object> body = response.getBody();
                    assertNotNull(body);
                    assertEquals("Service unavailable", body.get("message"));
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), body.get("status"));
                })
                .verifyComplete();
    }

    @Test
    void handleGenericException_shouldReturnInternalServerError() {
        Exception exception = new RuntimeException("Unexpected error");

        Mono<ResponseEntity<Map<String, Object>>> responseMono =
                exceptionHandler.handleGenericException(exception, exchange);

        StepVerifier.create(responseMono)
                .consumeNextWith(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                    Map<String, Object> body = response.getBody();
                    assertNotNull(body);
                    assertEquals("Unexpected error", body.get("message"));
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), body.get("status"));
                })
                .verifyComplete();
    }

    @Test
    void createErrorResponse_withNullRequestId_shouldNotIncludeRequestId() {
        when(request.getId()).thenReturn(null);
        Exception exception = new RuntimeException("Test exception");

        Mono<ResponseEntity<Map<String, Object>>> responseMono =
                exceptionHandler.handleGenericException(exception, exchange);

        StepVerifier.create(responseMono)
                .consumeNextWith(response -> {
                    Map<String, Object> body = response.getBody();
                    assertNotNull(body);
                    assertFalse(body.containsKey("requestId"));
                })
                .verifyComplete();
    }

    private org.springframework.validation.FieldError createFieldError(String field, String message) {
        org.springframework.validation.FieldError error = mock(org.springframework.validation.FieldError.class);
        when(error.getField()).thenReturn(field);
        when(error.getDefaultMessage()).thenReturn(message);
        return error;
    }
}