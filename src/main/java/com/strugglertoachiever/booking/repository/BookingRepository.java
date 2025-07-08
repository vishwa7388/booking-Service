package com.strugglertoachiever.booking.repository;

import com.strugglertoachiever.booking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {}
