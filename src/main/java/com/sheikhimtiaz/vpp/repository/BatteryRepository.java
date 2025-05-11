package com.sheikhimtiaz.vpp.repository;

import com.sheikhimtiaz.vpp.entity.Battery;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.data.domain.Pageable;

@Repository
public interface BatteryRepository extends ReactiveCrudRepository<Battery, Long> {
    Flux<Battery> findByPostcodeBetween(String from, String to, Pageable pageable);
    Flux<Battery> findByPostcodeBetweenAndCapacityBetween(String from, String to, int min, int max, Pageable pageable);

    Mono<Long> countByPostcodeBetween(String from, String to);
    Mono<Long> countByPostcodeBetweenAndCapacityBetween(String from, String to, int min, int max);

    @Query("SELECT SUM(b.capacity) FROM Battery b WHERE b.postcode BETWEEN :from AND :to")
    Mono<Double> sumCapacityByPostcodeBetween(String from, String to);

    @Query("SELECT SUM(b.capacity) FROM Battery b WHERE b.postcode BETWEEN :from AND :to AND b.capacity BETWEEN :min AND :max")
    Mono<Double> sumCapacityByPostcodeBetweenAndCapacityBetween(String from, String to, int min, int max);

    @Query(value = "SELECT AVG(capacity) FROM battery WHERE postcode BETWEEN :from AND :to")
    Mono<Double> avgCapacityByPostcodeBetween(String from, String to);

    @Query(value = "SELECT AVG(capacity) FROM battery WHERE postcode BETWEEN :from AND :to AND capacity BETWEEN :min AND :max")
    Mono<Double> avgCapacityByPostcodeBetweenAndCapacityBetween(String from, String to, int min, int max);
}
