package restaurantbookingmanagement.controller;

import restaurantbookingmanagement.model.*;
import restaurantbookingmanagement.service.*;
import restaurantbookingmanagement.view.*;
import restaurantbookingmanagement.utils.DebugUtil;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import restaurantbookingmanagement.ai.AIResponse;

/**
 * Controller xử lý các chức năng cho user
 */
public class UserController {
    private final BookingService bookingService;
    private final OrderService orderService;
    private final ConsoleView view;
    private final AuthController authController;
    private final AiController aiController = new AiController();
    private final AiService aiService = new AiService();
    
    // Thêm CustomerService
    private CustomerService customerService;
    
    public void setCustomerService(CustomerService customerService) {
        this.customerService = customerService;
    }
    
    public UserController(BookingService bookingService, OrderService orderService, 
                        ConsoleView view, AuthController authController) {
        this.bookingService = bookingService;
        this.orderService = orderService;
        this.view = view;
        this.authController = authController;
    }
    
    /**
     * Hiển thị menu cho user
     */
    public void showUserMenu() {
        Menu userMenu = new Menu("===== USER MENU =====", new String[]{
            "Xem menu",
            "Xem đơn hàng",
            "Xem bàn",
            "Đặt bàn",
            "Đặt món",
            "Tính tiền",
            "Hủy đặt bàn",
            "Hủy món",
            "Chat với AI",
        }) {
            @Override
            public void execute(int n) {
                switch (n) {
                    case 1 -> view.displayMenu(orderService.getAllMenuItems());
                    case 2 -> view.displayListOrder(orderService.getAllOrders());
                    case 3 -> view.displayTables(bookingService.getAllTables());
                    case 4 -> handleUserBooking();
                    case 5 -> handleUserOrder();
                    case 6 -> handleUserCalculateBill();
                    case 7 -> handleUserCancelBooking();
                    case 8 -> handleRemoveItemFromOrder();
                    case 9 -> chatWithAI();
                    default-> view.displayError("Lựa chọn không hợp lệ.");
                }
            }
        };
        userMenu.run();
    }
    
    /**
     * Xử lý đặt bàn cho user
     */
    private void handleUserBooking() {
        view.displayMessage("--- Đặt Bàn ---");
        view.displayTables(bookingService.getAllTables());
        Customer bookingCustomer = authController.getCurrentCustomer();
        String name, phone, email;
        if (bookingCustomer != null) {
            name = bookingCustomer.getName();
            phone = bookingCustomer.getPhone();
            email = bookingCustomer.getEmail();
            if (email == null || email.isEmpty()) {
                email = view.getInputHandler().getStringWithCancel("Nhập email (bắt buộc):");
                if (email == null) {
                    view.displayMessage("Đã hủy thao tác đặt bàn.");
                    return;
                }
                bookingCustomer.setEmail(email);
            }
        } else {
            name = view.getInputHandler().getStringWithCancel("Nhập tên của bạn:");
            if (name == null) {
                view.displayMessage("Đã hủy thao tác đặt bàn.");
                return;
            }
            phone = view.getInputHandler().getStringWithCancel("Nhập số điện thoại:");
            if (phone == null) {
                view.displayMessage("Đã hủy thao tác đặt bàn.");
                return;
            }
            email = view.getInputHandler().getStringWithCancel("Nhập email:");
            if (email == null) {
                view.displayMessage("Đã hủy thao tác đặt bàn.");
                return;
            }
            bookingCustomer = new Customer(0, name, phone, email, "guest", "");
        }
        Integer guests = getIntWithCancel("Số lượng khách:");
        if (guests == null) {
            view.displayMessage("Đã hủy thao tác đặt bàn.");
            return;
        }
        LocalDateTime bookingTime = null;
        while (bookingTime == null) {
            String dateStr = view.getInputHandler().getStringWithCancel("Nhập ngày giờ đặt bàn (dd/MM/yyyy HH:mm):");
            if (dateStr == null) {
                view.displayMessage("Đã hủy thao tác đặt bàn.");
                return;
            }
            try {
                bookingTime = LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            } catch (Exception e) {
                view.displayError("Định dạng ngày giờ không hợp lệ. Vui lòng thử lại.");
            }
        }
        Table table = bookingService.findAvailableTable(guests);
        if (table == null) {
            view.displayError("Không có bàn phù hợp cho thời gian và số lượng khách này.");
            return;
        }
        Booking booking = bookingService.createBooking(bookingCustomer, guests, bookingTime);
        view.displayBookingConfirmation(booking);
    }
    
