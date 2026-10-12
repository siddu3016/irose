package com.irose.olivehotel;

import com.irose.olivehotel.domain.*;
import com.irose.olivehotel.repository.BookingRepository;
import com.irose.olivehotel.repository.RoomRepository;
import com.irose.olivehotel.repository.RoomTypeRepository;
import com.irose.olivehotel.service.BookingException;
import com.irose.olivehotel.service.BookingService;
import com.irose.olivehotel.web.BookingForm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class HotelFlowTest {

    @Autowired MockMvc mvc;
    @Autowired BookingService service;
    @Autowired RoomTypeRepository roomTypes;
    @Autowired RoomRepository rooms;
    @Autowired BookingRepository bookings;

    private RoomType standard() {
        return roomTypes.findAllByOrderByPricePerNightAsc().get(0);
    }

    private BookingForm form(RoomType type, LocalDate in, LocalDate out) {
        BookingForm f = new BookingForm();
        f.setRoomTypeId(type.getId());
        f.setCheckInDate(in);
        f.setCheckOutDate(out);
        f.setAdults(2);
        f.setFullName("Test Guest");
        f.setPhone("9000000001");
        return f;
    }

    @Test
    void fullStayProducesCorrectInvoice() {
        RoomType type = standard();
        LocalDate today = LocalDate.now();
        Booking b = service.create(form(type, today, today.plusDays(2)));
        assertThat(b.getStatus()).isEqualTo(BookingStatus.PENDING);

        service.confirm(b.getId());
        Room room = service.assignableRooms(service.get(b.getId())).get(0);
        service.checkIn(b.getId(), room.getId(), "Passport", "X0000000");
        assertThat(rooms.findById(room.getId()).orElseThrow().getStatus()).isEqualTo(RoomStatus.OCCUPIED);

        service.addCharge(b.getId(), "Breakfast", new BigDecimal("300"));
        Invoice inv = service.checkOut(b.getId(), new BigDecimal("100"), "UPI");

        // 2 nights x 1250 + 300 - 100 = 2700; GST 5% = 135.00
        assertThat(inv.getRoomCharges()).isEqualByComparingTo("2500");
        assertThat(inv.getGstRate()).isEqualByComparingTo("5");
        assertThat(inv.getGstAmount()).isEqualByComparingTo("135.00");
        assertThat(inv.getTotal()).isEqualByComparingTo("2835.00");
        assertThat(rooms.findById(room.getId()).orElseThrow().getStatus()).isEqualTo(RoomStatus.CLEANING);
    }

    @Test
    void roomTypesShowOffers() throws Exception {
        assertThat(roomTypes.count()).isEqualTo(2);
        RoomType std = standard();
        assertThat(std.getPricePerNight()).isEqualByComparingTo("1250");
        assertThat(std.savings()).isEqualByComparingTo("500");
        assertThat(std.discountPercent()).isEqualTo(29);
        mvc.perform(get("/")).andExpect(content().string(org.hamcrest.Matchers.containsString("29% OFF")));
    }

    @Test
    void cannotOverbookARoomType() {
        RoomType type = standard();
        long count = rooms.countByRoomType(type);
        LocalDate in = LocalDate.now().plusDays(10);
        for (int i = 0; i < count; i++) {
            service.create(form(type, in, in.plusDays(1)));
        }
        assertThat(service.availableCount(type, in, in.plusDays(1))).isZero();
        assertThatThrownBy(() -> service.create(form(type, in, in.plusDays(1))))
                .isInstanceOf(BookingException.class);
        // Non-overlapping dates are still bookable.
        assertThat(service.availableCount(type, in.plusDays(1), in.plusDays(2))).isEqualTo(count);
    }

    @Test
    void rejectsInvalidDates() {
        LocalDate d = LocalDate.now().plusDays(3);
        assertThatThrownBy(() -> service.create(form(standard(), d, d))).isInstanceOf(BookingException.class);
        assertThatThrownBy(() -> service.create(form(standard(), d.minusDays(10), d))).isInstanceOf(BookingException.class);
    }

    @Test
    void publicPagesRender() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/rooms")).andExpect(status().isOk());
        mvc.perform(get("/book")).andExpect(status().isOk());
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void publicBookingRedirectsToConfirmation() throws Exception {
        LocalDate in = LocalDate.now().plusDays(1);
        String location = mvc.perform(post("/book").with(csrf())
                        .param("roomTypeId", standard().getId().toString())
                        .param("checkInDate", in.toString())
                        .param("checkOutDate", in.plusDays(1).toString())
                        .param("adults", "1").param("children", "0")
                        .param("fullName", "Web Guest").param("phone", "9000000002"))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        assertThat(location).startsWith("/booking/OLV");
        mvc.perform(get(location)).andExpect(status().isOk());
    }

    @Test
    void adminRequiresLogin() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPagesRender() throws Exception {
        LocalDate today = LocalDate.now();
        Booking b = service.create(form(standard(), today, today.plusDays(1)));
        Room room = service.assignableRooms(b).get(0);
        service.checkIn(b.getId(), room.getId(), null, null);
        service.addCharge(b.getId(), "Laundry", new BigDecimal("150"));

        for (String url : new String[]{"/admin", "/admin/bookings", "/admin/bookings?status=CHECKED_IN",
                "/admin/bookings/new", "/admin/bookings/" + b.getId(), "/admin/rooms", "/admin/room-types",
                "/admin/room-types/new", "/admin/room-types/" + standard().getId(), "/admin/guests",
                "/admin/guests/" + b.getGuest().getId()}) {
            mvc.perform(get(url)).andExpect(status().isOk());
        }

        Invoice inv = service.checkOut(b.getId(), BigDecimal.ZERO, "Cash");
        mvc.perform(get("/admin/invoices")).andExpect(status().isOk());
        mvc.perform(get("/admin/invoices/" + inv.getId())).andExpect(status().isOk());
        mvc.perform(get("/admin/bookings/" + b.getId())).andExpect(status().isOk());
    }
}
