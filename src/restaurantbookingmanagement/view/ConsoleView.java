package restaurantbookingmanagement.view;

import restaurantbookingmanagement.model.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import restaurantbookingmanagement.utils.InputHandler;
import restaurantbookingmanagement.view.dto.BookingRequest;
import restaurantbookingmanagement.model.Customer;
import restaurantbookingmanagement.view.dto.OrderRequest;

/**
 * View handles console interface
 */
public class ConsoleView {
    private InputHandler inputHandler = new InputHandler();
    
    public ConsoleView() {
    }
    
    
    public void showWelcomeMessage() {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                RESTAURANT BOOKING MANAGEMENT SYSTEM         ║");
        System.out.println("║                     AI RESTAURANT ASSISTANT                 ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Welcome to the smart restaurant booking system!");
        System.out.println("You can use natural language to book tables and order food.");
        System.out.println();
    }
    
    /**
     * Hiển thị màn hình lựa chọn role
     */
    public Role selectRole() {
        System.out.println("🔐 VUI LÒNG CHỌN ROLE ĐĂNG NHẬP:");
        System.out.println("──────────────────────────────────────────────────────────────");
        Role[] roles = Role.values();
        for (int i = 0; i < roles.length; i++) {
            System.out.printf("   %d. %s\n", i + 1, roles[i].getDisplayName());
            System.out.printf("      %s\n", roles[i].getDescription());
        }
        System.out.println("──────────────────────────────────────────────────────────────");
        int choice = inputHandler.getInt("Nhập lựa chọn (1-" + roles.length + "):");
        while (choice < 1 || choice > roles.length) {
            System.out.println("❌ Vui lòng nhập số từ 1 đến " + roles.length);
            choice = inputHandler.getInt("Nhập lựa chọn (1-" + roles.length + "):");
        }
        Role selectedRole = roles[choice - 1];
        System.out.println();
        System.out.println("✅ Đã chọn role: " + selectedRole.getDisplayName());
        System.out.println();
        return selectedRole;
    }
    
    /**
     * Hiển thị thông tin dựa trên role đã chọn
     */
    public void showRoleBasedInfo(Role role) {
        System.out.println("\n🎯 VAI TRÒ HIỆN TẠI: " + role.getDisplayName());
        System.out.println("──────────────────────────────────────────────────────────────");
        
        if (role == Role.USER) {
            System.out.println("👤 BẠN LÀ KHÁCH HÀNG");
            System.out.println("   • Xem menu và đặt bàn");
            System.out.println("   • Gọi món và tính bill");
            System.out.println("   • Hủy đặt bàn của mình");
        } else if (role == Role.MANAGER) {
            System.out.println("👨‍💼 BẠN LÀ QUẢN LÝ");
            System.out.println("   • Tất cả quyền của khách hàng");
            System.out.println("   • Quản lý menu và bàn");
            System.out.println("   • Quản lý khách hàng và đặt bàn");
        }
        
        System.out.println();
        System.out.println("💡 TIP: Gõ 'help' để xem danh sách lệnh chi tiết");
        System.out.println("──────────────────────────────────────────────────────────────");
    }
    
    public InputHandler getInputHandler() {
        return inputHandler;
    }
    
    public String getUserInput() {
        return inputHandler.getString("> ");
    }
    
    public String getUserInput(String prompt) {
        return inputHandler.getString(prompt);
    }
    
    public void displayMessage(String message) {
        System.out.println(message);
    }
    
    public void displayError(String error) {
        System.out.println("❌ Error: " + error);
    }
    
    public void displaySuccess(String message) {
        System.out.println("✅ " + message);
    }
    
    public void displayMenu(List<MenuItem> menu) {
        System.out.println("\n📋 RESTAURANT MENU:");
        System.out.println("──────────────────────────────────────────────────────────────");
        for (MenuItem item : menu) {
            System.out.printf("│ %-2d │ %-25s │ %-8s │ %s\n", 
                item.getItemId(), 
                item.getName(), 
                String.format("%.0f VND", item.getPrice()),
                item.getDescription());
        }
        System.out.println("──────────────────────────────────────────────────────────────");
    }
    
    public void displayTables(List<Table> tables) {
        System.out.println("\n🪑 TABLE LIST:");
        System.out.println("──────────────────────────────────────────────────────────────");
        for (Table table : tables) {
            String statusIcon = getStatusIcon(table.getStatus());
            System.out.printf("│ Table %-2d │ Capacity: %-2d people │ %s %s\n", 
                table.getTableId(), 
                table.getCapacity(),
                statusIcon,
                table.getStatus().getDescription());
        }
        System.out.println("──────────────────────────────────────────────────────────────");
    }
    
