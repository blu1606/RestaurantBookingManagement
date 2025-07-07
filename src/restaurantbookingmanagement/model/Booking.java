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
    private BookingState state;
    
    // No-args constructor for Gson deserialization
    public Booking() {
        this.status = "CONFIRMED";
        this.state = BookingStateFactory.fromStatus(this.status);
    }
    
    public Booking(int bookingId, Customer customer, Table table, LocalDateTime bookingTime, int numberOfGuests) {
        this.bookingId = bookingId;
        this.customer = customer;
        this.table = table;
        this.bookingTime = bookingTime;
        this.numberOfGuests = numberOfGuests;
        this.status = "CONFIRMED";
        this.state = BookingStateFactory.fromStatus(this.status);
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
    
    public BookingState getState() { return state; }
    
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
    
    public void setState(BookingState state) { this.state = state; this.status = state.getName(); }
    
    public void transitionTo(BookingState newState) {
        if (state.canTransitionTo(newState)) {
            setState(newState);
        } else {
            throw new IllegalStateException("Invalid state transition from " + state.getName() + " to " + newState.getName());
        }
    }
    
    public void syncStateWithStatus() {
        this.state = BookingStateFactory.fromStatus(this.status);
    }
    
    @Override
    public String toString() {
        return "Đặt bàn #" + bookingId + " - " + customer.getName() + 
               " - Bàn " + table.getTableId() + " - " + numberOfGuests + " người - " + 
               bookingTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    // State Pattern for Booking
    public static interface BookingState {
        void handle(Booking booking);
        boolean canTransitionTo(BookingState newState);
        String getName();
    }

    public static class ConfirmedState implements BookingState {
        @Override
        public void handle(Booking booking) {
            // Logic khi booking ở trạng thái CONFIRMED (nếu cần)
        }
        @Override
        public boolean canTransitionTo(BookingState newState) {
            return newState instanceof CancelledState || newState instanceof CompletedState;
        }
        @Override
        public String getName() { return "CONFIRMED"; }
    }

    public static class CancelledState implements BookingState {
        @Override
        public void handle(Booking booking) { }
        @Override
        public boolean canTransitionTo(BookingState newState) { return false; }
        @Override
        public String getName() { return "CANCELLED"; }
    }

    public static class CompletedState implements BookingState {
        @Override
        public void handle(Booking booking) { }
        @Override
        public boolean canTransitionTo(BookingState newState) { return false; }
        @Override
        public String getName() { return "COMPLETED"; }
    }

    // Factory for BookingState
    public static class BookingStateFactory {
        public static BookingState fromStatus(String status) {
            switch (status) {
                case "CONFIRMED": return new ConfirmedState();
                case "CANCELLED": return new CancelledState();
                case "COMPLETED": return new CompletedState();
                default: throw new IllegalArgumentException("Unknown status: " + status);
            }
        }
    }
} 