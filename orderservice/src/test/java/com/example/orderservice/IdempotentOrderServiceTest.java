package com.example.orderservice;

import com.example.bookingservice.event.BookingEvent;
import com.example.orderservice.client.InventoryServiceClient;
import com.example.orderservice.entity.Order;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.service.OrderService;
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
        "spring.datasource.url=jdbc:h2:mem:orderservice_test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cloud.compatibility-verifier.enabled=false"
})
class IdempotentOrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private InventoryServiceClient inventoryServiceClient;

    @Test
    @DisplayName("Phase 2 Test: Idempotent Consumer - Bỏ qua tin nhắn Kafka bị lặp")
    void testIdempotentConsumer() {
        BookingEvent event = BookingEvent.builder()
                .userId(1001L)
                .eventId(500L)
                .ticketCount(2L)
                .totalPrice(new BigDecimal("400000"))
                .build();

        // 1. First event processing: Order created
        System.out.println("\n---> LOG: Lần 1 - Nhận BookingEvent từ Kafka");
        orderService.orderEvent(event);

        List<Order> ordersAfterFirst = orderRepository.findAllByCustomerId(1001L);
        System.out.println("LOG: Số đơn hàng sau lần 1: " + ordersAfterFirst.size());
        Assertions.assertEquals(1, ordersAfterFirst.size());

        // 2. Second duplicate event processing: Should be SKIPPED by Idempotent Consumer
        System.out.println("\n---> LOG: Lần 2 (Spam / Lặp tin nhắn) - Nhận lại BookingEvent trùng từ Kafka");
        orderService.orderEvent(event);

        List<Order> ordersAfterSecond = orderRepository.findAllByCustomerId(1001L);
        System.out.println("LOG: Số đơn hàng sau lần 2: " + ordersAfterSecond.size());

        System.out.println("\n==========================================");
        System.out.println("RESULT: Idempotent Consumer hoạt động chuẩn 100%!");
        System.out.println("RESULT: Tin nhắn lặp 2 đã bị bỏ qua, không bị nhân đôi đơn hàng!");
        System.out.println("==========================================\n");

        Assertions.assertEquals(1, ordersAfterSecond.size(), "Idempotent Consumer phải bỏ qua tin nhắn bị lặp!");
    }
}
