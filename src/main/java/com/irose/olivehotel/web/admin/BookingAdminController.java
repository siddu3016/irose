package com.irose.olivehotel.web.admin;

import com.irose.olivehotel.domain.Booking;
import com.irose.olivehotel.domain.BookingSource;
import com.irose.olivehotel.domain.BookingStatus;
import com.irose.olivehotel.repository.BookingRepository;
import com.irose.olivehotel.repository.InvoiceRepository;
import com.irose.olivehotel.repository.RoomTypeRepository;
import com.irose.olivehotel.service.BookingException;
import com.irose.olivehotel.service.BookingService;
import com.irose.olivehotel.web.BookingForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/bookings")
public class BookingAdminController {

    private final BookingRepository bookings;
    private final RoomTypeRepository roomTypes;
    private final InvoiceRepository invoices;
    private final BookingService service;

    public BookingAdminController(BookingRepository bookings, RoomTypeRepository roomTypes,
                                  InvoiceRepository invoices, BookingService service) {
        this.bookings = bookings;
        this.roomTypes = roomTypes;
        this.invoices = invoices;
        this.service = service;
    }

    @ModelAttribute("active")
    String active() {
        return "bookings";
    }

    @GetMapping
    public String list(@RequestParam(required = false) BookingStatus status, Model model) {
        List<Booking> list = status == null ? bookings.findAllByOrderByCreatedAtDesc()
                : bookings.findByStatusOrderByCheckInDateAsc(status);
        model.addAttribute("bookings", list);
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("selectedStatus", status);
        return "admin/bookings";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        BookingForm form = new BookingForm();
        form.setCheckInDate(LocalDate.now());
        form.setCheckOutDate(LocalDate.now().plusDays(1));
        form.setSource(BookingSource.WALK_IN);
        model.addAttribute("bookingForm", form);
        return formPage(model);
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("bookingForm") BookingForm form, BindingResult result,
                         Model model, RedirectAttributes flash) {
        if (result.hasErrors()) {
            return formPage(model);
        }
        try {
            Booking b = service.create(form);
            flash.addFlashAttribute("success", "Booking " + b.getBookingCode() + " created.");
            return "redirect:/admin/bookings/" + b.getId();
        } catch (BookingException e) {
            model.addAttribute("error", e.getMessage());
            return formPage(model);
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Booking b = service.get(id);
        model.addAttribute("booking", b);
        model.addAttribute("assignableRooms", service.assignableRooms(b));
        invoices.findByBooking(b).ifPresent(inv -> model.addAttribute("invoice", inv));
        return "admin/booking-detail";
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable Long id, RedirectAttributes flash) {
        return run(id, flash, "Booking confirmed.", () -> service.confirm(id));
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes flash) {
        return run(id, flash, "Booking cancelled.", () -> service.cancel(id));
    }

    @PostMapping("/{id}/checkin")
    public String checkIn(@PathVariable Long id, @RequestParam Long roomId,
                          @RequestParam(required = false) String idProofType,
                          @RequestParam(required = false) String idProofNumber, RedirectAttributes flash) {
        return run(id, flash, "Guest checked in.", () -> service.checkIn(id, roomId, idProofType, idProofNumber));
    }

    @PostMapping("/{id}/charges")
    public String addCharge(@PathVariable Long id, @RequestParam String description,
                            @RequestParam BigDecimal amount, RedirectAttributes flash) {
        return run(id, flash, "Charge added.", () -> service.addCharge(id, description, amount));
    }

    @PostMapping("/{id}/charges/{chargeId}/delete")
    public String removeCharge(@PathVariable Long id, @PathVariable Long chargeId, RedirectAttributes flash) {
        return run(id, flash, "Charge removed.", () -> service.removeCharge(id, chargeId));
    }

    @PostMapping("/{id}/checkout")
    public String checkOut(@PathVariable Long id, @RequestParam(required = false) BigDecimal discount,
                           @RequestParam String paymentMethod, RedirectAttributes flash) {
        try {
            Long invoiceId = service.checkOut(id, discount, paymentMethod).getId();
            flash.addFlashAttribute("success", "Guest checked out. Invoice generated.");
            return "redirect:/admin/invoices/" + invoiceId;
        } catch (BookingException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/bookings/" + id;
        }
    }

    private String run(Long id, RedirectAttributes flash, String successMessage, Runnable action) {
        try {
            action.run();
            flash.addFlashAttribute("success", successMessage);
        } catch (BookingException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/bookings/" + id;
    }

    private String formPage(Model model) {
        model.addAttribute("roomTypes", roomTypes.findAllByOrderByPricePerNightAsc());
        model.addAttribute("sources", BookingSource.values());
        return "admin/booking-form";
    }
}
