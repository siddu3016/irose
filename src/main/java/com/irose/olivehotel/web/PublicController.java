package com.irose.olivehotel.web;

import com.irose.olivehotel.domain.BookingSource;
import com.irose.olivehotel.domain.RoomType;
import com.irose.olivehotel.repository.BookingRepository;
import com.irose.olivehotel.repository.RoomTypeRepository;
import com.irose.olivehotel.service.BookingException;
import com.irose.olivehotel.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class PublicController {

    private final RoomTypeRepository roomTypes;
    private final BookingRepository bookings;
    private final BookingService bookingService;

    public PublicController(RoomTypeRepository roomTypes, BookingRepository bookings, BookingService bookingService) {
        this.roomTypes = roomTypes;
        this.bookings = bookings;
        this.bookingService = bookingService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("roomTypes", roomTypes.findAllByOrderByPricePerNightAsc());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("tomorrow", LocalDate.now().plusDays(1));
        return "public/home";
    }

    @GetMapping("/rooms")
    public String rooms(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
                        Model model) {
        LocalDate in = checkIn != null && !checkIn.isBefore(LocalDate.now()) ? checkIn : LocalDate.now();
        LocalDate out = checkOut != null && checkOut.isAfter(in) ? checkOut : in.plusDays(1);
        Map<RoomType, Long> availability = new LinkedHashMap<>();
        for (RoomType t : roomTypes.findAllByOrderByPricePerNightAsc()) {
            availability.put(t, bookingService.availableCount(t, in, out));
        }
        model.addAttribute("availability", availability);
        model.addAttribute("checkIn", in);
        model.addAttribute("checkOut", out);
        model.addAttribute("today", LocalDate.now());
        return "public/rooms";
    }

    @GetMapping("/book")
    public String bookForm(@RequestParam(required = false) Long roomTypeId,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
                           Model model) {
        BookingForm form = new BookingForm();
        form.setRoomTypeId(roomTypeId);
        form.setCheckInDate(checkIn != null ? checkIn : LocalDate.now());
        form.setCheckOutDate(checkOut != null ? checkOut : form.getCheckInDate().plusDays(1));
        model.addAttribute("bookingForm", form);
        return bookPage(model);
    }

    @PostMapping("/book")
    public String book(@Valid @ModelAttribute("bookingForm") BookingForm form, BindingResult result, Model model) {
        form.setSource(BookingSource.WEBSITE);
        if (result.hasErrors()) {
            return bookPage(model);
        }
        try {
            return "redirect:/booking/" + bookingService.create(form).getBookingCode();
        } catch (BookingException e) {
            model.addAttribute("error", e.getMessage());
            return bookPage(model);
        }
    }

    @GetMapping("/booking/{code}")
    public String confirmation(@PathVariable String code, Model model) {
        model.addAttribute("booking", bookings.findByBookingCode(code)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND)));
        return "public/confirmation";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    private String bookPage(Model model) {
        model.addAttribute("roomTypes", roomTypes.findAllByOrderByPricePerNightAsc());
        model.addAttribute("today", LocalDate.now());
        return "public/book";
    }
}
