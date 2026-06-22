package com.dang.microservices.Service;

import com.dang.microservices.Repository.EventRepository;
import com.dang.microservices.Repository.VenueRepository;
import com.dang.microservices.entity.Event;
import com.dang.microservices.entity.Venue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryServiceTest {
    private EventRepository eventRepository;
    private VenueRepository venueRepository;
    private inventoryService service;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        venueRepository = mock(VenueRepository.class);
        service = new inventoryService(venueRepository, eventRepository);
    }

    @Test
    void updateEventCapacity_reducesAvailableTickets() {
        Event event = event(1L, 100L, 25L);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        service.updateEventCapacity(1L, 5L);

        verify(eventRepository).saveAndFlush(event);
        org.assertj.core.api.Assertions.assertThat(event.getLeftCapacity()).isEqualTo(20L);
    }

    @Test
    void updateEventCapacity_rejectsOverbooking() {
        Event event = event(1L, 100L, 3L);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.updateEventCapacity(1L, 4L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not enough tickets");

        verify(eventRepository, never()).saveAndFlush(event);
    }

    @Test
    void createEvent_rejectsLeftCapacityGreaterThanTotalCapacity() {
        Venue venue = new Venue(1L, "Demo Venue", "Ho Chi Minh City", 100L);
        Event event = event(null, 100L, 101L);
        when(venueRepository.findById(1L)).thenReturn(Optional.of(venue));

        assertThatThrownBy(() -> service.createEvent(event, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("leftCapacity");

        verify(eventRepository, never()).save(event);
    }

    @Test
    void restoreEventCapacity_rejectsCapacityAboveTotal() {
        Event event = event(1L, 100L, 95L);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.restoreEventCapacity(1L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceed total capacity");

        verify(eventRepository, never()).saveAndFlush(event);
    }

    private Event event(Long id, Long totalCapacity, Long leftCapacity) {
        Event event = new Event();
        event.setId(id);
        event.setName("Demo Event");
        event.setTotalCapacity(totalCapacity);
        event.setLeftCapacity(leftCapacity);
        event.setPrice(new BigDecimal("49.00"));
        return event;
    }
}
