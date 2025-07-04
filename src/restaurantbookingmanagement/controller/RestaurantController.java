package restaurantbookingmanagement.controller;

import restaurantbookingmanagement.model.*;
import restaurantbookingmanagement.service.*;
import restaurantbookingmanagement.view.*;
import restaurantbookingmanagement.ai.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import restaurantbookingmanagement.utils.DebugUtil;

/**
 * Controller manages application flow
 */
public class RestaurantController {
    private final BookingService bookingService;
    private final OrderService orderService;
    private final CustomerService customerService;
    private final ConsoleView view;
    private final AIAgentConnector aiConnector;
    private final AiService aiService = new AiService();
    private Customer currentCustomer;
    private Role currentRole;
    private int currentCustomerId;
    public RestaurantController(BookingService bookingService, OrderService orderService, ConsoleView view) {
        this.bookingService = bookingService;
        this.orderService = orderService;
        this.customerService = new CustomerService();
        this.view = view;
        this.aiConnector = new AIAgentConnector();
        this.currentCustomer = null;
        this.currentRole = null;
        this.currentCustomerId = getCurrentCustomerId();
    }
    
    /**
     * Run main application
     */
    public void run() {
        view.showWelcomeMessage();
        showEntryMenu();
    }

    // Menu động đầu vào: Đăng nhập, Đăng ký, Guest
    private void showEntryMenu() {
        String[] options = new String[]{"Đăng nhập", "Đăng ký", "Tiếp tục với tư cách khách (guest)"};
        Menu entryMenu = new Menu("===== ENTRY MENU =====", options) {
            @Override
            public void execute(int n) {
                switch (n) {
                    case 1 -> handleLogin();
                    case 2 -> handleRegister();
                    case 3 -> handleGuest();
                }
            }
        };
        entryMenu.run();
    }

    // Đăng nhập
    private boolean handleLogin() {
        view.displayMessage("--- Đăng nhập ---");
        String name = view.getInputHandler().getStringWithCancel("Nhập tên:");
        if (name == null) return false;
        String password = view.getInputHandler().getStringWithCancel("Nhập mật khẩu:");
        if (password == null) return false;
        Customer customer = customerService.findCustomerByName(name);
        if (customer == null || !password.equals(customer.getPassword())) {
            view.displayError("Sai tên hoặc mật khẩu.");
            return false;
        }
        currentCustomer = customer;
        currentRole = customer.getRole().equalsIgnoreCase("admin") ? Role.MANAGER : Role.USER;
        view.displaySuccess("Đăng nhập thành công với vai trò: " + currentRole);
        if (currentRole == Role.MANAGER) showManagerMenu();
        else showUserMenu();
        return true;
    }

    // Đăng ký
    private boolean handleRegister() {
        view.displayMessage("--- Đăng ký tài khoản mới ---");
        String name = view.getInputHandler().getStringWithCancel("Nhập tên:");
        if (name == null) return false;
        String phone = view.getInputHandler().getStringWithCancel("Nhập số điện thoại:");
        if (phone == null) return false;
        String email = view.getInputHandler().getStringWithCancel("Nhập email:");
        if (email == null) return false;
        String password = view.getInputHandler().getStringWithCancel("Tạo mật khẩu:");
        if (password == null) return false;
        // Kiểm tra trùng số điện thoại
        if (customerService.findCustomerByPhone(phone) != null) {
            view.displayError("Số điện thoại đã tồn tại. Vui lòng đăng nhập hoặc dùng số khác.");
            return false;
        }
        if (customerService.findCustomerByName(name) != null) {
            view.displayError("Tên đã tồn tại. Vui lòng đăng nhập hoặc dùng tên khác.");
            return false;
        }
        if (customerService.findCustomerByPhone(phone) != null) {
            view.displayError("Số điện thoại đã tồn tại. Vui lòng đăng nhập hoặc dùng số khác.");
            return false;
        }
        if (customerService.findCustomerByEmail(email) != null) {
            view.displayError("Email đã tồn tại. Vui lòng đăng nhập hoặc dùng email khác.");
            return false;
        }
        Customer newCustomer = new Customer(++currentCustomerId, name, phone, email, "user", password);
        customerService.createCustomer(newCustomer);
        view.displaySuccess("Đăng ký thành công. Đăng nhập tự động...");
        currentCustomer = newCustomer;
        currentRole = Role.USER;
        showUserMenu();
        return true;
    }

    // Guest
    private void handleGuest() {
        view.displayMessage("--- Tiếp tục với tư cách khách (guest) ---");
        currentCustomer = null;
        currentRole = Role.USER;
        showUserMenu();
    }

