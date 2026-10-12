package com.irose.olivehotel.service;

/** A business-rule violation whose message is safe to show to the user. */
public class BookingException extends RuntimeException {

    public BookingException(String message) {
        super(message);
    }
}
