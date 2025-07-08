package com.strugglertoachiever.booking.service;

import com.strugglertoachiever.booking.model.Booking;
import com.strugglertoachiever.booking.repository.BookingRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RestTemplate restTemplate;

    private static final String HOTEL_SERVICE_URL = "http://hotelservice/hotels/";
    private static final String PAYMENT_SERVICE_URL = "http://paymentservice/payments";

    @CircuitBreaker(name = "hotelServiceCB", fallbackMethod = "hotelFallback")
    @Bulkhead(name = "hotelServiceBulkhead", fallbackMethod = "hotelFallback")
    @Retry(name = "hotelServiceRetry", fallbackMethod = "hotelFallback")
    public ResponseEntity<String> createBooking(Booking booking) {
        // Step 1: Save Booking first
        Booking savedBooking = bookingRepository.save(booking);

        try {
            // Step 2: Call HotelService to verify or reserve hotel
            ResponseEntity<String> hotelResponse = restTemplate.getForEntity(
                    HOTEL_SERVICE_URL + booking.getHotelId(), String.class);

            if (!hotelResponse.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Hotel service failed");
            }

            // Step 3: Call PaymentService
            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("bookingId", savedBooking.getId());
            paymentRequest.put("amount", 2000); // hardcoded for now

            ResponseEntity<String> paymentResponse = restTemplate.postForEntity(
                    PAYMENT_SERVICE_URL, paymentRequest, String.class);

            if (!paymentResponse.getStatusCode().is2xxSuccessful()) {
                // Compensate hotel booking
                restTemplate.delete(HOTEL_SERVICE_URL + booking.getHotelId());
                throw new RuntimeException("Payment service failed");
            }

        } catch (Exception e) {
            // Final Compensation: delete booking
            bookingRepository.deleteById(savedBooking.getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Saga failed and all changes rolled back: " + e.getMessage());
        }

        return ResponseEntity.ok("Booking created, Hotel reserved and Payment successful");
    }

    public ResponseEntity<String> bookingFallback(Booking booking, Throwable t) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Booking failed due to downstream service issue: " + t.getMessage());
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id).orElse(null);
    }

    public void deleteBooking(Long id) {
        bookingRepository.deleteById(id);
    }
}
