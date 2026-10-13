# iRose Olive Business Hotel – Hotel Management System

Spring Boot 3 (Java 17) web application with a public hotel website and a front-office admin panel.

## Features

**Public website** (`http://localhost:8080`)
- Home page: hero, about, rooms, amenities, gallery, contact with map
- Live room availability search by dates
- Online booking request form, which gives the guest a booking reference
- Click-to-call and WhatsApp buttons (8374661394)

**Admin panel** (`http://localhost:8080/admin`)
- Dashboard: room counts, today's arrivals and departures, pending requests, in-house guests, revenue, colour-coded room board
- Bookings: create walk-in, phone or OTA bookings; confirm, cancel, check in (assign a room and record ID proof)
- Extra charges during a stay (food, laundry, extra bed…)
- Check-out with discount and payment method, which generates a printable GST tax invoice (CGST/SGST split)
- Rooms: add or delete rooms, set status (available, occupied, cleaning, maintenance)
- Room types & rates: edit prices, capacity, amenities and photos
- Guests: search and view stay history
- Invoices list

## Running

Open the folder in IntelliJ IDEA and run `OliveHotelApplication`, or from a terminal with Maven installed:

```
mvn spring-boot:run
```

Then open http://localhost:8080. Staff login is at http://localhost:8080/login.

Default login: `admin` / `admin123`. **Change it** in `src/main/resources/application.properties` before real use.

Data is stored in an H2 database file under `./data/`. The first run creates 3 room types and 12 rooms (101–104, 201–204, 301–304). Edit them from the admin panel.

## Configuration

Edit `src/main/resources/application.properties` to set:
- Hotel name, phone, email, address, check-in and check-out times
- GST rates and the tariff threshold. **Confirm these with your accountant.**
- Admin username and password

## Photos

See `src/main/resources/static/images/README.txt`.
