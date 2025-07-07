package restaurantbookingmanagement.controller;

import restaurantbookingmanagement.model.*;
import restaurantbookingmanagement.service.*;
import restaurantbookingmanagement.view.*;

/**
 * Controller chính điều phối toàn bộ ứng dụng
 */
public class MainController {
    private final AuthController authController;
    private final UserController userController;
    private final ManagerController managerController;
    private final ConsoleView view;
    private final MenuService menuService;
    private final TableService tableService;
    
    public MainController(BookingService bookingService, OrderService orderService, 
                        CustomerService customerService, ConsoleView view) {
        this.view = view;
        this.menuService = new MenuService();
        this.tableService = new TableService();
        
        // Khởi tạo các controller
        this.authController = new AuthController(customerService, view);
        
        // Khởi tạo các controller con
        MenuController menuController = new MenuController(menuService, view);
        TableController tableController = new TableController(tableService, view);
        CustomerController customerController = new CustomerController(customerService, view);
        BookingController bookingController = new BookingController(bookingService, view);
        OrderController orderController = new OrderController(orderService, view, menuService);
        
        // Khởi tạo user controller
        this.userController = new UserController(bookingService, orderService, view, authController);
        this.userController.setCustomerService(customerService);
        
        // Khởi tạo manager controller
        this.managerController = new ManagerController(menuController, tableController, 
                                                    customerController, bookingController, 
                                                    orderController, view, userController);
    }
    
    /**
     * Chạy ứng dụng chính
     */
    public void run() {
        view.showWelcomeMessage();
        showEntryMenu();
    }
    
    /**
     * Hiển thị menu đăng nhập/đăng ký
     */
    private void showEntryMenu() {
        authController.showEntryMenu();
        
        // Sau khi đăng nhập/đăng ký, hiển thị menu tương ứng
        Role currentRole = authController.getCurrentRole();
        if (currentRole == Role.MANAGER) {
            managerController.showManagerMenu();
        } else {
            userController.showUserMenu();
        }
    }
} 