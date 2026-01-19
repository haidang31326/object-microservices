package com.dang.microservices.reponse;

import lombok.*;

@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventInventoryResponse {
    private String event;
    private Long capacity;
    private String venue;
}
