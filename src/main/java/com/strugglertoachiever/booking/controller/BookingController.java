package com.strugglertoachiever.booking.controller;

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
    public String callHotelService() {
        String hotelServiceResponse = restTemplate.getForObject("http://HOTELSERVICE/hotels/ping", String.class);
        return "Response from HotelService: " + hotelServiceResponse;
    }

    @GetMapping("/ping")
    public String ping() {
        return "Booking Service is up";
    }
}