    /**
     * Xử lý đặt món cho user
     */
    private void handleUserOrder() {
        view.displayMessage("--- Đặt Món ---");
        // 1. Kiểm tra user đã có booking chưa
        List<Booking> userBookings = null;
        Customer currentCustomer = authController.getCurrentCustomer();
        if (currentCustomer != null) {
            userBookings = bookingService.getBookingsByCustomer(currentCustomer).stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()))
                .toList();
        }
        Booking currentBooking = null;
        if (userBookings != null && userBookings.size() > 1) {
            view.displayMessage("Bạn có nhiều bàn đang đặt. Vui lòng chọn bàn để gọi món:");
            for (int i = 0; i < userBookings.size(); i++) {
                Booking b = userBookings.get(i);
                view.displayMessage((i+1) + ". Bàn #" + b.getTable().getTableId() + " | Thời gian: " + b.getBookingTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            }
            Integer choice = getIntWithCancel("Chọn số thứ tự bàn muốn đặt món:");
            if (choice == null || choice < 1 || choice > userBookings.size()) {
                view.displayMessage("Đã hủy thao tác đặt món.");
                return;
            }
            currentBooking = userBookings.get(choice-1);
        } else if (userBookings != null && userBookings.size() == 1) {
            currentBooking = userBookings.get(0);
        } else {
            currentBooking = findCurrentBooking();
        }
        if (currentBooking == null) {
            view.displayError("❌ Bạn chưa đặt bàn. Vui lòng đặt bàn trước khi gọi món.");
            return;
        }
        // 2. Lấy order hiện tại cho booking
        Order order = orderService.getOrCreateOrderForBooking(currentBooking);
        // 3. Hiển thị menu và hỏi tên hoặc ID món
        view.displayMenu(orderService.getAllMenuItems());
        String input = view.getInputHandler().getStringWithCancel("Nhập tên hoặc ID món:");
        if (input == null) {
            view.displayMessage("Đã hủy thao tác đặt món.");
            return;
        }
        MenuItem item = null;
        try {
            int id = Integer.parseInt(input);
            item = orderService.findMenuItemById(id);
        } catch (NumberFormatException e) {
            item = orderService.findMenuItemByName(input);
        }
        if (item == null) {
            view.displayError("Không tìm thấy món ăn với tên hoặc ID này.");
            return;
        }
        // 4. Hỏi số lượng
        Integer quantity = getIntWithCancel("Nhập số lượng:");
        if (quantity == null) {
            view.displayMessage("Đã hủy thao tác đặt món.");
            return;
        }
        // 5. Thêm món vào order
        boolean added = orderService.addItemToOrder(order.getOrderId(), item.getItemId(), quantity);
        if (added) {
            view.displayMessage("Đã thêm món vào đơn hàng.");
        } else {
            view.displayError("Lỗi khi thêm món vào đơn hàng.");
            return;
        }
        // 6. Hiển thị lại order
        Order updatedOrder = orderService.findOrderById(order.getOrderId());
        if (updatedOrder != null) {
            view.displayOrder(updatedOrder);
        }
    }
    
    /**
     * Xử lý tính tiền cho user
     */
    private void handleUserCalculateBill() {
        view.displayMessage("--- Tính Tiền ---");
        
        // Lấy tất cả booking của user hiện tại
        Customer currentCustomer = authController.getCurrentCustomer();
        final List<Booking> userBookings;
        
        if (currentCustomer != null) {
            userBookings = bookingService.getBookingsByCustomer(currentCustomer).stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()))
                .toList();
        } else {
            userBookings = new ArrayList<>();
        }
        
        if (userBookings == null || userBookings.isEmpty()) {
            view.displayError("❌ Bạn chưa đặt bàn. Vui lòng đặt bàn trước khi tính tiền.");
            return;
        }
        
        // Nếu chỉ có 1 booking, xử lý trực tiếp
        if (userBookings.size() == 1) {
            handleSingleBillPayment(userBookings.get(0));
            return;
        }
        
        // Nếu có nhiều booking, hiển thị menu chọn bàn
        view.displayMessage("Bạn có " + userBookings.size() + " bàn đang hoạt động. Vui lòng chọn bàn để tính tiền:");
        
        String[] bookingOptions = new String[userBookings.size()];
        for (int i = 0; i < userBookings.size(); i++) {
            Booking b = userBookings.get(i);
            Order order = orderService.getOrCreateOrderForBooking(b);
            String status = "COMPLETED".equals(order.getStatus()) ? " (Đã thanh toán)" : " (Chưa thanh toán)";
            bookingOptions[i] = "Bàn #" + b.getTable().getTableId() + 
                               " | " + b.getNumberOfGuests() + " người" +
                               " | " + b.getBookingTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) +
                               status;
        }
        
        Menu bookingSelectionMenu = new Menu("--- CHỌN BÀN TÍNH TIỀN ---", bookingOptions) {
            @Override
            public void execute(int n) {
                if (n >= 1 && n <= userBookings.size()) {
                    Booking selectedBooking = userBookings.get(n - 1);
                    handleSingleBillPayment(selectedBooking);
                    // Thoát khỏi menu chọn bàn sau khi xử lý thanh toán
                    throw new RuntimeException("BILL_PROCESSED");
                } else {
                    view.displayError("Lựa chọn không hợp lệ.");
                }
            }
        };
        
        try {
            bookingSelectionMenu.run();
        } catch (RuntimeException e) {
            if ("BILL_PROCESSED".equals(e.getMessage())) {
                // Thoát khỏi menu chọn bàn một cách bình thường
                return;
            }
            // Nếu là exception khác, ném lại
            throw e;
        }
    }
    
    /**
     * Xử lý thanh toán cho một bàn cụ thể
     */
    private void handleSingleBillPayment(Booking booking) {
        Order order = orderService.getOrCreateOrderForBooking(booking);
        
        // Kiểm tra nếu order đã được thanh toán
        if ("COMPLETED".equals(order.getStatus())) {
            view.displayMessage("Đơn hàng này đã được thanh toán.");
            return;
        }
        
        // Hiển thị chi tiết hóa đơn
        view.displayMessage("=== CHI TIẾT HÓA ĐƠN ===");
        view.displayMessage("Bàn #" + booking.getTable().getTableId());
        view.displayMessage("Khách hàng: " + booking.getCustomer().getName());
        view.displayMessage("Số người: " + booking.getNumberOfGuests());
        view.displayMessage("Thời gian đặt: " + booking.getBookingTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        view.displayBillDetails(order);
        
        // Hiển thị menu động cho action pay hoặc cancel
        Menu paymentMenu = new Menu("--- THANH TOÁN ---", new String[]{
            "Thanh toán",
            "Hủy"
        }) {
            @Override
            public void execute(int n) {
                switch (n) {
                    case 1 -> {
                        handlePayment(order);
                        // Thoát khỏi menu thanh toán sau khi hoàn thành
                        throw new RuntimeException("PAYMENT_COMPLETED");
                    }
                    case 2 -> {
                        view.displayMessage("Đã hủy thanh toán.");
                        // Thoát khỏi menu thanh toán sau khi hủy
                        throw new RuntimeException("PAYMENT_CANCELLED");
                    }
                    default -> view.displayError("Lựa chọn không hợp lệ.");
                }
            }
        };
        
        try {
            paymentMenu.run();
        } catch (RuntimeException e) {
            if ("PAYMENT_COMPLETED".equals(e.getMessage()) || "PAYMENT_CANCELLED".equals(e.getMessage())) {
                // Thoát khỏi menu thanh toán một cách bình thường
                return;
            }
            // Nếu là exception khác, ném lại
            throw e;
        }
    }
    
    /**
     * Xử lý thanh toán
     */
    private void handlePayment(Order order) {
        // Cập nhật trạng thái order thành COMPLETED
        boolean success = orderService.completeOrder(order.getOrderId());
        if (success) {
            view.displaySuccess("✅ Thanh toán thành công!");
            view.displayMessage("Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi.");
            
            // Cập nhật trạng thái booking thành COMPLETED
            Booking booking = order.getBooking();
            if (booking != null) {
                bookingService.completeBooking(booking.getBookingId());
            }
        } else {
            view.displayError("❌ Lỗi khi thanh toán. Vui lòng thử lại.");
        }
    }
    
    /**
     * Xử lý hủy đặt bàn cho user
     */
    private void handleUserCancelBooking() {
        view.displayMessage("--- Hủy Đặt Bàn ---");
        Integer bookingId = getIntWithCancel("Nhập ID đặt bàn:");
        if (bookingId == null) {
            view.displayMessage("Đã hủy thao tác hủy đặt bàn.");
            return;
        }
        boolean deleted = bookingService.deleteBooking(bookingId);
        if (deleted) view.displaySuccess("Đã hủy đặt bàn.");
        else view.displayError("Không tìm thấy đặt bàn với ID này.");
    }
    
    /**
     * Xử lý hủy món cho user
     */
    private void handleRemoveItemFromOrder() {
        // 1. Kiểm tra user đã có booking chưa
        List<Booking> userBookings = null;
        Customer currentCustomer = authController.getCurrentCustomer();
        if (currentCustomer != null) {
            userBookings = bookingService.getBookingsByCustomer(currentCustomer).stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()))
                .toList();
        }
        Booking booking = null;
        if (userBookings != null && userBookings.size() > 1) {
            view.displayMessage("Bạn có nhiều bàn đang đặt. Vui lòng chọn bàn để hủy món:");
            for (int i = 0; i < userBookings.size(); i++) {
                Booking b = userBookings.get(i);
                view.displayMessage((i+1) + ". Bàn #" + b.getTable().getTableId() + " | Thời gian: " + b.getBookingTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            }
            Integer choice = getIntWithCancel("Chọn số thứ tự bàn muốn hủy món:");
            if (choice == null || choice < 1 || choice > userBookings.size()) {
                view.displayMessage("Đã hủy thao tác hủy món.");
                return;
            }
            booking = userBookings.get(choice-1);
        } else if (userBookings != null && userBookings.size() == 1) {
            booking = userBookings.get(0);
        } else {
            booking = findCurrentBooking();
        }
        if (booking == null) {
            view.displayError("❌ Bạn chưa đặt bàn. Vui lòng đặt bàn trước khi hủy món.");
            return;
        }
        Order order = orderService.getOrCreateOrderForBooking(booking);
        if (order.getItems().isEmpty()) {
            view.displayMessage("Đơn hàng chưa có món nào để hủy.");
            return;
        }
        // Hiển thị danh sách món đã đặt
        view.displayMessage("Các món đã đặt:");
        int idx = 1;
        for (Order.OrderItem oi : order.getItems()) {
            view.displayMessage(idx + ". " + oi.getItem().getName() + " (ID: " + oi.getItem().getItemId() + ", SL: " + oi.getAmount() + ")");
            idx++;
        }
        String input = view.getInputHandler().getStringWithCancel("Nhập tên, ID hoặc số thứ tự món muốn hủy:");
        if (input == null) {
            view.displayMessage("Đã hủy thao tác hủy món.");
            return;
        }
        Order.OrderItem toRemove = null;
        try {
            int num = Integer.parseInt(input);
            if (num >= 1 && num <= order.getItems().size()) {
                toRemove = order.getItems().get(num-1);
            } else {
                // Có thể là ID
                for (Order.OrderItem oi : order.getItems()) {
                    if (oi.getItem().getItemId() == num) {
                        toRemove = oi;
                        break;
                    }
                }
            }
        } catch (NumberFormatException e) {
            // Có thể là tên
            for (Order.OrderItem oi : order.getItems()) {
                if (oi.getItem().getName().equalsIgnoreCase(input.trim())) {
                    toRemove = oi;
                    break;
                }
            }
        }
        if (toRemove == null) {
            view.displayError("Không tìm thấy món phù hợp để hủy.");
            return;
        }
        boolean removed = orderService.removeItemFromOrder(order.getOrderId(), toRemove.getItem().getName());
        if (removed) {
            view.displaySuccess("Đã hủy món: " + toRemove.getItem().getName());
        } else {
            view.displayError("Lỗi khi hủy món.");
        }
        // Hiển thị lại order
        Order updatedOrder = orderService.findOrderById(order.getOrderId());
        if (updatedOrder != null) {
            view.displayOrder(updatedOrder);
        }
    }
    
    /**
     * Chat với AI
     */
    private void chatWithAI() {
        view.displayMessage("\n--- Chế độ Chat với AI (gõ 'back' để quay lại menu, 'debug' để bật/tắt debug) ---");
        String sessionId = "user_" + System.currentTimeMillis();
        while (true) {
            String userInput = view.getUserInput();
            if (userInput.equalsIgnoreCase("back") || userInput.equalsIgnoreCase("menu")) {
                view.displayMessage("Quay lại menu chính.");
                break;
            }
            if (userInput.equalsIgnoreCase("debug")) {
                DebugUtil.toggleDebug();
                view.displayMessage("Debug mode: " + (DebugUtil.isDebug() ? "ON" : "OFF"));
                continue;
            }
            if (userInput.isEmpty()) continue;

            // Gửi tới AI agent
            AIResponse response = aiController.chatWithAI(userInput, "USER", sessionId);
            if (response != null) {
                // Xử lý response thông qua AiService
                aiService.processAIResponse(response, orderService, bookingService, customerService, view);
            } else {
                view.displayMessage("AI: Xin lỗi, tôi không thể trả lời lúc này.");
            }
        }
    }
    
    /**
     * Tìm booking hiện tại của customer
     */
    private Booking findCurrentBooking() {
        Customer currentCustomer = authController.getCurrentCustomer();
        if (currentCustomer == null) {
            return null;
        }
        
        // Load customer từ database để có thông tin mới nhất
        Customer dbCustomer = loadCustomerFromDatabase(currentCustomer.getPhone());
        if (dbCustomer != null) {
            authController.setCurrentCustomer(dbCustomer);
        }
        
        // Kiểm tra xem customer có active booking không
        if (!currentCustomer.hasAnyActiveBooking()) {
            return null;
        }
        
        // Lấy booking đầu tiên trong danh sách active
        Integer firstBookingId = currentCustomer.getFirstActiveBookingId();
        if (firstBookingId == null) {
            return null;
        }
        
        return bookingService.findBookingById(firstBookingId);
    }
    
    /**
     * Load customer từ database theo số điện thoại
     */
    private Customer loadCustomerFromDatabase(String phone) {
        // Implementation sẽ được thêm sau
        return null;
    }
    
    /**
     * Hàm tiện ích cho nhập int có hỗ trợ cancel
     */
    private Integer getIntWithCancel(String message) {
        String input = view.getInputHandler().getStringWithCancel(message);
        if (input == null) return null;
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            view.displayError("Giá trị không hợp lệ. Vui lòng nhập số nguyên hoặc 'cancel' để hủy.");
            return getIntWithCancel(message);
        }
    }
} 