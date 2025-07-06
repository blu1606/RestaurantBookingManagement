package restaurantbookingmanagement.controller;

import restaurantbookingmanagement.model.*;
import restaurantbookingmanagement.service.*;
import restaurantbookingmanagement.view.*;
import java.util.List;

/**
 * Controller xử lý authentication và session management
 */
public class AuthController {
    private final CustomerService customerService;
    private final ConsoleView view;
    private Customer currentCustomer;
    private Role currentRole;
    private int currentCustomerId;
    
    public AuthController(CustomerService customerService, ConsoleView view) {
        this.customerService = customerService;
        this.view = view;
        this.currentCustomer = null;
        this.currentRole = null;
        this.currentCustomerId = getCurrentCustomerId();
    }
    
    /**
     * Hiển thị menu đăng nhập/đăng ký
     */
    public void showEntryMenu() {
        String[] options = new String[]{"Đăng nhập", "Đăng ký", "Tiếp tục với tư cách khách (guest)"};
        
        Menu entryMenu = new Menu("===== ENTRY MENU =====", options) {
            @Override
            public void execute(int n) {
                switch (n) {
                    case 1 -> {
                        if (handleLogin()) {
                            // Thoát khỏi menu bằng cách throw exception
                            throw new RuntimeException("LOGIN_SUCCESS");
                        }
                    }
                    case 2 -> {
                        if (handleRegister()) {
                            throw new RuntimeException("LOGIN_SUCCESS");
                        }
                    }
                    case 3 -> {
                        handleGuest();
                        throw new RuntimeException("LOGIN_SUCCESS");
                    }
                }
            }
        };
        
        try {
            entryMenu.run();
        } catch (RuntimeException e) {
            if ("LOGIN_SUCCESS".equals(e.getMessage())) {
                // Đăng nhập thành công, tiếp tục
                return;
            }
            throw e; // Re-throw nếu là exception khác
        }
    }
    
    /**
     * Xử lý đăng nhập
     */
    private boolean handleLogin() {
        view.displayMessage("--- Đăng nhập ---");
        String name = view.getInputHandler().getStringWithCancel("Nhập tên:");
        if (name == null) return false;
        String password = view.getInputHandler().getStringWithCancel("Nhập mật khẩu:");
        if (password == null) return false;
        
        Customer customer = customerService.findCustomerByName(name);
        System.out.println("DEBUG: Tìm customer với tên: " + name);
        System.out.println("DEBUG: Customer tìm được: " + (customer != null ? customer.getName() : "null"));
        
        if (customer == null) {
            view.displayError("Không tìm thấy tài khoản với tên: " + name);
            return false;
        }
        
        if (!password.equals(customer.getPassword())) {
            view.displayError("Sai mật khẩu.");
            return false;
        }
        
        currentCustomer = customer;
        currentRole = customer.getRole().equalsIgnoreCase("admin") ? Role.MANAGER : Role.USER;
        view.displaySuccess("Đăng nhập thành công với vai trò: " + currentRole);
        return true;
    }
    
    /**
     * Xử lý đăng ký
     */
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
        if (customerService.findCustomerByEmail(email) != null) {
            view.displayError("Email đã tồn tại. Vui lòng đăng nhập hoặc dùng email khác.");
            return false;
        }
        
        Customer newCustomer = new Customer(++currentCustomerId, name, phone, email, "user", password);
        customerService.createCustomer(newCustomer);
        view.displaySuccess("Đăng ký thành công. Đăng nhập tự động...");
        currentCustomer = newCustomer;
        currentRole = Role.USER;
        return true;
    }
    
    /**
     * Xử lý guest mode
     */
    private void handleGuest() {
        view.displayMessage("--- Tiếp tục với tư cách khách (guest) ---");
        currentCustomer = null;
        currentRole = Role.USER;
    }
    
    /**
     * Lấy customer ID hiện tại từ database
     */
    private int getCurrentCustomerId() {
        List<Customer> customers = customerService.getAllCustomers();
        return customers.stream().mapToInt(Customer::getCustomerId).max().orElse(0);
    }
    
    // Getters
    public Customer getCurrentCustomer() {
        return currentCustomer;
    }
    
    public Role getCurrentRole() {
        return currentRole;
    }
    
    public void setCurrentCustomer(Customer customer) {
        this.currentCustomer = customer;
    }
    
    public void setCurrentRole(Role role) {
        this.currentRole = role;
    }
} 