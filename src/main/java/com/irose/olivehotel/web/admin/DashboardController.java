package com.irose.olivehotel.web.admin;

import com.irose.olivehotel.domain.BookingStatus;
import com.irose.olivehotel.domain.RoomStatus;
import com.irose.olivehotel.repository.BookingRepository;
import com.irose.olivehotel.repository.InvoiceRepository;
import com.irose.olivehotel.repository.RoomRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;

@Controller
public class DashboardController {

    private final RoomRepository rooms;
    private final BookingRepository bookings;
    private final InvoiceRepository invoices;

    public DashboardController(RoomRepository rooms, BookingRepository bookings, InvoiceRepository invoices) {
        this.rooms = rooms;
        this.bookings = bookings;
        this.invoices = invoices;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        LocalDate today = LocalDate.now();
        model.addAttribute("totalRooms", rooms.count());
        model.addAttribute("available", rooms.countByStatus(RoomStatus.AVAILABLE));
        model.addAttribute("occupied", rooms.countByStatus(RoomStatus.OCCUPIED));
        model.addAttribute("cleaning", rooms.countByStatus(RoomStatus.CLEANING));
        model.addAttribute("maintenance", rooms.countByStatus(RoomStatus.MAINTENANCE));
        model.addAttribute("pending", bookings.findByStatusOrderByCheckInDateAsc(BookingStatus.PENDING));
        model.addAttribute("arrivals", bookings.findByCheckInDateAndStatusIn(today,
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)));
        model.addAttribute("departures", bookings.findByCheckOutDateAndStatus(today, BookingStatus.CHECKED_IN));
        model.addAttribute("inHouse", bookings.findByStatusOrderByCheckInDateAsc(BookingStatus.CHECKED_IN));
        model.addAttribute("revenueToday", invoices.sumTotalBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay()));
        model.addAttribute("revenueMonth", invoices.sumTotalBetween(today.withDayOfMonth(1).atStartOfDay(),
                today.withDayOfMonth(1).plusMonths(1).atStartOfDay()));
        model.addAttribute("rooms", rooms.findAllByOrderByRoomNumberAsc());
        model.addAttribute("active", "dashboard");
        return "admin/dashboard";
    }
}
