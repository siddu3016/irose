package com.irose.olivehotel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "hotel")
public record HotelProperties(
        String name,
        String phone,
        String email,
        String address,
        String checkInTime,
        String checkOutTime,
        Gst gst,
        Admin admin) {

    public record Gst(BigDecimal threshold, BigDecimal rateAtOrBelow, BigDecimal rateAbove) {

        /** GST percentage applicable for a given per-night room tariff. */
        public BigDecimal rateFor(BigDecimal nightlyTariff) {
            return nightlyTariff.compareTo(threshold) <= 0 ? rateAtOrBelow : rateAbove;
        }
    }

    public record Admin(String username, String password) {
    }

    /** Phone number formatted for WhatsApp links (India country code). */
    public String whatsappNumber() {
        String digits = phone.replaceAll("\\D", "");
        return digits.length() == 10 ? "91" + digits : digits;
    }
}
