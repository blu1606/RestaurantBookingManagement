package restaurantbookingmanagement.controller;

import restaurantbookingmanagement.model.*;
import restaurantbookingmanagement.service.*;
import restaurantbookingmanagement.view.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import restaurantbookingmanagement.ai.AIResponse;
import restaurantbookingmanagement.view.dto.BookingRequest;
import restaurantbookingmanagement.view.dto.OrderRequest;

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
        
    // Thêm trường MenuService, TableService
    private MenuService menuService;
    private TableService tableService;
    private CustomerService customerService;
    
    
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
    public boolean showUserMenuWithLogout() {
        final boolean[] logout = {false};
        Menu userMenu = new Menu("===== USER MENU =====", new String[]{
            "Xem menu",
            "Xem bookings",
            "Xem orders",
            "Xem bàn",
            "Đặt bàn",
            "Đặt món",
            "Tính tiền",
            "Hủy đặt bàn",
            "Hủy món",
            "Chat với AI",
            "Đăng xuất"
        }) {
            @Override
            public void execute(int n) {
                switch (n) {
                    case 1 -> {
                        if (menuService == null) {
                            view.displayError("Lỗi hệ thống: MenuService chưa được khởi tạo.");
                            return;
                        }
                        view.displayMenu(menuService.getAllMenuItems());
                    }
                    case 2 -> {
                        Customer currentCustomer = authController.getCurrentCustomer();
                        if (currentCustomer == null) {
                            view.displayError("Bạn chưa đăng nhập.");
                            return;
                        }
                        List<Booking> userBookings = bookingService.getBookingsByCustomer(currentCustomer);
                        List<Order> allOrders = orderService.getAllOrders();
                        view.displayBookingsWithTotal(userBookings, allOrders);
                    }
                    case 3 -> {
                        Customer currentCustomer = authController.getCurrentCustomer();
                        List<Order> orders = orderService.getOrdersByCustomer(currentCustomer);
                        if (orders.isEmpty()) {
                            view.displayMessage("Không có đơn hàng nào.");
                        } else {
                            for (Order order : orders) {
                                view.displayOrderTable(order);
                            }
                        }
                    }
                    case 4 -> {
                        if (tableService == null) {
                            view.displayError("Lỗi hệ thống: TableService chưa được khởi tạo.");
                            return;
                        }
                        view.displayTables(tableService.getAllTables());
                    }
                    case 5 -> handleUserBooking();
                    case 6 -> handleUserOrder();
                    case 7 -> handleUserCalculateBill();
                    case 8 -> handleUserCancelBooking();
                    case 9 -> handleRemoveItemFromOrder();
                    case 10 -> chatWithAI();
                    case 11 -> { logout[0] = true; throw new RuntimeException("LOGOUT"); }
                    default-> view.displayError("Lựa chọn không hợp lệ.");
                }
            }
        };
        try {
            userMenu.run();
        } catch (RuntimeException e) {
            if ("LOGOUT".equals(e.getMessage())) {
                return true;
            }
            throw e;
        }
        return logout[0];
    }
    
    /**
     * Xử lý đặt bàn cho user
     */
    private void handleUserBooking() {
        view.displayMessage("--- Đặt Bàn ---");
        if (tableService == null) {
            view.displayError("Lỗi hệ thống: TableService chưa được khởi tạo.");
            return;
        }
        view.displayTables(tableService.getAllTables());
        BookingRequest req = view.getBookingRequest(authController.getCurrentCustomer());
        if (req == null) {
            view.displayMessage("Đã hủy thao tác đặt bàn.");
            return;
        }
        Booking booking = bookingService.createBooking(req);
        if (booking == null) {
            view.displayError("Không có bàn phù hợp hoặc thời gian bị trùng.");
            return;
        }
        view.displayBookingConfirmation(booking);
    }
    
    /**
     * Xử lý đặt món cho user
     */
    private void handleUserOrder() {
        view.displayMessage("--- Đặt Món ---");
        Customer currentCustomer = authController.getCurrentCustomer();
        List<Booking> userBookings = (currentCustomer != null)
            ? bookingService.getBookingsByCustomer(currentCustomer).stream().filter(b -> "CONFIRMED".equals(b.getStatus())).toList()
            : null;
        OrderRequest req = view.getOrderRequest(userBookings, menuService.getAllMenuItems());
        if (req == null) {
            view.displayMessage("Đã hủy thao tác đặt món.");
            return;
        }
        boolean added = orderService.addOrderItem(req, menuService);
        if (added) {
            view.displayMessage("Đã thêm món vào đơn hàng.");
            Order updatedOrder = orderService.getOrderForBooking(req.getBooking());
            if (updatedOrder != null) {
                view.displayOrder(updatedOrder);
            }
        } else {
            view.displayError("Lỗi khi thêm món vào đơn hàng.");
        }
    }
    
    /**
     * Xử lý tính tiền cho user
     */
    private void handleUserCalculateBill() {
        view.displayMessage("--- Tính Tiền ---");
        Customer currentCustomer = authController.getCurrentCustomer();
        List<Booking> userBookings = (currentCustomer != null)
            ? bookingService.getBookingsByCustomer(currentCustomer).stream().filter(b -> "CONFIRMED".equals(b.getStatus())).toList()
            : new ArrayList<>();
        Booking booking = view.getBookingForPayment(userBookings);
        if (booking == null) return;
        handleSingleBillPayment(booking);
    }
    
    /**
     * Xử lý thanh toán cho một bàn cụ thể
     */
    private void handleSingleBillPayment(Booking booking) {
        Order order = orderService.getOrderForBooking(booking);
        
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
        Customer currentCustomer = authController.getCurrentCustomer();
        List<Booking> userBookings = (currentCustomer != null)
            ? bookingService.getBookingsByCustomer(currentCustomer).stream().filter(b -> "CONFIRMED".equals(b.getStatus())).toList()
            : new ArrayList<>();
        Integer bookingId = view.getBookingIdForCancel(userBookings);
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
        Customer currentCustomer = authController.getCurrentCustomer();
        List<Booking> userBookings = (currentCustomer != null)
            ? bookingService.getBookingsByCustomer(currentCustomer).stream().filter(b -> "CONFIRMED".equals(b.getStatus())).toList()
            : null;
        Booking booking = (userBookings != null && !userBookings.isEmpty()) ? userBookings.get(0) : null;
        if (userBookings != null && userBookings.size() > 1) {
            booking = view.getBookingForPayment(userBookings); // reuse chọn booking
        } else if (booking == null) {
            booking = findCurrentBooking();
        }
        if (booking == null) {
            view.displayError("❌ Bạn chưa đặt bàn. Vui lòng đặt bàn trước khi hủy món.");
            return;
        }
        Order order = orderService.getOrderForBooking(booking);
        Order.OrderItem toRemove = view.getOrderItemForRemove(order);
        if (toRemove == null) {
            view.displayMessage("Đã hủy thao tác hủy món.");
            return;
        }
        boolean removed = orderService.removeItemFromOrder(order.getOrderId(), toRemove.getItem().getName());
        if (removed) {
            view.displaySuccess("Đã hủy món: " + toRemove.getItem().getName());
        } else {
            view.displayError("Lỗi khi hủy món.");
        }
        Order updatedOrder = orderService.findOrderById(order.getOrderId());
        if (updatedOrder != null) {
            view.displayOrder(updatedOrder);
        }
    }
    
    /**
     * Chat với AI
     */
    private void chatWithAI() {
        Customer currentCustomer = authController.getCurrentCustomer();
        if (currentCustomer == null) {
            view.displayError("Bạn cần đăng nhập trước khi đặt bàn qua AI.");
            return;
        }
        view.displayMessage("\n--- Chế độ Chat với AI (gõ 'back' để quay lại menu) ---");
        while (true) {
            String userInput = view.getUserInput();
            if (userInput == null || userInput.equalsIgnoreCase("back") || userInput.equalsIgnoreCase("menu")) {
                view.displayMessage("Quay lại menu chính.");
                break;
        }
        // Gọi AI Agent, truyền thông tin user
        AIResponse aiResponse = aiController.chatWithAI(userInput, "user", currentCustomer);
        // Xử lý action từ AIResponse qua AiService
        aiService.processAIResponse(aiResponse, orderService, bookingService, customerService, view);
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

    public void setCustomerService(CustomerService customerService) {
        this.customerService = customerService;
    }

    public void setTableService(TableService tableService) {
        this.tableService = tableService;
    }

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

   
} 