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