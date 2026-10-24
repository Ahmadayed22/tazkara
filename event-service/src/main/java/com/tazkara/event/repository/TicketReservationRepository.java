package com.tazkara.event.repository;
import com.tazkara.event.domain.TicketReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketReservationRepository extends JpaRepository<TicketReservation, Long> {

    Optional<TicketReservation> findByBookingReference(String bookingReference);
}