    private String getStatusIcon(TableStatus status) {
        switch (status) {
            case AVAILABLE: return "🟢";
            case OCCUPIED: return "🔴";
            case RESERVED: return "🟡";
            case MAINTENANCE: return "🔧";
            default: return "❓";
        }
    }
    
    public void displayBookingConfirmation(Booking booking) {
        System.out.println("\n🎉 BOOKING CONFIRMED SUCCESSFULLY!");
        System.out.println("──────────────────────────────────────────────────────────────");
        System.out.println("📋 Booking Information:");
        System.out.println("   • Booking ID: #" + booking.getBookingId());
        System.out.println("   • Customer: " + booking.getCustomer().getName());
        System.out.println("   • Phone: " + booking.getCustomer().getPhone());
        System.out.println("   • Table: #" + booking.getTable().getTableId());
        System.out.println("   • Guests: " + booking.getNumberOfGuests());
        System.out.println("   • Time: " + booking.getBookingTime().format(
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        System.out.println("   • Status: " + booking.getStatus());
        System.out.println("──────────────────────────────────────────────────────────────");
    }
    
    public void displayListOrder(List<Order> orders) {
        System.out.println("\n🍽️  ORDER DETAILS:");
        System.out.println("──────────────────────────────────────────────────────────────");
        System.out.println(orders.toString());
        System.out.println("──────────────────────────────────────────────────────────────");
    }

    public void displayOrder(Order order) {
        System.out.println("\n🍽️  ORDER DETAILS:");
        System.out.println("──────────────────────────────────────────────────────────────");
        System.out.println(order.toString());
        System.out.println("──────────────────────────────────────────────────────────────");
    }
    
    public void displayBookings(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            System.out.println("📝 No bookings found.");
            return;
        }
        
        System.out.println("\n📝 BOOKING LIST:");
        System.out.println("──────────────────────────────────────────────────────────────");
        for (Booking booking : bookings) {
            System.out.println("• " + booking.toString());
        }
        System.out.println("──────────────────────────────────────────────────────────────");
    }
    
    /**
     * Hiển thị danh sách booking kèm tổng tiền (nếu có order)
     */
    public void displayBookingsWithTotal(List<Booking> bookings, List<Order> orders) {
        if (bookings.isEmpty()) {
            System.out.println("📝 No bookings found.");
            return;
        }
        System.out.println("\n📝 BOOKING LIST:");
        System.out.println("──────────────────────────────────────────────────────────────────────────────");
        for (Booking booking : bookings) {
            String bookingInfo = booking.toString();
            double total = 0.0;
            if (orders != null) {
                for (Order order : orders) {
                    if (order.getBooking() != null && order.getBooking().getBookingId() == booking.getBookingId()) {
                        total += order.getTotalAmount();
                    }
                }
            }
            if (total > 0) {
                bookingInfo += String.format(" | Tổng tiền: %.0f VND", total);
            }
            System.out.println("• " + bookingInfo);
        }
        System.out.println("──────────────────────────────────────────────────────────────────────────────");
    }
    
    /**
     * Hiển thị hướng dẫn sử dụng
     */
    public void displayHelp() {
        System.out.println("\n📖 HƯỚNG DẪN SỬ DỤNG");
        System.out.println("──────────────────────────────────────────────────────────────");
        System.out.println("🍽️  LỆNH CƠ BẢN:");
        System.out.println("   • show menu - Xem menu");
        System.out.println("   • show tables - Xem bàn");
        System.out.println("   • show bookings - Xem đặt bàn");
        System.out.println("   • help - Xem hướng dẫn");
        System.out.println("   • exit - Thoát");
        System.out.println();
        System.out.println("💡 TIP: Bạn có thể nói tiếng Việt tự nhiên với AI!");
        System.out.println("   Ví dụ: 'cho tôi xem menu', 'đặt bàn tối nay 6h 4 người'");
        System.out.println("──────────────────────────────────────────────────────────────");
    }
    
    /**
     * Hiển thị hướng dẫn sử dụng dựa trên role
     */
    public void displayHelp(Role role) {
        System.out.println("\n📖 HƯỚNG DẪN SỬ DỤNG CHO " + role.getDisplayName().toUpperCase());
        System.out.println("──────────────────────────────────────────────────────────────");
        System.out.println("🍽️  LỆNH CƠ BẢN:");
        System.out.println("   • show menu - Xem menu");
        System.out.println("   • show tables - Xem bàn");
        System.out.println("   • show bookings - Xem đặt bàn");
        System.out.println("   • help - Xem hướng dẫn");
        System.out.println("   • exit - Thoát");
        
        if (role == Role.MANAGER) {
            System.out.println();
            System.out.println("🔧 LỆNH QUẢN LÝ (CHỈ DÀNH CHO MANAGER):");
            System.out.println("   • add menu <tên> <giá> <mô tả> - Thêm món ăn");
            System.out.println("   • delete menu <id> - Xóa món ăn");
            System.out.println("   • add table <sức chứa> - Thêm bàn");
            System.out.println("   • delete booking <id> - Xóa đặt bàn");
            System.out.println("   • customers - Xem danh sách khách hàng");
            System.out.println("   • fix - Sửa lỗi dữ liệu");
        }
        
        System.out.println();
        System.out.println("💡 TIP: Bạn có thể nói tiếng Việt tự nhiên với AI!");
        System.out.println("   Ví dụ: 'cho tôi xem menu', 'đặt bàn tối nay 6h 4 người'");
        if (role == Role.MANAGER) {
            System.out.println("   Ví dụ: 'thêm món phở bò giá 45000', 'xóa đặt bàn số 5'");
        }
        System.out.println("──────────────────────────────────────────────────────────────");
    }
    
    public void displayGoodbye() {
        System.out.println("\n👋 Thank you for using our system!");
        System.out.println("See you again! ️");
    }
    
    public void displayBillDetails(Order order) {
        System.out.println("\n🧾 CHI TIẾT BILL:");
        System.out.println("──────────────────────────────────────────────────────────────");
        System.out.printf("%-25s | %-8s | %-10s | %-10s\n", "Tên món", "Số lượng", "Đơn giá", "Thành tiền");
        System.out.println("──────────────────────────────────────────────────────────────");
        double total = 0.0;
        for (Order.OrderItem oi : order.getItems()) {
            String name = oi.getItem().getName();
            int qty = oi.getAmount();
            double price = oi.getItem().getPrice();
            double lineTotal = price * qty;
            total += lineTotal;
            System.out.printf("%-25s | %-8d | %-10.0f | %-10.0f\n", name, qty, price, lineTotal);
        }
        System.out.println("──────────────────────────────────────────────────────────────");
        System.out.printf("%-25s   %-8s   %-10s   %-10.0f VND\n", "TỔNG CỘNG", "", "", total);
        System.out.println("──────────────────────────────────────────────────────────────");
    }
    
    public void displayOrderTable(Order order) {
        System.out.println("\n🧾 CHI TIẾT ĐƠN HÀNG:");
        System.out.println("──────────────────────────────────────────────────────────────");
        System.out.printf("%-25s | %-8s | %-10s | %-10s\n", "Tên món", "Số lượng", "Đơn giá", "Thành tiền");
        System.out.println("──────────────────────────────────────────────────────────────");
        double total = 0.0;
        for (Order.OrderItem oi : order.getItems()) {
            String name = oi.getItem() != null ? oi.getItem().getName() : ("ID: " + oi.getItemId());
            int qty = oi.getAmount();
            double price = oi.getItem() != null ? oi.getItem().getPrice() : 0.0;
            double lineTotal = price * qty;
            total += lineTotal;
            System.out.printf("%-25s | %-8d | %-10.0f | %-10.0f\n", name, qty, price, lineTotal);
        }
        System.out.println("──────────────────────────────────────────────────────────────");
        System.out.printf("%-25s   %-8s   %-10s   %-10.0f VND\n", "TỔNG CỘNG", "", "", total);
        System.out.println("──────────────────────────────────────────────────────────────");
    }
    
    /**
     * Gom toàn bộ input đặt bàn, trả về BookingRequest DTO
     */
    public BookingRequest getBookingRequest(Customer currentCustomer) {
        String name = null, phone = null, email = null;
        if (currentCustomer != null) {
            name = currentCustomer.getName();
            phone = currentCustomer.getPhone();
            email = currentCustomer.getEmail();
            if (email == null || email.isEmpty()) {
                email = inputHandler.getStringWithCancel("Nhập email (bắt buộc):");
                if (email == null) return null;
            }
        } else {
            name = inputHandler.getStringWithCancel("Nhập tên của bạn:");
            if (name == null) return null;
            phone = inputHandler.getStringWithCancel("Nhập số điện thoại:");
            if (phone == null) return null;
            email = inputHandler.getStringWithCancel("Nhập email:");
            if (email == null) return null;
        }
        Integer guests = null;
        while (guests == null) {
            String guestsStr = inputHandler.getStringWithCancel("Số lượng khách:");
            if (guestsStr == null) return null;
            try { guests = Integer.parseInt(guestsStr); } catch (Exception e) { displayError("Số lượng không hợp lệ."); }
        }
        java.time.LocalDateTime bookingTime = null;
        while (bookingTime == null) {
            String dateStr = inputHandler.getStringWithCancel("Nhập ngày giờ đặt bàn (dd/MM/yyyy HH:mm):");
            if (dateStr == null) return null;
            try {
                bookingTime = java.time.LocalDateTime.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            } catch (Exception e) { displayError("Định dạng ngày giờ không hợp lệ. Vui lòng thử lại."); }
        }
        return new BookingRequest(name, phone, email, guests, bookingTime);
    }
    
    /**
     * Gom toàn bộ input đặt món, trả về OrderRequest DTO
     */
    public OrderRequest getOrderRequest(List<Booking> userBookings, List<MenuItem> menuItems) {
        if (userBookings == null || userBookings.isEmpty()) {
            displayError("❌ Bạn chưa đặt bàn. Vui lòng đặt bàn trước khi gọi món.");
            return null;
        }
        Booking selectedBooking = null;
        if (userBookings.size() > 1) {
            displayMessage("Bạn có nhiều bàn đang đặt. Vui lòng chọn bàn để gọi món:");
            for (int i = 0; i < userBookings.size(); i++) {
                Booking b = userBookings.get(i);
                displayMessage((i+1) + ". Bàn #" + b.getTable().getTableId() + " | Thời gian: " + b.getBookingTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            }
            Integer choice = null;
            while (choice == null || choice < 1 || choice > userBookings.size()) {
                choice = getInputHandler().getInt("Chọn số thứ tự bàn muốn đặt món:");
            }
            selectedBooking = userBookings.get(choice-1);
        } else {
            selectedBooking = userBookings.get(0);
        }
        displayMenu(menuItems);
        String input = getInputHandler().getStringWithCancel("Nhập tên hoặc ID món:");
        if (input == null) return null;
        Integer quantity = null;
        while (quantity == null) {
            String qtyStr = getInputHandler().getStringWithCancel("Nhập số lượng:");
            if (qtyStr == null) return null;
            try { quantity = Integer.parseInt(qtyStr); } catch (Exception e) { displayError("Số lượng không hợp lệ."); }
        }
        return new OrderRequest(selectedBooking, input, quantity);
    }
    
    /**
     * Gom input chọn booking cần thanh toán, trả về Booking hoặc null
     */
    public Booking getBookingForPayment(List<Booking> userBookings) {
        if (userBookings == null || userBookings.isEmpty()) {
            displayError("❌ Bạn chưa đặt bàn. Vui lòng đặt bàn trước khi tính tiền.");
            return null;
        }
        if (userBookings.size() == 1) {
            return userBookings.get(0);
        }
        displayMessage("Bạn có " + userBookings.size() + " bàn đang hoạt động. Vui lòng chọn bàn để tính tiền:");
        for (int i = 0; i < userBookings.size(); i++) {
            Booking b = userBookings.get(i);
            displayMessage((i+1) + ". Bàn #" + b.getTable().getTableId() +
                " | " + b.getNumberOfGuests() + " người" +
                " | " + b.getBookingTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }
        Integer choice = null;
        while (choice == null || choice < 1 || choice > userBookings.size()) {
            choice = getInputHandler().getInt("Chọn số thứ tự bàn muốn thanh toán:");
        }
        return userBookings.get(choice-1);
    }
    
    /**
     * Gom input chọn booking cần hủy, trả về bookingId hoặc null
     */
    public Integer getBookingIdForCancel(List<Booking> userBookings) {
        if (userBookings == null || userBookings.isEmpty()) {
            displayError("❌ Bạn chưa đặt bàn nào để hủy.");
            return null;
        }
        displayMessage("Chọn ID đặt bàn cần hủy:");
        for (Booking b : userBookings) {
            displayMessage("- ID: " + b.getBookingId() + ", Bàn: #" + b.getTable().getTableId() + ", Thời gian: " + b.getBookingTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }
        Integer bookingId = null;
        while (bookingId == null) {
            String input = getInputHandler().getStringWithCancel("Nhập ID đặt bàn:");
            if (input == null) return null;
            try { bookingId = Integer.parseInt(input); } catch (Exception e) { displayError("ID không hợp lệ."); }
        }
        return bookingId;
    }
    
    /**
     * Gom input chọn món cần hủy khỏi order, trả về Order.OrderItem hoặc null
     */
    public Order.OrderItem getOrderItemForRemove(Order order) {
        if (order == null || order.getItems().isEmpty()) {
            displayMessage("Đơn hàng chưa có món nào để hủy.");
            return null;
        }
        displayMessage("Các món đã đặt:");
        int idx = 1;
        for (Order.OrderItem oi : order.getItems()) {
            displayMessage(idx + ". " + oi.getItem().getName() + " (ID: " + oi.getItem().getItemId() + ", SL: " + oi.getAmount() + ")");
            idx++;
        }
        String input = getInputHandler().getStringWithCancel("Nhập tên, ID hoặc số thứ tự món muốn hủy:");
        if (input == null) return null;
        Order.OrderItem toRemove = null;
        try {
            int num = Integer.parseInt(input);
            if (num >= 1 && num <= order.getItems().size()) {
                toRemove = order.getItems().get(num-1);
            } else {
                for (Order.OrderItem oi : order.getItems()) {
                    if (oi.getItem().getItemId() == num) {
                        toRemove = oi;
                        break;
                    }
                }
            }
        } catch (NumberFormatException e) {
            for (Order.OrderItem oi : order.getItems()) {
                if (oi.getItem().getName().equalsIgnoreCase(input.trim())) {
                    toRemove = oi;
                    break;
                }
            }
        }
        if (toRemove == null) {
            displayError("Không tìm thấy món phù hợp để hủy.");
        }
        return toRemove;
    }
    
    // ==== ĐĂNG NHẬP/ĐĂNG KÝ ==== //
    /**
     * Nhập tên đăng nhập (có thể mở rộng validate)
     */
    public String getLoginName() {
        displayMessage("--- Đăng nhập ---");
        return inputHandler.getStringWithCancel("Nhập tên:");
    }

    /**
     * Nhập mật khẩu đăng nhập
     */
    public String getLoginPassword() {
        return inputHandler.getStringWithCancel("Nhập mật khẩu:");
    }

    /**
     * Hiển thị lỗi đăng nhập
     */
    public void displayLoginError(String msg) {
        displayError(msg);
    }

    /**
     * Hiển thị thành công đăng nhập
     */
    public void displayLoginSuccess(Role role) {
        displaySuccess("Đăng nhập thành công với vai trò: " + role);
    }

    /**
     * Gom toàn bộ input đăng ký, trả về Customer (hoặc null nếu hủy)
     */
    public Customer getRegisterInfo(int nextCustomerId) {
        displayMessage("--- Đăng ký tài khoản mới ---");
        String name = inputHandler.getStringWithCancel("Nhập tên:");
        if (name == null) return null;
        String phone = inputHandler.getStringWithCancel("Nhập số điện thoại:");
        if (phone == null) return null;
        String email = inputHandler.getStringWithCancel("Nhập email:");
        if (email == null) return null;
        String password = inputHandler.getStringWithCancel("Tạo mật khẩu:");
        if (password == null) return null;
        return new Customer(nextCustomerId, name, phone, email, "user", password);
    }

    /**
     * Hiển thị lỗi đăng ký
     */
    public void displayRegisterError(String msg) {
        displayError(msg);
    }

    /**
     * Hiển thị thành công đăng ký
     */
    public void displayRegisterSuccess() {
        displaySuccess("Đăng ký thành công. Đăng nhập tự động...");
    }

    /**
     * Hiển thị chế độ guest
     */
    public void displayGuestMode() {
        displayMessage("--- Tiếp tục với tư cách khách (guest) ---");
    }

    /**
     * Nhập số nguyên có hỗ trợ cancel (gom logic lặp lại từ controller)
     */
    public Integer getIntWithCancel(String message) {
        String input = inputHandler.getStringWithCancel(message);
        if (input == null) return null;
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            displayError("Giá trị không hợp lệ. Vui lòng nhập số nguyên hoặc 'cancel' để hủy.");
            return getIntWithCancel(message);
        }
    }

    /**
     * Hiển thị danh sách khách hàng
     */
    public void displayAllCustomers(List<Customer> customers) {
        if (customers == null || customers.isEmpty()) {
            displayMessage("Không có khách hàng nào.");
            return;
        }
        displayMessage("\nDANH SÁCH KHÁCH HÀNG:");
        for (Customer c : customers) {
            displayMessage("- ID: " + c.getCustomerId() + ", Tên: " + c.getName() + ", SĐT: " + c.getPhone() + (c.getEmail() != null && !c.getEmail().isEmpty() ? ", Email: " + c.getEmail() : ""));
        }
    }
} 