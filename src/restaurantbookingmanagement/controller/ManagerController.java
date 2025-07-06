package restaurantbookingmanagement.controller;

import restaurantbookingmanagement.model.*;
import restaurantbookingmanagement.service.*;
import restaurantbookingmanagement.view.*;
import restaurantbookingmanagement.utils.DebugUtil;

/**
 * Controller xử lý các chức năng cho manager
 */
public class ManagerController {
    private final MenuController menuController;
    private final TableController tableController;
    private final CustomerController customerController;
    private final BookingController bookingController;
    private final OrderController orderController;
    private final ConsoleView view;
    private final UserController userController;
    
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
    public void showManagerMenu() {
        Menu managerMenu = new Menu("===== MANAGER MENU =====", new String[]{
            "Quản lý món ăn",
            "Quản lý bàn",
            "Quản lý khách hàng",
            "Quản lý đặt bàn",
            "Quản lý đơn hàng",
            "Xem Menu User",
            "Chat với AI",
            "Bật/Tắt Debug Mode"
        }) {
            @Override
            public void execute(int n) {
                switch (n) {
                    case 1 -> handleMenuManagement();
                    case 2 -> handleTableManagement();
                    case 3 -> handleCustomerManagement();
                    case 4 -> handleBookingManagement();
                    case 5 -> handleOrderManagement();
                    case 6 -> viewMenuUser();
                    case 7 -> chatWithAI();
                    case 8 -> toggleDebugMode();
                    default-> view.displayError("Lựa chọn không hợp lệ.");
                }
            }
        };
        managerMenu.run();
    }
    
    /**
     * Xử lý quản lý món ăn
     */
    private void handleMenuManagement() {
        menuController.handleMenuManagement();
    }
    
    /**
     * Xử lý quản lý bàn
     */
    private void handleTableManagement() {
        tableController.handleTableManagement();
    }
    
    /**
     * Xử lý quản lý khách hàng
     */
    private void handleCustomerManagement() {
        customerController.handleCustomerManagement();
    }
    
    /**
     * Xử lý quản lý đặt bàn
     */
    private void handleBookingManagement() {
        bookingController.handleBookingManagement();
    }
    
    /**
     * Xử lý quản lý đơn hàng
     */
    private void handleOrderManagement() {
        orderController.handleOrderManagement();
    }
    
    /**
     * Xem menu user
     */
    private void viewMenuUser() {
        userController.showUserMenu();
    }
    
    /**
     * Chat với AI
     */
    private void chatWithAI() {
        view.displayMessage("\n--- Chế độ Chat với AI (gõ 'back' để quay lại menu, 'debug' để bật/tắt debug) ---");
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
            view.displayMessage("AI chat chức năng sẽ được implement sau.");
        }
    }
    
    /**
     * Bật/tắt debug mode
     */
    private void toggleDebugMode() {
        DebugUtil.toggleDebug();
        view.displayMessage("Debug mode: " + (DebugUtil.isDebug() ? "ON" : "OFF"));
    }
} 