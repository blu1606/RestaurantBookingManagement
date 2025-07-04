package restaurantbookingmanagement.model;

/**
 * Entity đại diện cho món ăn trong menu
 */
public class MenuItem {
    private int itemId;
    private String name;
    private double price;
    private String description;
    
    // No-args constructor for Gson deserialization
    public MenuItem() {
    }
    
    public MenuItem(int itemId, String name, double price, String description) {
        this.itemId = itemId;
        this.name = name;
        this.price = price;
        this.description = description;
    }
    
    // Getters
    public int getItemId() {
        return itemId;
    }
    
    public String getName() {
        return name;
    }
    
    public double getPrice() {
        return price;
    }
    
    public String getDescription() {
        return description;
    }
    
    // Setters
    public void setItemId(int itemId) {
        this.itemId = itemId;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return name + " - " + String.format("%.0f", price) + " VND";
    }
} 