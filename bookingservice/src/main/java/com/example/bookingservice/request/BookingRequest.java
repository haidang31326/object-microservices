package com.example.bookingservice.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookingRequest {
    @NotNull(message = "userId is required")
    @Positive(message = "userId must be positive")
    private Long userId;

    @NotNull(message = "eventId is required")
    @Positive(message = "eventId must be positive")
    private Long eventId;

    @NotNull(message = "ticketCount is required")
    @Positive(message = "ticketCount must be positive")
    private Long ticketCount;
}
