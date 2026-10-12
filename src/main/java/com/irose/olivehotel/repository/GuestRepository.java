package com.irose.olivehotel.repository;

import com.irose.olivehotel.domain.Guest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GuestRepository extends JpaRepository<Guest, Long> {

    Optional<Guest> findFirstByPhone(String phone);

    List<Guest> findByFullNameContainingIgnoreCaseOrPhoneContainingOrderByFullNameAsc(String name, String phone);
}
