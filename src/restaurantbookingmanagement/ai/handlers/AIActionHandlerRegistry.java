package restaurantbookingmanagement.ai.handlers;

import java.util.HashMap;
import java.util.Map;
import restaurantbookingmanagement.ai.AIResponse;
import restaurantbookingmanagement.view.ConsoleView;
import restaurantbookingmanagement.model.Customer;
import restaurantbookingmanagement.model.Booking;
import restaurantbookingmanagement.model.MenuItem;
import restaurantbookingmanagement.model.Table;
import restaurantbookingmanagement.model.CustomerInfo;

public class AIActionHandlerRegistry {
    private final Map<String, AIActionHandler> handlers = new HashMap<>();
    public AIActionHandlerRegistry() {
        handlers.put("create_booking", new CreateBookingHandler());
        handlers.put("add_menu", new AddMenuHandler());
        handlers.put("delete_menu", new DeleteMenuHandler());
        handlers.put("update_menu", new UpdateMenuHandler());
        handlers.put("add_item_to_order", new AddItemToOrderHandler());
        handlers.put("remove_item_from_order", new RemoveItemFromOrderHandler());
        handlers.put("complete_order", new CompleteOrderHandler());
        handlers.put("calculate_bill", new CalculateBillHandler());
        handlers.put("get_revenue", new GetRevenueHandler());
        handlers.put("show_menu", new ShowMenuHandler());
        handlers.put("add_table", new AddTableHandler());
        handlers.put("delete_table", new DeleteTableHandler());
        handlers.put("update_table", new UpdateTableHandler());
        handlers.put("search_tables", new SearchTablesHandler());
        handlers.put("show_available_tables", new ShowAvailableTablesHandler());
        handlers.put("show_all_tables", new ShowAllTablesHandler());
        handlers.put("cancel_booking", new CancelBookingHandler());
        handlers.put("complete_booking", new CompleteBookingHandler());
        handlers.put("update_booking", new UpdateBookingHandler());
        handlers.put("delete_booking", new DeleteBookingHandler());
        handlers.put("fix_data", new FixDataHandler());
        handlers.put("create_customer", new CreateCustomerHandler());
        handlers.put("update_customer", new UpdateCustomerHandler());
        handlers.put("delete_customer", new DeleteCustomerHandler());
        handlers.put("get_customer_info", new GetCustomerInfoHandler());
        handlers.put("customer_search", new CustomerSearchHandler());
    }
    // --- Handler implementations ---
    public static class DeleteMenuHandler implements AIActionHandler {
        @Override
        public void handle(AIResponse response, ServiceContext context, ConsoleView view) {
            Integer itemId = AIResponseUtils.getInt(response.getParameters(), "itemId");
            if (itemId == null) {
                view.displayError("❌ Thiếu ID món ăn cần xóa.");
                return;
            }
            boolean success = context.getMenuService().deleteMenuItem(itemId);
            if (success) {
                view.displaySuccess("✅ Đã xóa món ăn #" + itemId);
            } else {
                view.displayError("❌ Không tìm thấy món ăn #" + itemId + " hoặc không thể xóa.");
            }
        }
    }
    public static class UpdateMenuHandler implements AIActionHandler {
        @Override
        public void handle(AIResponse response, ServiceContext context, ConsoleView view) {
            Integer itemId = AIResponseUtils.getInt(response.getParameters(), "itemId");
            String name = AIResponseUtils.getString(response.getParameters(), "name");
            Double price = AIResponseUtils.getDouble(response.getParameters(), "price");
            String description = AIResponseUtils.getString(response.getParameters(), "description");
            if (itemId == null) {
                view.displayError("❌ Thiếu ID món ăn cần cập nhật.");
                return;
            }
            boolean success = context.getMenuService().updateMenuItem(itemId, name, String.valueOf(price), description);
            if (success) {
                view.displaySuccess("✅ Đã cập nhật món ăn #" + itemId);
            } else {
                view.displayError("❌ Không thể cập nhật món ăn #" + itemId);
            }
        }
    }
    public static class AddItemToOrderHandler implements AIActionHandler {
        @Override
        public void handle(AIResponse response, ServiceContext context, ConsoleView view) {
            Integer orderId = AIResponseUtils.getInt(response.getParameters(), "orderId");
            String itemName = AIResponseUtils.getString(response.getParameters(), "itemName");
            Integer quantity = AIResponseUtils.getInt(response.getParameters(), "quantity");
            if (orderId == null || itemName == null || quantity == null) {
                view.displayError("❌ Thiếu thông tin orderId, itemName hoặc quantity.");
                return;
            }
            boolean success = context.getOrderService().addItemToOrder(orderId, itemName, quantity);
            if (success) {
                view.displaySuccess("✅ Đã thêm " + quantity + " " + itemName + " vào đơn hàng #" + orderId);
            } else {
                view.displayError("❌ Không thể thêm món vào đơn hàng.");
            }
        }
    }
    public static class RemoveItemFromOrderHandler implements AIActionHandler {
        @Override
        public void handle(AIResponse response, ServiceContext context, ConsoleView view) {
            Integer orderId = AIResponseUtils.getInt(response.getParameters(), "orderId");
            String itemName = AIResponseUtils.getString(response.getParameters(), "itemName");
            if (orderId == null || itemName == null) {
                view.displayError("❌ Thiếu thông tin orderId hoặc itemName.");
                return;
            }
            boolean success = context.getOrderService().removeItemFromOrder(orderId, itemName);
            if (success) {
                view.displaySuccess("✅ Đã xóa " + itemName + " khỏi đơn hàng #" + orderId);
            } else {
                view.displayError("❌ Không thể xóa món khỏi đơn hàng.");
            }
        }
    }
    public static class CompleteOrderHandler implements AIActionHandler {
        @Override
        public void handle(AIResponse response, ServiceContext context, ConsoleView view) {
            Integer orderId = AIResponseUtils.getInt(response.getParameters(), "orderId");
            if (orderId == null) {
                view.displayError("❌ Thiếu ID đơn hàng cần hoàn thành.");
                return;
            }
            boolean success = context.getOrderService().completeOrder(orderId);
            if (success) {
                view.displaySuccess("✅ Đã hoàn thành đơn hàng #" + orderId);
            } else {
                view.displayError("❌ Không thể hoàn thành đơn hàng.");
            }
        }
    }
    public static class CalculateBillHandler implements AIActionHandler {
        @Override
        public void handle(AIResponse response, ServiceContext context, ConsoleView view) {
            Integer bookingId = AIResponseUtils.getInt(response.getParameters(), "bookingId");
            if (bookingId == null) {
                view.displayError("❌ Thiếu ID booking cần tính tiền.");
                return;
            }
            double bill = context.getOrderService().calculateBillForBooking(bookingId);
            view.displaySuccess("💰 Tổng tiền cho booking #" + bookingId + ": " + String.format("%.0f VND", bill));
        }
    }
    public static class GetRevenueHandler implements AIActionHandler {
        @Override
        public void handle(AIResponse response, ServiceContext context, ConsoleView view) {
            double revenue = context.getOrderService().getTotalRevenue();
            view.displaySuccess("💰 Tổng doanh thu: " + String.format("%.0f VND", revenue));
        }
    }
    public static class ShowMenuHandler implements AIActionHandler {
        @Override
        public void handle(AIResponse response, ServiceContext context, ConsoleView view) {
            view.displayMenu(context.getMenuService().getAllMenuItems());
        }
    }
    public static class AddTableHandler implements AIActionHandler {
        @Override
        public void handle(AIResponse response, ServiceContext context, ConsoleView view) {
            Integer capacity = AIResponseUtils.getInt(response.getParameters(), "capacity");
            if (capacity == null || capacity <= 0 || capacity > 12) {
                view.displayError("❌ Sức chứa phải từ 1 đến 12 người.");
                return;
            }
            Table newTable = context.getTableService().addTable(capacity);
            if (newTable != null) {
                view.displaySuccess("✅ Đã thêm bàn mới: Bàn #" + newTable.getTableId() + " cho " + capacity + " người");
            } else {
                view.displayError("❌ Không thể thêm bàn. Vui lòng thử lại.");
            }
        }
    }
    public static class DeleteTableHandler implements AIActionHandler {
        @Override
        public void handle(AIResponse response, ServiceContext context, ConsoleView view) {
            Integer tableId = AIResponseUtils.getInt(response.getParameters(), "tableId");
            if (tableId == null) {
                view.displayError("❌ Thiếu ID bàn cần xóa.");
                return;
            }
            boolean success = context.getTableService().deleteTable(tableId);
            if (success) {
                view.displaySuccess("✅ Đã xóa bàn #" + tableId);
            } else {
                view.displayError("❌ Không thể xóa bàn #" + tableId);
            }
        }
    }
    public static class UpdateTableHandler implements AIActionHandler {
        @Override
        public void handle(AIResponse response, ServiceContext context, ConsoleView view) {
            Integer tableId = AIResponseUtils.getInt(response.getParameters(), "tableId");
            Integer capacity = AIResponseUtils.getInt(response.getParameters(), "capacity");
            String status = AIResponseUtils.getString(response.getParameters(), "status");
            if (tableId == null) {
                view.displayError("❌ Thiếu ID bàn cần cập nhật.");
                return;
            }
            boolean success = context.getTableService().updateTable(tableId, capacity != null ? String.valueOf(capacity) : null, status);
            if (success) {
                view.displaySuccess("✅ Đã cập nhật bàn #" + tableId);
            } else {
                view.displayError("❌ Không thể cập nhật bàn #" + tableId);
            }
        }
    }
    public static class SearchTablesHandler implements AIActionHandler {
        @Override
        public void handle(AIResponse response, ServiceContext context, ConsoleView view) {
            String keyword = AIResponseUtils.getString(response.getParameters(), "keyword");
            if (keyword == null || keyword.trim().isEmpty()) {
                view.displayError("❌ Thiếu từ khóa tìm kiếm.");
                return;
            }
            java.util.List<Table> results = context.getTableService().searchTables(keyword);
            if (results.isEmpty()) {
                view.displayMessage("📝 Không tìm thấy bàn nào phù hợp với: " + keyword);
            } else {
                view.displayMessage("📝 Tìm thấy " + results.size() + " bàn:");
                for (Table table : results) {
                    view.displayMessage("  - Bàn #" + table.getTableId() + " (" + table.getCapacity() + " người, " + table.getStatus() + ")");
                }
            }
        }
    }
    public static class ShowAvailableTablesHandler implements AIActionHandler {
        @Override
        public void handle(AIResponse response, ServiceContext context, ConsoleView view) {
            java.util.List<Table> availableTables = context.getTableService().getAvailableTables();
            if (availableTables.isEmpty()) {
                view.displayMessage("📝 Không có bàn nào có sẵn.");
            } else {
                view.displayMessage("📝 Có " + availableTables.size() + " bàn có sẵn:");
                for (Table table : availableTables) {
                    view.displayMessage("  - Bàn #" + table.getTableId() + " (" + table.getCapacity() + " người, " + table.getStatus() + ")");
                }
            }
        }
    }
    public static class ShowAllTablesHandler implements AIActionHandler {
        @Override
        public void handle(AIResponse response, ServiceContext context, ConsoleView view) {
            java.util.List<Table> allTables = context.getTableService().getAllTables();
            if (allTables.isEmpty()) {
                view.displayMessage("📝 Không có bàn nào.");
            } else {
                view.displayMessage("📝 Có " + allTables.size() + " bàn:");
                for (Table table : allTables) {
                    view.displayMessage("  - Bàn #" + table.getTableId() + " (" + table.getCapacity() + " người, " + table.getStatus() + ")");
                }
            }
        }
    }
    public static class CancelBookingHandler implements AIActionHandler {
        @Override
        public void handle(AIResponse response, ServiceContext context, ConsoleView view) {
            Integer bookingId = AIResponseUtils.getInt(response.getParameters(), "bookingId");
            if (bookingId == null) {
                view.displayError("❌ Thiếu ID đặt bàn cần hủy.");
                return;
            }
            boolean success = context.getBookingService().cancelBooking(bookingId);
            if (success) {
                view.displaySuccess("✅ Đã hủy đặt bàn #" + bookingId);
            } else {
                view.displayError("❌ Không thể hủy đặt bàn #" + bookingId);
            }
        }
    }
    public static class CompleteBookingHandler implements AIActionHandler {
        @Override
        public void handle(AIResponse response, ServiceContext context, ConsoleView view) {
            Integer bookingId = AIResponseUtils.getInt(response.getParameters(), "bookingId");
            if (bookingId == null) {
                view.displayError("❌ Thiếu ID đặt bàn cần hoàn thành.");
                return;
            }
            context.getBookingService().completeBooking(bookingId);
            view.displaySuccess("✅ Đã hoàn thành đặt bàn #" + bookingId);
        }
    }
    public static class UpdateBookingHandler implements AIActionHandler {
        @Override
        public void handle(AIResponse response, ServiceContext context, ConsoleView view) {
            Integer bookingId = AIResponseUtils.getInt(response.getParameters(), "bookingId");
            Integer guests = AIResponseUtils.getInt(response.getParameters(), "guests");
            String dateTime = AIResponseUtils.getString(response.getParameters(), "time");
            if (bookingId == null) {
                view.displayError("❌ Thiếu ID đặt bàn cần cập nhật.");
                return;
            }
            boolean success = context.getBookingService().updateBooking(bookingId, guests != null ? String.valueOf(guests) : null, dateTime);
            if (success) {
                view.displaySuccess("✅ Đã cập nhật đặt bàn #" + bookingId);
            } else {
                view.displayError("❌ Không thể cập nhật đặt bàn #" + bookingId);
            }
        }
    }
    public static class DeleteBookingHandler implements AIActionHandler {
        @Override
        public void handle(AIResponse response, ServiceContext context, ConsoleView view) {
            Integer bookingId = AIResponseUtils.getInt(response.getParameters(), "bookingId");
            if (bookingId == null) {
                view.displayError("❌ Thiếu ID đặt bàn cần xóa.");
                return;
            }
            boolean success = context.getBookingService().deleteBooking(bookingId);
            if (success) {
                view.displaySuccess("✅ Đã xóa đặt bàn #" + bookingId);
            } else {
                view.displayError("❌ Không tìm thấy đặt bàn #" + bookingId + " hoặc không thể xóa.");
            }
        }
    }
    public static class FixDataHandler implements AIActionHandler {
        @Override
        public void handle(AIResponse response, ServiceContext context, ConsoleView view) {
            context.getBookingService().fixBookingsWithNullCustomer();
            view.displaySuccess("✅ Đã kiểm tra và sửa các lỗi dữ liệu.");
        }
    }
    public static class CreateCustomerHandler implements AIActionHandler {
        @Override
        public void handle(AIResponse response, ServiceContext context, ConsoleView view) {
            String name = AIResponseUtils.getString(response.getParameters(), "customerName");
            String phone = AIResponseUtils.getString(response.getParameters(), "customerPhone");
            String email = AIResponseUtils.getString(response.getParameters(), "customerEmail");
            if (name == null || phone == null) {
                view.displayError("❌ Thiếu tên hoặc số điện thoại khách hàng.");
                return;
            }
            Customer customer = context.getCustomerService().createCustomer(name, phone, email);
            if (customer != null) {
                view.displaySuccess("✅ Đã tạo khách hàng: " + name + " (" + phone + ")");
            } else {
                view.displayError("❌ Không thể tạo khách hàng.");
            }
        }
    }
    public static class UpdateCustomerHandler implements AIActionHandler {
        @Override
        public void handle(AIResponse response, ServiceContext context, ConsoleView view) {
            Integer customerId = AIResponseUtils.getInt(response.getParameters(), "customerId");
            String name = AIResponseUtils.getString(response.getParameters(), "customerName");
            String phone = AIResponseUtils.getString(response.getParameters(), "customerPhone");
            String email = AIResponseUtils.getString(response.getParameters(), "customerEmail");
            if (customerId == null) {
                view.displayError("❌ Thiếu ID khách hàng cần cập nhật.");
                return;
            }
            boolean success = context.getCustomerService().updateCustomer(customerId, name, phone, email);
            if (success) {
                view.displaySuccess("✅ Đã cập nhật khách hàng #" + customerId);
            } else {
                view.displayError("❌ Không thể cập nhật khách hàng #" + customerId);
            }
        }
    }
    public static class DeleteCustomerHandler implements AIActionHandler {
        @Override
        public void handle(AIResponse response, ServiceContext context, ConsoleView view) {
            Integer customerId = AIResponseUtils.getInt(response.getParameters(), "customerId");
            if (customerId == null) {
                view.displayError("❌ Thiếu ID khách hàng cần xóa.");
                return;
            }
            boolean success = context.getCustomerService().deleteCustomer(customerId);
            if (success) {
                view.displaySuccess("✅ Đã xóa khách hàng #" + customerId);
            } else {
                view.displayError("❌ Không thể xóa khách hàng #" + customerId + " (có thể đang có đặt bàn)");
            }
        }
    }
    public static class GetCustomerInfoHandler implements AIActionHandler {
        @Override
        public void handle(AIResponse response, ServiceContext context, ConsoleView view) {
            Integer customerId = AIResponseUtils.getInt(response.getParameters(), "customerId");
            if (customerId == null) {
                view.displayError("❌ Thiếu ID khách hàng cần xem thông tin.");
                return;
            }
            CustomerInfo customerInfo = context.getCustomerService().getCustomerInfo(customerId);
            if (customerInfo != null) {
                context.getCustomerService().displayCustomerInfo(customerInfo);
            } else {
                view.displayError("❌ Không tìm thấy khách hàng #" + customerId);
            }
        }
    }
    public static class CustomerSearchHandler implements AIActionHandler {
        @Override
        public void handle(AIResponse response, ServiceContext context, ConsoleView view) {
            String searchTerm = AIResponseUtils.getString(response.getParameters(), "searchTerm");
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                view.displayError("❌ Thiếu từ khóa tìm kiếm.");
                return;
            }
            java.util.List<Customer> results = context.getCustomerService().searchCustomers(searchTerm);
            if (results.isEmpty()) {
                view.displayMessage("📝 Không tìm thấy khách hàng nào phù hợp với: " + searchTerm);
            } else {
                view.displayMessage("📝 Tìm thấy " + results.size() + " khách hàng:");
                for (Customer customer : results) {
                    view.displayMessage("  - " + customer.getName() + " (" + customer.getPhone() + ")");
                }
            }
        }
    }
    public static class CreateBookingHandler implements AIActionHandler {
        @Override
        public void handle(AIResponse response, ServiceContext context, ConsoleView view) {
            String customerName = AIResponseUtils.getString(response.getParameters(), "customerName");
            String customerPhone = AIResponseUtils.getString(response.getParameters(), "customerPhone");
            Integer guests = AIResponseUtils.getInt(response.getParameters(), "guests");
            String dateTime = AIResponseUtils.getString(response.getParameters(), "time");
            if (customerName == null || customerPhone == null || guests == null || dateTime == null) {
                view.displayError("❌ Thiếu thông tin khách hàng hoặc thời gian đặt bàn.");
                return;
            }
            try {
                Customer customer = new Customer(0, customerName, customerPhone);
                java.time.LocalDateTime bookingTime = java.time.LocalDateTime.parse(dateTime, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                Booking booking = context.getBookingService().createBooking(customer, guests, bookingTime);
                if (booking != null) {
                    view.displaySuccess("✅ Đã tạo đặt bàn #" + booking.getBookingId() + " cho " + customerName);
                } else {
                    view.displayError("❌ Không thể tạo đặt bàn. Có thể bàn đã hết hoặc thời gian bị trùng.");
                }
            } catch (Exception e) {
                view.displayError("❌ Lỗi định dạng thời gian. Vui lòng sử dụng định dạng dd/MM/yyyy HH:mm");
            }
        }
    }
    public static class AddMenuHandler implements AIActionHandler {
        @Override
        public void handle(AIResponse response, ServiceContext context, ConsoleView view) {
            String name = AIResponseUtils.getString(response.getParameters(), "name");
            Double price = AIResponseUtils.getDouble(response.getParameters(), "price");
            String description = AIResponseUtils.getString(response.getParameters(), "description");
            if (name == null || price == null) {
                view.displayError("❌ Thiếu thông tin tên hoặc giá món ăn.");
                return;
            }
            MenuItem newItem = context.getMenuService().addMenuItem(name, price, description != null ? description : "");
            if (newItem != null) {
                view.displaySuccess("✅ Đã thêm món ăn mới: " + name + " - " + String.format("%.0f VND", price));
            } else {
                view.displayError("❌ Không thể thêm món ăn. Vui lòng thử lại.");
            }
        }
    }
    public AIActionHandler get(String action) {
        return handlers.get(action);
    }
} 