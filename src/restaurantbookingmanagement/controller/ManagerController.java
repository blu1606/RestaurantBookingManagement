package restaurantbookingmanagement.controller;

import restaurantbookingmanagement.view.*;
import restaurantbookingmanagement.utils.DebugUtil;
import restaurantbookingmanagement.ai.AIResponse;

/**
 * Controller xử lý các chức năng cho manager (chỉ điều phối, không chứa logic nhập/xuất hoặc nghiệp vụ chi tiết)
 */
public class ManagerController {
    private final MenuController menuController;
    private final TableController tableController;
    private final CustomerController customerController;
    private final BookingController bookingController;
    private final OrderController orderController;
    private final ConsoleView view;
    private final UserController userController;
    private final AiController aiController = new AiController();
    private final restaurantbookingmanagement.service.AiService aiService = new restaurantbookingmanagement.service.AiService();
    
    public ManagerController(MenuController menuController, TableController tableController,
                           CustomerController customerController, BookingController bookingController,
                           OrderController orderController, ConsoleView view, UserController userController) {
        this.menuController = menuController;
        this.tableController = tableController;
        this.customerController = customerController;
        this.bookingController = bookingController;
        this.orderController = orderController;
        this.view = view;
        this.userController = userController;
    }
    
    /**
     * Hiển thị menu cho manager
     */
    public boolean showManagerMenuWithLogout() {
        final boolean[] logout = {false};
        Menu managerMenu = new Menu("===== MANAGER MENU =====", new String[]{
            "Quản lý món ăn",
            "Quản lý bàn",
            "Quản lý khách hàng",
            "Quản lý đặt bàn",
            "Quản lý đơn hàng",
            "Xem Menu User",
            "Chat với AI",
            "Bật/Tắt Debug Mode",
            "Đăng xuất"
        }) {
            @Override
            public void execute(int n) {
                switch (n) {
                    case 1 -> menuController.handleMenuManagement();
                    case 2 -> tableController.handleTableManagement();
                    case 3 -> customerController.handleCustomerManagement();
                    case 4 -> bookingController.handleBookingManagement();
                    case 5 -> orderController.handleOrderManagement();
                    case 6 -> userController.showUserMenuWithLogout();
                    case 7 -> chatWithAI();
                    case 8 -> toggleDebugMode();
                    case 9 -> { logout[0] = true; throw new RuntimeException("LOGOUT"); }
                    default-> view.displayError("Lựa chọn không hợp lệ.");
                }
            }
        };
        try {
        managerMenu.run();
        } catch (RuntimeException e) {
            if ("LOGOUT".equals(e.getMessage())) {
                return true;
    }
            throw e;
        }
        return logout[0];
    }
    
    /**
     * Chat với AI (chỉ điều phối, nhập xuất đã gom về view)
     */
    private void chatWithAI() {
        view.displayMessage("\n--- Chế độ Chat với AI (gõ 'back' để quay lại menu, 'debug' để bật/tắt debug) ---");
        String sessionId = "manager_" + System.currentTimeMillis();
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
            AIResponse response = aiController.chatWithAI(userInput, "MANAGER", null);
            if (response != null) {
                            // Xử lý response thông qua AiService với đầy đủ services
            aiService.processAIResponse(response, orderController.getOrderService(), 
                                     bookingController.getBookingService(), 
                                     customerController.getCustomerService(), view);
            } else {
                view.displayMessage("AI: Xin lỗi, tôi không thể trả lời lúc này.");
            }
        }
    }
    
    /**
     * Bật/tắt debug mode (chỉ điều phối)
     */
    private void toggleDebugMode() {
        DebugUtil.toggleDebug();
        view.displayMessage("Debug mode: " + (DebugUtil.isDebug() ? "ON" : "OFF"));
    }
} 