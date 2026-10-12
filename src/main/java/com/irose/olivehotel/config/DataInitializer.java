package com.irose.olivehotel.config;

import com.irose.olivehotel.domain.Room;
import com.irose.olivehotel.domain.RoomType;
import com.irose.olivehotel.repository.RoomRepository;
import com.irose.olivehotel.repository.RoomTypeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Creates starter room types and rooms the first time the app runs (empty database).
 * Prices and room numbers are placeholders - edit them from the admin panel.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final String ROOM_AMENITIES =
            "Air conditioning, Wi-Fi, TV, Attached bathroom, Closet, Chair, Mineral water (extra charge)";

    private final RoomTypeRepository roomTypes;
    private final RoomRepository rooms;

    public DataInitializer(RoomTypeRepository roomTypes, RoomRepository rooms) {
        this.roomTypes = roomTypes;
        this.rooms = rooms;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (roomTypes.count() > 0) {
            return;
        }
        RoomType standard = roomTypes.save(type("Standard Double Room",
                "Cosy air-conditioned room with a queen bed and attached bathroom, ideal for business travellers.",
                "1250", "1750", 2, "Queen bed", 129, "standard.jpg", ROOM_AMENITIES));
        RoomType deluxe = roomTypes.save(type("Deluxe Double Room",
                "Spacious air-conditioned room with a king bed, closet and attached bathroom.",
                "1499", "2199", 2, "King bed", 140, "deluxe.jpg", ROOM_AMENITIES));

        for (int floor = 1; floor <= 3; floor++) {
            for (int n = 1; n <= 4; n++) {
                RoomType t = floor == 1 ? standard : deluxe;
                Room r = new Room();
                r.setRoomNumber(floor + "0" + n);
                r.setFloor(floor);
                r.setRoomType(t);
                rooms.save(r);
            }
        }
    }

    private static RoomType type(String name, String desc, String price, String originalPrice, int maxGuests,
                                 String bed, int sqft, String image, String amenities) {
        RoomType t = new RoomType();
        t.setName(name);
        t.setDescription(desc);
        t.setPricePerNight(new BigDecimal(price));
        t.setOriginalPrice(new BigDecimal(originalPrice));
        t.setMaxGuests(maxGuests);
        t.setBedType(bed);
        t.setSizeSqft(sqft);
        t.setImageFile(image);
        t.setAmenities(amenities);
        return t;
    }
}
