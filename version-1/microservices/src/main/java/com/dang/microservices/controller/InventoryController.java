package com.dang.microservices.controller;

import com.dang.microservices.Service.inventoryService;
import com.dang.microservices.reponse.EventInventoryResponse;
import com.dang.microservices.reponse.VenueInventoryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class InventoryController {
    private inventoryService inventoryService;
    @Autowired
    public InventoryController(final inventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }
    @GetMapping
    public List<EventInventoryResponse> inventoryGetAllEvents() {
        return inventoryService.getAllEvents();
    }
    @GetMapping("/inventory/venue/{venueId}")
    public VenueInventoryResponse inventoryGetVenueById(@RequestBody Long venueId) {
        return inventoryService.getVenueById(venueId);
    }
    @GetMapping("/inventory/event/{eventId} ")
    public EventInventoryResponse inventoryGetEventById(@RequestBody Long eventId) {
        return inventoryService.getEventById(eventId);
    }
}
