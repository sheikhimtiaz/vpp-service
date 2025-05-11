package com.sheikhimtiaz.vpp.service;

import com.sheikhimtiaz.vpp.entity.Battery;
import com.sheikhimtiaz.vpp.model.BatteryDto;
import com.sheikhimtiaz.vpp.model.BatteryQueryResponse;
import com.sheikhimtiaz.vpp.repository.BatteryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class BatteryServiceTest {

    @Mock
    private KafkaTemplate<String, BatteryDto> kafkaTemplate;

    @Mock
    private BatteryRepository batteryRepository;

    @Mock
    private ReactiveRedisTemplate<String, BatteryQueryResponse> redisTemplate;

    @Mock
    private ReactiveValueOperations<String, BatteryQueryResponse> valueOperations;

    @InjectMocks
    private BatteryService batteryService;

    private List<BatteryDto> testBatteryDtos;
    private List<Battery> testBatteries;
    private BatteryQueryResponse expectedResponse;
    private String cacheKey;
    private int pageNumber;
    private int pageSize;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageNumber = 0;
        pageSize = 50;
        pageable = PageRequest.of(pageNumber, pageSize, Sort.by("name"));

        testBatteryDtos = Arrays.asList(
                new BatteryDto("Battery A", "2000", 5000),
                new BatteryDto("Battery B", "3000", 7000)
        );

        testBatteries = Arrays.asList(
                new Battery(UUID.randomUUID().toString(), "Battery A", "2000", 5000),
                new Battery(UUID.randomUUID().toString(), "Battery B", "3000", 7000)
        );

        List<String> names = Arrays.asList("Battery A", "Battery B");
        expectedResponse = new BatteryQueryResponse(names, 12000, 6000, 2, 0, 50);

        cacheKey = "battery-stats::from=1000:to=4000:min=null:max=null:page=0:size=50";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }


    @Test
    void shouldReturnFromCacheWhenExists() {
        String from = "1000";
        String to = "2000";
        Optional<Integer> min = Optional.of(10);
        Optional<Integer> max = Optional.of(100);
        String cacheKey = "battery-stats::from=1000:to=2000:min=10:max=100:page=0:size=50";
        BatteryQueryResponse cachedResponse = new BatteryQueryResponse(List.of("A", "B"), 150.0, 75.0, 2, 0, 50);

        when(valueOperations.get(cacheKey)).thenReturn(Mono.just(cachedResponse));

        Mono<BatteryQueryResponse> result = batteryService.getBatteries(from, to, min, max, Optional.of(pageNumber), Optional.of(pageSize));

        StepVerifier.create(result)
                .expectNextMatches(resp -> resp.getTotalWattCapacity() == 150.0 && resp.getAverageWattCapacity() == 75.0)
                .verifyComplete();

        verify(valueOperations).get(cacheKey);
        verifyNoInteractions(batteryRepository);
    }

    @Test
    void shouldQueryDatabaseAndCacheWhenNotInCache() {
        String from = "3000";
        String to = "4000";
        Optional<Integer> min = Optional.empty();
        Optional<Integer> max = Optional.empty();
        String cacheKey = "battery-stats::from=3000:to=4000:min=null:max=null:page=0:size=50";

        Battery battery1 = new Battery("X", "3100", 50);
        Battery battery2 = new Battery("Y", "3200", 70);
        List<Battery> batteryList = List.of(battery1, battery2);

        when(valueOperations.get(cacheKey)).thenReturn(Mono.empty());

        when(batteryRepository.findByPostcodeBetween(from, to, pageable))
                .thenReturn(Flux.fromIterable(batteryList));

        when(batteryRepository.countByPostcodeBetween(from, to))
                .thenReturn(Mono.just(2L));
        when(batteryRepository.sumCapacityByPostcodeBetween(from, to))
                .thenReturn(Mono.just(120.0));
        when(batteryRepository.avgCapacityByPostcodeBetween(from, to))
                .thenReturn(Mono.just(60.0));

        when(valueOperations.set(eq(cacheKey), any(), eq(Duration.ofMinutes(10))))
                .thenReturn(Mono.just(true));

        Mono<BatteryQueryResponse> result = batteryService.getBatteries(from, to, min, max,
                Optional.of(pageNumber), Optional.of(pageSize));

        StepVerifier.create(result)
                .expectNextMatches(resp ->
                        resp.getBatteryNames().equals(List.of("X", "Y")) &&
                                resp.getTotalWattCapacity() == 120.0 &&
                                resp.getAverageWattCapacity() == 60.0 &&
                                resp.getTotalBatteries() == 2 &&
                                resp.getPage() == 0 &&
                                resp.getSize() == 50
                )
                .verifyComplete();

        verify(batteryRepository).findByPostcodeBetween(from, to, pageable);
        verify(batteryRepository).countByPostcodeBetween(from, to);
        verify(batteryRepository).sumCapacityByPostcodeBetween(from, to);
        verify(batteryRepository).avgCapacityByPostcodeBetween(from, to);
        verify(valueOperations).set(eq(cacheKey), any(), eq(Duration.ofMinutes(10)));
    }

    @Test
    void shouldQueryDatabaseAndCacheWithMinMaxWhenNotInCache() {
        String from = "3000";
        String to = "4000";
        Optional<Integer> min = Optional.of(40);
        Optional<Integer> max = Optional.of(80);
        String cacheKey = "battery-stats::from=3000:to=4000:min=40:max=80:page=0:size=50";

        Battery battery1 = new Battery("X", "3100", 50);
        Battery battery2 = new Battery("Y", "3200", 70);
        List<Battery> batteryList = List.of(battery1, battery2);

        when(valueOperations.get(cacheKey)).thenReturn(Mono.empty());

        when(batteryRepository.findByPostcodeBetweenAndCapacityBetween(from, to, min.get(), max.get(), pageable))
                .thenReturn(Flux.fromIterable(batteryList));

        when(batteryRepository.countByPostcodeBetweenAndCapacityBetween(from, to, min.get(), max.get()))
                .thenReturn(Mono.just(2L));
        when(batteryRepository.sumCapacityByPostcodeBetweenAndCapacityBetween(from, to, min.get(), max.get()))
                .thenReturn(Mono.just(120.0));
        when(batteryRepository.avgCapacityByPostcodeBetweenAndCapacityBetween(from, to, min.get(), max.get()))
                .thenReturn(Mono.just(60.0));

        when(valueOperations.set(eq(cacheKey), any(), eq(Duration.ofMinutes(10))))
                .thenReturn(Mono.just(true));

        Mono<BatteryQueryResponse> result = batteryService.getBatteries(from, to, min, max,
                Optional.of(pageNumber), Optional.of(pageSize));

        StepVerifier.create(result)
                .expectNextMatches(resp ->
                        resp.getBatteryNames().equals(List.of("X", "Y")) &&
                                resp.getTotalWattCapacity() == 120.0 &&
                                resp.getAverageWattCapacity() == 60.0 &&
                                resp.getTotalBatteries() == 2 &&
                                resp.getPage() == 0 &&
                                resp.getSize() == 50
                )
                .verifyComplete();

        verify(batteryRepository).findByPostcodeBetweenAndCapacityBetween(from, to, min.get(), max.get(), pageable);
        verify(batteryRepository).countByPostcodeBetweenAndCapacityBetween(from, to, min.get(), max.get());
        verify(batteryRepository).sumCapacityByPostcodeBetweenAndCapacityBetween(from, to, min.get(), max.get());
        verify(batteryRepository).avgCapacityByPostcodeBetweenAndCapacityBetween(from, to, min.get(), max.get());
        verify(valueOperations).set(eq(cacheKey), any(), eq(Duration.ofMinutes(10)));
    }

    @Test
    void shouldReturnFromCacheWhenAvailable() {
        String from = "3000";
        String to = "4000";
        Optional<Integer> min = Optional.empty();
        Optional<Integer> max = Optional.empty();
        String cacheKey = "battery-stats::from=3000:to=4000:min=null:max=null:page=0:size=50";

        BatteryQueryResponse cachedResponse = new BatteryQueryResponse(
                List.of("X", "Y"), 120.0, 60.0, 2, 0, 50
        );

        when(valueOperations.get(cacheKey)).thenReturn(Mono.just(cachedResponse));

        Mono<BatteryQueryResponse> result = batteryService.getBatteries(from, to, min, max,
                Optional.of(pageNumber), Optional.of(pageSize));

        StepVerifier.create(result)
                .expectNext(cachedResponse)
                .verifyComplete();

        verify(batteryRepository, never()).findByPostcodeBetween(anyString(), anyString(), any(Pageable.class));
        verify(valueOperations).get(cacheKey);
    }

    @Test
    void getBatteries_cacheHit_shouldReturnFromCache() {
        when(valueOperations.get(anyString())).thenReturn(Mono.just(expectedResponse));

        StepVerifier.create(batteryService.getBatteries("1000", "4000", Optional.empty(), Optional.empty(), Optional.of(pageNumber), Optional.of(pageSize)))
                .expectNext(expectedResponse)
                .verifyComplete();

        verify(valueOperations).get(anyString());
        verify(batteryRepository, never()).findByPostcodeBetween(anyString(), anyString(), any(Pageable.class));
    }

    @Test
    void getBatteries_cacheMiss_shouldQueryDatabaseAndCache() {
        when(valueOperations.get(anyString())).thenReturn(Mono.empty());

        when(batteryRepository.findByPostcodeBetween("1000", "4000", pageable))
                .thenReturn(Flux.fromIterable(testBatteries));

        when(batteryRepository.countByPostcodeBetween("1000", "4000"))
                .thenReturn(Mono.just(2L));
        when(batteryRepository.sumCapacityByPostcodeBetween("1000", "4000"))
                .thenReturn(Mono.just(12000.0));
        when(batteryRepository.avgCapacityByPostcodeBetween("1000", "4000"))
                .thenReturn(Mono.just(6000.0));

        when(valueOperations.set(anyString(), any(BatteryQueryResponse.class), any(Duration.class)))
                .thenReturn(Mono.just(Boolean.TRUE));

        StepVerifier.create(batteryService.getBatteries("1000", "4000", Optional.empty(), Optional.empty(), Optional.of(pageNumber), Optional.of(pageSize)))
                .expectNextMatches(response ->
                        response.getBatteryNames().size() == 2 &&
                                response.getTotalWattCapacity() == 12000 &&
                                response.getAverageWattCapacity() == 6000 &&
                                response.getTotalBatteries() == 2 &&
                                response.getPage() == pageNumber &&
                                response.getSize() == pageSize)
                .verifyComplete();

        verify(valueOperations).get(anyString());
        verify(batteryRepository).findByPostcodeBetween("1000", "4000", pageable);
        verify(batteryRepository).countByPostcodeBetween("1000", "4000");
        verify(batteryRepository).sumCapacityByPostcodeBetween("1000", "4000");
        verify(batteryRepository).avgCapacityByPostcodeBetween("1000", "4000");
        verify(valueOperations).set(anyString(), any(BatteryQueryResponse.class), any(Duration.class));
    }

    @Test
    void getBatteries_withCapacityFilter_shouldQueryWithFilter() {
        when(valueOperations.get(anyString())).thenReturn(Mono.empty());

        when(batteryRepository.findByPostcodeBetweenAndCapacityBetween("1000", "4000", 4000, 8000, pageable))
                .thenReturn(Flux.fromIterable(testBatteries));

        when(batteryRepository.countByPostcodeBetweenAndCapacityBetween("1000", "4000", 4000, 8000))
                .thenReturn(Mono.just(2L));
        when(batteryRepository.sumCapacityByPostcodeBetweenAndCapacityBetween("1000", "4000", 4000, 8000))
                .thenReturn(Mono.just(12000.0));
        when(batteryRepository.avgCapacityByPostcodeBetweenAndCapacityBetween("1000", "4000", 4000, 8000))
                .thenReturn(Mono.just(6000.0));

        when(valueOperations.set(anyString(), any(BatteryQueryResponse.class), any(Duration.class)))
                .thenReturn(Mono.just(Boolean.TRUE));

        StepVerifier.create(batteryService.getBatteries("1000", "4000", Optional.of(4000), Optional.of(8000), Optional.of(pageNumber), Optional.of(pageSize)))
                .expectNextMatches(response ->
                        response.getBatteryNames().size() == 2 &&
                                response.getTotalWattCapacity() == 12000 &&
                                response.getAverageWattCapacity() == 6000 &&
                                response.getTotalBatteries() == 2 &&
                                response.getPage() == pageNumber &&
                                response.getSize() == pageSize)
                .verifyComplete();

        verify(valueOperations).get(anyString());
        verify(batteryRepository).findByPostcodeBetweenAndCapacityBetween("1000", "4000", 4000, 8000, pageable);
        verify(batteryRepository).countByPostcodeBetweenAndCapacityBetween("1000", "4000", 4000, 8000);
        verify(batteryRepository).sumCapacityByPostcodeBetweenAndCapacityBetween("1000", "4000", 4000, 8000);
        verify(batteryRepository).avgCapacityByPostcodeBetweenAndCapacityBetween("1000", "4000", 4000, 8000);
        verify(valueOperations).set(anyString(), any(BatteryQueryResponse.class), any(Duration.class));
    }

    @Test
    void queryDatabaseAndCache_noBatteries_shouldReturnEmptyStats() {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("name"));

        when(batteryRepository.findByPostcodeBetween("5000", "6000", pageable))
                .thenReturn(Flux.empty());

        when(batteryRepository.countByPostcodeBetween("5000", "6000"))
                .thenReturn(Mono.just(0L));
        when(batteryRepository.sumCapacityByPostcodeBetween("5000", "6000"))
                .thenReturn(Mono.just(0.0));
        when(batteryRepository.avgCapacityByPostcodeBetween("5000", "6000"))
                .thenReturn(Mono.just(0.0));

        when(valueOperations.set(anyString(), any(BatteryQueryResponse.class), any(Duration.class)))
                .thenReturn(Mono.just(Boolean.TRUE));

        StepVerifier.create(batteryService.queryDatabaseAndCache("5000", "6000", Optional.empty(), Optional.empty(), pageNumber, pageSize, pageable, "test-key"))
                .expectNextMatches(response ->
                        response.getBatteryNames().isEmpty() &&
                                response.getTotalWattCapacity() == 0 &&
                                response.getAverageWattCapacity() == 0 &&
                                response.getTotalBatteries() == 0 &&
                                response.getPage() == pageNumber &&
                                response.getSize() == pageSize)
                .verifyComplete();

        verify(batteryRepository).findByPostcodeBetween("5000", "6000", pageable);
        verify(batteryRepository).countByPostcodeBetween("5000", "6000");
        verify(batteryRepository).sumCapacityByPostcodeBetween("5000", "6000");
        verify(batteryRepository).avgCapacityByPostcodeBetween("5000", "6000");
    }

    @Test
    void getBatteries_cacheError_shouldFallbackToDatabase() {
        when(valueOperations.get(anyString())).thenReturn(Mono.error(new RuntimeException("Cache error")));

        when(batteryRepository.findByPostcodeBetween("1000", "4000", pageable))
                .thenReturn(Flux.fromIterable(testBatteries));

        when(batteryRepository.countByPostcodeBetween("1000", "4000"))
                .thenReturn(Mono.just(2L));
        when(batteryRepository.sumCapacityByPostcodeBetween("1000", "4000"))
                .thenReturn(Mono.just(12000.0));
        when(batteryRepository.avgCapacityByPostcodeBetween("1000", "4000"))
                .thenReturn(Mono.just(6000.0));

        when(valueOperations.set(anyString(), any(BatteryQueryResponse.class), any(Duration.class)))
                .thenReturn(Mono.just(Boolean.TRUE));

        StepVerifier.create(batteryService.getBatteries("1000", "4000", Optional.empty(), Optional.empty(), Optional.of(pageNumber), Optional.of(pageSize)))
                .expectNextMatches(response ->
                        response.getBatteryNames().size() == 2 &&
                                response.getAverageWattCapacity() == 6000 &&
                                response.getTotalWattCapacity() == 12000 &&
                                response.getTotalBatteries() == 2 &&
                                response.getPage() == pageNumber &&
                                response.getSize() == pageSize)
                .verifyComplete();

        verify(valueOperations).get(anyString());
        verify(batteryRepository).findByPostcodeBetween("1000", "4000", pageable);
        verify(batteryRepository).countByPostcodeBetween("1000", "4000");
        verify(batteryRepository).sumCapacityByPostcodeBetween("1000", "4000");
        verify(batteryRepository).avgCapacityByPostcodeBetween("1000", "4000");
    }

    @Test
    void getBatteries_withNullStatistics_shouldHandleGracefully() {
        when(valueOperations.get(anyString())).thenReturn(Mono.empty());

        when(batteryRepository.findByPostcodeBetween("1000", "4000", pageable))
                .thenReturn(Flux.fromIterable(testBatteries));

        when(batteryRepository.countByPostcodeBetween("1000", "4000"))
                .thenReturn(Mono.just(2L));
        when(batteryRepository.sumCapacityByPostcodeBetween("1000", "4000"))
                .thenReturn(Mono.empty());
        when(batteryRepository.avgCapacityByPostcodeBetween("1000", "4000"))
                .thenReturn(Mono.empty());

        when(valueOperations.set(anyString(), any(BatteryQueryResponse.class), any(Duration.class)))
                .thenReturn(Mono.just(Boolean.TRUE));

        StepVerifier.create(batteryService.getBatteries("1000", "4000", Optional.empty(), Optional.empty(), Optional.of(pageNumber), Optional.of(pageSize)))
                .expectNextMatches(response ->
                        response.getBatteryNames().size() == 2 &&
                                response.getTotalWattCapacity() == 0.0 &&
                                response.getAverageWattCapacity() == 0.0 &&
                                response.getTotalBatteries() == 2 &&
                                response.getPage() == pageNumber &&
                                response.getSize() == pageSize)
                .verifyComplete();

        verify(valueOperations).get(anyString());
        verify(batteryRepository).findByPostcodeBetween("1000", "4000", pageable);
        verify(batteryRepository).countByPostcodeBetween("1000", "4000");
        verify(batteryRepository).sumCapacityByPostcodeBetween("1000", "4000");
        verify(batteryRepository).avgCapacityByPostcodeBetween("1000", "4000");
    }
}