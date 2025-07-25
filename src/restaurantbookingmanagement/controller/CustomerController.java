package restaurantbookingmanagement.controller;

import restaurantbookingmanagement.model.*;
import restaurantbookingmanagement.service.*;
import restaurantbookingmanagement.view.*;
import java.util.List;

/**
 * Controller quản lý khách hàng
 */
public class CustomerController {
    private final CustomerService customerService;
    private final ConsoleView view;
    
    public CustomerController(CustomerService customerService, ConsoleView view) {
        this.customerService = customerService;
        this.view = view;
    }
    
    /**
     * Hiển thị menu quản lý khách hàng
     */
    public void handleCustomerManagement() {
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
    
    /**
     * Xem danh sách khách hàng
     */
    private void viewCustomersMenu() {
        view.displayAllCustomers(customerService.getAllCustomers());
    }
    
    /**
     * Thêm khách hàng
     */
    private void addCustomerMenu() {
        Customer newCustomer = view.getRegisterInfo(-1); // -1: auto id
        if (newCustomer == null) {
            view.displayMessage("Đã hủy thao tác thêm khách hàng.");
            return;
        }
        boolean created = customerService.createCustomerIfNotExists(newCustomer);
        if (created) view.displaySuccess("Đã thêm khách hàng mới.");
        else view.displayError("Khách hàng đã tồn tại (trùng tên, SĐT hoặc email).");
    }
    
    /**
     * Sửa khách hàng (gom nhập/xuất về view, nghiệp vụ về service)
     */
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
    
    /**
     * Xóa khách hàng (gom nhập/xuất về view, nghiệp vụ về service)
     */
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
    
    /**
     * Tìm kiếm khách hàng (gom nhập/xuất về view, nghiệp vụ về service)
     */
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
            view.displayAllCustomers(results);
        }
    }
    
    /**
     * Tìm khách hàng theo tên
     */
    public Customer findCustomerByName(String name) {
        return customerService.findCustomerByName(name);
    }
    
    /**
     * Tìm khách hàng theo số điện thoại
     */
    public Customer findCustomerByPhone(String phone) {
        return customerService.findCustomerByPhone(phone);
    }
    
    /**
     * Tìm khách hàng theo email
     */
    public Customer findCustomerByEmail(String email) {
        return customerService.findCustomerByEmail(email);
    }
    
    /**
     * Tạo khách hàng mới
     */
    public void createCustomer(Customer customer) {
        customerService.createCustomer(customer);
    }
    
    /**
     * Hiển thị tất cả khách hàng (gom về view)
     */
    public void displayAllCustomers() {
        view.displayAllCustomers(customerService.getAllCustomers());
    }
    
    /**
     * Getter cho CustomerService
     */
    public CustomerService getCustomerService() {
        return customerService;
    }
} 