    /*
     * =============================================================================
     * ============================== MENU =========================================
     * =============================================================================
     */
    // Menu cho User
    private void showUserMenu() {
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
                    case 8 -> handleUserCancelOrder();
                    case 9 -> chatWithAI();
                    default-> view.displayError("Lựa chọn không hợp lệ.");
                }
            }
        };
        userMenu.run();
    }

    // Menu cho Manager
    private void showManagerMenu() {
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

    // Menu quản lý món ăn cho Manager
    private void handleMenuManagement() {
        Menu menu = new Menu("--- Quản lý Món Ăn ---", new String[]{
            "Xem danh sách món ăn",
            "Thêm món ăn",
            "Xóa món ăn",
            "Sửa món ăn",
            "Tìm kiếm món ăn",
        }) {
            @Override
            public void execute(int n) {
                switch (n) {
                    case 1 -> view.displayMenu(orderService.getAllMenuItems());
                    case 2 -> addMenuItemMenu();
                    case 3 -> deleteMenuItemMenu();
                    case 4 -> editMenuItemMenu();
                    case 5 -> searchMenuItemMenu();
                    default -> view.displayError("Lựa chọn không hợp lệ.");
                }
            }
        };
        menu.run();
    }

    // Menu quản lý bàn cho Manager
    private void handleTableManagement() {
        Menu menu = new Menu("--- Quản lý Bàn ---", new String[]{
            "Xem danh sách bàn",
            "Thêm bàn mới",
            "Sửa bàn",
            "Xóa bàn",
            "Tìm kiếm bàn",
        }) {
            @Override
            public void execute(int n) {
                switch (n) {
                    case 1 -> view.displayTables(bookingService.getAllTables());
                    case 2 -> addTableMenu();
                    case 3 -> editTableMenu();
                    case 4 -> deleteTableMenu();
                    case 5 -> searchTableMenu();
                    default -> view.displayError("Lựa chọn không hợp lệ.");
                }
            }
        };
        menu.run();
    }

    // Menu quản lý khách hàng 
    private void handleCustomerManagement() {
        Menu menu = new Menu("--- Quản lý Khách Hàng ---", new String[]{
            "Xem danh sách khách hàng",
            "Thêm khách hàng",
            "Sửa khách hàng",
            "Xóa khách hàng",
            "Tìm kiếm khách hàng",
        }) {
            @Override
            public void execute(int n) {
                switch (n) {
                    case 1 -> viewCustomersMenu();
                    case 2 -> addCustomerMenu();
                    case 3 -> editCustomerMenu();
                    case 4 -> deleteCustomerMenu();
                    case 5 -> searchCustomerMenu();
                    default -> view.displayError("Lựa chọn không hợp lệ.");
                }
            }
        };
        menu.run();
    }

    // Menu quản lý đặt bàn cho Manager
    private void handleBookingManagement() {
        Menu menu = new Menu("--- Quản lý Đặt Bàn ---", new String[]{
            "Xem danh sách đặt bàn",
            "Xóa đặt bàn",
            "Sửa đặt bàn",
            "Tìm kiếm đặt bàn",
        }) {
            @Override
            public void execute(int n) {
                switch (n) {
                    case 1 -> viewBookingsMenu();
                    case 2 -> deleteBookingMenu();
                    case 3 -> editBookingMenu();
                    case 4 -> searchBookingMenu();
                    default -> view.displayError("Lựa chọn không hợp lệ.");
                }
            }
        };
        menu.run();
    }

    // Menu tìm kiếm đặt bàn cho Manager
    private void searchBookingMenu() {
        Menu menu = new Menu("--- Tìm kiếm Đặt Bàn ---", new String[]{
            "Tìm theo ID",
            "Tìm theo tên khách hàng",
            "Tìm theo số điện thoại khách hàng",
        }) {
            @Override
            public void execute(int n) {
                switch (n) {
                    case 1 -> searchBookingById();
                    case 2 -> searchBookingByName();
                    case 3 -> searchBookingByPhone();
                    default -> view.displayError("Lựa chọn không hợp lệ.");
                }
            }
        };
        menu.run();
    }

    // Menu quản lý đơn hàng cho Manager
    private void handleOrderManagement() {
        Menu menu = new Menu("--- Quản lý Đơn Hàng ---", new String[]{
            "Xem danh sách đơn hàng",
            "Thêm đơn hàng",
            "Sửa đơn hàng",
            "Xóa đơn hàng",
            "Tìm kiếm đơn hàng",
        }) {
            @Override
            public void execute(int n) {
                switch (n) {
                    case 1 -> viewOrdersMenu();
                    case 2 -> addOrderMenu();
                    case 3 -> editOrderMenu();
                    case 4 -> deleteOrderMenu();
                    case 5 -> searchOrderMenu();
                    default -> view.displayError("Lựa chọn không hợp lệ.");
                }
            }
        };
        menu.run();
    }

    // Menu tìm kiếm đơn hàng cho Manager
    private void searchOrderMenu() {
        Menu menu = new Menu("--- Tìm kiếm Đơn Hàng ---", new String[]{
            "Tìm theo ID",
            "Tìm theo tên món",
            "Tìm theo giá",
            "Tìm theo mô tả",
        }) {
            @Override
            public void execute(int n) {
                switch (n) {
                    case 1 -> searchOrderById();
                    case 2 -> searchOrderByName();
                    case 3 -> searchOrderByPrice();
                    case 4 -> searchOrderByDescription();
                    default -> view.displayError("Lựa chọn không hợp lệ.");
                }
            }
        };
        menu.run();
    }

    /*
     * =============================================================================
     * ============================== CHAT VỚI AI ==================================
     * =============================================================================
     */
    // Chế độ chat với AI
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
            if (aiConnector.isAIAgentAvailable()) {
                processUserInput(userInput);
            } else {
                view.displayError("AI Agent is not available. Please use basic commands.");
            }
        }
    }

    /*
     * =============================================================================
     * ============================== Handling Funtion==============================
     * =============================================================================
     */
    // lấy customer id hiện tại từ database
    private int getCurrentCustomerId() {
        List<Customer> customers = customerService.getAllCustomers();
        return customers.stream().mapToInt(Customer::getCustomerId).max().orElse(0);
    }
    // Xử lý đặt món cho user
    private void handleUserOrder() {
        view.displayMessage("--- Đặt Món ---");
        // 1. Kiểm tra user đã có booking chưa
        List<Booking> userBookings = null;
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
        // 3. Hiển thị menu và hỏi tên món
        view.displayMenu(orderService.getAllMenuItems());
        String name = view.getInputHandler().getStringWithCancel("Nhập tên món:");
        if (name == null) {
            view.displayMessage("Đã hủy thao tác đặt món.");
            return;
        }
        // 4. Hỏi số lượng
        Integer quantity = getIntWithCancel("Nhập số lượng:");
        if (quantity == null) {
            view.displayMessage("Đã hủy thao tác đặt món.");
            return;
        }
        // 5. Thêm món vào order
        boolean added = orderService.addItemToOrder(order.getOrderId(), name, quantity);
        if (added) {
            view.displayMessage("Đã thêm món vào đơn hàng.");
        } else {
            view.displayError("Không tìm thấy món ăn hoặc lỗi khi thêm món vào đơn hàng.");
            return;
        }
        // 6. Hiển thị lại order
        Order updatedOrder = orderService.findOrderById(order.getOrderId());
        if (updatedOrder != null) {
            view.displayOrder(updatedOrder);
        }
    }
    // Xử lý tính tiền cho user
    private void handleUserCalculateBill() {
        view.displayMessage("--- Tính Tiền ---");
        double total = orderService.calculateTotal();
        view.displayMessage("Tổng tiền: " + total);
    }
    // Xử lý hủy đặt bàn cho user
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
    // Xử lý hủy đơn hàng cho user
    private void handleUserCancelOrder() {
        view.displayMessage("--- Hủy Đơn Hàng ---");
        Integer orderId = getIntWithCancel("Nhập ID đơn hàng:");
        if (orderId == null) {
            view.displayMessage("Đã hủy thao tác hủy đơn hàng.");
            return;
        }
        boolean deleted = orderService.deleteOrder(orderId);
        if (deleted) view.displaySuccess("Đã hủy đơn hàng.");
        else view.displayError("Không tìm thấy đơn hàng với ID này.");
    }
    // Xử lý đặt bàn cho user
    private void handleUserBooking() {
        view.displayMessage("--- Đặt Bàn ---");
        view.displayTables(bookingService.getAllTables());
        Customer bookingCustomer = currentCustomer;
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

    // Add món ăn
    private void addMenuItemMenu() {
        String name = view.getInputHandler().getStringWithCancel("Nhập tên món:");
        if (name == null) {
            view.displayMessage("Đã hủy thao tác thêm món ăn.");
            return;
        }
        Double price = getDoubleWithCancel("Nhập giá:");
        if (price == null) {
            view.displayMessage("Đã hủy thao tác thêm món ăn.");
            return;
        }
        String desc = view.getInputHandler().getStringWithCancel("Nhập mô tả:");
        if (desc == null) {
            view.displayMessage("Đã hủy thao tác thêm món ăn.");
            return;
        }
        orderService.addMenuItem(name, price, desc);
        view.displaySuccess("Đã thêm món ăn mới.");
    }
    private void deleteMenuItemMenu() {
        Integer delId = getIntWithCancel("Nhập ID món ăn cần xóa:");
        if (delId == null) {
            view.displayMessage("Đã hủy thao tác xóa món ăn.");
            return;
        }
        boolean deleted = orderService.deleteMenuItem(delId);
        if (deleted) view.displaySuccess("Đã xóa món ăn.");
        else view.displayError("Không tìm thấy món ăn với ID này.");
    }
    private void editMenuItemMenu() {
        Integer editId = getIntWithCancel("Nhập ID món ăn cần sửa:");
        if (editId == null) {
            view.displayMessage("Đã hủy thao tác sửa món ăn.");
            return;
        }
        String newName = view.getInputHandler().getStringWithCancel("Tên mới (bỏ trống để giữ nguyên):");
        if (newName == null) {
            view.displayMessage("Đã hủy thao tác sửa món ăn.");
            return;
        }
        String priceStr = view.getInputHandler().getStringWithCancel("Giá mới (bỏ trống để giữ nguyên):");
        if (priceStr == null) {
            view.displayMessage("Đã hủy thao tác sửa món ăn.");
            return;
        }
        String newDesc = view.getInputHandler().getStringWithCancel("Mô tả mới (bỏ trống để giữ nguyên):");
        if (newDesc == null) {
            view.displayMessage("Đã hủy thao tác sửa món ăn.");
            return;
        }
        boolean ok = orderService.updateMenuItem(editId, newName, priceStr, newDesc);
        if (ok) view.displaySuccess("Đã cập nhật món ăn.");
        else view.displayError("Không tìm thấy món ăn hoặc giá không hợp lệ.");
    }
    private void searchMenuItemMenu() {
        String keyword = view.getInputHandler().getStringWithCancel("Nhập tên hoặc ID món ăn:");
        if (keyword == null) {
            view.displayMessage("Đã hủy thao tác tìm kiếm món ăn.");
            return;
        }
        try {
            int id = Integer.parseInt(keyword);
            MenuItem found = orderService.findMenuItemById(id);
            if (found != null) view.displayMessage(found.toString());
            else view.displayError("Không tìm thấy món ăn với ID này.");
        } catch (NumberFormatException e) {
            MenuItem found = orderService.findMenuItemByName(keyword);
            if (found != null) view.displayMessage(found.toString());
            else view.displayError("Không tìm thấy món ăn với tên này.");
        }
    }

    

    private void addTableMenu() {
        Integer cap = getIntWithCancel("Nhập sức chứa bàn:");
        if (cap == null) {
            view.displayMessage("Đã hủy thao tác thêm bàn.");
            return;
        }
        bookingService.addTable(cap);
        view.displaySuccess("Đã thêm bàn mới.");
    }
    private void editTableMenu() {
        Integer id = getIntWithCancel("Nhập ID bàn cần sửa:");
        if (id == null) {
            view.displayMessage("Đã hủy thao tác sửa bàn.");
            return;
        }
        String capStr = view.getInputHandler().getStringWithCancel("Sức chứa mới (bỏ trống để giữ nguyên):");
        if (capStr == null) {
            view.displayMessage("Đã hủy thao tác sửa bàn.");
            return;
        }
        String statusStr = view.getInputHandler().getStringWithCancel("Trạng thái mới (AVAILABLE/OCCUPIED/RESERVED/MAINTENANCE, bỏ trống để giữ nguyên):");
        if (statusStr == null) {
            view.displayMessage("Đã hủy thao tác sửa bàn.");
            return;
        }
        boolean ok = bookingService.updateTable(id, capStr, statusStr);
        if (ok) view.displaySuccess("Đã cập nhật bàn.");
        else view.displayError("Không tìm thấy bàn hoặc dữ liệu không hợp lệ.");
    }
    private void deleteTableMenu() {
        Integer delId = getIntWithCancel("Nhập ID bàn cần xóa:");
        if (delId == null) {
            view.displayMessage("Đã hủy thao tác xóa bàn.");
            return;
        }
        boolean ok = bookingService.deleteTable(delId);
        if (ok) view.displaySuccess("Đã xóa bàn.");
        else view.displayError("Không tìm thấy bàn.");
    }
    private void searchTableMenu() {
        String keyword = view.getInputHandler().getStringWithCancel("Nhập ID, sức chứa hoặc trạng thái bàn:");
        if (keyword == null) {
            view.displayMessage("Đã hủy thao tác tìm kiếm bàn.");
            return;
        }
        List<Table> result = bookingService.searchTables(keyword);
        if (result.isEmpty()) view.displayError("Không tìm thấy bàn phù hợp.");
        else for (Table t : result) view.displayMessage(t.toString());
    }

    
    private void viewCustomersMenu() {
        List<Customer> customers = customerService.getAllCustomers();
        for (Customer c : customers) view.displayMessage(c.toString());
    }
    private void addCustomerMenu() {
        String name = view.getInputHandler().getStringWithCancel("Nhập tên khách hàng:");
        if (name == null) {
            view.displayMessage("Đã hủy thao tác thêm khách hàng.");
            return;
        }
        String phone = view.getInputHandler().getStringWithCancel("Nhập số điện thoại:");
        if (phone == null) {
            view.displayMessage("Đã hủy thao tác thêm khách hàng.");
            return;
        }
        String email = view.getInputHandler().getStringWithCancel("Nhập email:");
        if (email == null) {
            view.displayMessage("Đã hủy thao tác thêm khách hàng.");
            return;
        }
        customerService.createCustomer(name, phone, email);
        view.displaySuccess("Đã thêm khách hàng mới.");
    }
    private void editCustomerMenu() {
        String phone = view.getInputHandler().getStringWithCancel("Nhập số điện thoại khách hàng cần sửa:");
        if (phone == null) {
            view.displayMessage("Đã hủy thao tác sửa khách hàng.");
            return;
        }
        String newName = view.getInputHandler().getStringWithCancel("Tên mới (bỏ trống để giữ nguyên):");
        if (newName == null) {
            view.displayMessage("Đã hủy thao tác sửa khách hàng.");
            return;
        }
        String newPhone = view.getInputHandler().getStringWithCancel("Số điện thoại mới (bỏ trống để giữ nguyên):");
        if (newPhone == null) {
            view.displayMessage("Đã hủy thao tác sửa khách hàng.");
            return;
        }
        String newEmail = view.getInputHandler().getStringWithCancel("Email mới (bỏ trống để giữ nguyên):");
        if (newEmail == null) {
            view.displayMessage("Đã hủy thao tác sửa khách hàng.");
            return;
        }
        boolean ok = customerService.updateCustomer(phone, newName, newPhone, newEmail);
        if (ok) view.displaySuccess("Đã cập nhật khách hàng.");
        else view.displayError("Không tìm thấy khách hàng.");
    }
    private void deleteCustomerMenu() {
        String phone = view.getInputHandler().getStringWithCancel("Nhập số điện thoại khách hàng cần xóa:");
        if (phone == null) {
            view.displayMessage("Đã hủy thao tác xóa khách hàng.");
            return;
        }
        boolean ok = customerService.deleteCustomer(phone);
        if (ok) view.displaySuccess("Đã xóa khách hàng.");
        else view.displayError("Không tìm thấy khách hàng.");
    }
    private void searchCustomerMenu() {
        String keyword = view.getInputHandler().getStringWithCancel("Nhập tên hoặc số điện thoại khách hàng:");
        if (keyword == null) {
            view.displayMessage("Đã hủy thao tác tìm kiếm khách hàng.");
            return;
        }
        List<Customer> results = customerService.searchCustomers(keyword);
        if (results.isEmpty()) {
            view.displayError("Không tìm thấy khách hàng phù hợp.");
        } else {
            for (Customer c : results) view.displayMessage(c.toString());
        }
    }

    
    private void viewBookingsMenu() {
        List<Booking> bookings = bookingService.getAllBookings();
        view.displayBookings(bookings);
    }
    private void deleteBookingMenu() {
        Integer id = getIntWithCancel("Nhập ID đặt bàn cần xóa:");
        if (id == null) {
            view.displayMessage("Đã hủy thao tác xóa đặt bàn.");
            return;
        }
        boolean deleted = bookingService.deleteBooking(id);
        if (deleted) view.displaySuccess("Đã xóa đặt bàn.");
        else view.displayError("Không tìm thấy hoặc không thể xóa đặt bàn này.");
    }
    private void editBookingMenu() {
        Integer id = getIntWithCancel("Nhập ID đặt bàn cần sửa:");
        if (id == null) {
            view.displayMessage("Đã hủy thao tác sửa đặt bàn.");
            return;
        }
        String guestsStr = view.getInputHandler().getStringWithCancel("Số khách mới (bỏ trống để giữ nguyên):");
        if (guestsStr == null) {
            view.displayMessage("Đã hủy thao tác sửa đặt bàn.");
            return;
        }
        String timeStr = view.getInputHandler().getStringWithCancel("Thời gian mới (dd/MM/yyyy HH:mm, bỏ trống để giữ nguyên):");
        if (timeStr == null) {
            view.displayMessage("Đã hủy thao tác sửa đặt bàn.");
            return;
        }
        boolean ok = bookingService.updateBooking(id, guestsStr, timeStr);
        if (ok) view.displaySuccess("Đã cập nhật đặt bàn.");
        else view.displayError("Không tìm thấy đặt bàn hoặc dữ liệu không hợp lệ.");
    }
    
    private void searchBookingById() {
        Integer id = getIntWithCancel("Nhập ID đặt bàn:");
        if (id == null) {
            view.displayMessage("Đã hủy thao tác tìm kiếm đặt bàn.");
            return;
        }
        Booking booking = bookingService.findBookingById(id);
        if (booking != null) view.displayMessage(booking.toString());
        else view.displayError("Không tìm thấy đặt bàn với ID này.");
    }
    private void searchBookingByName() {
        String name = view.getInputHandler().getStringWithCancel("Nhập tên khách hàng:");
        if (name == null) {
            view.displayMessage("Đã hủy thao tác tìm kiếm đặt bàn.");
            return;
        }
        List<Booking> bookings = bookingService.getAllBookings();
        boolean found = false;
        for (Booking b : bookings) {
            if (b.getCustomer().getName().toLowerCase().contains(name.toLowerCase())) {
                view.displayMessage(b.toString());
                found = true;
            }
        }
        if (!found) view.displayError("Không tìm thấy đặt bàn cho tên khách này.");
    }
    private void searchBookingByPhone() {
        String phone = view.getInputHandler().getStringWithCancel("Nhập số điện thoại khách hàng:");
        if (phone == null) {
            view.displayMessage("Đã hủy thao tác tìm kiếm đặt bàn.");
            return;
        }
        List<Booking> bookings = bookingService.getAllBookings();
        boolean found = false;
        for (Booking b : bookings) {
            if (b.getCustomer().getPhone().contains(phone)) {
                view.displayMessage(b.toString());
                found = true;
            }
        }
        if (!found) view.displayError("Không tìm thấy đặt bàn cho số điện thoại này.");
    }

    
    private void viewOrdersMenu() {
        List<Order> orders = orderService.getAllOrders();
        view.displayListOrder(orders);
    }
    private void addOrderMenu() {
        String name = view.getInputHandler().getStringWithCancel("Nhập tên món:");
        if (name == null) {
            view.displayMessage("Đã hủy thao tác thêm đơn hàng.");
            return;
        }
        Double price = getDoubleWithCancel("Nhập giá:");
        if (price == null) {
            view.displayMessage("Đã hủy thao tác thêm đơn hàng.");
            return;
        }
        String desc = view.getInputHandler().getStringWithCancel("Nhập mô tả:");
        if (desc == null) {
            view.displayMessage("Đã hủy thao tác thêm đơn hàng.");
            return;
        }
        orderService.addOrder(name, price, desc);
        view.displaySuccess("Đã thêm món ăn mới.");
    }
    private void editOrderMenu() {
        Integer id = getIntWithCancel("Nhập ID đơn hàng cần sửa:");
        if (id == null) {
            view.displayMessage("Đã hủy thao tác sửa đơn hàng.");
            return;
        }
        String name = view.getInputHandler().getStringWithCancel("Tên món mới (bỏ trống để giữ nguyên):");
        if (name == null) {
            view.displayMessage("Đã hủy thao tác sửa đơn hàng.");
            return;
        }
        Double price = getDoubleWithCancel("Giá mới (bỏ trống để giữ nguyên):");
        if (price == null) {
            view.displayMessage("Đã hủy thao tác sửa đơn hàng.");
            return;
        }
        String desc = view.getInputHandler().getStringWithCancel("Mô tả mới (bỏ trống để giữ nguyên):");
        if (desc == null) {
            view.displayMessage("Đã hủy thao tác sửa đơn hàng.");
            return;
        }
        boolean ok = orderService.updateOrder(id, name, price, desc);
        if (ok) view.displaySuccess("Đã cập nhật đơn hàng.");
        else view.displayError("Không tìm thấy đơn hàng.");
    }
    private void deleteOrderMenu() {
        Integer id = getIntWithCancel("Nhập ID đơn hàng cần xóa:");
        if (id == null) {
            view.displayMessage("Đã hủy thao tác xóa đơn hàng.");
            return;
        }
        boolean deleted = orderService.deleteOrder(id);
        if (deleted) view.displaySuccess("Đã xóa đơn hàng.");
        else view.displayError("Không tìm thấy đơn hàng.");
    }
    
    private void searchOrderById() {
        Integer id = getIntWithCancel("Nhập ID đơn hàng:");
        if (id == null) {
            view.displayMessage("Đã hủy thao tác tìm kiếm đơn hàng.");
            return;
        }
        Order order = orderService.findOrderById(id);
        if (order != null) view.displayMessage(order.toString());
        else view.displayError("Không tìm thấy đơn hàng với ID này.");
    }
    private void searchOrderByName() {
        String name = view.getInputHandler().getStringWithCancel("Nhập tên món:");
        if (name == null) {
            view.displayMessage("Đã hủy thao tác tìm kiếm đơn hàng.");
            return;
        }
        List<Order> orders = orderService.getAllOrders();
        boolean found = false;
        for (Order o : orders) {
            if (o.getName().toLowerCase().contains(name.toLowerCase())) {
                view.displayMessage(o.toString());
                found = true;
            }
        }
        if (!found) view.displayError("Không tìm thấy đơn hàng cho tên món này.");
    }
    private void searchOrderByPrice() {
        Double price = getDoubleWithCancel("Nhập giá:");
        if (price == null) {
            view.displayMessage("Đã hủy thao tác tìm kiếm đơn hàng.");
            return;
        }
        List<Order> orders = orderService.getAllOrders();
        boolean found = false;
        for (Order o : orders) {
            if (o.getPrice() == price) {
                view.displayMessage(o.toString());
                found = true;
            }
        }
        if (!found) view.displayError("Không tìm thấy đơn hàng với giá này.");
    }
    private void searchOrderByDescription() {
        String desc = view.getInputHandler().getStringWithCancel("Nhập mô tả:");
        if (desc == null) {
            view.displayMessage("Đã hủy thao tác tìm kiếm đơn hàng.");
            return;
        }
        List<Order> orders = orderService.getAllOrders();
        boolean found = false;
        for (Order o : orders) {
            if (o.getDescription().toLowerCase().contains(desc.toLowerCase())) {
                view.displayMessage(o.toString());
                found = true;
            }
        }
        if (!found) view.displayError("Không tìm thấy đơn hàng với mô tả này.");
    }
    private void viewMenuUser() {
        showUserMenu();
    }

    /*
     * =============================================================================
     * ============================== AI PROCESS =====================================
     * =============================================================================
     */
    
    /**
     * Process user input
     */
    private void processUserInput(String userInput) {
        String roleString = currentRole == Role.MANAGER ? "MANAGER" : "USER";
        DebugUtil.debugPrint("[DEBUG] Gửi tới AI: " + userInput + " (role: " + roleString + ")");
        AIResponse aiResponse = aiConnector.processUserInput(userInput, roleString);
        if (aiResponse == null) {
            view.displayError("No response received from AI Agent");
            return;
        }
        if (aiResponse.getNaturalResponse() != null && !aiResponse.getNaturalResponse().isEmpty()) {
            view.displayMessage(aiResponse.getNaturalResponse());
        }
        DebugUtil.debugPrint("[DEBUG] AIResponse: " + aiResponse.toString());
        processAIAction(aiResponse);
    }
    
    /**
     * Process action from AI Agent
     */
    private void processAIAction(AIResponse aiResponse) {
        if (aiResponse.isClarifyAction()) {
            return;
        }
        if (aiResponse.isShowMenuAction()) {
            aiService.processShowMenuAction(orderService, view);
            return;
        }
        if (aiResponse.isShowTablesAction()) {
            view.displayTables(bookingService.getAllTables());
            return;
        }
        if (aiResponse.isShowBookingsAction()) {
            view.displayBookings(bookingService.getAllBookings());
            return;
        }
        if (aiResponse.isCollectCustomerInfoAction()) {
            return;
        }
        if (aiResponse.isBookingAction()) {
            processBookingAction(aiResponse);
        }
        if (aiResponse.isOrderAction()) {
            processOrderAction(aiResponse);
        }
        if (aiResponse.isCalculateBillAction()) {
            processCalculateBillAction(aiResponse);
        }
        if (aiResponse.isCancelAction()) {
            processCancelAction(aiResponse);
        }
        if (currentRole != Role.MANAGER) {
            return;
        }
        if (aiResponse.isAddMenuAction()) {
            aiService.processAddMenuAction(aiResponse, orderService, view);
            return;
        }
        if (aiResponse.isDeleteMenuAction()) {
            aiService.processDeleteMenuAction(aiResponse, orderService, view);
            return;
        }
        if (aiResponse.isAddTableAction()) {
            aiService.processAddTableAction(aiResponse, bookingService, view);
            return;
        }
        if (aiResponse.isDeleteBookingAction()) {
            aiService.processDeleteBookingAction(aiResponse, bookingService, view);
            return;
        }
        if (aiResponse.isFixDataAction()) {
            aiService.processFixDataAction(bookingService, view);
            return;
        }
        if (aiResponse.isShowCustomersAction()) {
            customerService.displayAllCustomers();
        }
        if (aiResponse.isCustomerSearchAction()) {
            aiService.processCustomerSearchAction(aiResponse, customerService, view);
            return;
        }
    }
    
    /**
     * Process booking action
     */
    private void processBookingAction(AIResponse aiResponse) {
        Integer guests = aiResponse.getGuestsCount();
        String timeStr = aiResponse.getBookingTime();
        
        if (guests == null || guests <= 0) {
            view.displayError("Please specify a valid number of guests");
            return;
        }
        
        // Get customer information from AI response
        String customerName = aiResponse.getCustomerName();
        String customerPhone = aiResponse.getCustomerPhone();
        
        // Debug: Log what we received from AI
        DebugUtil.debugPrint("🔍 DEBUG - AI Response customer info:");
        DebugUtil.debugPrint("   - customerName: " + customerName);
        DebugUtil.debugPrint("   - customerPhone: " + customerPhone);
        
        // Create or update customer
        if (customerName != null && customerPhone != null) {
            // Create new customer with provided info
            currentCustomer = new Customer(0, customerName, customerPhone);
            DebugUtil.debugPrint("✅ Created new customer: " + customerName + " - " + customerPhone);
        } else if (currentCustomer == null) {
            // Create default customer if no info provided
            currentCustomer = new Customer(0, "Customer", "0123456789");
            DebugUtil.debugPrint("⚠️ Created default customer because no info from AI");
        }
        
        // Debug: Log what customer we're using for booking
        DebugUtil.debugPrint("🔍 DEBUG - Using customer for booking:");
        DebugUtil.debugPrint("   - Name: " + currentCustomer.getName());
        DebugUtil.debugPrint("   - Phone: " + currentCustomer.getPhone());
        DebugUtil.debugPrint("   - ID: " + currentCustomer.getCustomerId());
        
        // Parse time
        LocalDateTime bookingTime = parseBookingTime(timeStr);
        if (bookingTime == null) {
            view.displayError("Please specify a valid time");
            return;
        }
        
        // Create booking
        Booking booking = bookingService.createBooking(currentCustomer, guests, bookingTime);
        if (booking != null) {
            // Cập nhật currentCustomer với customer đã được lưu từ database
            currentCustomer = booking.getCustomer();
            
            view.displayBookingConfirmation(booking);
            
            // If there's an order at the same time
            if (aiResponse.isOrderAction()) {
                processOrderAction(aiResponse);
            }
        } else {
            view.displayError("Cannot book table. Please try with a different time.");
        }
    }
    
    /**
     * Process order action
     */
    private void processOrderAction(AIResponse aiResponse) {
        List<Map<String, Object>> dishes = aiResponse.getDishes();
        
        if (dishes == null || dishes.isEmpty()) {
            view.displayError("Please specify dishes");
            return;
        }
        
        // Tìm booking hiện tại của customer
        Booking currentBooking = findCurrentBooking();
        if (currentBooking == null) {
            // Nếu chưa có currentCustomer, hỏi số điện thoại
            if (currentCustomer == null) {
                view.displayMessage("💡 Vui lòng nhập số điện thoại để xác định khách hàng:");
                String phone = view.getUserInput();
                Customer foundCustomer = customerService.findCustomerByPhone(phone.trim());
                if (foundCustomer == null) {
                    view.displayError("❌ Không tìm thấy khách hàng với số điện thoại này. Vui lòng đặt bàn trước.");
                    return;
                }
                currentCustomer = foundCustomer;
            }
            // Hỏi user nhập số bàn
            view.displayError("❌ Bạn chưa đặt bàn hoặc chưa chọn bàn. Vui lòng nhập số bàn để gọi món.");
            view.displayMessage("💡 Nhập số bàn (tableId) bạn muốn gọi món: ");
            String input = view.getUserInput();
            try {
                int tableId = Integer.parseInt(input.trim());
                // Tìm booking của customer với tableId này
                Booking bookingByTable = findBookingByTableId(tableId);
                if (bookingByTable == null) {
                    view.displayError("❌ Không tìm thấy đặt bàn cho số bàn này hoặc bạn không sở hữu bàn này.");
                    return;
                }
                currentBooking = bookingByTable;
            } catch (Exception e) {
                view.displayError("❌ Số bàn không hợp lệ.");
                return;
            }
        }
        
        // Tạo hoặc lấy order hiện có cho booking
        Order order = orderService.getOrCreateOrderForBooking(currentBooking);
        
        // Thêm items vào order
        for (Map<String, Object> dish : dishes) {
            String dishName = (String) dish.get("name");
            Integer quantity = (Integer) dish.get("quantity");
            
            if (dishName != null && quantity != null && quantity > 0) {
                orderService.addItemToOrder(order.getOrderId(), dishName, quantity);
            }
        }
        
        // Display order
        Order updatedOrder = orderService.findOrderById(order.getOrderId());
        if (updatedOrder != null) {
            view.displayOrder(updatedOrder);
        }
    }
    
    /**
     * Process calculate bill action
     */
    private void processCalculateBillAction(AIResponse aiResponse) {
        // Tìm booking hiện tại của customer
        Booking currentBooking = findCurrentBooking();
        if (currentBooking == null) {
            view.displayError("❌ Bạn chưa đặt bàn. Vui lòng đặt bàn trước khi tính bill.");
            return;
        }
        
        // Tính bill cho booking
        double totalBill = orderService.calculateBillForBooking(currentBooking.getBookingId());
        List<Order> orders = orderService.getOrdersForBooking(currentBooking.getBookingId());
        
        // Hiển thị bill
        view.displayMessage("🧾 BILL - Bàn #" + currentBooking.getTable().getTableId());
        view.displayMessage("👤 Khách hàng: " + currentBooking.getCustomer().getName());
        view.displayMessage("📅 Thời gian đặt: " + currentBooking.getBookingTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        view.displayMessage("👥 Số người: " + currentBooking.getNumberOfGuests());
        view.displayMessage("");
        
        if (orders.isEmpty()) {
            view.displayMessage("📝 Chưa có món ăn nào được đặt.");
        } else {
            view.displayMessage("📋 Các món đã đặt:");
            for (Order order : orders) {
                if (!order.getStatus().equals("COMPLETED")) {
                    view.displayOrder(order);
                }
            }
        }
        
        view.displayMessage("💰 Tổng cộng: " + String.format("%.0f", totalBill) + " VND");
    }
    
    /**
     * Process cancel action
     */
    private void processCancelAction(AIResponse aiResponse) {
        Integer bookingId = aiResponse.getBookingId();
        
        if (bookingId == null) {
            view.displayError("Please specify the booking ID to cancel");
            return;
        }
        
        boolean success = bookingService.cancelBooking(bookingId);
        if (success) {
            view.displaySuccess("Booking #" + bookingId + " cancelled successfully");
        } else {
            view.displayError("Booking #" + bookingId + " not found or cannot be cancelled");
        }
    }
    
    /**
     * Parse booking time
     */
    private LocalDateTime parseBookingTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            // Default to tonight 19:00
            return LocalDateTime.now().withHour(19).withMinute(0).withSecond(0).withNano(0);
        }
        
        try {
            // Try parse ISO format
            return LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            // Try parse other formats
            try {
                if (timeStr.contains("tonight")) {
                    return LocalDateTime.now().withHour(19).withMinute(0).withSecond(0).withNano(0);
                } else if (timeStr.contains("tomorrow")) {
                    return LocalDateTime.now().plusDays(1).withHour(19).withMinute(0).withSecond(0).withNano(0);
                } else if (timeStr.contains("afternoon")) {
                    return LocalDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0);
                }
            } catch (Exception ex) {
                // Ignore
            }
        }
        
        return null;
    }
    
    /**
     * Load customer from database by phone number
     */
    private Customer loadCustomerFromDatabase(String phone) {
        List<Customer> customers = customerService.getAllCustomers();
        return customers.stream()
                .filter(c -> c.getPhone().equals(phone))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Find current booking for the current customer
     */
    private Booking findCurrentBooking() {
        DebugUtil.debugPrint("🔍 DEBUG - findCurrentBooking:");
        DebugUtil.debugPrint("   - currentCustomer: " + (currentCustomer != null ? currentCustomer.getName() + " (" + currentCustomer.getPhone() + ")" : "null"));
        
        if (currentCustomer == null) {
            DebugUtil.debugPrint("   - Result: currentCustomer is null");
            return null;
        }
        
        // Load customer từ database để có thông tin mới nhất
        Customer dbCustomer = loadCustomerFromDatabase(currentCustomer.getPhone());
        if (dbCustomer != null) {
            currentCustomer = dbCustomer;
            DebugUtil.debugPrint("   - Loaded customer from DB: " + currentCustomer.getName() + " (" + currentCustomer.getPhone() + ")");
        }
        
        // Kiểm tra xem customer có active booking không
        DebugUtil.debugPrint("   - activeBookingIds: " + currentCustomer.getActiveBookingIds());
        if (!currentCustomer.hasAnyActiveBooking()) {
            DebugUtil.debugPrint("   - Result: no active bookings");
            return null;
        }
        
        // Lấy booking đầu tiên trong danh sách active
        Integer firstBookingId = currentCustomer.getFirstActiveBookingId();
        DebugUtil.debugPrint("   - firstBookingId: " + firstBookingId);
        if (firstBookingId == null) {
            DebugUtil.debugPrint("   - Result: firstBookingId is null");
            return null;
        }
        
        Booking booking = bookingService.findBookingById(firstBookingId);
        DebugUtil.debugPrint("   - found booking: " + (booking != null ? "Booking #" + booking.getBookingId() : "null"));
        return booking;
    }
    
    // Thêm phương thức tìm booking theo tableId cho customer hiện tại
    private Booking findBookingByTableId(int tableId) {
        if (currentCustomer == null) return null;
        List<Booking> bookings = bookingService.getBookingsByCustomer(currentCustomer);
        for (Booking b : bookings) {
            if (b.getTable() != null && b.getTable().getTableId() == tableId && "CONFIRMED".equals(b.getStatus())) {
                return b;
            }
        }
        return null;
    }

    // Thêm các hàm tiện ích cho nhập int/double có hỗ trợ cancel
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
    private Double getDoubleWithCancel(String message) {
        String input = view.getInputHandler().getStringWithCancel(message);
        if (input == null) return null;
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            view.displayError("Giá trị không hợp lệ. Vui lòng nhập số thực hoặc 'cancel' để hủy.");
            return getDoubleWithCancel(message);
        }
    }

    private void toggleDebugMode() {
        DebugUtil.toggleDebug();
        view.displayMessage("Debug mode: " + (DebugUtil.isDebug() ? "ON" : "OFF"));
    }
} 