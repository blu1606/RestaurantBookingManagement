package restaurantbookingmanagement.view;

import restaurantbookingmanagement.model.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import restaurantbookingmanagement.utils.InputHandler;

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
        System.out.println("See you again! ��️");
    }
    
} 