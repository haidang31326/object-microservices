package com.example.orderservice.service;

import com.example.bookingservice.event.BookingEvent;
import com.example.orderservice.client.InventoryServiceClient;
import com.example.orderservice.entity.Order;
import com.example.orderservice.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
@Slf4j
@Service
public class OrderService {
    private OrderRepository orderRepository;
    private InventoryServiceClient inventoryServiceClient;

    @Autowired
    public OrderService(OrderRepository orderRepository
    , InventoryServiceClient inventoryServiceClient) {
        this.orderRepository = orderRepository;
        this.inventoryServiceClient = inventoryServiceClient;
    }

    @KafkaListener(topics = "booking", groupId = "order-service")
    public void orderEvent(BookingEvent bookingevent) {

    log.info("Received Order Event: {}", bookingevent);

    Order order =createOrder(bookingevent);
    orderRepository.saveAndFlush(order);
    inventoryServiceClient.updateEventCapacity(bookingevent.getEventId(), bookingevent.getTicketCount());
    log.info("Inventory updated for eventId {} after booking {} tickets", order.getEventId(), order.getTicketCount());
    }

    public Order createOrder(BookingEvent bookingevent) {
        return Order.builder()
                .customerId(bookingevent.getUserId())
                .eventId(bookingevent.getEventId())
                .ticketCount(bookingevent.getTicketCount())
                .totalPrice(bookingevent.getTotalPrice())
                .build();
    }
}
