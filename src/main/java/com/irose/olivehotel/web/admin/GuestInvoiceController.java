package com.irose.olivehotel.web.admin;

import com.irose.olivehotel.domain.Guest;
import com.irose.olivehotel.repository.BookingRepository;
import com.irose.olivehotel.repository.GuestRepository;
import com.irose.olivehotel.repository.InvoiceRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class GuestInvoiceController {

    private final GuestRepository guests;
    private final BookingRepository bookings;
    private final InvoiceRepository invoices;

    public GuestInvoiceController(GuestRepository guests, BookingRepository bookings, InvoiceRepository invoices) {
        this.guests = guests;
        this.bookings = bookings;
        this.invoices = invoices;
    }

    @GetMapping("/guests")
    public String guests(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("guests", q == null || q.isBlank()
                ? guests.findAll(Sort.by("fullName"))
                : guests.findByFullNameContainingIgnoreCaseOrPhoneContainingOrderByFullNameAsc(q.trim(), q.trim()));
        model.addAttribute("q", q);
        model.addAttribute("active", "guests");
        return "admin/guests";
    }

    @GetMapping("/guests/{id}")
    public String guest(@PathVariable Long id, Model model) {
        Guest g = guests.findById(id).orElseThrow();
        model.addAttribute("guest", g);
        model.addAttribute("bookings", bookings.findByGuestOrderByCheckInDateDesc(g));
        model.addAttribute("active", "guests");
        return "admin/guest-detail";
    }

    @GetMapping("/invoices")
    public String invoices(Model model) {
        model.addAttribute("invoices", invoices.findAllByOrderByIssuedAtDesc());
        model.addAttribute("active", "invoices");
        return "admin/invoices";
    }

    @GetMapping("/invoices/{id}")
    public String invoice(@PathVariable Long id, Model model) {
        model.addAttribute("invoice", invoices.findById(id).orElseThrow());
        return "admin/invoice";
    }
}
