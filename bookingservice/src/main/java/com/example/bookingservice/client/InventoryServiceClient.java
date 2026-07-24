package com.example.bookingservice.client;

import com.example.bookingservice.response.InventoryResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Service
@Slf4j
public class InventoryServiceClient {

    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackGetInventory")
    public InventoryResponse getInventory(Long eventId) {
        final RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(inventoryServiceUrl + "/event/" + eventId, InventoryResponse.class);
    }

    public InventoryResponse fallbackGetInventory(Long eventId, Throwable throwable) {
        log.warn("Circuit Breaker OPEN: Inventory Service unavailable for eventId {}. Reason: {}", eventId, throwable.getMessage());
        return InventoryResponse.builder()
                .eventId(eventId)
                .event("Inventory Service maintenance mode (Fallback)")
                .leftCapacity(0L)
                .ticketPrice(BigDecimal.ZERO)
                .build();
    }
}
