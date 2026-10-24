package com.tazkara.event.repository;

import com.tazkara.event.domain.Event;
import com.tazkara.event.domain.EventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;


public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    /**
     * SELECT ... FOR UPDATE: concurrent reservations/cancellations of the same event wait for each other,
     * so two attendees can never both take the last ticket.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") Long id);

    Page<Event> findByOrganizerId(Long organizerId, Pageable pageable);

    Page<Event> findByOrganizerIdAndStatus(Long organizerId, EventStatus status, Pageable pageable);
}
