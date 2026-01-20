package com.dang.microservices.reponse;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventInventoryResponse {
    private Long eventId;
    private String event;
    private Long capacity;
    private String venue;
    private BigDecimal price;
}
