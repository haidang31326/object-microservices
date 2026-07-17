package com.example.orderservice.Controller;

import com.example.orderservice.entity.Order;
import com.example.orderservice.service.OrderService;
import com.example.orderservice.exception.PaymentProcessingException;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class PaymentController {
    @Value("${stripe.api.key}")
    private String stripeApiKey;
    @Value("${stripe.frontend.success.url}")
    private String successUrl;
    @Value("${stripe.frontend.cancel.url}")
    private String cancelUrl;
    private final OrderService orderService;

    public PaymentController(OrderService orderService, @Value("${stripe.api.key}") String apiKey) {
        this.orderService = orderService;
        Stripe.apiKey = apiKey;
    }

    @PostMapping("/{orderId}/checkout")
    public ResponseEntity<Map<String, String>> createCheckoutSession(@PathVariable Long orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}&orderId=" + orderId)
                    .setCancelUrl(cancelUrl + "?orderId=" + orderId)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder().setCurrency("vnd").setUnitAmount(order.getTotalPrice().longValue()).setProductData(
                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder().setName("Vé xem sự kiện - Đơn hàng #" + orderId).build()
                                            ).build()
                                    ).build())
                    // Đóng gói metadata để Stripe gửi trả lại qua Webhook sau khi thanh toán thành công
                    .putMetadata("orderId", String.valueOf(orderId))
                    .build();
            Session session = Session.create(params);
            // 3. Trả về link trang thanh toán của Stripe cho Frontend redirect
            Map<String, String> response = new HashMap<>();
            response.put("checkoutUrl", session.getUrl());
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            throw new PaymentProcessingException("Không thể tạo phiên thanh toán Stripe: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new PaymentProcessingException("Lỗi hệ thống khi xử lý thanh toán: " + e.getMessage(), e);
        }
    }

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            // Xác thực tính hợp lệ của chữ ký từ Stripe gửi tới
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(400).body("Invalid signature signature check failed");
        }
        // Xử lý sự kiện thanh toán thành công
        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
            if (session != null) {
                String orderIdStr = session.getMetadata().get("orderId");
                Long orderId = Long.parseLong(orderIdStr);
                // Cập nhật trạng thái đơn hàng thành PAID
                orderService.markOrderAsPaid(orderId);

                System.out.println("Đã xác nhận thanh toán thành công qua Stripe cho đơn hàng #" + orderId);
            }
        }
        return ResponseEntity.ok("Received");
    }
}

