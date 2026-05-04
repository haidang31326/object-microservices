package com.example.orderservice.Controller;

import com.example.orderservice.Response.OrderResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/orders")
public class OrderController {
    private final OrderRepository orderRepository;
    private final com.example.orderservice.service.OrderService orderService;

    public OrderController(OrderRepository orderRepository, com.example.orderservice.service.OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }
    @GetMapping("/history")
    public ResponseEntity<List<Order>> getOrderHistory(Long CustomerID) {
        return ResponseEntity.ok(orderRepository.findAllByCustomerId(CustomerID));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@org.springframework.web.bind.annotation.PathVariable("orderId") Long orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.ok().build();
    }
}
