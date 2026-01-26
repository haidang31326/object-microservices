package com.dang.microservices.controller;

import com.dang.microservices.Service.inventoryService;
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
    public  @ResponseBody VenueInventoryResponse inventoryGetVenueById(@PathVariable("venueId") Long venueId) {
        return inventoryService.getVenueById(venueId);
    }
    @GetMapping("/inventory/events")
    public List<EventInventoryResponse> inventoryGetEventById() {
        return inventoryService.getAllEvents();
    }
    @GetMapping("/inventory/event/{eventId}")
    public EventInventoryResponse inventoryGetEventById(@PathVariable("eventId") Long eventId) {

        return inventoryService.getEventInventory(eventId);
    }
    @PutMapping("/inventory/event/{eventId}/capacity/{capacity}")
    public ResponseEntity<Void> updateEventCapacity(@PathVariable("eventId") Long eventId, @PathVariable("capacity") Long capacity) {
        inventoryService.updateEventCapacity(eventId, capacity);
        return ResponseEntity.ok().build();
    }
}
