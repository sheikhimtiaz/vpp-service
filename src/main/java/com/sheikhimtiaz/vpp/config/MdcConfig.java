package com.sheikhimtiaz.vpp.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Signal;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@Configuration
@Slf4j
public class MdcConfig {

    private static final String REQUEST_ID_KEY = "requestId";

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public WebFilter mdcFilter() {
        return (exchange, chain) -> {
            String requestId = exchange.getRequest().getHeaders().getFirst("Request-ID");
            if (requestId == null) {
                requestId = UUID.randomUUID().toString();
            }

            log.debug("Setting request ID in context: {}", requestId);
            String finalRequestId = requestId;

            return chain.filter(exchange)
                    .contextWrite(context -> context.put(REQUEST_ID_KEY, finalRequestId))
                    .doOnEach(signal -> {
                        if (signal.isOnNext() || signal.isOnError() || signal.isOnComplete()) {
                            try {
                                String contextRequestId = signal.getContextView().getOrEmpty(REQUEST_ID_KEY)
                                        .map(Object::toString)
                                        .orElse(null);

                                if (contextRequestId != null) {
                                    MDC.put(REQUEST_ID_KEY, contextRequestId);
                                }
                            } catch (Exception e) {
                                log.warn("Error handling MDC: {}", e.getMessage());
                            } finally {
                                MDC.clear();
                            }
                        }
                    });
        };
    }

    private static <T> Consumer<Signal<T>> logOnNext(Consumer<Signal<T>> consumer) {
        return signal -> {
            if (!signal.isOnComplete() && !signal.isOnError()) {
                Optional<String> requestId = signal.getContextView().getOrEmpty(REQUEST_ID_KEY);

                if (requestId.isPresent()) {
                    MDC.put(REQUEST_ID_KEY, requestId.get());
                    try {
                        consumer.accept(signal);
                    } finally {
                        MDC.clear();
                    }
                } else {
                    consumer.accept(signal);
                }
            }
        };
    }
}