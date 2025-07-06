package restaurantbookingmanagement.ai;

import java.util.List;
import java.util.Map;
import restaurantbookingmanagement.utils.DebugUtil;

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
    
    public boolean isShowAvailableTablesAction() {
        return "show_available_tables".equals(action);
    }
    
    public boolean isShowAllTablesAction() {
        return "show_all_tables".equals(action);
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
    
    public String getCustomerEmail() {
        if (parameters != null && parameters.containsKey("email")) {
            return (String) parameters.get("email");
        }
        return null;
    }
    
    public Integer getCustomerId() {
        if (parameters != null && parameters.containsKey("customerId")) {
            Object customerId = parameters.get("customerId");
            if (customerId instanceof Integer) {
                return (Integer) customerId;
            } else if (customerId instanceof String) {
                try {
                    return Integer.parseInt((String) customerId);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
    
    public Integer getOrderId() {
        if (parameters != null && parameters.containsKey("orderId")) {
            Object orderId = parameters.get("orderId");
            if (orderId instanceof Integer) {
                return (Integer) orderId;
            } else if (orderId instanceof String) {
                try {
                    return Integer.parseInt((String) orderId);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
    
    public String getItemName() {
        if (parameters != null && parameters.containsKey("itemName")) {
            return (String) parameters.get("itemName");
        }
        return null;
    }
    
    public Integer getQuantity() {
        if (parameters != null && parameters.containsKey("quantity")) {
            Object quantity = parameters.get("quantity");
            if (quantity instanceof Integer) {
                return (Integer) quantity;
            } else if (quantity instanceof String) {
                try {
                    return Integer.parseInt((String) quantity);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
    
    public Integer getTableId() {
        if (parameters != null && parameters.containsKey("tableId")) {
            Object tableId = parameters.get("tableId");
            if (tableId instanceof Integer) {
                return (Integer) tableId;
            } else if (tableId instanceof String) {
                try {
                    return Integer.parseInt((String) tableId);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
    
    public String getTableStatus() {
        if (parameters != null && parameters.containsKey("status")) {
            return (String) parameters.get("status");
        }
        return null;
    }
    
    public String getSearchKeyword() {
        if (parameters != null && parameters.containsKey("keyword")) {
            return (String) parameters.get("keyword");
        }
        return null;
    }
    
    public boolean isCalculateBillAction() {
        return "calculate_bill".equals(action);
    }
    
    // Java Service Integration methods
    public boolean requiresJavaService() {
        if (parameters != null && parameters.containsKey("requiresJavaService")) {
            Object requires = parameters.get("requiresJavaService");
            if (requires instanceof Boolean) {
                return (Boolean) requires;
            } else if (requires instanceof String) {
                return Boolean.parseBoolean((String) requires);
            }
        }
        return false;
    }
    
    public String getJavaServiceType() {
        if (parameters != null && parameters.containsKey("javaServiceType")) {
            return (String) parameters.get("javaServiceType");
        }
        return null;
    }
    
    public boolean isJavaServiceAction() {
        boolean result = requiresJavaService() && getJavaServiceType() != null && !getJavaServiceType().isEmpty();
        DebugUtil.debugPrint("🔍 DEBUG - isJavaServiceAction: " + result);
        DebugUtil.debugPrint("   - requiresJavaService: " + requiresJavaService());
        DebugUtil.debugPrint("   - javaServiceType: " + getJavaServiceType());
        return result;
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