package com.dang.microservices.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Entity
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "venue")
public class Venue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "venue name is required")
    @Column(name = "name")
    private String name;

    @NotBlank(message = "venue address is required")
    @Column(name = "address")
    private String address;

    @NotNull(message = "totalCapacity is required")
    @Positive(message = "totalCapacity must be positive")
    @Column(name = "total_capacity")
    private Long totalCapacity;
}
