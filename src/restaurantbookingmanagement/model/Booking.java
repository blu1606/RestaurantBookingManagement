package restaurantbookingmanagement.model;

import java.time.LocalDateTime;

/**
 * Entity đại diện cho việc đặt bàn
 */
public class Booking {
    private int bookingId;
    private Customer customer;
    private Table table;
    private LocalDateTime bookingTime;
    private int numberOfGuests;
    private String status; // "CONFIRMED", "CANCELLED", "COMPLETED"
    
    // No-args constructor for Gson deserialization
    public Booking() {
        this.status = "CONFIRMED";
    }
    
    public Booking(int bookingId, Customer customer, Table table, LocalDateTime bookingTime, int numberOfGuests) {
        this.bookingId = bookingId;
        this.customer = customer;
        this.table = table;
        this.bookingTime = bookingTime;
        this.numberOfGuests = numberOfGuests;
        this.status = "CONFIRMED";
    }
    
    // Getters
    public int getBookingId() {
        return bookingId;
    }
    
    public Customer getCustomer() {
        return customer;
    }
    
    public Table getTable() {
        return table;
    }
    
    public LocalDateTime getBookingTime() {
        return bookingTime;
    }
    
    public int getNumberOfGuests() {
        return numberOfGuests;
    }
    
    public String getStatus() {
        return status;
    }
    
    // Setters
    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }
    
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
    
    public void setTable(Table table) {
        this.table = table;
    }
    
    public void setBookingTime(LocalDateTime bookingTime) {
        this.bookingTime = bookingTime;
    }
    
    public void setNumberOfGuests(int numberOfGuests) {
        this.numberOfGuests = numberOfGuests;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    @Override
    public String toString() {
        return "Đặt bàn #" + bookingId + " - " + customer.getName() + 
               " - Bàn " + table.getTableId() + " - " + numberOfGuests + " người - " + 
               bookingTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
} 