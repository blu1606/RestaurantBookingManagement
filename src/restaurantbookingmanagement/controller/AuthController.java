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
                return;
            }
            throw e;
        }
    }
    
    /**
     * Xử lý đăng nhập
     */
    private boolean handleLogin() {
        String name = view.getLoginName();
        if (name == null) return false;
        String password = view.getLoginPassword();
        if (password == null) return false;
        
        Customer customer = customerService.findCustomerByName(name);
        if (customer == null) {
            view.displayLoginError("Không tìm thấy tài khoản với tên: " + name);
            return false;
        }
        if (!password.equals(customer.getPassword())) {
            view.displayLoginError("Sai mật khẩu.");
            return false;
        }
        currentCustomer = customer;
        currentRole = customer.getRole().equalsIgnoreCase("admin") ? Role.MANAGER : Role.USER;
        view.displayLoginSuccess(currentRole);
        return true;
    }
    
    /**
     * Xử lý đăng ký
     */
    private boolean handleRegister() {
        Customer newCustomer = view.getRegisterInfo(currentCustomerId + 1);
        if (newCustomer == null) return false;
        // Kiểm tra trùng lặp đã chuyển về service
        boolean created = customerService.createCustomerIfNotExists(newCustomer);
        if (!created) {
            view.displayRegisterError("Thông tin đăng ký đã tồn tại. Vui lòng kiểm tra lại.");
            return false;
        }
        view.displayRegisterSuccess();
        currentCustomer = newCustomer;
        currentRole = Role.USER;
        currentCustomerId++;
        return true;
    }
    
    /**
     * Xử lý guest mode
     */
    private void handleGuest() {
        view.displayGuestMode();
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