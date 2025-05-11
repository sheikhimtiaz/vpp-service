package com.sheikhimtiaz.vpp.service;

import com.sheikhimtiaz.vpp.entity.Battery;
import com.sheikhimtiaz.vpp.model.BatteryDto;
import com.sheikhimtiaz.vpp.model.BatteryQueryResponse;
import com.sheikhimtiaz.vpp.repository.BatteryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.ArgumentMatchers.*;
        import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class BatteryConsumerServiceTest {

    @Mock
    private BatteryRepository batteryRepository;

    @Mock
    private ReactiveRedisTemplate<String, BatteryQueryResponse> redisTemplate;

    @InjectMocks
    private BatteryConsumerService batteryConsumerService;

    @Captor
    private ArgumentCaptor<Battery> batteryCaptor;

    private BatteryDto testBatteryDto;
    private Battery savedBattery;

    @BeforeEach
    void setUp() {
        testBatteryDto = new BatteryDto("Test Battery", "2500", 6000);

        savedBattery = new Battery();
        savedBattery.setId(UUID.randomUUID().toString());
        savedBattery.setName("Test Battery");
        savedBattery.setPostcode("2500");
        savedBattery.setCapacity(6000);
    }

    @Test
    void consume_shouldSaveBattery() {
        when(batteryRepository.save(any(Battery.class))).thenReturn(Mono.just(savedBattery));

        when(redisTemplate.keys(anyString())).thenReturn(Flux.empty());

        batteryConsumerService.consume(testBatteryDto);

        verify(batteryRepository).save(batteryCaptor.capture());
        Battery capturedBattery = batteryCaptor.getValue();

        assertNull(capturedBattery.getId());
        assertEquals(testBatteryDto.getName(), capturedBattery.getName());
        assertEquals(testBatteryDto.getPostcode(), capturedBattery.getPostcode());
        assertEquals(testBatteryDto.getCapacity(), capturedBattery.getCapacity());
    }

    @Test
    void consume_shouldHandleExceptions() {
        when(batteryRepository.save(any(Battery.class))).thenReturn(Mono.error(new RuntimeException("Database error")));

        assertDoesNotThrow(() -> batteryConsumerService.consume(testBatteryDto));

        verify(batteryRepository).save(any(Battery.class));
    }

    @Test
    void invalidateAffectedCacheEntries_shouldDeleteMatchingKeys() {
        String matchingKey = "battery-stats::from=2000:to=3000:min=5000:max=7000";
        String nonMatchingKey = "battery-stats::from=4000:to=5000:min=8000:max=9000";

        when(redisTemplate.keys("battery-stats::*"))
                .thenReturn(Flux.just(matchingKey, nonMatchingKey));

        when(batteryRepository.save(any(Battery.class))).thenReturn(Mono.just(savedBattery));

        batteryConsumerService.consume(testBatteryDto);

        verify(redisTemplate).keys("battery-stats::*");
        verify(redisTemplate).delete(matchingKey);
        verify(redisTemplate, never()).delete(nonMatchingKey);
    }

    @Test
    void isCacheKeyAffectedByBattery_shouldIdentifyMatchingKeys() {
        // Postcode and capacity both in range
        assertTrue(batteryConsumerService.isCacheKeyAffectedByBattery(
                "battery-stats::from=2000:to=3000:min=5000:max=7000", "2500", 6000));

        // Postcode in range but capacity out of range
        assertFalse(batteryConsumerService.isCacheKeyAffectedByBattery(
                "battery-stats::from=2000:to=3000:min=7000:max=8000", "2500", 6000));

        // Postcode out of range but capacity in range
        assertFalse(batteryConsumerService.isCacheKeyAffectedByBattery(
                "battery-stats::from=3000:to=4000:min=5000:max=7000", "2500", 6000));

        // No capacity filter (null values)
        assertTrue(batteryConsumerService.isCacheKeyAffectedByBattery(
                "battery-stats::from=2000:to=3000:min=null:max=null", "2500", 6000));

        // Invalid key format returns true just to be safe
        assertTrue(batteryConsumerService.isCacheKeyAffectedByBattery(
                "invalid-key", "2500", 6000));
    }
}