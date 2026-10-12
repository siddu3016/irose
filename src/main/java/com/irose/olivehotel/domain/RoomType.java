package com.irose.olivehotel.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Entity
public class RoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    @NotNull
    @DecimalMin("0.0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerNight;

    /** Optional "was" price shown struck through next to the offer price. */
    @DecimalMin("0.0")
    @Column(precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Min(1)
    private int maxGuests = 2;

    private String bedType;

    private Integer sizeSqft;

    /** Comma-separated list, e.g. "Wi-Fi, AC, TV". */
    @Column(length = 1000)
    private String amenities;

    /** File name under /images/rooms/, e.g. "deluxe.jpg". */
    private String imageFile;

    public List<String> amenityList() {
        if (amenities == null || amenities.isBlank()) {
            return List.of();
        }
        return Arrays.stream(amenities.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public boolean hasOffer() {
        return originalPrice != null && pricePerNight != null && originalPrice.compareTo(pricePerNight) > 0;
    }

    public BigDecimal savings() {
        return hasOffer() ? originalPrice.subtract(pricePerNight) : BigDecimal.ZERO;
    }

    public int discountPercent() {
        return hasOffer() ? savings().multiply(BigDecimal.valueOf(100))
                .divide(originalPrice, 0, java.math.RoundingMode.HALF_UP).intValue() : 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(BigDecimal pricePerNight) { this.pricePerNight = pricePerNight; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public int getMaxGuests() { return maxGuests; }
    public void setMaxGuests(int maxGuests) { this.maxGuests = maxGuests; }
    public String getBedType() { return bedType; }
    public void setBedType(String bedType) { this.bedType = bedType; }
    public Integer getSizeSqft() { return sizeSqft; }
    public void setSizeSqft(Integer sizeSqft) { this.sizeSqft = sizeSqft; }
    public String getAmenities() { return amenities; }
    public void setAmenities(String amenities) { this.amenities = amenities; }
    public String getImageFile() { return imageFile; }
    public void setImageFile(String imageFile) { this.imageFile = imageFile; }
}
