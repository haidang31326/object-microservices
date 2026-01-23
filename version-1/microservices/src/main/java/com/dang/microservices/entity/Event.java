package com.dang.microservices.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "event")
public class Event {
    @Id
    private Long id;
    @Column(name = "name")
    private String name;
    @Column(name = "total_capacity")
    private Long totalCapacity;
    @Column(name = "left_capacity")
    private Long leftCapacity;


    @ManyToOne
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @Column(name = "price")
    private BigDecimal price;
}
