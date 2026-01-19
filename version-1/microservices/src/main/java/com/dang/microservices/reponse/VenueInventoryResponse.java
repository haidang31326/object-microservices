package com.dang.microservices.reponse;

import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;

@Data
@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VenueInventoryResponse {
    private Long venueId;
    private String venueName;
    private Long totalCapacity;
}
