package com.sheikhimtiaz.vpp.controller;

import com.sheikhimtiaz.vpp.model.BatteryDto;
import com.sheikhimtiaz.vpp.model.BatteryQueryResponse;
import com.sheikhimtiaz.vpp.service.BatteryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(BatteryController.class)
@ActiveProfiles("test")
public class BatteryControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private BatteryService batteryService;

    private List<BatteryDto> testBatteries;
    private BatteryQueryResponse testResponse;
    private int pageNumber;
    private int pageSize;
    private Pageable pageable;

    @TestConfiguration
    static class BatteryServiceConfig {
        @Bean
        public BatteryService batteryService() {
            return Mockito.mock(BatteryService.class);
        }
    }

    @BeforeEach
    public void setup() {
        Mockito.reset(batteryService);
        pageNumber = 0;
        pageSize = 50;
        pageable = PageRequest.of(pageNumber, pageSize, Sort.by("name"));

        testBatteries = Arrays.asList(
                new BatteryDto("Battery1", "12345", 100),
                new BatteryDto("Battery2", "23456", 200)
        );

        testResponse = new BatteryQueryResponse(
                Arrays.asList("Battery1", "Battery2"),
                150.0,
                300.0,
                2, 0 ,50
        );

        when(batteryService.registerBatteries(anyList())).thenReturn(Mono.empty());
        when(batteryService.getBatteries(any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(testResponse));
    }

    @Test
    public void testRegisterBatteries() {
        webTestClient.post()
                .uri("/batteries")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testBatteries)
                .exchange()
                .expectStatus().isOk();

        verify(batteryService, times(1)).registerBatteries(testBatteries);
    }

    @Test
    public void testQueryBatteries() {
        String from = "10000";
        String to = "20000";
        int min = 50;
        int max = 300;
        int page = 0;
        int size = 50;

        when(batteryService.getBatteries(eq(from), eq(to), eq(Optional.of(min)), eq(Optional.of(max)), eq(Optional.of(pageNumber)), eq(Optional.of(pageSize))))
                .thenReturn(Mono.just(testResponse));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/batteries")
                        .queryParam("postcodeFrom", from)
                        .queryParam("postcodeTo", to)
                        .queryParam("minCapacity", min)
                        .queryParam("maxCapacity", max)
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(BatteryQueryResponse.class)
                .isEqualTo(testResponse);

        verify(batteryService, times(1))
                .getBatteries(from, to, Optional.of(min), Optional.of(max), Optional.of(pageNumber), Optional.of(pageSize));
    }

    @Test
    public void testQueryBatteriesWithoutOptionalParams() {
        String from = "10000";
        String to = "20000";

        when(batteryService.getBatteries(eq(from), eq(to),
                eq(Optional.empty()), eq(Optional.empty()),
                eq(Optional.of(pageNumber)), eq(Optional.of(pageSize))))
                .thenReturn(Mono.just(testResponse));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/batteries")
                        .queryParam("postcodeFrom", from)
                        .queryParam("postcodeTo", to)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(BatteryQueryResponse.class)
                .isEqualTo(testResponse);

        verify(batteryService, times(1))
                .getBatteries(from, to, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Test
    public void testQueryBatteriesWithOnlyMinCapacity() {
        String from = "10000";
        String to = "20000";
        int min = 50;

        when(batteryService.getBatteries(eq(from), eq(to), eq(Optional.of(min)), eq(Optional.empty()),
                eq(Optional.of(pageNumber)), eq(Optional.of(pageSize))))
                .thenReturn(Mono.just(testResponse));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/batteries")
                        .queryParam("postcodeFrom", from)
                        .queryParam("postcodeTo", to)
                        .queryParam("minCapacity", min)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(BatteryQueryResponse.class)
                .isEqualTo(testResponse);

        verify(batteryService, times(1))
                .getBatteries(from, to, Optional.of(min), Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Test
    public void testQueryBatteriesWithOnlyMaxCapacity() {
        String from = "10000";
        String to = "20000";
        int max = 300;

        when(batteryService.getBatteries(eq(from), eq(to), eq(Optional.empty()), eq(Optional.of(max)),
                eq(Optional.empty()), eq(Optional.empty())))
                .thenReturn(Mono.just(testResponse));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/batteries")
                        .queryParam("postcodeFrom", from)
                        .queryParam("postcodeTo", to)
                        .queryParam("maxCapacity", max)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(BatteryQueryResponse.class)
                .isEqualTo(testResponse);

        verify(batteryService, times(1))
                .getBatteries(from, to, Optional.empty(), Optional.of(max), Optional.empty(), Optional.empty());
    }
}
