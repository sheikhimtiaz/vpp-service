package com.sheikhimtiaz.vpp.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
class ReactiveContextUtilsTest {

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void withMdc_function_shouldAddMdcContextToMono() {
        Map<String, String> mdcContext = new HashMap<>();
        mdcContext.put("requestId", "test-123");
        mdcContext.put("userId", "user-456");

        AtomicReference<Map<String, String>> capturedContext = new AtomicReference<>();

        Mono<String> mono = Mono.deferContextual(ctx -> {
            Map<String, String> contextMap = new HashMap<>();
            ctx.stream().forEach(entry -> contextMap.put(entry.getKey().toString(), entry.getValue().toString()));
            capturedContext.set(contextMap);
            return Mono.just("result");
        });

        Function<Mono<String>, Mono<String>> withMdcTyped = ReactiveContextUtils.withMdc(mdcContext);
        mono = withMdcTyped.apply(mono);

        StepVerifier.create(mono)
                .expectNext("result")
                .verifyComplete();

        assertNotNull(capturedContext.get());
        assertEquals("test-123", capturedContext.get().get("requestId"));
        assertEquals("user-456", capturedContext.get().get("userId"));
    }

    @Test
    void withMdc_direct_shouldAddMdcContextToMono() {
        Map<String, String> mdcContext = new HashMap<>();
        mdcContext.put("requestId", "test-123");
        mdcContext.put("userId", "user-456");

        AtomicReference<Map<String, String>> capturedContext = new AtomicReference<>();

        Mono<String> mono = Mono.deferContextual(ctx -> {
            Map<String, String> contextMap = new HashMap<>();
            ctx.stream().forEach(entry -> contextMap.put(entry.getKey().toString(), entry.getValue().toString()));
            capturedContext.set(contextMap);
            return Mono.just("result");
        });

        mono = ReactiveContextUtils.withMdc(mono, mdcContext);

        StepVerifier.create(mono)
                .expectNext("result")
                .verifyComplete();

        assertNotNull(capturedContext.get());
        assertEquals("test-123", capturedContext.get().get("requestId"));
        assertEquals("user-456", capturedContext.get().get("userId"));
    }

    @Test
    void withMdc_direct_shouldHandleNullMdcContext() {
        Mono<String> mono = Mono.just("result");

        Mono<String> result = ReactiveContextUtils.withMdc(mono, null);

        StepVerifier.create(result)
                .expectNext("result")
                .verifyComplete();
    }

    @Test
    void withCurrentMdc_Mono_shouldApplyCurrentMdcToMono() {
        MDC.put("requestId", "test-123");
        MDC.put("userId", "user-456");

        AtomicReference<Map<String, String>> capturedContext = new AtomicReference<>();

        Mono<String> mono = Mono.deferContextual(ctx -> {
            Map<String, String> contextMap = new HashMap<>();
            ctx.stream().forEach(entry -> contextMap.put(entry.getKey().toString(), entry.getValue().toString()));
            capturedContext.set(contextMap);
            return Mono.just("result");
        });

        mono = ReactiveContextUtils.withCurrentMdc(mono);

        StepVerifier.create(mono)
                .expectNext("result")
                .verifyComplete();

        assertNotNull(capturedContext.get());
        assertEquals("test-123", capturedContext.get().get("requestId"));
        assertEquals("user-456", capturedContext.get().get("userId"));
    }

    @Test
    void withCurrentMdc_Mono_shouldHandleEmptyMdc() {
        AtomicReference<Map<String, String>> capturedContext = new AtomicReference<>();

        Mono<String> mono = Mono.deferContextual(ctx -> {
            Map<String, String> contextMap = new HashMap<>();
            ctx.stream().forEach(entry -> contextMap.put(entry.getKey().toString(), entry.getValue().toString()));
            capturedContext.set(contextMap);
            return Mono.just("result");
        });

        mono = ReactiveContextUtils.withCurrentMdc(mono);

        StepVerifier.create(mono)
                .expectNext("result")
                .verifyComplete();

        assertNotNull(capturedContext.get());
    }

    @Test
    void withCurrentMdc_Flux_shouldApplyCurrentMdcToFlux() {
        MDC.put("requestId", "test-123");
        MDC.put("userId", "user-456");

        AtomicReference<Map<String, String>> capturedContext = new AtomicReference<>();

        Flux<String> flux = Flux.deferContextual(ctx -> {
            Map<String, String> contextMap = new HashMap<>();
            ctx.stream().forEach(entry -> contextMap.put(entry.getKey().toString(), entry.getValue().toString()));
            capturedContext.set(contextMap);
            return Flux.just("result1", "result2");
        });

        flux = ReactiveContextUtils.withCurrentMdc(flux);

        StepVerifier.create(flux)
                .expectNext("result1", "result2")
                .verifyComplete();

        assertNotNull(capturedContext.get());
        assertEquals("test-123", capturedContext.get().get("requestId"));
        assertEquals("user-456", capturedContext.get().get("userId"));
    }

    @Test
    void withCurrentMdc_Flux_shouldHandleEmptyMdc() {
        AtomicReference<Map<String, String>> capturedContext = new AtomicReference<>();

        Flux<String> flux = Flux.deferContextual(ctx -> {
            Map<String, String> contextMap = new HashMap<>();
            ctx.stream().forEach(entry -> contextMap.put(entry.getKey().toString(), entry.getValue().toString()));
            capturedContext.set(contextMap);
            return Flux.just("result1", "result2");
        });

        flux = ReactiveContextUtils.withCurrentMdc(flux);

        StepVerifier.create(flux)
                .expectNext("result1", "result2")
                .verifyComplete();

        assertNotNull(capturedContext.get());
    }

    @Test
    void contextWithRequestId_shouldCreateMapWithRequestId() {
        String requestId = UUID.randomUUID().toString();

        Map<String, String> result = ReactiveContextUtils.contextWithRequestId(requestId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(requestId, result.get("requestId"));
    }

    @Test
    void getRequestIdFromContext_shouldReturnRequestId() {
        String requestId = UUID.randomUUID().toString();
        Context context = Context.of("requestId", requestId);

        String result = ReactiveContextUtils.getRequestIdFromContext(context);

        assertEquals(requestId, result);
    }

    @Test
    void getRequestIdFromContext_shouldReturnUnknownWhenRequestIdNotPresent() {
        Context context = Context.empty();

        String result = ReactiveContextUtils.getRequestIdFromContext(context);

        assertEquals("unknown", result);
    }

    @Test
    void e2e_shouldPropagateRequestIdThroughChain() {
        String requestId = UUID.randomUUID().toString();
        Map<String, String> mdcContext = ReactiveContextUtils.contextWithRequestId(requestId);

        Mono<String> result = Mono.deferContextual(ctx -> {
            String id = ReactiveContextUtils.getRequestIdFromContext((Context) ctx);
            return Mono.just(id);
        });
        result = ReactiveContextUtils.withMdc(result, mdcContext);

        StepVerifier.create(result)
                .expectNext(requestId)
                .verifyComplete();
    }
}