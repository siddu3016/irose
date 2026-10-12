package com.irose.olivehotel.repository;

import com.irose.olivehotel.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingCode(String bookingCode);

    List<Booking> findAllByOrderByCreatedAtDesc();

    List<Booking> findByStatusOrderByCheckInDateAsc(BookingStatus status);

    List<Booking> findByGuestOrderByCheckInDateDesc(Guest guest);

    List<Booking> findByCheckInDateAndStatusIn(LocalDate date, Collection<BookingStatus> statuses);

    List<Booking> findByCheckOutDateAndStatus(LocalDate date, BookingStatus status);

    /** Bookings of a room type whose stay overlaps [from, to). */
    @Query("""
            select count(b) from Booking b
            where b.roomType = :type and b.status in :statuses
              and b.checkInDate < :to and b.checkOutDate > :from
            """)
    long countOverlapping(@Param("type") RoomType type,
                          @Param("from") LocalDate from,
                          @Param("to") LocalDate to,
                          @Param("statuses") Collection<BookingStatus> statuses);

    /** Rooms already assigned to an active booking overlapping [from, to). */
    @Query("""
            select b.room from Booking b
            where b.room is not null and b.status in :statuses
              and b.checkInDate < :to and b.checkOutDate > :from
              and b.id <> :excludeId
            """)
    List<Room> findAssignedRooms(@Param("from") LocalDate from,
                                 @Param("to") LocalDate to,
                                 @Param("statuses") Collection<BookingStatus> statuses,
                                 @Param("excludeId") Long excludeId);
}
