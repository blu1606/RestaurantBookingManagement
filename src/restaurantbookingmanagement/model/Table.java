package restaurantbookingmanagement.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity đại diện cho bàn trong nhà hàng
 */
public class Table {
    private int tableId;
    private int capacity;
    private TableStatus status;
    private List<Integer> orderIds; // Danh sách orderId của bàn này
    
    // No-args constructor for Gson deserialization
    public Table() {
        this.status = TableStatus.AVAILABLE;
        this.orderIds = new ArrayList<>();
    }
    
    public Table(int tableId, int capacity) {
        this.tableId = tableId;
        this.capacity = capacity;
        this.status = TableStatus.AVAILABLE;
        this.orderIds = new ArrayList<>();
    }
    
    // Getters
    public int getTableId() {
        return tableId;
    }
    
    public int getCapacity() {
        return capacity;
    }
    
    public TableStatus getStatus() {
        return status;
    }
    
    public List<Integer> getOrderIds() {
        return orderIds;
    }
    
    // Setters
    public void setTableId(int tableId) {
        this.tableId = tableId;
    }
    
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
    
    public void setStatus(TableStatus status) {
        this.status = status;
    }
    
    public void setOrderIds(List<Integer> orderIds) {
        this.orderIds = orderIds;
    }
    
    public void addOrderId(int orderId) {
        if (!orderIds.contains(orderId)) {
            orderIds.add(orderId);
        }
    }
    
    public void removeOrderId(int orderId) {
        orderIds.remove(Integer.valueOf(orderId));
    }
    
    @Override
    public String toString() {
        return "Bàn " + tableId + " (Sức chứa: " + capacity + " người, Trạng thái: " + status.getDescription() + ")";
    }
} 