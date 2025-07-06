package restaurantbookingmanagement.service;

import restaurantbookingmanagement.model.*;
import restaurantbookingmanagement.ai.AIResponse;
import restaurantbookingmanagement.view.ConsoleView;
import restaurantbookingmanagement.utils.DebugUtil;
import java.util.List;

public class AiService {
    public void processAddMenuAction(AIResponse aiResponse, OrderService orderService, ConsoleView view) {
        String name = aiResponse.getMenuItemName();
        Double price = aiResponse.getMenuItemPrice();
        String description = aiResponse.getMenuItemDescription();
        if (name == null || price == null) {
            view.displayError("❌ Thiếu thông tin tên hoặc giá món ăn.");
            return;
        }
        MenuItem newItem = orderService.addMenuItem(name, price, description != null ? description : "");
        if (newItem != null) {
            view.displaySuccess("✅ Đã thêm món ăn mới: " + name + " - " + String.format("%.0f VND", price));
        } else {
            view.displayError("❌ Không thể thêm món ăn. Vui lòng thử lại.");
        }
    }
    public void processDeleteMenuAction(AIResponse aiResponse, OrderService orderService, ConsoleView view) {
        Integer itemId = aiResponse.getMenuItemId();
        if (itemId == null) {
            view.displayError("❌ Thiếu ID món ăn cần xóa.");
            return;
        }
        boolean success = orderService.deleteMenuItem(itemId);
        if (success) {
            view.displaySuccess("✅ Đã xóa món ăn #" + itemId);
        } else {
            view.displayError("❌ Không tìm thấy món ăn #" + itemId + " hoặc không thể xóa.");
        }
    }
    public void processAddTableAction(AIResponse aiResponse, BookingService bookingService, ConsoleView view) {
        Integer capacity = aiResponse.getTableCapacity();
        if (capacity == null || capacity <= 0 || capacity > 12) {
            view.displayError("❌ Sức chứa phải từ 1 đến 12 người.");
            return;
        }
        Table newTable = bookingService.addTable(capacity);
        if (newTable != null) {
            view.displaySuccess("✅ Đã thêm bàn mới: Bàn #" + newTable.getTableId() + " cho " + capacity + " người");
        } else {
            view.displayError("❌ Không thể thêm bàn. Vui lòng thử lại.");
        }
    }
    public void processDeleteBookingAction(AIResponse aiResponse, BookingService bookingService, ConsoleView view) {
        Integer bookingId = aiResponse.getBookingId();
        if (bookingId == null) {
            view.displayError("❌ Thiếu ID đặt bàn cần xóa.");
            return;
        }
        boolean success = bookingService.deleteBooking(bookingId);
        if (success) {
            view.displaySuccess("✅ Đã xóa đặt bàn #" + bookingId);
        } else {
            view.displayError("❌ Không tìm thấy đặt bàn #" + bookingId + " hoặc không thể xóa.");
        }
    }
    public void processFixDataAction(BookingService bookingService, ConsoleView view) {
        bookingService.fixBookingsWithNullCustomer();
        view.displaySuccess("✅ Đã kiểm tra và sửa các lỗi dữ liệu.");
    }
    
