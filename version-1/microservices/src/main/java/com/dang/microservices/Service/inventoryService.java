package com.dang.microservices.Service;

import com.dang.microservices.Repository.EventRepository;
import com.dang.microservices.Repository.VenueRepository;
import com.dang.microservices.entity.Event;
import com.dang.microservices.entity.Venue;
import com.dang.microservices.reponse.EventInventoryResponse;
import com.dang.microservices.reponse.VenueInventoryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class inventoryService {
    private VenueRepository venueRepository;
    private EventRepository eventRepository;

    @Autowired
    public inventoryService(final VenueRepository venueRepository, final EventRepository eventRepository) {
        this.venueRepository = venueRepository;
        this.eventRepository = eventRepository;
    }

    public List<EventInventoryResponse> getAllEvents() {
        final List<Event> events = eventRepository.findAll();

        return events.stream().map(event -> EventInventoryResponse.builder()
                .event(event.getName())
                .capacity(event.getLeftCapacity())
                .venue(event.getVenue().getName())
                .build()).collect(Collectors.toList());
    }

    public VenueInventoryResponse getVenueById(Long venueId) {
        final Venue venue = venueRepository.findById(venueId).orElseThrow();

        return VenueInventoryResponse.builder()
                .venueId(venue.getId())
                .venueName(venue.getName())
                .totalCapacity(venue.getTotalCapacity())
                .build();

    }
}
