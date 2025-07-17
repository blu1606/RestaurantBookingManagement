package restaurantbookingmanagement.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity đại diện cho đơn hàng
 */
public class Order {
    private int orderId;
    private transient Booking booking;
    private int bookingId; // Thêm trường bookingId để serialize
    private List<OrderItem> items;
    private LocalDateTime orderTime;
    private String status; // "PENDING", "PREPARING", "READY", "COMPLETED"
    private double totalAmount;
    private int tableId; // ID của bàn
    
    public Order(int orderId, Booking booking) {
        this.orderId = orderId;
        this.booking = booking;
        this.bookingId = booking != null ? booking.getBookingId() : 0;
        this.items = new ArrayList<>();
        this.orderTime = LocalDateTime.now();
        this.status = "PENDING";
        this.totalAmount = 0.0;
        this.tableId = booking != null && booking.getTable() != null ? booking.getTable().getTableId() : 0;
    }
    
    // Getters
    public int getOrderId() {
        return orderId;
    }
    
    public Booking getBooking() {
        return booking;
    }
    
    public List<OrderItem> getItems() {
        return items;
    }
    
    public LocalDateTime getOrderTime() {
        return orderTime;
    }
    
    public String getStatus() {
        return status;
    }
    
    public double getTotalAmount() {
        return totalAmount;
    }
    
    public int getTableId() {
        return tableId;
    }
    
    public int getBookingId() {
        return booking != null ? booking.getBookingId() : bookingId;
    }
    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }
    public void setBooking(Booking booking) {
        this.booking = booking;
        this.bookingId = booking != null ? booking.getBookingId() : 0;
        if (booking != null && booking.getTable() != null) {
            this.tableId = booking.getTable().getTableId();
        }
    }
    public Table getTable() {
        return booking != null ? booking.getTable() : null;
    }
    public void setTable(Table table) {
        if (booking != null) {
            booking.setTable(table);
            this.tableId = table != null ? table.getTableId() : 0;
        }
    }
    
    // Getter cho tên món đầu tiên (dùng cho các thao tác đơn giản)
    public String getName() {
        if (items.isEmpty()) return "";
        return items.get(0).getItem().getName();
    }
    
    // Getter cho giá món đầu tiên
    public double getPrice() {
        if (items.isEmpty()) return 0.0;
        return items.get(0).getItem().getPrice();
    }
    
    // Getter cho mô tả món đầu tiên
    public String getDescription() {
        if (items.isEmpty()) return "";
        return items.get(0).getItem().getDescription();
    }
    
    // Setters
    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }
    
    public void setItems(List<OrderItem> items) {
        this.items = items;
        calculateTotalAmount();
    }
    
    public void setOrderTime(LocalDateTime orderTime) {
        this.orderTime = orderTime;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public void setTableId(int tableId) {
        this.tableId = tableId;
    }
    
    // Business methods
    public void addItem(int itemId, int amount) {
        for (OrderItem oi : items) {
            if (oi.getItemId() == itemId) {
                oi.setAmount(oi.getAmount() + amount);
                calculateTotalAmount();
                return;
            }
        }
        items.add(new OrderItem(itemId, amount));
        calculateTotalAmount();
    }
    
    public void removeItem(int itemId) {
        items.removeIf(oi -> oi.getItemId() == itemId);
        calculateTotalAmount();
    }
    
    public void updateItemAmount(int itemId, int amount) {
        for (OrderItem oi : items) {
            if (oi.getItemId() == itemId) {
                if (amount <= 0) items.remove(oi);
                else oi.setAmount(amount);
                break;
            }
        }
        calculateTotalAmount();
    }
    
    private void calculateTotalAmount() {
        totalAmount = 0.0;
        for (OrderItem oi : items) {
            if (oi.getItem() != null) {
                totalAmount += oi.getItem().getPrice() * oi.getAmount();
            }
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String customerName = "N/A";
        if (booking != null && booking.getCustomer() != null) {
            customerName = booking.getCustomer().getName();
        }
        sb.append("Đơn hàng #").append(orderId).append(" - ").append(customerName);
        sb.append("\nThời gian: ").append(orderTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        sb.append("\nTrạng thái: ").append(status);
        sb.append("\nCác món đã đặt:");
        
        for (OrderItem oi : items) {
            sb.append("\n  - ItemID: ").append(oi.getItemId()).append(" x").append(oi.getAmount());
        }
        
        sb.append("\nTổng cộng: ").append(String.format("%.0f", totalAmount)).append(" VND");
        return sb.toString();
    }
    
    // Inner class OrderItem
    public static class OrderItem {
        private int itemId;
        private int amount;
        private MenuItem item;
        
        public OrderItem() {}
        
        public OrderItem(int itemId, int amount) {
            this.itemId = itemId;
            this.amount = amount;
        }
        
        public int getItemId() {
            return itemId;
        }
        
        public int getAmount() {
            return amount;
        }
        
        public void setItemId(int itemId) {
            this.itemId = itemId;
        }
        
        public void setAmount(int amount) {
            this.amount = amount;
        }
        
        public MenuItem getItem() {
            return item;
        }
        
        public void setItem(MenuItem item) {
            this.item = item;
        }
    }
} 