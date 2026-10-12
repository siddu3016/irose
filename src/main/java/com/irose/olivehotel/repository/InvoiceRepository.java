package com.irose.olivehotel.repository;

import com.irose.olivehotel.domain.Booking;
import com.irose.olivehotel.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByBooking(Booking booking);

    List<Invoice> findAllByOrderByIssuedAtDesc();

    @Query("select coalesce(sum(i.total), 0) from Invoice i where i.issuedAt >= :from and i.issuedAt < :to")
    BigDecimal sumTotalBetween(LocalDateTime from, LocalDateTime to);
}
