package com.example.bookingservice.service;

import com.example.bookingservice.client.InventoryServiceClient;
import com.example.bookingservice.entity.Customer;
import com.example.bookingservice.event.BookingEvent;
import com.example.bookingservice.repository.CustomerRepository;
import com.example.bookingservice.request.BookingRequest;
import com.example.bookingservice.response.BookingResponse;
import com.example.bookingservice.response.InventoryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@Service
@Slf4j
public class BookingService {
    private final CustomerRepository customerRepository;
    private final InventoryServiceClient inventoryServiceClient;
    private final KafkaTemplate<String, BookingEvent> bookingEventKafkaTemplate;

    @Autowired
    public BookingService(CustomerRepository customerRepository,InventoryServiceClient inventoryServiceClient
                            , KafkaTemplate<String, BookingEvent> bookingEventKafkaTemplate) {
        this.customerRepository = customerRepository;
        this.inventoryServiceClient = inventoryServiceClient;
        this.bookingEventKafkaTemplate = bookingEventKafkaTemplate;
    }

    public BookingResponse createBooking(@RequestBody BookingRequest bookingRequest) {
        final Customer customer = customerRepository.findById(bookingRequest.getUserId()).orElse(null);
        if(customer == null){
            throw new RuntimeException("User not found");
        }
        final InventoryResponse inventoryResponse = inventoryServiceClient.getInventory(bookingRequest.getEventId());
        System.out.println("Inventory Response: " + inventoryResponse);
        if(inventoryResponse.getCapacity() < bookingRequest.getTicketCount()) {
            throw new RuntimeException("Not Enough inventory");
        }
        final BookingEvent bookingEvent = createBookingEvent(bookingRequest, customer, inventoryResponse);
        log.info("Booking Event: " + bookingEvent);
        bookingEventKafkaTemplate.send("booking", bookingEvent);
        return BookingResponse.builder()
                .userId(bookingRequest.getUserId())
                .eventId(bookingRequest.getEventId())
                .ticketCount(bookingRequest.getTicketCount())
                .totalPrice(bookingEvent.getTotalPrice())
                .build();
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
