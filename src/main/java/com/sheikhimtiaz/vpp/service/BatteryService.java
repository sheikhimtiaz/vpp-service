package com.sheikhimtiaz.vpp.service;

import com.sheikhimtiaz.vpp.entity.Battery;
import com.sheikhimtiaz.vpp.exception.KafkaPublishException;
import com.sheikhimtiaz.vpp.model.BatteryDto;
import com.sheikhimtiaz.vpp.model.BatteryQueryResponse;
import com.sheikhimtiaz.vpp.repository.BatteryRepository;
import com.sheikhimtiaz.vpp.util.ReactiveContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.sheikhimtiaz.vpp.constant.AppConstants.CACHE_KEY_BATTERY_QUERY_FORMAT;

@Slf4j
@Service
public class BatteryService {

    private final KafkaTemplate<String, BatteryDto> kafkaTemplate;
    private final BatteryRepository batteryRepository;
    private final ReactiveRedisTemplate<String, BatteryQueryResponse> redisTemplate;

    public BatteryService(KafkaTemplate<String, BatteryDto> kafkaTemplate,
                          BatteryRepository batteryRepository,
                          ReactiveRedisTemplate<String, BatteryQueryResponse> redisTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.batteryRepository = batteryRepository;
        this.redisTemplate = redisTemplate;
    }

    public Mono<String> registerBatteries(List<BatteryDto> batteries) {
        log.info("Processing registration of {} batteries", batteries.size());
        return Flux.fromIterable(batteries)
                .doOnNext(batteryDto -> {
                    log.debug("Sending battery to Kafka: {}", batteryDto);
                    kafkaTemplate.send("battery-topic", batteryDto);
                    log.debug("Battery sent to Kafka: {}", batteryDto.getName());
//                    try {
//                    } catch (Exception ex) {
//                        log.error("Failed to send battery to Kafka: {}", batteryDto.getName(), ex);
//                    }
                })
                .doOnComplete(() -> log.info("All {} batteries sent to Kafka", batteries.size()))
                .doOnError(err -> Mono.just("Failed to register batteries."))
                .then(Mono.just("Successfully registered " + batteries.size() + " batteries"));
    }

    public Mono<BatteryQueryResponse> getBatteries(String from, String to,
                                                   Optional<Integer> min, Optional<Integer> max,
                                                   Optional<Integer> pageNUmber, Optional<Integer> size) {
        int pageNum = pageNUmber.orElse(0);
        int pageSize = size.orElse(50);
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by("name"));

        log.info("Querying batteries: postcodeRange=[{} to {}], capacityRange=[{} to {}], page={}, size={}",
                from, to, min.orElse(null), max.orElse(null), pageNum, pageSize);

        String cacheKey = generateCacheKey(from, to, min, max, pageNum, pageSize);

        Map<String, String> mdcContext = MDC.getCopyOfContextMap();

        return redisTemplate.opsForValue().get(cacheKey)
                .doOnSubscribe(s -> log.debug("Checking cache for key: {}", cacheKey))
                .doOnNext(cached -> log.debug("Cache hit for key: {}", cacheKey))
                .onErrorResume(e -> {
                    log.error("Error retrieving from cache: {}", e.getMessage(), e);
                    return Mono.empty();
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("Cache miss for key: {}", cacheKey);
                    Mono<BatteryQueryResponse> result = queryDatabaseAndCache(from, to, min, max, pageNum, pageSize, pageable, cacheKey);

                    return mdcContext != null ? ReactiveContextUtils.withMdc(result, mdcContext) : result;
                }));
    }

    public Mono<BatteryQueryResponse> queryDatabaseAndCache(String from, String to,
                                                            Optional<Integer> min, Optional<Integer> max,
                                                            int page, int size, Pageable pageable, String cacheKey) {
        log.debug("Querying database for batteries");

        Flux<Battery> batteriesFlux;
        Mono<Long> countMono;
        Mono<Double> sumMono;
        Mono<Double> avgMono;

        if (min.isPresent() && max.isPresent()) {
            log.debug("Using capacity range filter: min={}, max={}", min.get(), max.get());
            batteriesFlux = batteryRepository.findByPostcodeBetweenAndCapacityBetween(from, to, min.get(), max.get(), pageable);
            countMono = batteryRepository.countByPostcodeBetweenAndCapacityBetween(from, to, min.get(), max.get());
            sumMono = batteryRepository.sumCapacityByPostcodeBetweenAndCapacityBetween(from, to, min.get(), max.get())
                    .switchIfEmpty(Mono.just(0.0));
            avgMono = batteryRepository.avgCapacityByPostcodeBetweenAndCapacityBetween(from, to, min.get(), max.get())
                    .switchIfEmpty(Mono.just(0.0));
        } else {
            log.debug("Using postcode range filter only");
            batteriesFlux = batteryRepository.findByPostcodeBetween(from, to, pageable);
            countMono = batteryRepository.countByPostcodeBetween(from, to);
            sumMono = batteryRepository.sumCapacityByPostcodeBetween(from, to).switchIfEmpty(Mono.just(0.0));;
            avgMono = batteryRepository.avgCapacityByPostcodeBetween(from, to).switchIfEmpty(Mono.just(0.0));;
        }

        Mono<List<String>> namesMono = batteriesFlux
                .map(Battery::getName)
                .collectList();

        return Mono.zip(namesMono, sumMono, avgMono, countMono)
                .map(tuple -> {
                    List<String> names = tuple.getT1();
                    Double totalCapacity = tuple.getT2() != null ? tuple.getT2() : 0.0;
                    Double avgCapacity = tuple.getT3() != null ? tuple.getT3() : 0.0;
                    long totalCount = tuple.getT4();

                    log.debug("Query results: names.size={}, totalCapacity={}, avgCapacity={}, totalCount={}",
                            names.size(), totalCapacity, avgCapacity, totalCount);

                    return new BatteryQueryResponse(
                            names,
                            totalCapacity,
                            avgCapacity,
                            totalCount,
                            page,
                            size
                    );
                })
                .flatMap(result -> {
                    log.debug("Caching results with key: {}, expires in 10 minutes", cacheKey);
                    return redisTemplate.opsForValue().set(cacheKey, result, Duration.ofMinutes(10))
                            .doOnSuccess(success -> log.debug("Successfully cached results"))
                            .doOnError(error -> log.error("Failed to cache results: {}", error.getMessage(), error))
                            .thenReturn(result);
                });
    }

    private String generateCacheKey(String from, String to, Optional<Integer> min, Optional<Integer> max, int page, int size) {
        String key = String.format(CACHE_KEY_BATTERY_QUERY_FORMAT,
                from, to, min.orElse(null), max.orElse(null), page, size);
        log.trace("Generated cache key: {}", key);
        return key;
    }
}
