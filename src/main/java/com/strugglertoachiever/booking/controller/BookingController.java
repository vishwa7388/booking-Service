package com.strugglertoachiever.booking.controller;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/hotel-ping")
    @CircuitBreaker(name = "hotelServiceCB", fallbackMethod = "hotelServiceFallback")
    public String callHotelService() {
        String hotelServiceResponse = restTemplate.getForObject("http://HOTELSERVICE/hotels/ping", String.class);
        return "Response from HotelService: " + hotelServiceResponse;
    }

    // Fallback method signature must match the main method plus Throwable at the end
    public String hotelServiceFallback(Throwable t) {
        return "Hotel service is unavailable (from Circuit Breaker fallback). Please try again later.";
    }

    @GetMapping("/ping")
    public String ping() {
        return "Booking Service is up";
    }
}