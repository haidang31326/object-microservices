package com.dang.microservices;

import com.dang.microservices.Repository.EventRepository;
import com.dang.microservices.Service.inventoryService;
import com.dang.microservices.entity.Event;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
@ActiveProfiles("test")
public class InventoryConcurrencyTest {

    @Autowired
    private inventoryService inventoryService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private com.dang.microservices.Repository.VenueRepository venueRepository;

    @Test
    @DisplayName("Test Concurrency: 10 Threads cùng bấm mua vé đồng thời -> Optimistic Locking chặn Overselling")
    void testConcurrentTicketBooking() throws InterruptedException {
        // 1. Khởi tạo Venue & Event có 10 vé
        com.dang.microservices.entity.Venue venue = new com.dang.microservices.entity.Venue();
        venue.setName("San Van Dong My Dinh");
        venue.setAddress("Ha Noi");
        venue.setTotalCapacity(1000L);
        venue = venueRepository.save(venue);

        Event event = new Event();
        event.setName("Concert Son Tung M-TP 2026");
        event.setTotalCapacity(10L);
        event.setLeftCapacity(10L);
        event.setPrice(new BigDecimal("500000.00"));
        event.setVenue(venue);
        event = eventRepository.saveAndFlush(event);

        Long eventId = event.getId();
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 2. Giả lập 10 Threads bấm mua vé cùng 1 miligiây
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                try {
                    inventoryService.updateEventCapacity(eventId, 1L);
                    successCount.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException e) {
                    System.out.println("LOG: Thread bị Xung đột Version -> Optimistic Lock chặn mua trùng vé!");
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // 3. Kiểm tra số lượng vé còn lại trong CSDL
        Event updatedEvent = eventRepository.findById(eventId).orElseThrow();
        System.out.println("\n==========================================");
        System.out.println("RESULT: Số giao dịch thành công: " + successCount.get());
        System.out.println("RESULT: Số giao dịch bị chặn bởi Optimistic Lock: " + failureCount.get());
        System.out.println("RESULT: Số lượng vé còn lại trong DB: " + updatedEvent.getLeftCapacity());
        System.out.println("==========================================\n");

        Assertions.assertTrue(updatedEvent.getLeftCapacity() >= 0, "Kho vé không bao giờ được âm (Overselling)!");
        Assertions.assertEquals(10L - successCount.get(), updatedEvent.getLeftCapacity(), "Số vé trong DB phải khớp đúng số đơn hàng mua thành công!");
    }
}
