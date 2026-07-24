package com.example.bookingservice.service;

import com.example.bookingservice.client.InventoryServiceClient;
import com.example.bookingservice.entity.Customer;
import com.example.bookingservice.entity.OutboxEvent;
import com.example.bookingservice.event.BookingEvent;
import com.example.bookingservice.exception.NotEnoughInventoryException;
import com.example.bookingservice.exception.UserNotFoundException;
import com.example.bookingservice.repository.CustomerRepository;
import com.example.bookingservice.repository.OutboxRepository;
import com.example.bookingservice.request.BookingRequest;
import com.example.bookingservice.response.BookingResponse;
import com.example.bookingservice.response.InventoryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class BookingService {
    private final CustomerRepository customerRepository;
    private final InventoryServiceClient inventoryServiceClient;
    private final KafkaTemplate<String, BookingEvent> bookingEventKafkaTemplate;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public BookingService(CustomerRepository customerRepository,
                          InventoryServiceClient inventoryServiceClient,
                          KafkaTemplate<String, BookingEvent> bookingEventKafkaTemplate,
                          OutboxRepository outboxRepository) {
        this.customerRepository = customerRepository;
        this.inventoryServiceClient = inventoryServiceClient;
        this.bookingEventKafkaTemplate = bookingEventKafkaTemplate;
        this.outboxRepository = outboxRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public BookingResponse createBooking(@RequestBody BookingRequest bookingRequest) {
        final Customer customer = customerRepository.findById(bookingRequest.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Customer not found"));

        final InventoryResponse inventoryResponse = inventoryServiceClient.getInventory(bookingRequest.getEventId());
        log.debug("Inventory Response: {}", inventoryResponse);

        if (inventoryResponse.getLeftCapacity() < bookingRequest.getTicketCount()) {
            throw new NotEnoughInventoryException("Not enough inventory");
        }

        final BookingEvent bookingEvent = createBookingEvent(bookingRequest, customer, inventoryResponse);
        log.info("Booking Event created: {}", bookingEvent);

        // 1. Transactional Outbox Pattern: Save OutboxEvent as PENDING inside the SAME DB Transaction
        try {
            String eventJson = objectMapper.writeValueAsString(bookingEvent);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("BOOKING")
                    .aggregateId(bookingRequest.getEventId())
                    .payload(eventJson)
                    .status("PENDING")
                    .build();
            outboxRepository.save(outboxEvent);
            log.info("Saved Outbox Event PENDING for eventId: {}", bookingRequest.getEventId());
        } catch (Exception e) {
            log.error("Failed to serialize BookingEvent to Outbox JSON", e);
            throw new RuntimeException("Outbox event creation failed", e);
        }

        return BookingResponse.builder()
                .userId(bookingRequest.getUserId())
                .eventId(bookingRequest.getEventId())
                .ticketCount(bookingRequest.getTicketCount())
                .totalPrice(bookingEvent.getTotalPrice())
                .build();
    }

    // 2. Background Worker (@Scheduled): Periodically scans PENDING outbox events and publishes to Kafka asynchronously
    @Scheduled(fixedRate = 5000)
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository.findByStatus("PENDING");
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Scanning Outbox: Found {} PENDING events to publish to Kafka...", pendingEvents.size());
        for (OutboxEvent outbox : pendingEvents) {
            try {
                BookingEvent event = objectMapper.readValue(outbox.getPayload(), BookingEvent.class);
                bookingEventKafkaTemplate.send("booking", event);

                outbox.setStatus("PROCESSED");
                outboxRepository.save(outbox);
                log.info("Successfully published Outbox Event #{} to Kafka topic 'booking'", outbox.getId());
            } catch (Exception e) {
                log.error("Failed to process Outbox Event #{}", outbox.getId(), e);
            }
        }
    }

    private BookingEvent createBookingEvent(BookingRequest bookingRequest, Customer customer, InventoryResponse inventoryResponse) {
        return BookingEvent.builder()
                .eventId(bookingRequest.getEventId())
                .userId(bookingRequest.getUserId())
                .ticketCount(bookingRequest.getTicketCount())
                .totalPrice(inventoryResponse.getTicketPrice().multiply(BigDecimal.valueOf(bookingRequest.getTicketCount())))
                .build();
    }
}
