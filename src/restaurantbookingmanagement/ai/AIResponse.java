package restaurantbookingmanagement.ai;

import java.util.List;
import java.util.Map;

/**
 * Class đại diện cho phản hồi từ AI Agent
 */
public class AIResponse {
    private String action;
    private Map<String, Object> parameters;
    private String naturalResponse;
    
    public AIResponse() {
    }
    
    public AIResponse(String action, Map<String, Object> parameters, String naturalResponse) {
        this.action = action;
        this.parameters = parameters;
        this.naturalResponse = naturalResponse;
    }
    
    // Getters
    public String getAction() {
        return action;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public String getNaturalResponse() {
        return naturalResponse;
    }
    
    // Setters
    public void setAction(String action) {
        this.action = action;
    }
    
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
    
    public void setNaturalResponse(String naturalResponse) {
        this.naturalResponse = naturalResponse;
    }
    
    // Helper methods
    public boolean isBookingAction() {
        return "book_table".equals(action) || "book_and_order".equals(action);
    }
    
    public boolean isOrderAction() {
        return "order_food".equals(action) || "book_and_order".equals(action);
    }
    
    public boolean isCancelAction() {
        return "cancel_booking".equals(action);
    }
    
    public boolean isClarifyAction() {
        return "clarify".equals(action);
    }
    
    public boolean isShowMenuAction() {
        return "show_menu".equals(action);
    }
    
    public boolean isShowTablesAction() {
        return "show_tables".equals(action);
    }
    
    public boolean isShowBookingsAction() {
        return "show_bookings".equals(action);
    }
    
    public boolean isCollectCustomerInfoAction() {
        return "collect_customer_info".equals(action);
    }
    
    // Manager actions
    public boolean isAddMenuAction() {
        return "add_menu".equals(action);
    }
    
    public boolean isDeleteMenuAction() {
        return "delete_menu".equals(action);
    }
    
    public boolean isAddTableAction() {
        return "add_table".equals(action);
    }
    
    public boolean isDeleteBookingAction() {
        return "delete_booking".equals(action);
    }
    
    public boolean isFixDataAction() {
        return "fix_data".equals(action);
    }
    
    public boolean isShowCustomersAction() {
        return "show_customers".equals(action);
    }
    
    public boolean isCustomerInfoAction() {
        return "customer_info".equals(action);
    }
    
    public boolean isCustomerSearchAction() {
        return "customer_search".equals(action);
    }
    
    // Helper methods for Manager actions
    public String getMenuItemName() {
        if (parameters != null && parameters.containsKey("name")) {
            return (String) parameters.get("name");
        }
        return null;
    }
    
    public Double getMenuItemPrice() {
        if (parameters != null && parameters.containsKey("price")) {
            Object price = parameters.get("price");
            if (price instanceof Double) {
                return (Double) price;
            } else if (price instanceof Integer) {
                return ((Integer) price).doubleValue();
            } else if (price instanceof String) {
                try {
                    return Double.parseDouble((String) price);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
    
    public String getMenuItemDescription() {
        if (parameters != null && parameters.containsKey("description")) {
            return (String) parameters.get("description");
        }
        return null;
    }
    
    public Integer getMenuItemId() {
        if (parameters != null && parameters.containsKey("itemId")) {
            Object itemId = parameters.get("itemId");
            if (itemId instanceof Integer) {
                return (Integer) itemId;
            } else if (itemId instanceof String) {
                try {
                    return Integer.parseInt((String) itemId);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
    
    public Integer getTableCapacity() {
        if (parameters != null && parameters.containsKey("capacity")) {
            Object capacity = parameters.get("capacity");
            if (capacity instanceof Integer) {
                return (Integer) capacity;
            } else if (capacity instanceof String) {
                try {
                    return Integer.parseInt((String) capacity);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
    
    public String getSearchTerm() {
        if (parameters != null && parameters.containsKey("searchTerm")) {
            return (String) parameters.get("searchTerm");
        }
        return null;
    }
    
    public Integer getGuestsCount() {
        if (parameters != null && parameters.containsKey("guests")) {
            Object guests = parameters.get("guests");
            if (guests instanceof Integer) {
                return (Integer) guests;
            } else if (guests instanceof String) {
                try {
                    return Integer.parseInt((String) guests);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
    
    public String getBookingTime() {
        if (parameters != null && parameters.containsKey("time")) {
            return (String) parameters.get("time");
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getDishes() {
        if (parameters != null && parameters.containsKey("dishes")) {
            Object dishes = parameters.get("dishes");
            if (dishes instanceof List) {
                return (List<Map<String, Object>>) dishes;
            }
        }
        return null;
    }
    
    public Integer getBookingId() {
        if (parameters != null && parameters.containsKey("bookingId")) {
            Object bookingId = parameters.get("bookingId");
            if (bookingId instanceof Integer) {
                return (Integer) bookingId;
            } else if (bookingId instanceof String) {
                try {
                    return Integer.parseInt((String) bookingId);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
    
    public String getCustomerName() {
        if (parameters != null && parameters.containsKey("customerName")) {
            return (String) parameters.get("customerName");
        }
        return null;
    }
    
    public String getCustomerPhone() {
        if (parameters != null && parameters.containsKey("customerPhone")) {
            return (String) parameters.get("customerPhone");
        }
        return null;
    }
    
    public boolean isCalculateBillAction() {
        return "calculate_bill".equals(action);
    }
    
    @Override
    public String toString() {
        return "AIResponse{" +
                "action='" + action + '\'' +
                ", parameters=" + parameters +
                ", naturalResponse='" + naturalResponse + '\'' +
                '}';
    }
} 