    /**
     * Xử lý tất cả các actions từ AI Agent một cách thống nhất
     */
    public void processAIResponse(AIResponse aiResponse, OrderService orderService, 
                                BookingService bookingService, CustomerService customerService, 
                                ConsoleView view) {
        try {
            DebugUtil.debugPrint("🔍 DEBUG - processAIResponse called");
            DebugUtil.debugPrint("   - Action: " + aiResponse.getAction());
            DebugUtil.debugPrint("   - IsJavaServiceAction: " + aiResponse.isJavaServiceAction());
            DebugUtil.debugPrint("   - RequiresJavaService: " + aiResponse.requiresJavaService());
            DebugUtil.debugPrint("   - JavaServiceType: " + aiResponse.getJavaServiceType());
            
            // Check if this is a Java service action
            if (aiResponse.isJavaServiceAction()) {
                String serviceType = aiResponse.getJavaServiceType();
                System.out.println("🔧 Processing Java Service Action: " + aiResponse.getAction() + 
                                 " with Service: " + serviceType);
                
                switch (aiResponse.getAction()) {
                    // OrderService actions
                    case "add_menu":
                        processAddMenuAction(aiResponse, orderService, view);
                        break;
                    case "delete_menu":
                        processDeleteMenuAction(aiResponse, orderService, view);
                        break;
                    case "update_menu":
                        processUpdateMenuAction(aiResponse, orderService, view);
                        break;
                    case "add_item_to_order":
                        processAddItemToOrderAction(aiResponse, orderService, view);
                        break;
                    case "remove_item_from_order":
                        processRemoveItemFromOrderAction(aiResponse, orderService, view);
                        break;
                    case "complete_order":
                        processCompleteOrderAction(aiResponse, orderService, view);
                        break;
                    case "calculate_bill":
                        processCalculateBillAction(aiResponse, orderService, view);
                        break;
                    case "get_revenue":
                        processGetRevenueAction(aiResponse, orderService, view);
                        break;
                    case "show_menu":
                        processShowMenuAction(aiResponse, orderService, view);
                        break;
                    
                    // BookingService actions
                    case "add_table":
                        processAddTableAction(aiResponse, bookingService, view);
                        break;
                    case "delete_table":
                        processDeleteTableAction(aiResponse, bookingService, view);
                        break;
                    case "update_table":
                        processUpdateTableAction(aiResponse, bookingService, view);
                        break;
                    case "search_tables":
                        processSearchTablesAction(aiResponse, bookingService, view);
                        break;
                    case "show_available_tables":
                        DebugUtil.debugPrint("🔍 DEBUG - Processing show_available_tables case");
                        processShowAvailableTablesAction(aiResponse, bookingService, view);
                        break;
                    case "show_all_tables":
                        processShowAllTablesAction(aiResponse, bookingService, view);
                        break;
                    case "create_booking":
                        processCreateBookingAction(aiResponse, bookingService, view);
                        break;
                    case "cancel_booking":
                        processCancelBookingAction(aiResponse, bookingService, view);
                        break;
                    case "complete_booking":
                        processCompleteBookingAction(aiResponse, bookingService, view);
                        break;
                    case "update_booking":
                        processUpdateBookingAction(aiResponse, bookingService, view);
                        break;
                    case "delete_booking":
                        processDeleteBookingAction(aiResponse, bookingService, view);
                        break;
                    case "fix_data":
                        processFixDataAction(bookingService, view);
                        break;
                    
                    // CustomerService actions
                    case "create_customer":
                        processCreateCustomerAction(aiResponse, customerService, view);
                        break;
                    case "update_customer":
                        processUpdateCustomerAction(aiResponse, customerService, view);
                        break;
                    case "delete_customer":
                        processDeleteCustomerAction(aiResponse, customerService, view);
                        break;
                    case "get_customer_info":
                        processGetCustomerInfoAction(aiResponse, customerService, view);
                        break;
                    case "customer_search":
                        processCustomerSearchAction(aiResponse, customerService, view);
                        break;
                    
                    default:
                        view.displayError("❌ Không hỗ trợ action: " + aiResponse.getAction());
                        break;
                }
            } else {
                // Handle non-Java service actions (like booking, ordering, etc.)
                view.displayMessage(aiResponse.getNaturalResponse());
            }
        } catch (Exception e) {
            view.displayError("❌ Lỗi xử lý AI response: " + e.getMessage());
            System.err.println("🔥 Error processing AI response: " + e.getMessage());
        }
    }
    public void processCustomerSearchAction(AIResponse aiResponse, CustomerService customerService, ConsoleView view) {
        String searchTerm = aiResponse.getSearchTerm();
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            view.displayError("❌ Thiếu từ khóa tìm kiếm.");
            return;
        }
        List<Customer> results = customerService.searchCustomers(searchTerm);
        if (results.isEmpty()) {
            view.displayMessage("📝 Không tìm thấy khách hàng nào phù hợp với: " + searchTerm);
        } else {
            view.displayMessage("📝 Tìm thấy " + results.size() + " khách hàng:");
            for (Customer customer : results) {
                view.displayMessage("  - " + customer.getName() + " (" + customer.getPhone() + ")");
            }
        }
    }
    public void processShowMenuAction(OrderService orderService, ConsoleView view) {
        view.displayMenu(orderService.getAllMenuItems());
    }
    
    /**
     * Xử lý action show_menu từ AI Agent
     */
    public void processShowMenuAction(AIResponse aiResponse, OrderService orderService, ConsoleView view) {
        if (aiResponse.isShowMenuAction()) {
            view.displayMenu(orderService.getAllMenuItems());
        }
    }
    
    /**
     * Xử lý action fix_data từ AI Agent
     */
    public void processFixDataAction(AIResponse aiResponse, BookingService bookingService, ConsoleView view) {
        if (aiResponse.isFixDataAction()) {
            bookingService.fixBookingsWithNullCustomer();
            view.displaySuccess("✅ Đã kiểm tra và sửa các lỗi dữ liệu.");
        }
    }
    
    // OrderService actions
    public void processUpdateMenuAction(AIResponse aiResponse, OrderService orderService, ConsoleView view) {
        Integer itemId = aiResponse.getMenuItemId();
        String name = aiResponse.getMenuItemName();
        Double price = aiResponse.getMenuItemPrice();
        String description = aiResponse.getMenuItemDescription();
        
        if (itemId == null) {
            view.displayError("❌ Thiếu ID món ăn cần cập nhật.");
            return;
        }
        
        boolean success = orderService.updateMenuItem(itemId, name, String.valueOf(price), description);
        if (success) {
            view.displaySuccess("✅ Đã cập nhật món ăn #" + itemId);
        } else {
            view.displayError("❌ Không thể cập nhật món ăn #" + itemId);
        }
    }
    
    public void processAddItemToOrderAction(AIResponse aiResponse, OrderService orderService, ConsoleView view) {
        Integer orderId = aiResponse.getOrderId();
        String itemName = aiResponse.getItemName();
        Integer quantity = aiResponse.getQuantity();
        
        if (orderId == null || itemName == null || quantity == null) {
            view.displayError("❌ Thiếu thông tin orderId, itemName hoặc quantity.");
            return;
        }
        
        boolean success = orderService.addItemToOrder(orderId, itemName, quantity);
        if (success) {
            view.displaySuccess("✅ Đã thêm " + quantity + " " + itemName + " vào đơn hàng #" + orderId);
        } else {
            view.displayError("❌ Không thể thêm món vào đơn hàng.");
        }
    }
    
    public void processRemoveItemFromOrderAction(AIResponse aiResponse, OrderService orderService, ConsoleView view) {
        Integer orderId = aiResponse.getOrderId();
        String itemName = aiResponse.getItemName();
        
        if (orderId == null || itemName == null) {
            view.displayError("❌ Thiếu thông tin orderId hoặc itemName.");
            return;
        }
        
        boolean success = orderService.removeItemFromOrder(orderId, itemName);
        if (success) {
            view.displaySuccess("✅ Đã xóa " + itemName + " khỏi đơn hàng #" + orderId);
        } else {
            view.displayError("❌ Không thể xóa món khỏi đơn hàng.");
        }
    }
    
    public void processCompleteOrderAction(AIResponse aiResponse, OrderService orderService, ConsoleView view) {
        Integer orderId = aiResponse.getOrderId();
        
        if (orderId == null) {
            view.displayError("❌ Thiếu ID đơn hàng cần hoàn thành.");
            return;
        }
        
        boolean success = orderService.completeOrder(orderId);
        if (success) {
            view.displaySuccess("✅ Đã hoàn thành đơn hàng #" + orderId);
        } else {
            view.displayError("❌ Không thể hoàn thành đơn hàng #" + orderId);
        }
    }
    
    public void processCalculateBillAction(AIResponse aiResponse, OrderService orderService, ConsoleView view) {
        Integer bookingId = aiResponse.getBookingId();
        
        if (bookingId == null) {
            view.displayError("❌ Thiếu ID booking cần tính tiền.");
            return;
        }
        
        double bill = orderService.calculateBillForBooking(bookingId);
        view.displaySuccess("💰 Tổng tiền cho booking #" + bookingId + ": " + String.format("%.0f VND", bill));
    }
    
    public void processGetRevenueAction(AIResponse aiResponse, OrderService orderService, ConsoleView view) {
        double revenue = orderService.getTotalRevenue();
        view.displaySuccess("💰 Tổng doanh thu: " + String.format("%.0f VND", revenue));
    }
    
    // BookingService actions
    public void processDeleteTableAction(AIResponse aiResponse, BookingService bookingService, ConsoleView view) {
        Integer tableId = aiResponse.getTableId();
        
        if (tableId == null) {
            view.displayError("❌ Thiếu ID bàn cần xóa.");
            return;
        }
        
        boolean success = bookingService.deleteTable(tableId);
        if (success) {
            view.displaySuccess("✅ Đã xóa bàn #" + tableId);
        } else {
            view.displayError("❌ Không thể xóa bàn #" + tableId);
        }
    }
    
    public void processUpdateTableAction(AIResponse aiResponse, BookingService bookingService, ConsoleView view) {
        Integer tableId = aiResponse.getTableId();
        Integer capacity = aiResponse.getTableCapacity();
        String status = aiResponse.getTableStatus();
        
        if (tableId == null) {
            view.displayError("❌ Thiếu ID bàn cần cập nhật.");
            return;
        }
        
        boolean success = bookingService.updateTable(tableId, 
            capacity != null ? String.valueOf(capacity) : null, 
            status);
        if (success) {
            view.displaySuccess("✅ Đã cập nhật bàn #" + tableId);
        } else {
            view.displayError("❌ Không thể cập nhật bàn #" + tableId);
        }
    }
    
    public void processSearchTablesAction(AIResponse aiResponse, BookingService bookingService, ConsoleView view) {
        String keyword = aiResponse.getSearchKeyword();
        
        if (keyword == null || keyword.trim().isEmpty()) {
            view.displayError("❌ Thiếu từ khóa tìm kiếm.");
            return;
        }
        
        List<Table> results = bookingService.searchTables(keyword);
        if (results.isEmpty()) {
            view.displayMessage("📝 Không tìm thấy bàn nào phù hợp với: " + keyword);
        } else {
            view.displayMessage("📝 Tìm thấy " + results.size() + " bàn:");
            for (Table table : results) {
                view.displayMessage("  - Bàn #" + table.getTableId() + " (" + table.getCapacity() + " người, " + table.getStatus() + ")");
            }
        }
    }
    
    public void processShowAvailableTablesAction(AIResponse aiResponse, BookingService bookingService, ConsoleView view) {
        DebugUtil.debugPrint("🔍 DEBUG - processShowAvailableTablesAction called");
        DebugUtil.debugPrint("   - Action: " + aiResponse.getAction());
        DebugUtil.debugPrint("   - RequiresJavaService: " + aiResponse.requiresJavaService());
        DebugUtil.debugPrint("   - JavaServiceType: " + aiResponse.getJavaServiceType());
        
        List<Table> availableTables = bookingService.getAvailableTables();
        DebugUtil.debugPrint("   - Available tables count: " + availableTables.size());
        
        if (availableTables.isEmpty()) {
            view.displayMessage("📝 Không có bàn nào có sẵn.");
        } else {
            view.displayMessage("📝 Có " + availableTables.size() + " bàn có sẵn:");
            for (Table table : availableTables) {
                view.displayMessage("  - Bàn #" + table.getTableId() + " (" + table.getCapacity() + " người, " + table.getStatus() + ")");
            }
        }
    }
    
    public void processShowAllTablesAction(AIResponse aiResponse, BookingService bookingService, ConsoleView view) {
        List<Table> allTables = bookingService.getAllTables();
        if (allTables.isEmpty()) {
            view.displayMessage("📝 Không có bàn nào.");
        } else {
            view.displayMessage("📝 Có " + allTables.size() + " bàn:");
            for (Table table : allTables) {
                view.displayMessage("  - Bàn #" + table.getTableId() + " (" + table.getCapacity() + " người, " + table.getStatus() + ")");
            }
        }
    }
    
    public void processCreateBookingAction(AIResponse aiResponse, BookingService bookingService, ConsoleView view) {
        String customerName = aiResponse.getCustomerName();
        String customerPhone = aiResponse.getCustomerPhone();
        Integer guests = aiResponse.getGuestsCount();
        String dateTime = aiResponse.getBookingTime();
        
        if (customerName == null || customerPhone == null || guests == null || dateTime == null) {
            view.displayError("❌ Thiếu thông tin khách hàng hoặc thời gian đặt bàn.");
            return;
        }
        
        try {
            Customer customer = new Customer(0, customerName, customerPhone);
            java.time.LocalDateTime bookingTime = java.time.LocalDateTime.parse(dateTime, 
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            
            Booking booking = bookingService.createBooking(customer, guests, bookingTime);
            if (booking != null) {
                view.displaySuccess("✅ Đã tạo đặt bàn #" + booking.getBookingId() + " cho " + customerName);
            } else {
                view.displayError("❌ Không thể tạo đặt bàn. Có thể bàn đã hết hoặc thời gian bị trùng.");
            }
        } catch (Exception e) {
            view.displayError("❌ Lỗi định dạng thời gian. Vui lòng sử dụng định dạng dd/MM/yyyy HH:mm");
        }
    }
    
    public void processCancelBookingAction(AIResponse aiResponse, BookingService bookingService, ConsoleView view) {
        Integer bookingId = aiResponse.getBookingId();
        
        if (bookingId == null) {
            view.displayError("❌ Thiếu ID đặt bàn cần hủy.");
            return;
        }
        
        boolean success = bookingService.cancelBooking(bookingId);
        if (success) {
            view.displaySuccess("✅ Đã hủy đặt bàn #" + bookingId);
        } else {
            view.displayError("❌ Không thể hủy đặt bàn #" + bookingId);
        }
    }
    
    public void processCompleteBookingAction(AIResponse aiResponse, BookingService bookingService, ConsoleView view) {
        Integer bookingId = aiResponse.getBookingId();
        
        if (bookingId == null) {
            view.displayError("❌ Thiếu ID đặt bàn cần hoàn thành.");
            return;
        }
        
        bookingService.completeBooking(bookingId);
        view.displaySuccess("✅ Đã hoàn thành đặt bàn #" + bookingId);
    }
    
    public void processUpdateBookingAction(AIResponse aiResponse, BookingService bookingService, ConsoleView view) {
        Integer bookingId = aiResponse.getBookingId();
        Integer guests = aiResponse.getGuestsCount();
        String dateTime = aiResponse.getBookingTime();
        
        if (bookingId == null) {
            view.displayError("❌ Thiếu ID đặt bàn cần cập nhật.");
            return;
        }
        
        boolean success = bookingService.updateBooking(bookingId, 
            guests != null ? String.valueOf(guests) : null, 
            dateTime);
        if (success) {
            view.displaySuccess("✅ Đã cập nhật đặt bàn #" + bookingId);
        } else {
            view.displayError("❌ Không thể cập nhật đặt bàn #" + bookingId);
        }
    }
    
    // CustomerService actions
    public void processCreateCustomerAction(AIResponse aiResponse, CustomerService customerService, ConsoleView view) {
        String name = aiResponse.getCustomerName();
        String phone = aiResponse.getCustomerPhone();
        String email = aiResponse.getCustomerEmail();
        
        if (name == null || phone == null) {
            view.displayError("❌ Thiếu tên hoặc số điện thoại khách hàng.");
            return;
        }
        
        Customer customer = customerService.createCustomer(name, phone, email);
        if (customer != null) {
            view.displaySuccess("✅ Đã tạo khách hàng: " + name + " (" + phone + ")");
        } else {
            view.displayError("❌ Không thể tạo khách hàng.");
        }
    }
    
    public void processUpdateCustomerAction(AIResponse aiResponse, CustomerService customerService, ConsoleView view) {
        Integer customerId = aiResponse.getCustomerId();
        String name = aiResponse.getCustomerName();
        String phone = aiResponse.getCustomerPhone();
        String email = aiResponse.getCustomerEmail();
        
        if (customerId == null) {
            view.displayError("❌ Thiếu ID khách hàng cần cập nhật.");
            return;
        }
        
        boolean success = customerService.updateCustomer(customerId, name, phone, email);
        if (success) {
            view.displaySuccess("✅ Đã cập nhật khách hàng #" + customerId);
        } else {
            view.displayError("❌ Không thể cập nhật khách hàng #" + customerId);
        }
    }
    
    public void processDeleteCustomerAction(AIResponse aiResponse, CustomerService customerService, ConsoleView view) {
        Integer customerId = aiResponse.getCustomerId();
        
        if (customerId == null) {
            view.displayError("❌ Thiếu ID khách hàng cần xóa.");
            return;
        }
        
        boolean success = customerService.deleteCustomer(customerId);
        if (success) {
            view.displaySuccess("✅ Đã xóa khách hàng #" + customerId);
        } else {
            view.displayError("❌ Không thể xóa khách hàng #" + customerId + " (có thể đang có đặt bàn)");
        }
    }
    
    public void processGetCustomerInfoAction(AIResponse aiResponse, CustomerService customerService, ConsoleView view) {
        Integer customerId = aiResponse.getCustomerId();
        
        if (customerId == null) {
            view.displayError("❌ Thiếu ID khách hàng cần xem thông tin.");
            return;
        }
        
        CustomerInfo customerInfo = customerService.getCustomerInfo(customerId);
        if (customerInfo != null) {
            customerService.displayCustomerInfo(customerInfo);
        } else {
            view.displayError("❌ Không tìm thấy khách hàng #" + customerId);
        }
    }
} 