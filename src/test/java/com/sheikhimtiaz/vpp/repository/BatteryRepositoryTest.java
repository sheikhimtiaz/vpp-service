package com.sheikhimtiaz.vpp.repository;

import com.sheikhimtiaz.vpp.entity.Battery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

@DataR2dbcTest
@ActiveProfiles("test")
public class BatteryRepositoryTest {

    @Autowired
    private BatteryRepository batteryRepository;

    private List<Battery> testBatteries;
    private int pageNumber;
    private int pageSize;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageNumber = 0;
        pageSize = 50;
        pageable = PageRequest.of(pageNumber, pageSize, Sort.by("name"));

        batteryRepository.deleteAll().block();

        testBatteries = Arrays.asList(
                new Battery("Battery1", "2000", 100),
                new Battery("Battery2", "2500", 150),
                new Battery("Battery3", "3000", 200),
                new Battery("Battery4", "3500", 250),
                new Battery("Battery5", "5000", 1000),
                new Battery("Battery6", "6000", 2000),
                new Battery("Battery7", "7000", 3000),
                new Battery("Battery8", "8000", 4000),
                new Battery("Battery9", "9000", 5000)
        );
        batteryRepository.saveAll(testBatteries).collectList().block();
    }


    @Test
    public void testFindByPostcodeBetween() {
        Flux<Battery> result = batteryRepository.findByPostcodeBetween("6000", "8000", pageable);

        StepVerifier.create(result)
                .expectNextMatches(battery -> battery.getName().equals("Battery6") && battery.getPostcode().equals("6000"))
                .expectNextMatches(battery -> battery.getName().equals("Battery7") && battery.getPostcode().equals("7000"))
                .expectNextMatches(battery -> battery.getName().equals("Battery8") && battery.getPostcode().equals("8000"))
                .verifyComplete();
    }

    @Test
    public void testFindByPostcodeBetween_NoResults() {
        Flux<Battery> result = batteryRepository.findByPostcodeBetween("9001", "9900", pageable);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    public void testFindByPostcodeBetweenAndCapacityBetween() {
        Flux<Battery> result = batteryRepository.findByPostcodeBetweenAndCapacityBetween("6000", "9000", 2500, 4500, pageable);

        StepVerifier.create(result)
                .expectNextMatches(battery -> battery.getName().equals("Battery7") && battery.getCapacity() == 3000)
                .expectNextMatches(battery -> battery.getName().equals("Battery8") && battery.getCapacity() == 4000)
                .verifyComplete();
    }

    @Test
    public void testFindByPostcodeBetweenAndCapacityBetween_NoResults() {
        Flux<Battery> result = batteryRepository.findByPostcodeBetweenAndCapacityBetween("5000", "7000", 3500, 4500, pageable);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    public void testFindByPostcodeBetweenAndCapacityBetween_BoundaryValues() {
        Flux<Battery> result = batteryRepository.findByPostcodeBetweenAndCapacityBetween("7000", "8000", 3000, 4000, pageable);

        StepVerifier.create(result)
                .expectNextMatches(battery -> battery.getName().equals("Battery7") && battery.getCapacity() == 3000)
                .expectNextMatches(battery -> battery.getName().equals("Battery8") && battery.getCapacity() == 4000)
                .verifyComplete();
    }

    @Test
    void findByPostcodeBetween_shouldReturnCorrectBatteries() {
        Flux<Battery> result = batteryRepository.findByPostcodeBetween("2000", "3000", pageable);

        StepVerifier.create(result)
                .expectNextMatches(battery -> battery.getName().equals("Battery1") && battery.getPostcode().equals("2000"))
                .expectNextMatches(battery -> battery.getName().equals("Battery2") && battery.getPostcode().equals("2500"))
                .expectNextMatches(battery -> battery.getName().equals("Battery3") && battery.getPostcode().equals("3000"))
                .verifyComplete();
    }

    @Test
    void findByPostcodeBetweenAndCapacityBetween_shouldReturnCorrectBatteries() {
        Flux<Battery> result = batteryRepository.findByPostcodeBetweenAndCapacityBetween("2000", "3500", 150, 250, pageable);

        StepVerifier.create(result)
                .expectNextMatches(battery -> battery.getName().equals("Battery2") && battery.getCapacity() == 150.0)
                .expectNextMatches(battery -> battery.getName().equals("Battery3") && battery.getCapacity() == 200.0)
                .expectNextMatches(battery -> battery.getName().equals("Battery4") && battery.getCapacity() == 250.0)
                .verifyComplete();
    }

    @Test
    void findByPostcodeBetweenAndCapacityBetween_withNoMatches_shouldReturnEmpty() {
        Flux<Battery> result = batteryRepository.findByPostcodeBetweenAndCapacityBetween("2000", "3500", 300, 400, pageable);

        StepVerifier.create(result)
                .verifyComplete();
    }
}