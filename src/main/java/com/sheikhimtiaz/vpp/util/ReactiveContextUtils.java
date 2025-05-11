package com.sheikhimtiaz.vpp.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class for working with MDC in a reactive context
 */
@Slf4j
public class ReactiveContextUtils {

    private static final String REQUEST_ID_KEY = "requestId";

    /**
     * Apply MDC context to a Mono
     */
    public static <T> Function<Mono<T>, Mono<T>> withMdc(Map<String, String> mdcContext) {
        return mono -> mono
                .contextWrite(reactorContext -> {
                    if (mdcContext != null) {
                        Context newContext = reactorContext;
                        for (Map.Entry<String, String> entry : mdcContext.entrySet()) {
                            newContext = newContext.put(entry.getKey(), entry.getValue());
                        }
                        return newContext;
                    }
                    return reactorContext;
                });
    }

    /**
     * Apply MDC context directly to a Mono
     */
    public static <T> Mono<T> withMdc(Mono<T> mono, Map<String, String> mdcContext) {
        if (mdcContext == null) {
            return mono;
        }

        return mono.contextWrite(reactorContext -> {
            Context newContext = reactorContext;
            for (Map.Entry<String, String> entry : mdcContext.entrySet()) {
                newContext = newContext.put(entry.getKey(), entry.getValue());
            }
            return newContext;
        });
    }

    /**
     * Apply current MDC context to a Mono
     */
    public static <T> Mono<T> withCurrentMdc(Mono<T> mono) {
        Map<String, String> currentMdc = MDC.getCopyOfContextMap();
        if (currentMdc != null) {
            return withMdc(mono, currentMdc);
        }
        return mono;
    }

    /**
     * Apply current MDC context to a Flux
     */
    public static <T> Flux<T> withCurrentMdc(Flux<T> flux) {
        Map<String, String> currentMdc = MDC.getCopyOfContextMap();
        if (currentMdc != null) {
            return flux.contextWrite(context -> {
                Context newContext = context;
                for (Map.Entry<String, String> entry : currentMdc.entrySet()) {
                    newContext = newContext.put(entry.getKey(), entry.getValue());
                }
                return newContext;
            });
        }
        return flux;
    }

    /**
     * Create a map with a request ID
     */
    public static Map<String, String> contextWithRequestId(String requestId) {
        Map<String, String> context = new HashMap<>();
        context.put(REQUEST_ID_KEY, requestId);
        return context;
    }

    /**
     * Get the request ID from the reactive context
     */
    public static String getRequestIdFromContext(Context context) {
        return context.getOrEmpty(REQUEST_ID_KEY)
                .map(Object::toString)
                .orElse("unknown");
    }
}