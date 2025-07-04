package restaurantbookingmanagement.model;

import java.util.List;

/**
 * Class chứa thông tin khách hàng và lịch sử booking
 */
public class CustomerInfo {
    private Customer customer;
    private List<Booking> bookings;
    
    public CustomerInfo() {
    }
    
    public CustomerInfo(Customer customer, List<Booking> bookings) {
        this.customer = customer;
        this.bookings = bookings;
    }
    
    // Getters
    public Customer getCustomer() {
        return customer;
    }
    
    public List<Booking> getBookings() {
        return bookings;
    }
    
    // Setters
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
    
    public void setBookings(List<Booking> bookings) {
        this.bookings = bookings;
    }
    
    /**
     * Lấy số booking đang hoạt động (CONFIRMED)
     */
    public long getActiveBookingsCount() {
        return bookings.stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()))
                .count();
    }
    
    /**
     * Lấy số booking đã hủy
     */
    public long getCancelledBookingsCount() {
        return bookings.stream()
                .filter(b -> "CANCELLED".equals(b.getStatus()))
                .count();
    }
    
    /**
     * Lấy số booking đã hoàn thành
     */
    public long getCompletedBookingsCount() {
        return bookings.stream()
                .filter(b -> "COMPLETED".equals(b.getStatus()))
                .count();
    }
    
    /**
     * Lấy booking gần nhất
     */
    public Booking getLatestBooking() {
        return bookings.stream()
                .max((b1, b2) -> b1.getBookingTime().compareTo(b2.getBookingTime()))
                .orElse(null);
    }
    
    /**
     * Lấy booking đang hoạt động (CONFIRMED)
     */
    public List<Booking> getActiveBookings() {
        return bookings.stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()))
                .collect(java.util.stream.Collectors.toList());
    }
    
    @Override
    public String toString() {
        return "CustomerInfo{" +
                "customer=" + customer +
                ", bookingsCount=" + bookings.size() +
                ", activeBookings=" + getActiveBookingsCount() +
                '}';
    }
} 