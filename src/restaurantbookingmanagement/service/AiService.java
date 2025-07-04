package restaurantbookingmanagement.service;

import restaurantbookingmanagement.model.*;
import restaurantbookingmanagement.ai.AIResponse;
import restaurantbookingmanagement.view.ConsoleView;
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
} 