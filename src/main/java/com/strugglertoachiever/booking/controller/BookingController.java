package com.strugglertoachiever.booking.controller;

import com.strugglertoachiever.booking.model.Booking;
import com.strugglertoachiever.booking.service.BookingService;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.function.Supplier;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @GetMapping("/hotel-ping")
    @Bulkhead(name = "hotelServiceBulkhead", type = Bulkhead.Type.THREADPOOL, fallbackMethod = "hotelServiceFallback")
    @Retry(name = "hotelServiceRetry", fallbackMethod = "hotelServiceRetryFallback")
    @RateLimiter(name = "hotelServiceRateLimiter", fallbackMethod = "hotelFallback")
    public String callHotelService() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("hotelServiceCB");

        Supplier<String> supplier = () -> restTemplate.getForObject(
                "http://hotelservice/hotels/ping", String.class
        );

        // Decorate the supplier with circuit breaker and fallback
        Supplier<String> decoratedSupplier = Decorators.ofSupplier(supplier)
                .withCircuitBreaker(circuitBreaker)
                .withFallback(
                        // Handle any exception from RestTemplate
                        ex -> "Hotel service is unavailable (programmatic fallback). Please try again later."
                )
                .decorate();

        // Execute the decorated supplier and return the result
        return decoratedSupplier.get();
    }

    @GetMapping("/ping")
    public String ping() {
        return "Booking Service is up";
    }

    public String hotelServiceFallback(BulkheadFullException ex) {
        return "Hotel Service is busy; please try later.";
    }

    public String hotelServiceRetryFallback(Exception ex) {
        return "Hotel Service is temporarily unavailable. Please try again later.";
    }

    public String hotelFallback(Throwable t) {
        return "Hotel service is busy, please try again later.";
    }

    @PostMapping
    public ResponseEntity<String> createBooking(@RequestBody Booking booking) {
        return bookingService.createBooking(booking);
    }

    @GetMapping
    public List<Booking> getAllBookings() {
        return bookingService.getAllBookings();
    }

    @GetMapping("/{id}")
    public Booking getBookingById(@PathVariable Long id) {
        return bookingService.getBookingById(id);
    }

    @DeleteMapping("/{id}")
    public void deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
    }
}