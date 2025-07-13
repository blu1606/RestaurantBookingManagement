package restaurantbookingmanagement.view.dto;

import java.time.LocalDateTime;

public class BookingRequest {
    private final String name;
    private final String phone;
    private final String email;
    private final int guests;
    private final LocalDateTime bookingTime;

    public BookingRequest(String name, String phone, String email, int guests, LocalDateTime bookingTime) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.guests = guests;
        this.bookingTime = bookingTime;
    }

    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public int getGuests() { return guests; }
    public LocalDateTime getBookingTime() { return bookingTime; }
} 