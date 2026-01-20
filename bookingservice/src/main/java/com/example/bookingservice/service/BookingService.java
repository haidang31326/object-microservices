package com.example.bookingservice.service;

import com.example.bookingservice.entity.Customer;
import com.example.bookingservice.repository.CustomerRepository;
import com.example.bookingservice.request.BookingRequest;
import com.example.bookingservice.response.BookingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public class BookingService {
    private final CustomerRepository customerRepository;

    @Autowired
    public BookingService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public BookingResponse createBooking(@RequestBody BookingRequest bookingRequest) {
        final Customer customer = customerRepository.findById(bookingRequest.getUserId()).orElse(null);
        if(customer == null){
            throw new RuntimeException("User not found");
        }

        // Dummy response for illustration
        return BookingResponse.builder()
                .build();
    }

}
