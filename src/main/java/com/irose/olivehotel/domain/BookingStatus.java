package com.irose.olivehotel.domain;

public enum BookingStatus {
    PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED;

    /** Statuses that hold inventory for the booked dates. */
    public static final java.util.List<BookingStatus> ACTIVE = java.util.List.of(PENDING, CONFIRMED, CHECKED_IN);
}
