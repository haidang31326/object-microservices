package com.dang.microservices.Service;

import com.dang.microservices.Repository.EventRepository;
import com.dang.microservices.Repository.VenueRepository;
import com.dang.microservices.entity.Event;
import com.dang.microservices.entity.Venue;
import com.dang.microservices.reponse.EventInventoryResponse;
import com.dang.microservices.reponse.VenueInventoryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
@Slf4j
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
                .venue(event.getVenue())
               /* .price(event.getPrice())*/
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
    public EventInventoryResponse getEventInventory(Long eventId) {

        final Event event = eventRepository.findById(eventId).orElseThrow();

        return EventInventoryResponse.builder()
                .event(event.getName())
                .capacity(event.getLeftCapacity())
                .venue(event.getVenue())
                .eventId(event.getId())
                .ticketPrice(event.getPrice())
                .build();
    }
    public void updateEventCapacity(Long eventId, Long ticketsBooked) {
        final Event event = eventRepository.findById(eventId).orElseThrow();
        event.setLeftCapacity(event.getLeftCapacity() - ticketsBooked);
        eventRepository.saveAndFlush(event);
        log.info("Updated event capacity for eventId {}: new capacity is {}", eventId, event.getLeftCapacity());
    }
}
