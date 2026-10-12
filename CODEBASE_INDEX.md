# Codebase Index – iRose Olive Business Hotel

A map of every file in the project and what it does.

## 1. Folder structure

```
olive bussiness hotel/
├── pom.xml                          Maven build file (dependencies)
├── README.md                        How to run and configure
├── CODEBASE_INDEX.md                This file
├── data/                            H2 database file (created on first run)
└── src/
    ├── main/
    │   ├── java/com/irose/olivehotel/
    │   │   ├── OliveHotelApplication.java   Starting point (main method)
    │   │   ├── config/        Settings, security, starter data
    │   │   ├── domain/        Database tables (entities) and enums
    │   │   ├── repository/    Database queries
    │   │   ├── service/       Business rules (booking, check-in/out, billing)
    │   │   └── web/           Controllers (URLs → pages)
    │   │       └── admin/     Staff panel controllers
    │   └── resources/
    │       ├── application.properties   Hotel name, phone, GST, admin login
    │       ├── static/        CSS and images
    │       └── templates/     HTML pages (Thymeleaf)
    │           ├── fragments/ Shared parts (header, footer, sidebar)
    │           ├── public/    Guest website pages
    │           └── admin/     Staff panel pages
    └── test/                  Automated tests
```

## 2. How a request flows

```
Browser → Controller (web/) → Service (service/) → Repository (repository/) → H2 Database
                ↓
        Template (templates/) → HTML page back to browser
```

## 3. Java files

### Startup & configuration — `config/`

| File | Purpose |
|---|---|
| `OliveHotelApplication.java` | Main class. Run this to start the app. |
| `config/HotelProperties.java` | Reads the `hotel.*` settings (name, phone, address, GST rates, admin login). Also builds the WhatsApp number. |
| `config/SecurityConfig.java` | Login setup. `/admin/**` needs a login and everything else is public. |
| `config/GlobalModelAdvice.java` | Makes `${hotel}` (name, phone…) available on every page. |
| `config/DataInitializer.java` | On first run, creates 3 room types and 12 rooms. |

### Database tables — `domain/`

| File | Table / Type | Key fields |
|---|---|---|
| `RoomType.java` | Room category | name, pricePerNight, maxGuests, bedType, sizeSqft, amenities, imageFile |
| `Room.java` | Physical room | roomNumber, floor, roomType, status |
| `RoomStatus.java` | Enum | AVAILABLE, OCCUPIED, CLEANING, MAINTENANCE |
| `Guest.java` | Guest | fullName, phone, email, city, idProofType, idProofNumber |
| `Booking.java` | Reservation | bookingCode, guest, roomType, room, dates, adults/children, nightlyRate, status, source, extraCharges |
| `BookingStatus.java` | Enum | PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT, or CANCELLED |
| `BookingSource.java` | Enum | WEBSITE, WALK_IN, PHONE, OTA |
| `ExtraCharge.java` | Extra charge on a stay | description, amount (food, laundry…) |
| `Invoice.java` | Final bill | invoiceNumber, roomCharges, extraCharges, discount, gstRate, gstAmount, total, paymentMethod |

### Database access — `repository/`

| File | Purpose |
|---|---|
| `RoomTypeRepository.java` | List room types sorted by price |
| `RoomRepository.java` | Rooms by type or status, plus counts |
| `GuestRepository.java` | Find a guest by phone, search by name or phone |
| `BookingRepository.java` | Find by code or status, today's arrivals and departures, **overlap queries for availability** |
| `InvoiceRepository.java` | Invoice list and revenue totals |
| `ExtraChargeRepository.java` | Basic CRUD for extra charges |

### Business logic — `service/`

| File | Purpose |
|---|---|
| `BookingService.java` | **Core of the system.** Availability check, create booking, confirm, cancel, check-in (room assignment), add or remove charges, check-out and invoice/GST calculation. |
| `BookingException.java` | Error with a user-friendly message (e.g. "No rooms available"). |

### Controllers — `web/`

| File | URLs | Purpose |
|---|---|---|
| `web/PublicController.java` | `/`, `/rooms`, `/book`, `/booking/{code}`, `/login` | Guest website |
| `web/BookingForm.java` | — | Form data and validation for a new booking |
| `web/admin/DashboardController.java` | `/admin` | Dashboard stats and room board |
| `web/admin/BookingAdminController.java` | `/admin/bookings/**` | List, create, confirm, cancel, check-in, charges, check-out |
| `web/admin/RoomAdminController.java` | `/admin/rooms/**`, `/admin/room-types/**` | Manage rooms, statuses, room types and prices |
| `web/admin/GuestInvoiceController.java` | `/admin/guests/**`, `/admin/invoices/**` | Guest search and history, invoice list and print view |

## 4. HTML pages — `templates/`

| Template | URL | What it shows |
|---|---|---|
| `fragments/layout.html` | — | Shared head, navbar, footer, call/WhatsApp buttons, admin sidebar, alerts, status badge |
| `fragments/booking-fields.html` | — | Booking form fields (used by both public and admin forms) |
| `public/home.html` | `/` | Hero, availability search, about, rooms, amenities, gallery, contact and map |
| `public/rooms.html` | `/rooms` | Room types with free-room count for the chosen dates |
| `public/book.html` | `/book` | Online booking request form |
| `public/confirmation.html` | `/booking/{code}` | Booking reference and summary |
| `login.html` | `/login` | Staff login |
| `admin/dashboard.html` | `/admin` | Stats, pending requests, arrivals and departures, room board, in-house guests |
| `admin/bookings.html` | `/admin/bookings` | All bookings with status filter |
| `admin/booking-form.html` | `/admin/bookings/new` | Front-desk booking |
| `admin/booking-detail.html` | `/admin/bookings/{id}` | Booking info, check-in, charges, check-out |
| `admin/rooms.html` | `/admin/rooms` | Add rooms, change status, delete |
| `admin/room-types.html` | `/admin/room-types` | Room types and prices |
| `admin/room-type-form.html` | `/admin/room-types/new`, `/{id}` | Add or edit a room type |
| `admin/guests.html` | `/admin/guests` | Guest search |
| `admin/guest-detail.html` | `/admin/guests/{id}` | Guest details and stay history |
| `admin/invoices.html` | `/admin/invoices` | All invoices |
| `admin/invoice.html` | `/admin/invoices/{id}` | Printable GST tax invoice |

## 5. Static files — `static/`

| File | Purpose |
|---|---|
| `css/style.css` | Olive/gold theme, public and admin layout, print styles |
| `images/placeholder.svg` | Shown when a photo is missing |
| `images/README.txt` | Which photo file names to add (hero, about, rooms, gallery) |

## 6. Configuration & tests

| File | Purpose |
|---|---|
| `src/main/resources/application.properties` | Database location, hotel details, GST rates, admin username and password |
| `src/test/resources/application.properties` | In-memory database for tests |
| `src/test/java/.../HotelFlowTest.java` | 7 tests: full stay and invoice math, no overbooking, date validation, every page loads, admin login required |

## 7. Booking lifecycle

```
Website request ─► PENDING ──confirm──► CONFIRMED ──check-in──► CHECKED_IN ──check-out──► CHECKED_OUT
Front desk ───────────────────────────► CONFIRMED                 (room OCCUPIED)          (invoice created,
                    │                       │                                               room → CLEANING)
                    └───────cancel──────────┴──► CANCELLED
```

Room status: `AVAILABLE → OCCUPIED (check-in) → CLEANING (check-out) → AVAILABLE ("Mark clean")`
