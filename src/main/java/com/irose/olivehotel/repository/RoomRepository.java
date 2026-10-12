package com.irose.olivehotel.repository;

import com.irose.olivehotel.domain.Room;
import com.irose.olivehotel.domain.RoomStatus;
import com.irose.olivehotel.domain.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findAllByOrderByRoomNumberAsc();

    List<Room> findByRoomTypeOrderByRoomNumberAsc(RoomType roomType);

    long countByRoomType(RoomType roomType);

    long countByStatus(RoomStatus status);

    boolean existsByRoomNumber(String roomNumber);
}
