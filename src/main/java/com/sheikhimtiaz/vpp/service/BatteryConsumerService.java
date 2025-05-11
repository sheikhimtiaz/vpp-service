package com.sheikhimtiaz.vpp.service;

import com.sheikhimtiaz.vpp.entity.Battery;
import com.sheikhimtiaz.vpp.model.BatteryDto;
import com.sheikhimtiaz.vpp.model.BatteryQueryResponse;
import com.sheikhimtiaz.vpp.repository.BatteryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class BatteryConsumerService {

    private final BatteryRepository batteryRepository;
    private final ReactiveRedisTemplate<String, BatteryQueryResponse> redisTemplate;

    public BatteryConsumerService(BatteryRepository batteryRepository,
                                  ReactiveRedisTemplate<String, BatteryQueryResponse> redisTemplate){
        this.batteryRepository = batteryRepository;
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(topics = "battery-topic", groupId = "battery-group")
    public void consume(BatteryDto event) {
        try {
            log.info("Received battery event: {}", event);
            Battery battery = new Battery();
            battery.setName(event.getName());
            battery.setPostcode(event.getPostcode());
            battery.setCapacity(event.getCapacity());
            batteryRepository.save(battery)
                    .doOnSuccess(savedBattery -> {
                        log.info("Battery saved successfully: {}", savedBattery.getId());
                        invalidateAffectedCacheEntries(savedBattery.getPostcode(), savedBattery.getCapacity());
                    })
                    .doOnError(error -> log.error("Error saving battery: {}", error.getMessage(), error))
                    .subscribe();
        } catch (Exception e) {
            log.error("Error processing battery event: {}", event, e);
            throw new RuntimeException(e);
        }
    }

    private void invalidateAffectedCacheEntries(String postcode, double capacity) {
        redisTemplate.keys("battery-stats::*")
                .flatMap(key -> {
                    if (isCacheKeyAffectedByBattery(key, postcode, capacity)) {
                        log.debug("Invalidating cache key: {}", key);
                        return redisTemplate.delete(key);
                    }
                    return Mono.just(false);
                })
                .subscribe(
                        deleted -> {
                            if (deleted.equals(true)) {
                                log.debug("Cache entry invalidated");
                            }
                        },
                        error -> log.error("Error invalidating cache: {}", error.getMessage(), error)
                );
    }

    public boolean isCacheKeyAffectedByBattery(String key, String postcode, double capacity) {
        try {
            String[] parts = key.split("::");
            if (parts.length != 2) return true;

            String[] params = parts[1].split(":");
            String from = null;
            String to = null;
            Integer min = null;
            Integer max = null;

            for (String param : params) {
                String[] kv = param.split("=");
                if (kv.length != 2) continue;

                switch (kv[0]) {
                    case "from":
                        from = kv[1];
                        break;
                    case "to":
                        to = kv[1];
                        break;
                    case "min":
                        min = "null".equals(kv[1]) ? null : Integer.parseInt(kv[1]);
                        break;
                    case "max":
                        max = "null".equals(kv[1]) ? null : Integer.parseInt(kv[1]);
                        break;
                }
            }

            boolean postcodeInRange = from != null && to != null &&
                    postcode.compareTo(from) >= 0 && postcode.compareTo(to) <= 0;
            boolean capacityInRange = (min == null || capacity >= min) && (max == null || capacity <= max);

            return postcodeInRange && capacityInRange;
        } catch (Exception e) {
            log.error("Error parsing cache key: {}", e.getMessage());
            return true;
        }
    }
}
