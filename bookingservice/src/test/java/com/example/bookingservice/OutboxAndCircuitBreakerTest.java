package com.example.bookingservice;

import com.example.bookingservice.client.InventoryServiceClient;
import com.example.bookingservice.entity.Customer;
import com.example.bookingservice.entity.OutboxEvent;
import com.example.bookingservice.repository.CustomerRepository;
import com.example.bookingservice.repository.OutboxRepository;
import com.example.bookingservice.request.BookingRequest;
import com.example.bookingservice.response.BookingResponse;
import com.example.bookingservice.response.InventoryResponse;
import com.example.bookingservice.service.BookingService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:bookingservice_test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cloud.compatibility-verifier.enabled=false"
})
class OutboxAndCircuitBreakerTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @MockitoBean
    private InventoryServiceClient inventoryServiceClient;

    @MockitoBean
    private org.springframework.kafka.core.KafkaTemplate<String, com.example.bookingservice.event.BookingEvent> bookingEventKafkaTemplate;

    @Test
    @DisplayName("Phase 2 Test: Transactional Outbox Pattern + Scheduled Worker")
    void testTransactionalOutboxPattern() {
        // 1. Setup Customer
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("Nguyen Van A");
        customer.setEmail("a@gmail.com");
        customerRepository.save(customer);

        // 2. Mock Inventory Service Response
        Mockito.when(inventoryServiceClient.getInventory(101L))
                .thenReturn(InventoryResponse.builder()
                        .eventId(101L)
                        .event("Live Concert")
                        .leftCapacity(50L)
                        .ticketPrice(new BigDecimal("200000"))
                        .build());

        // 3. Create Booking
        BookingRequest request = new BookingRequest();
        request.setUserId(1L);
        request.setEventId(101L);
        request.setTicketCount(2L);

        BookingResponse response = bookingService.createBooking(request);

        // 4. Verify OutboxEvent saved in PENDING state inside Transaction
        List<OutboxEvent> pendingEvents = outboxRepository.findByStatus("PENDING");
        System.out.println("\n---> LOG: Số bản ghi Outbox PENDING vừa lưu trong DB: " + pendingEvents.size());
        Assertions.assertEquals(1, pendingEvents.size());

        // 5. Trigger Scheduled Outbox Worker
        bookingService.processOutboxEvents();

        List<OutboxEvent> remainingPending = outboxRepository.findByStatus("PENDING");
        List<OutboxEvent> processedEvents = outboxRepository.findByStatus("PROCESSED");

        System.out.println("==========================================");
        System.out.println("RESULT: Số bản ghi Outbox PENDING còn lại: " + remainingPending.size());
        System.out.println("RESULT: Số bản ghi Outbox PROCESSED đã chuyển trạng thái: " + processedEvents.size());
        System.out.println("==========================================\n");

        Assertions.assertEquals(0, remainingPending.size());
        Assertions.assertEquals(1, processedEvents.size());
    }

    @Test
    @DisplayName("Phase 2 Test: Circuit Breaker Fallback khi Inventory Service gặp sự cố")
    void testInventoryCircuitBreakerFallback() {
        InventoryServiceClient client = new InventoryServiceClient();
        InventoryResponse fallbackResponse = client.fallbackGetInventory(999L, new RuntimeException("Service Unavailable"));

        System.out.println("\n---> LOG: Kích hoạt Circuit Breaker Fallback cho eventId 999L");
        System.out.println("LOG: Thông báo Fallback: " + fallbackResponse.getEvent());

        Assertions.assertEquals("Inventory Service maintenance mode (Fallback)", fallbackResponse.getEvent());
        Assertions.assertEquals(0L, fallbackResponse.getLeftCapacity());
    }
}
