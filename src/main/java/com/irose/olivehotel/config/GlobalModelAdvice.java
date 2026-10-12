package com.irose.olivehotel.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Makes hotel details (name, phone, address...) available to every template as ${hotel}. */
@ControllerAdvice
public class GlobalModelAdvice {

    private final HotelProperties hotel;

    public GlobalModelAdvice(HotelProperties hotel) {
        this.hotel = hotel;
    }

    @ModelAttribute("hotel")
    public HotelProperties hotel() {
        return hotel;
    }
}
