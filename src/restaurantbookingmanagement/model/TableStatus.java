package restaurantbookingmanagement.model;

/**
 * Enum định nghĩa trạng thái của bàn
 */
public enum TableStatus {
    AVAILABLE("Có sẵn"),
    OCCUPIED("Đang sử dụng"),
    RESERVED("Đã đặt trước"),
    MAINTENANCE("Bảo trì");
    
    private final String description;
    
    TableStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

// End of TableStatus enum

// State Pattern for Table
interface TableState {
    void handle(Table table);
    boolean canTransitionTo(TableState newState);
    String getName();
}

class AvailableState implements TableState {
    @Override
    public void handle(Table table) { }
    @Override
    public boolean canTransitionTo(TableState newState) {
        return newState instanceof ReservedState || newState instanceof OccupiedState || newState instanceof MaintenanceState;
    }
    @Override
    public String getName() { return "AVAILABLE"; }
}

class ReservedState implements TableState {
    @Override
    public void handle(Table table) { }
    @Override
    public boolean canTransitionTo(TableState newState) {
        return newState instanceof OccupiedState || newState instanceof AvailableState;
    }
    @Override
    public String getName() { return "RESERVED"; }
}

class OccupiedState implements TableState {
    @Override
    public void handle(Table table) { }
    @Override
    public boolean canTransitionTo(TableState newState) {
        return newState instanceof AvailableState;
    }
    @Override
    public String getName() { return "OCCUPIED"; }
}

class MaintenanceState implements TableState {
    @Override
    public void handle(Table table) { }
    @Override
    public boolean canTransitionTo(TableState newState) {
        return newState instanceof AvailableState;
    }
    @Override
    public String getName() { return "MAINTENANCE"; }
} 