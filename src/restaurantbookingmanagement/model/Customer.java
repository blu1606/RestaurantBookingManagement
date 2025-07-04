package restaurantbookingmanagement.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity đại diện cho khách hàng
 */
public class Customer {
    private int customerId;
    private String name;
    private String phone;
    private String email;
    private List<Integer> activeBookingIds; // Danh sách booking ID đang hoạt động
    private String role; // "admin", "user", "guest"
    private String password;
    
    // No-args constructor for Gson deserialization
    public Customer() {
        this.activeBookingIds = new ArrayList<>();
        this.role = "user";
        this.password = "";
    }
    
    public Customer(int customerId, String name, String phone) {
        this.customerId = customerId;
        this.name = name;
        this.phone = phone;
        this.email = "";
        this.activeBookingIds = new ArrayList<>();
        this.role = "user";
        this.password = "";
    }
    
    public Customer(int customerId, String name, String phone, String email) {
        this.customerId = customerId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.activeBookingIds = new ArrayList<>();
        this.role = "user";
        this.password = "";
    }
    
    // Constructor đầy đủ
    public Customer(int customerId, String name, String phone, String email, String role, String password) {
        this.customerId = customerId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.activeBookingIds = new ArrayList<>();
        this.role = role;
        this.password = password;
    }
    
    // Getters
    public int getCustomerId() {
        return customerId;
    }
    
    public String getName() {
        return name;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public String getEmail() {
        return email;
    }
    
    public List<Integer> getActiveBookingIds() {
        return activeBookingIds;
    }
    
    public String getRole() {
        return role;
    }
    
    public String getPassword() {
        return password;
    }
    
    // Setters
    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public void setActiveBookingIds(List<Integer> activeBookingIds) {
        this.activeBookingIds = activeBookingIds;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    // Helper methods for booking management
    public void addBookingId(int bookingId) {
        if (!activeBookingIds.contains(bookingId)) {
            activeBookingIds.add(bookingId);
        }
    }
    
    public void removeBookingId(int bookingId) {
        activeBookingIds.remove(Integer.valueOf(bookingId));
    }
    
    public boolean hasActiveBooking(int bookingId) {
        return activeBookingIds.contains(bookingId);
    }
    
    public boolean hasAnyActiveBooking() {
        return !activeBookingIds.isEmpty();
    }
    
    public Integer getFirstActiveBookingId() {
        return activeBookingIds.isEmpty() ? null : activeBookingIds.get(0);
    }
    
    @Override
    public String toString() {
        return name + " (SĐT: " + phone + ", Role: " + role + ")";
    }
} 