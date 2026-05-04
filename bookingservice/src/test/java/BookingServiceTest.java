import com.example.bookingservice.client.InventoryServiceClient;
import com.example.bookingservice.entity.Customer;
import com.example.bookingservice.response.BookingEvent;
import com.example.bookingservice.exception.NotEnoughInventoryException;
import com.example.bookingservice.exception.UserNotFoundException;
import com.example.bookingservice.repository.CustomerRepository;
import com.example.bookingservice.request.BookingRequest;
import com.example.bookingservice.response.BookingResponse;
import com.example.bookingservice.response.InventoryResponse;
import com.example.bookingservice.response.VenueResponse;
import com.example.bookingservice.service.BookingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {
    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private InventoryServiceClient inventoryServiceClient;

    @Mock
    private KafkaTemplate<String, BookingEvent> kafkaTemplate;

    @InjectMocks
    private BookingService bookingService;


    @Test
    void testCreateBooking_Success() {
        BookingRequest request = new BookingRequest();
        request.setUserId(1L);
        request.setEventId(1L);
        request.setTicketCount(2L);

        Customer mockCustomer = new Customer();
        mockCustomer.setId(1L);
        mockCustomer.setName("Nguyen Van A");
        mockCustomer.setEmail("test@gmail.com");
        mockCustomer.setAddress("Hanoi");

        InventoryResponse mockInventory = new InventoryResponse();
        mockInventory.setCapacity(100L);
        mockInventory.setEvent("Football Match");
        mockInventory.setEventId(1L);
        mockInventory.setLeftCapacity(10L);
        mockInventory.setTicketPrice(new BigDecimal("100.00"));


        VenueResponse mockVenue = new VenueResponse();
        mockVenue.setId(1L);
        mockVenue.setName("My Dinh Stadium");
        mockVenue.setTotalCapacity(40000L);
        mockInventory.setVenue(mockVenue);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(mockCustomer));
        when(inventoryServiceClient.getInventory(1L)).thenReturn(mockInventory);

        BookingResponse response = bookingService.createBooking(request);

        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertEquals(2, response.getTicketCount());
        assertEquals(new BigDecimal("200.00"), response.getTotalPrice());

        verify(kafkaTemplate, times(1)).send(eq("booking"), any(BookingEvent.class));
    }


    @Test
    void testCreateBooking_UserNotFound_ThrowsException() {
        BookingRequest request = new BookingRequest();
        request.setUserId(99L);
        request.setEventId(1L);
        request.setTicketCount(2L);

        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            bookingService.createBooking(request);
        });

        verify(kafkaTemplate, never()).send(anyString(), any(BookingEvent.class));
    }


    @Test
    void testCreateBooking_NotEnoughInventory_ThrowsException() {
        BookingRequest request = new BookingRequest();
        request.setUserId(1L);
        request.setEventId(1L);
        request.setTicketCount(50L);

        Customer mockCustomer = new Customer();
        mockCustomer.setId(1L);
        mockCustomer.setName("Nguyen Van A");
        mockCustomer.setEmail("test@gmail.com");
        mockCustomer.setAddress("Hanoi");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(mockCustomer));

        InventoryResponse mockInventory = new InventoryResponse();
        mockInventory.setCapacity(100L);
        mockInventory.setEvent("Football Match");
        mockInventory.setEventId(1L);
        mockInventory.setLeftCapacity(10L);
        mockInventory.setTicketPrice(new BigDecimal("100.00"));


        mockInventory.setEvent("Football Match");


        VenueResponse mockVenue = new VenueResponse();
        mockInventory.setVenue(mockVenue);
        mockVenue.setTotalCapacity(40000L);
        mockVenue.setId(1L);
        mockVenue.setName("My Dinh Stadium");
        mockVenue.setAddress("Hanoi");


        when(inventoryServiceClient.getInventory(1L)).thenReturn(mockInventory);

        assertThrows(NotEnoughInventoryException.class, () -> {
            bookingService.createBooking(request);
        });

        verify(kafkaTemplate, never()).send(anyString(), any(BookingEvent.class));
    }
}
