package com.dang.microservices.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "event name is required")
    @Column(name = "name")
    private String name;

    @NotNull(message = "totalCapacity is required")
    @Positive(message = "totalCapacity must be positive")
    @Column(name = "total_capacity")
    private Long totalCapacity;

    @NotNull(message = "leftCapacity is required")
    @PositiveOrZero(message = "leftCapacity must be zero or positive")
    @Column(name = "left_capacity")
    private Long leftCapacity;

    @NotNull(message = "price is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "price must be greater than zero")
    @Column(name = "ticket_price")
    private BigDecimal price;

    @ManyToOne
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @Version
    @Column(name = "version")
    private Integer version;


}
