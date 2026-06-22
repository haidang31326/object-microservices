package com.example.orderservice.service;

import com.example.bookingservice.event.BookingEvent;
import com.example.orderservice.client.InventoryServiceClient;
import com.example.orderservice.entity.Order;
import com.example.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {
    private OrderRepository orderRepository;
    private InventoryServiceClient inventoryServiceClient;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        inventoryServiceClient = mock(InventoryServiceClient.class);
        orderService = new OrderService(orderRepository, inventoryServiceClient);
    }

    @Test
    void createOrder_mapsBookingEventToOrder() {
        BookingEvent event = bookingEvent();

        Order order = orderService.createOrder(event);

        assertThat(order.getCustomerId()).isEqualTo(1L);
        assertThat(order.getEventId()).isEqualTo(2L);
        assertThat(order.getTicketCount()).isEqualTo(3L);
        assertThat(order.getTotalPrice()).isEqualByComparingTo("147.00");
    }

    @Test
    void orderEvent_savesOrderAndUpdatesInventory() {
        BookingEvent event = bookingEvent();

        orderService.orderEvent(event);

        verify(orderRepository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(order ->
                order.getCustomerId().equals(1L)
                        && order.getEventId().equals(2L)
                        && order.getTicketCount().equals(3L)
                        && order.getTotalPrice().compareTo(new BigDecimal("147.00")) == 0
        ));
        verify(inventoryServiceClient).updateEventCapacity(2L, 3L);
    }

    @Test
    void cancelOrder_deletesOrderAndRestoresInventory() {
        Order order = Order.builder()
                .id(10L)
                .customerId(1L)
                .eventId(2L)
                .ticketCount(3L)
                .totalPrice(new BigDecimal("147.00"))
                .build();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        orderService.cancelOrder(10L);

        verify(orderRepository).delete(order);
        verify(inventoryServiceClient).restoreEventCapacity(2L, 3L);
    }

    private BookingEvent bookingEvent() {
        return BookingEvent.builder()
                .userId(1L)
                .eventId(2L)
                .ticketCount(3L)
                .totalPrice(new BigDecimal("147.00"))
                .build();
    }
}
