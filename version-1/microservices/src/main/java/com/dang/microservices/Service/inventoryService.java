package com.dang.microservices.Service;

import com.dang.microservices.Repository.EventRepository;
import com.dang.microservices.Repository.VenueRepository;
import com.dang.microservices.controller.InventoryController;
import com.dang.microservices.entity.Event;
import com.dang.microservices.entity.Venue;
import com.dang.microservices.exception.EventNotFoundException;
import com.dang.microservices.exception.VenueNotFoundException;
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

        final Event event = eventRepository.findById(eventId).orElseThrow(() ->  new EventNotFoundException("Event with id " + eventId + " not found"));
        return EventInventoryResponse.builder()
                .event(event.getName())
                .capacity(event.getLeftCapacity())
                .venue(event.getVenue())
                .eventId(event.getId())
                .ticketPrice(event.getPrice())
                .build();
    }
    public void updateEventCapacity(Long eventId, Long ticketsBooked) {
        final Event event = eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException("Event with id " + eventId + " not found"));
        event.setLeftCapacity(event.getLeftCapacity() - ticketsBooked);
        eventRepository.saveAndFlush(event);
        log.info("Updated event capacity for eventId {}: new capacity is {}", eventId, event.getLeftCapacity());
    }
    public VenueInventoryResponse createVenue(Venue venue) {
        final Venue savedVenue = venueRepository.save(venue);
        return VenueInventoryResponse.builder()
                .venueId(savedVenue.getId())
                .venueName(savedVenue.getName())
                .totalCapacity(savedVenue.getTotalCapacity())
                .build();

    }
    public EventInventoryResponse createEvent(Event event, Long venueId ) {
        Venue venue = venueRepository.findById(venueId)
            .orElseThrow(() -> new VenueNotFoundException("Venue with id " + venueId + " not found"));
        event.setVenue(venue);
        final Event savedEvent = eventRepository.save(event);
        return EventInventoryResponse.builder()
                .eventId(savedEvent.getId())
                .event(savedEvent.getName())
                .capacity(savedEvent.getLeftCapacity())
                .venue(savedEvent.getVenue())
                .ticketPrice(savedEvent.getPrice())
                .build();
    }

    public EventInventoryResponse updateEvent(Long eventId, Event updatedEvent) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event with id " + eventId + " not found"));
        if(updatedEvent.getName() != null) event.setName(updatedEvent.getName());
        if(updatedEvent.getPrice() != null) event.setPrice(updatedEvent.getPrice());
        if(updatedEvent.getTotalCapacity() != null) event.setTotalCapacity(updatedEvent.getTotalCapacity());
        if(updatedEvent.getLeftCapacity() != null) event.setLeftCapacity(updatedEvent.getLeftCapacity());
        Event savedEvent = eventRepository.save(event);
        return EventInventoryResponse.builder()
                .eventId(savedEvent.getId())
                .event(savedEvent.getName())
                .capacity(savedEvent.getLeftCapacity())
                .venue(savedEvent.getVenue())
                .ticketPrice(savedEvent.getPrice())
                .build();
    }

    public void deleteEvent(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new EventNotFoundException("Event with id " + eventId + " not found");
        }
        eventRepository.deleteById(eventId);
    }

    public void restoreEventCapacity(Long eventId, Long ticketsCancelled) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException("Event with id " + eventId + " not found"));
        event.setLeftCapacity(event.getLeftCapacity() + ticketsCancelled);
        eventRepository.saveAndFlush(event);
        log.info("Restored event capacity for eventId {}: new capacity is {}", eventId, event.getLeftCapacity());
    }
}
