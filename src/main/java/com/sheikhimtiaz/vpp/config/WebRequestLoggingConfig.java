package com.sheikhimtiaz.vpp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.WebFilter;
import java.util.UUID;

@Configuration
@Slf4j
public class WebRequestLoggingConfig {

    @Bean
    public WebFilter logFilter() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String requestId = UUID.randomUUID().toString();

            log.info("Incoming Request: [{}] {} {} from {}",
                    requestId,
                    request.getMethod(),
                    request.getURI(),
                    request.getRemoteAddress());

            long startTime = System.currentTimeMillis();

            return chain.filter(exchange.mutate()
                            .request(request.mutate()
                                    .header("Request-ID", requestId)
                                    .build())
                            .build())
                    .doFinally(signalType -> {
                        long duration = System.currentTimeMillis() - startTime;
                        log.info("Completed Request: [{}] {} {} - Took {} ms",
                                requestId,
                                request.getMethod(),
                                request.getURI(),
                                duration);
                    });
        };
    }
}