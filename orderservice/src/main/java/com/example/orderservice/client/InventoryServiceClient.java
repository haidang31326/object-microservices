package com.example.orderservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class InventoryServiceClient {

    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackUpdateEventCapacity")
    public ResponseEntity<Void> updateEventCapacity(Long eventId, Long ticketCount) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.put(inventoryServiceUrl + "/event/" + eventId + "/capacity/" + ticketCount, null);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<Void> fallbackUpdateEventCapacity(Long eventId, Long ticketCount, Throwable throwable) {
        log.warn("Circuit Breaker OPEN: Failed to update inventory capacity for eventId {}. Reason: {}", eventId, throwable.getMessage());
        return ResponseEntity.status(503).build();
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackRestoreEventCapacity")
    public ResponseEntity<Void> restoreEventCapacity(Long eventId, Long ticketCount) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.put(inventoryServiceUrl + "/event/" + eventId + "/restore/" + ticketCount, null);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<Void> fallbackRestoreEventCapacity(Long eventId, Long ticketCount, Throwable throwable) {
        log.warn("Circuit Breaker OPEN: Failed to restore inventory capacity for eventId {}. Reason: {}", eventId, throwable.getMessage());
        return ResponseEntity.status(503).build();
    }
}
