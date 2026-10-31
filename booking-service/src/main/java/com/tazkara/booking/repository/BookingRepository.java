package com.tazkara.booking.repository;

import com.tazkara.booking.domain.Booking;
import com.tazkara.booking.domain.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;


public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("select b from Booking b where b.reference.value = :reference")
    Optional<Booking> findByReference(@Param("reference") String reference);

    /** Row lock: payment, cancellation and the expiry job on the same booking run one after the other. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.reference.value = :reference")
    Optional<Booking> findByReferenceForUpdate(@Param("reference") String reference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    boolean existsByReferenceValue(String reference);

    Page<Booking> findByAttendeeId(Long attendeeId, Pageable pageable);

    Page<Booking> findByAttendeeIdAndStatus(Long attendeeId, BookingStatus status, Pageable pageable);

    Page<Booking> findByEventId(Long eventId, Pageable pageable);

    Page<Booking> findByEventIdAndStatus(Long eventId, BookingStatus status, Pageable pageable);

    /**
     * Transaction-scoped PostgreSQL advisory lock on (attendee, event). Two parallel bookings of the same
     * attendee for the same event are serialised, so the 6-ticket limit cannot be bypassed by racing requests.
     * The lock is released automatically at commit or rollback.
     */
    @Query(value = "select 1 from pg_advisory_xact_lock(:key)", nativeQuery = true)
    Integer lockAttendeeEvent(@Param("key") long key);

    @Query("""
            select coalesce(sum(l.quantity), 0L) from Booking b join b.lines l
            where b.attendeeId = :attendeeId and b.eventId = :eventId and b.status in :statuses
            """)
    Long countTickets(@Param("attendeeId") Long attendeeId, @Param("eventId") Long eventId,
                      @Param("statuses") Collection<BookingStatus> statuses);

    @Query("""
            select b.id from Booking b
            where b.status = :status and b.holdExpiresAt <= :now
            order by b.holdExpiresAt
            """)
    List<Long> findOverdueIds(@Param("status") BookingStatus status, @Param("now") Instant now, Pageable limit);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.eventId = :eventId and b.status in :statuses order by b.id")
    List<Booking> findByEventIdAndStatusInForUpdate(@Param("eventId") Long eventId,
                    @Param("statuses") Collection<BookingStatus> statuses);
                                                    
                     @Query("""
            select new com.tazkara.booking.repository.TicketSalesRow(
                l.ticketTypeId, b.status, sum(l.quantity), sum(l.unitPrice * l.quantity))
            from Booking b join b.lines l
            where b.eventId = :eventId and b.status in :statuses
            group by l.ticketTypeId, b.status
            """)
    List<TicketSalesRow> summarizeByTicketType(@Param("eventId") Long eventId,
                                               @Param("statuses") Collection<BookingStatus> statuses);

    @Query("""
            select distinct b from Booking b join fetch b.lines
            where b.eventId = :eventId and b.status = :status
            order by b.confirmedAt, b.id
            """)
    List<Booking> findWithLinesByEventIdAndStatus(@Param("eventId") Long eventId,
                                                  @Param("status") BookingStatus status);
}
