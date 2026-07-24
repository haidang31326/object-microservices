package com.example.bookingservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String aggregateType; // BOOKING

    @Column(nullable = false)
    private Long aggregateId; // EventId or UserId

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload; // JSON representation of BookingEvent

    @Column(nullable = false)
    private String status; // PENDING, PROCESSED, FAILED

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "PENDING";
        }
    }
}
