package restaurantbookingmanagement.model;

/**
 * Enum định nghĩa các role trong hệ thống
 */
public enum Role {
    USER("User", "Khách hàng - Có quyền xem thông tin và đặt bàn"),
    MANAGER("Manager", "Quản lý - Có quyền CRUD tất cả dữ liệu");
    
    private final String displayName;
    private final String description;
    
    Role(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
} 