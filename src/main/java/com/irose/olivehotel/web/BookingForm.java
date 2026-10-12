package com.irose.olivehotel.web;

import com.irose.olivehotel.domain.BookingSource;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Data entered on the public booking page or the front-desk "new booking" page. */
public class BookingForm {

    @NotNull(message = "Choose a room type")
    private Long roomTypeId;

    @NotNull(message = "Choose a check-in date")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkInDate;

    @NotNull(message = "Choose a check-out date")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkOutDate;

    @Min(value = 1, message = "At least 1 adult")
    private int adults = 1;

    @Min(0)
    private int children;

    @NotBlank(message = "Enter your name")
    private String fullName;

    @NotBlank(message = "Enter your phone number")
    @Pattern(regexp = "[0-9+ -]{10,15}", message = "Enter a valid phone number")
    private String phone;

    @Email(message = "Enter a valid email")
    private String email;

    private String city;

    @Size(max = 1000)
    private String specialRequests;

    private BookingSource source = BookingSource.WEBSITE;

    public Long getRoomTypeId() { return roomTypeId; }
    public void setRoomTypeId(Long roomTypeId) { this.roomTypeId = roomTypeId; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; }
    public int getAdults() { return adults; }
    public void setAdults(int adults) { this.adults = adults; }
    public int getChildren() { return children; }
    public void setChildren(int children) { this.children = children; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getSpecialRequests() { return specialRequests; }
    public void setSpecialRequests(String specialRequests) { this.specialRequests = specialRequests; }
    public BookingSource getSource() { return source; }
    public void setSource(BookingSource source) { this.source = source; }
}
