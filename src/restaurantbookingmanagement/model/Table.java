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
    private TableState state;
    
    // No-args constructor for Gson deserialization
    public Table() {
        this.status = TableStatus.AVAILABLE;
        this.orderIds = new ArrayList<>();
        this.state = TableStateFactory.fromStatus(this.status);
    }
    
    public Table(int tableId, int capacity) {
        this.tableId = tableId;
        this.capacity = capacity;
        this.status = TableStatus.AVAILABLE;
        this.orderIds = new ArrayList<>();
        this.state = TableStateFactory.fromStatus(this.status);
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
    
    public TableState getState() { return state; }
    
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
    
    public void setState(TableState state) { this.state = state; this.status = TableStatus.valueOf(state.getName()); }
    
    public void transitionTo(TableState newState) {
        if (state.canTransitionTo(newState)) {
            setState(newState);
        } else {
            throw new IllegalStateException("Invalid state transition from " + state.getName() + " to " + newState.getName());
        }
    }
    
    public void syncStateWithStatus() {
        this.state = TableStateFactory.fromStatus(this.status);
    }
    
    @Override
    public String toString() {
        return "Bàn " + tableId + " (Sức chứa: " + capacity + " người, Trạng thái: " + status.getDescription() + ")";
    }
    
    // Factory for TableState
    public static class TableStateFactory {
        public static TableState fromStatus(TableStatus status) {
            switch (status) {
                case AVAILABLE: return new AvailableState();
                case RESERVED: return new ReservedState();
                case OCCUPIED: return new OccupiedState();
                case MAINTENANCE: return new MaintenanceState();
                default: throw new IllegalArgumentException("Unknown status: " + status);
            }
        }
    }
} 