package com.dang.microservices.controller;

import com.dang.microservices.Service.inventoryService;
import com.dang.microservices.entity.Event;
import com.dang.microservices.entity.Venue;
import com.dang.microservices.reponse.EventInventoryResponse;
import com.dang.microservices.reponse.VenueInventoryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class InventoryController {
    private inventoryService inventoryService;
    @Autowired
    public InventoryController(final inventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }
    @GetMapping("/inventory/venue/{venueId}")
    public  @ResponseBody ResponseEntity<VenueInventoryResponse> inventoryGetVenueById(@PathVariable("venueId") Long venueId) {
        return ResponseEntity.ok(inventoryService.getVenueById(venueId));
    }
    @GetMapping("/inventory/events")
    public ResponseEntity<List<EventInventoryResponse>> inventoryGetAllEvents() {
        return ResponseEntity.ok(inventoryService.getAllEvents());
    }
    @GetMapping("/inventory/event/{eventId}")
    public ResponseEntity<EventInventoryResponse> inventoryGetEventById(@PathVariable("eventId") Long eventId) {

        return ResponseEntity.ok(inventoryService.getEventInventory(eventId));
    }
    @PutMapping("/inventory/event/{eventId}/capacity/{capacity}")
    public ResponseEntity<Void> updateEventCapacity(@PathVariable("eventId") Long eventId, @PathVariable("capacity") Long capacity) {
        inventoryService.updateEventCapacity(eventId, capacity);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/inventory/venue/create")
    public ResponseEntity<VenueInventoryResponse> createVenue(@RequestBody Venue venue) {
        return ResponseEntity.ok(inventoryService.createVenue(venue));
    }
    @PostMapping("/inventory/event/create/venue/{venueId}")
    public ResponseEntity<EventInventoryResponse> createEvent(@RequestBody Event event,@PathVariable Long venueId) {
        return ResponseEntity.ok(inventoryService.createEvent(event, venueId));
    }

    @PutMapping("/inventory/event/{eventId}")
    public ResponseEntity<EventInventoryResponse> updateEvent(@PathVariable("eventId") Long eventId, @RequestBody Event updatedEvent) {
        return ResponseEntity.ok(inventoryService.updateEvent(eventId, updatedEvent));
    }

    @DeleteMapping("/inventory/event/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable("eventId") Long eventId) {
        inventoryService.deleteEvent(eventId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/inventory/event/{eventId}/restore/{capacity}")
    public ResponseEntity<Void> restoreEventCapacity(@PathVariable("eventId") Long eventId, @PathVariable("capacity") Long capacity) {
        inventoryService.restoreEventCapacity(eventId, capacity);
        return ResponseEntity.ok().build();
    }

}
