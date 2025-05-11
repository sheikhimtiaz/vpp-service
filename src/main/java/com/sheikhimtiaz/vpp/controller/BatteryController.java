package com.sheikhimtiaz.vpp.controller;

import com.sheikhimtiaz.vpp.model.BatteryDto;
import com.sheikhimtiaz.vpp.model.BatteryQueryResponse;
import com.sheikhimtiaz.vpp.service.BatteryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/batteries")
@Slf4j
public class BatteryController {

    private final BatteryService batteryService;

    public BatteryController(BatteryService batteryService) {
        this.batteryService = batteryService;
    }

    @PostMapping
    public Mono<ResponseEntity<String>> register(@RequestBody List<BatteryDto> batteries) {
        log.info("Received register request with {} batteries", batteries.size());
        return batteryService.registerBatteries(batteries)
                .map(successMessage -> {
                    log.info("Successfully registered {} batteries", batteries.size());
                    return ResponseEntity.ok(successMessage);
                })
                .onErrorResume(error -> {
                    log.error("Error registering batteries: {}", error.getMessage(), error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Failed to register batteries: " + error.getMessage()));
                });
    }

    @GetMapping
    public Mono<BatteryQueryResponse> query(
            @RequestParam String postcodeFrom,
            @RequestParam String postcodeTo,
            @RequestParam(required = false) Optional<Integer> minCapacity,
            @RequestParam(required = false) Optional<Integer> maxCapacity,
            @RequestParam(required = false) Optional<Integer> page,
            @RequestParam(required = false) Optional<Integer> size) {
        log.info("Received query request: postcodeRange=[{} to {}], capacityRange=[{} to {}], page={}, size={}",
                postcodeFrom, postcodeTo,
                minCapacity.orElse(null), maxCapacity.orElse(null),
                page.orElse(0), size.orElse(50));

        return batteryService.getBatteries(postcodeFrom, postcodeTo, minCapacity, maxCapacity, page, size)
                .doOnSuccess(response -> log.info("Query returned {} batteries, total count: {}",
                        response.getBatteryNames().size(), response.getTotalBatteries()))
                .doOnError(error -> log.error("Error querying batteries: {}", error.getMessage(), error));
    }
}
