package com.irose.olivehotel.service;

import com.irose.olivehotel.config.HotelProperties;
import com.irose.olivehotel.domain.*;
import com.irose.olivehotel.repository.*;
import com.irose.olivehotel.web.BookingForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class BookingService {

    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BookingRepository bookings;
    private final RoomTypeRepository roomTypes;
    private final RoomRepository rooms;
    private final GuestRepository guests;
    private final InvoiceRepository invoices;
    private final HotelProperties hotel;

    public BookingService(BookingRepository bookings, RoomTypeRepository roomTypes, RoomRepository rooms,
                          GuestRepository guests, InvoiceRepository invoices, HotelProperties hotel) {
        this.bookings = bookings;
        this.roomTypes = roomTypes;
        this.rooms = rooms;
        this.guests = guests;
        this.invoices = invoices;
        this.hotel = hotel;
    }

    @Transactional(readOnly = true)
    public long availableCount(RoomType type, LocalDate from, LocalDate to) {
        long total = rooms.countByRoomType(type);
        long booked = bookings.countOverlapping(type, from, to, BookingStatus.ACTIVE);
        return Math.max(0, total - booked);
    }

    public Booking create(BookingForm form) {
        LocalDate in = form.getCheckInDate();
        LocalDate out = form.getCheckOutDate();
        if (!out.isAfter(in)) {
            throw new BookingException("Check-out date must be after check-in date.");
        }
        if (in.isBefore(LocalDate.now())) {
            throw new BookingException("Check-in date cannot be in the past.");
        }
        RoomType type = roomTypes.findById(form.getRoomTypeId())
                .orElseThrow(() -> new BookingException("Selected room type was not found."));
        if (form.getAdults() + form.getChildren() > type.getMaxGuests()) {
            throw new BookingException(type.getName() + " allows at most " + type.getMaxGuests() + " guests.");
        }
        if (availableCount(type, in, out) < 1) {
            throw new BookingException("Sorry, no " + type.getName() + " rooms are available for those dates.");
        }

        Guest guest = guests.findFirstByPhone(form.getPhone().trim()).orElseGet(Guest::new);
        guest.setFullName(form.getFullName().trim());
        guest.setPhone(form.getPhone().trim());
        if (form.getEmail() != null && !form.getEmail().isBlank()) {
            guest.setEmail(form.getEmail().trim());
        }
        if (form.getCity() != null && !form.getCity().isBlank()) {
            guest.setCity(form.getCity().trim());
        }
        guest = guests.save(guest);

        Booking booking = new Booking();
        booking.setBookingCode(newBookingCode());
        booking.setGuest(guest);
        booking.setRoomType(type);
        booking.setCheckInDate(in);
        booking.setCheckOutDate(out);
        booking.setAdults(form.getAdults());
        booking.setChildren(form.getChildren());
        booking.setNightlyRate(type.getPricePerNight());
        booking.setSpecialRequests(form.getSpecialRequests());
        booking.setSource(form.getSource());
        // Front-desk bookings are confirmed immediately; website requests wait for staff confirmation.
        booking.setStatus(form.getSource() == BookingSource.WEBSITE ? BookingStatus.PENDING : BookingStatus.CONFIRMED);
        return bookings.save(booking);
    }

    public void confirm(Long id) {
        Booking b = get(id);
        requireStatus(b, BookingStatus.PENDING);
        b.setStatus(BookingStatus.CONFIRMED);
    }

    public void cancel(Long id) {
        Booking b = get(id);
        if (b.getStatus() != BookingStatus.PENDING && b.getStatus() != BookingStatus.CONFIRMED) {
            throw new BookingException("Only pending or confirmed bookings can be cancelled.");
        }
        b.setStatus(BookingStatus.CANCELLED);
        b.setRoom(null);
    }

    /** Rooms of the booking's type that are free for its dates and ready for a guest. */
    @Transactional(readOnly = true)
    public List<Room> assignableRooms(Booking b) {
        List<Room> taken = bookings.findAssignedRooms(b.getCheckInDate(), b.getCheckOutDate(),
                BookingStatus.ACTIVE, b.getId());
        return rooms.findByRoomTypeOrderByRoomNumberAsc(b.getRoomType()).stream()
                .filter(r -> r.getStatus() == RoomStatus.AVAILABLE)
                .filter(r -> taken.stream().noneMatch(t -> t.getId().equals(r.getId())))
                .toList();
    }

    public void checkIn(Long id, Long roomId, String idProofType, String idProofNumber) {
        Booking b = get(id);
        if (b.getStatus() != BookingStatus.CONFIRMED && b.getStatus() != BookingStatus.PENDING) {
            throw new BookingException("Only pending or confirmed bookings can be checked in.");
        }
        Room room = assignableRooms(b).stream().filter(r -> r.getId().equals(roomId)).findFirst()
                .orElseThrow(() -> new BookingException("That room is not available for this booking."));
        b.setRoom(room);
        b.setStatus(BookingStatus.CHECKED_IN);
        b.setActualCheckIn(LocalDateTime.now());
        room.setStatus(RoomStatus.OCCUPIED);
        Guest g = b.getGuest();
        if (idProofType != null && !idProofType.isBlank()) {
            g.setIdProofType(idProofType.trim());
        }
        if (idProofNumber != null && !idProofNumber.isBlank()) {
            g.setIdProofNumber(idProofNumber.trim());
        }
    }

    public void addCharge(Long id, String description, BigDecimal amount) {
        Booking b = get(id);
        requireStatus(b, BookingStatus.CHECKED_IN);
        if (description == null || description.isBlank() || amount == null || amount.signum() <= 0) {
            throw new BookingException("Enter a description and an amount greater than zero.");
        }
        ExtraCharge c = new ExtraCharge();
        c.setBooking(b);
        c.setDescription(description.trim());
        c.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        b.getExtraCharges().add(c);
    }

    public void removeCharge(Long id, Long chargeId) {
        Booking b = get(id);
        requireStatus(b, BookingStatus.CHECKED_IN);
        b.getExtraCharges().removeIf(c -> c.getId().equals(chargeId));
    }

    public Invoice checkOut(Long id, BigDecimal discount, String paymentMethod) {
        Booking b = get(id);
        requireStatus(b, BookingStatus.CHECKED_IN);
        BigDecimal disc = discount == null ? BigDecimal.ZERO : discount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = b.roomCharges().add(b.extraChargesTotal());
        if (disc.signum() < 0 || disc.compareTo(subtotal) > 0) {
            throw new BookingException("Discount must be between 0 and the bill subtotal.");
        }

        BigDecimal gstRate = hotel.gst().rateFor(b.getNightlyRate());
        BigDecimal taxable = subtotal.subtract(disc);
        BigDecimal gst = taxable.multiply(gstRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        Invoice inv = new Invoice();
        inv.setInvoiceNumber(String.format("INV-%d-%05d", LocalDate.now().getYear(), invoices.count() + 1));
        inv.setBooking(b);
        inv.setRoomCharges(b.roomCharges().setScale(2, RoundingMode.HALF_UP));
        inv.setExtraCharges(b.extraChargesTotal().setScale(2, RoundingMode.HALF_UP));
        inv.setDiscount(disc);
        inv.setGstRate(gstRate);
        inv.setGstAmount(gst);
        inv.setTotal(taxable.add(gst).setScale(2, RoundingMode.HALF_UP));
        inv.setPaymentMethod(paymentMethod);

        b.setStatus(BookingStatus.CHECKED_OUT);
        b.setActualCheckOut(LocalDateTime.now());
        if (b.getRoom() != null) {
            b.getRoom().setStatus(RoomStatus.CLEANING);
        }
        return invoices.save(inv);
    }

    @Transactional(readOnly = true)
    public Booking get(Long id) {
        return bookings.findById(id).orElseThrow(() -> new BookingException("Booking not found."));
    }

    private void requireStatus(Booking b, BookingStatus expected) {
        if (b.getStatus() != expected) {
            throw new BookingException("This action needs a " + expected.name().replace('_', ' ').toLowerCase()
                    + " booking (current status: " + b.getStatus().name().replace('_', ' ').toLowerCase() + ").");
        }
    }

    private String newBookingCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder("OLV").append(LocalDate.now().format(CODE_DATE));
            for (int i = 0; i < 4; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (bookings.findByBookingCode(code).isPresent());
        return code;
    }
}
