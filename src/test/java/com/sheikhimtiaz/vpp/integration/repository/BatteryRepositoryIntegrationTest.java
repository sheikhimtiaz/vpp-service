package com.sheikhimtiaz.vpp.integration.repository;

import com.sheikhimtiaz.vpp.entity.Battery;
import com.sheikhimtiaz.vpp.repository.BatteryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class BatteryRepositoryIntegrationTest {
    private int pageNumber;
    private int pageSize;
    private Pageable pageable;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14-alpine")
            .withDatabaseName("vppdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
                String.format("r2dbc:postgresql://%s:%d/%s",
                        postgres.getHost(),
                        postgres.getFirstMappedPort(),
                        postgres.getDatabaseName()));
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.liquibase.url", postgres::getJdbcUrl);
        registry.add("spring.liquibase.user", postgres::getUsername);
        registry.add("spring.liquibase.password", postgres::getPassword);
    }

    @Autowired
    private BatteryRepository batteryRepository;

    private List<Battery> testBatteries = Arrays.asList(
            new Battery("Battery A", "1000", 5000),
            new Battery("Battery B", "2000", 7500),
            new Battery("Battery C", "3000", 10000),
            new Battery("Battery D", "4000", 12500),
            new Battery("Battery E", "5000", 15000)
    );

    @BeforeEach
    public void setup() {
        batteryRepository.deleteAll()
                .thenMany(Flux.fromIterable(testBatteries))
                .flatMap(batteryRepository::save)
                .blockLast();
        pageNumber = 0;
        pageSize = 50;
        pageable = PageRequest.of(pageNumber, pageSize, Sort.by("name"));
    }

    @Test
    public void testFindByPostcodeBetween() {
        Flux<Battery> result = batteryRepository.findByPostcodeBetween("2000", "4000", pageable);

        StepVerifier.create(result.sort(Comparator.comparing(Battery::getPostcode)))
                .expectNextMatches(battery ->
                        battery.getPostcode().equals("2000") && battery.getName().equals("Battery B"))
                .expectNextMatches(battery ->
                        battery.getPostcode().equals("3000") && battery.getName().equals("Battery C"))
                .expectNextMatches(battery ->
                        battery.getPostcode().equals("4000") && battery.getName().equals("Battery D"))
                .expectComplete()
                .verify();
    }

    @Test
    public void testFindByPostcodeBetween_NoResults() {
        Flux<Battery> result = batteryRepository.findByPostcodeBetween("6000", "7000", pageable);

        StepVerifier.create(result)
                .expectComplete()
                .verify();
    }

    @Test
    public void testFindByPostcodeBetweenAndCapacityBetween() {
        Flux<Battery> result = batteryRepository.findByPostcodeBetweenAndCapacityBetween(
                "2000", "5000", 10000, 15000, pageable);

        StepVerifier.create(result.sort(Comparator.comparing(Battery::getPostcode)))
                .expectNextMatches(battery ->
                        battery.getPostcode().equals("3000") &&
                                battery.getCapacity() == 10000 &&
                                battery.getName().equals("Battery C"))
                .expectNextMatches(battery ->
                        battery.getPostcode().equals("4000") &&
                                battery.getCapacity() == 12500 &&
                                battery.getName().equals("Battery D"))
                .expectNextMatches(battery ->
                        battery.getPostcode().equals("5000") &&
                                battery.getCapacity() == 15000 &&
                                battery.getName().equals("Battery E"))
                .expectComplete()
                .verify();
    }

    @Test
    public void testFindByPostcodeBetweenAndCapacityBetween_PartialMatch() {
        Flux<Battery> result = batteryRepository.findByPostcodeBetweenAndCapacityBetween(
                "1000", "3000", 6000, 8000, pageable);

        StepVerifier.create(result)
                .expectNextMatches(battery ->
                        battery.getPostcode().equals("2000") &&
                                battery.getCapacity() == 7500 &&
                                battery.getName().equals("Battery B"))
                .expectComplete()
                .verify();
    }
}