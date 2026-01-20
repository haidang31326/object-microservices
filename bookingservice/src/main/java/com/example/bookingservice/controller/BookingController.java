package com.example.bookingservice.controller;

import com.example.bookingservice.request.BookingRequest;
import com.example.bookingservice.response.BookingResponse;
import com.example.bookingservice.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ap1/v1")
public class BookingController {
    private BookingService bookingService;

    @Autowired
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/bookings")
    public BookingResponse createBooking(@RequestBody BookingRequest bookingRequest) {

        return bookingService.createBooking(bookingRequest);
    }
}
