package restaurantbookingmanagement.service.validator;

import restaurantbookingmanagement.model.Booking;

public class BookingValidator {
    public static boolean isValid(Booking booking) {
        return booking != null &&
               booking.getCustomer() != null &&
               booking.getTable() != null &&
               booking.getBookingTime() != null &&
               booking.getNumberOfGuests() > 0 &&
               (booking.getStatus().equals("CONFIRMED") || booking.getStatus().equals("CANCELLED") || booking.getStatus().equals("COMPLETED"));
    }
    public static String validate(Booking booking) {
        if (booking == null) return "Booking is null";
        if (booking.getCustomer() == null) return "Customer is required";
        if (booking.getTable() == null) return "Table is required";
        if (booking.getBookingTime() == null) return "Booking time is required";
        if (booking.getNumberOfGuests() <= 0) return "Number of guests must be > 0";
        if (!(booking.getStatus().equals("CONFIRMED") || booking.getStatus().equals("CANCELLED") || booking.getStatus().equals("COMPLETED"))) return "Invalid status";
        return null;
    }
} 