package restaurantbookingmanagement.service;

import restaurantbookingmanagement.model.*;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;

/**
 * Service xử lý logic nghiệp vụ khách hàng
 */
public class CustomerService {
    private final FileService fileService;
    private final BookingService bookingService;
    
    public CustomerService() {
        this.fileService = new FileService();
        this.bookingService = new BookingService();
    }
    
    /**
     * Tạo khách hàng mới
     */
    public Customer createCustomer(String name, String phone, String email) {
        List<Customer> customers = fileService.readCustomersFromFile();
        
        // Kiểm tra xem khách hàng đã tồn tại chưa
        boolean customerExists = customers.stream()
                .anyMatch(c -> c.getPhone().equals(phone));
        
        if (customerExists) {
            return customers.stream()
                    .filter(c -> c.getPhone().equals(phone))
                    .findFirst()
                    .orElse(null);
        }
        
        // Tạo customerId mới
        int nextCustomerId = customers.stream()
                .mapToInt(Customer::getCustomerId)
                .max()
                .orElse(0) + 1;
        
        Customer newCustomer = new Customer(nextCustomerId, name, phone);
        if (email != null && !email.isEmpty()) {
            newCustomer.setEmail(email);
        }
        
        customers.add(newCustomer);
        fileService.writeCustomersToFile(customers);
        
        return newCustomer;
    }
    
    /**
     * Tạo khách hàng mới với đầy đủ thông tin (role, password, ...)
     */
    public Customer createCustomer(Customer customer) {
        List<Customer> customers = fileService.readCustomersFromFile();
        // Kiểm tra trùng số điện thoại
        boolean exists = customers.stream().anyMatch(c -> c.getPhone().equals(customer.getPhone()));
        if (exists) {
            return customers.stream().filter(c -> c.getPhone().equals(customer.getPhone())).findFirst().orElse(null);
        }
        // Gán customerId mới
        int nextCustomerId = customers.stream().mapToInt(Customer::getCustomerId).max().orElse(0) + 1;
        customer.setCustomerId(nextCustomerId);
        customers.add(customer);
        fileService.writeCustomersToFile(customers);
        return customer;
    }
    
    /**
     * Tìm khách hàng theo số điện thoại
     */
    public Customer findCustomerByPhone(String phone) {
        List<Customer> customers = fileService.readCustomersFromFile();
        return customers.stream()
                .filter(c -> c.getPhone().equals(phone))
                .findFirst()
                .orElse(null);
    }

    /**
     * Tìm khách hàng theo tên
     */
    public Customer findCustomerByName(String name) {
        List<Customer> customers = fileService.readCustomersFromFile();
        return customers.stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Tìm khách hàng theo ID
     */
    public Customer findCustomerById(int customerId) {
        List<Customer> customers = fileService.readCustomersFromFile();
        return customers.stream()
                .filter(c -> c.getCustomerId() == customerId)
                .findFirst()
                .orElse(null);
    }

    public Customer findCustomerByEmail(String email) {
        List<Customer> customers = fileService.readCustomersFromFile();
        return customers.stream()
                .filter(c -> c.getEmail().equals(email))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Lấy tất cả khách hàng
     */
    public List<Customer> getAllCustomers() {
        return new ArrayList<>(fileService.readCustomersFromFile());
    }
    
    /**
     * Cập nhật thông tin khách hàng theo ID
     */
    public boolean updateCustomer(int customerId, String name, String phone, String email) {
        List<Customer> customers = fileService.readCustomersFromFile();
        
        for (int i = 0; i < customers.size(); i++) {
            if (customers.get(i).getCustomerId() == customerId) {
                Customer updatedCustomer = new Customer(customerId, name, phone);
                if (email != null && !email.isEmpty()) {
                    updatedCustomer.setEmail(email);
                }
                customers.set(i, updatedCustomer);
                fileService.writeCustomersToFile(customers);
                return true;
            }
        }
        return false;
    }


    
    /**
     * Xóa khách hàng (chỉ khi không có booking nào)
     */
    public boolean deleteCustomer(int customerId) {
        List<Booking> customerBookings = bookingService.getAllBookings().stream()
                .filter(b -> b.getCustomer().getCustomerId() == customerId)
                .collect(Collectors.toList());
        
        if (!customerBookings.isEmpty()) {
            return false; // Không thể xóa vì có booking
        }
        
        List<Customer> customers = fileService.readCustomersFromFile();
        customers.removeIf(c -> c.getCustomerId() == customerId);
        fileService.writeCustomersToFile(customers);
        return true;
    }
    
    /**
     * Lấy thông tin chi tiết khách hàng và trạng thái booking
     */
    public CustomerInfo getCustomerInfo(int customerId) {
        Customer customer = findCustomerById(customerId);
        if (customer == null) {
            return null;
        }
        
        List<Booking> customerBookings = bookingService.getAllBookings().stream()
                .filter(b -> b.getCustomer().getCustomerId() == customerId)
                .collect(Collectors.toList());
        
        return new CustomerInfo(customer, customerBookings);
    }
    
    /**
     * Lấy thông tin khách hàng theo số điện thoại
     */
    public CustomerInfo getCustomerInfoByPhone(String phone) {
        Customer customer = findCustomerByPhone(phone);
        if (customer == null) {
            return null;
        }
        
        return getCustomerInfo(customer.getCustomerId());
    }
    
    /**
     * Hiển thị thông tin khách hàng và trạng thái booking
     */
    public void displayCustomerInfo(CustomerInfo customerInfo) {
        if (customerInfo == null) {
            System.out.println("❌ Không tìm thấy thông tin khách hàng");
            return;
        }
        
        Customer customer = customerInfo.getCustomer();
        List<Booking> bookings = customerInfo.getBookings();
        
        System.out.println("\n👤 THÔNG TIN KHÁCH HÀNG");
        System.out.println("──────────────────────────────────────────────────────────────");
        System.out.println("📋 Thông tin cá nhân:");
        System.out.println("   • ID: #" + customer.getCustomerId());
        System.out.println("   • Tên: " + customer.getName());
        System.out.println("   • SĐT: " + customer.getPhone());
        if (customer.getEmail() != null && !customer.getEmail().isEmpty()) {
            System.out.println("   • Email: " + customer.getEmail());
        }
        
        System.out.println("\n📅 Lịch sử đặt bàn (" + bookings.size() + " booking):");
        if (bookings.isEmpty()) {
            System.out.println("   • Chưa có booking nào");
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            
            for (Booking booking : bookings) {
                String statusIcon = getStatusIcon(booking.getStatus());
                String statusText = getStatusText(booking.getStatus());
                
                System.out.println("   " + statusIcon + " Booking #" + booking.getBookingId());
                System.out.println("      • Bàn: #" + booking.getTable().getTableId());
                System.out.println("      • Số người: " + booking.getNumberOfGuests());
                System.out.println("      • Thời gian: " + booking.getBookingTime().format(formatter));
                System.out.println("      • Trạng thái: " + statusText);
                System.out.println();
            }
        }
        
        // Thống kê
        long confirmedCount = bookings.stream().filter(b -> "CONFIRMED".equals(b.getStatus())).count();
        long cancelledCount = bookings.stream().filter(b -> "CANCELLED".equals(b.getStatus())).count();
        long completedCount = bookings.stream().filter(b -> "COMPLETED".equals(b.getStatus())).count();
        
        System.out.println("📊 Thống kê:");
        System.out.println("   • Đã xác nhận: " + confirmedCount);
        System.out.println("   • Đã hủy: " + cancelledCount);
        System.out.println("   • Đã hoàn thành: " + completedCount);
        System.out.println("──────────────────────────────────────────────────────────────");
    }
    
    /**
     * Hiển thị thông tin khách hàng theo số điện thoại
     */
    public void displayCustomerInfoByPhone(String phone) {
        CustomerInfo customerInfo = getCustomerInfoByPhone(phone);
        displayCustomerInfo(customerInfo);
    }
    
    /**
     * Lấy icon cho trạng thái booking
     */
    private String getStatusIcon(String status) {
        switch (status) {
            case "CONFIRMED": return "✅";
            case "CANCELLED": return "❌";
            case "COMPLETED": return "🎉";
            default: return "❓";
        }
    }
    
    /**
     * Lấy text cho trạng thái booking
     */
    private String getStatusText(String status) {
        switch (status) {
            case "CONFIRMED": return "Đã xác nhận";
            case "CANCELLED": return "Đã hủy";
            case "COMPLETED": return "Đã hoàn thành";
            default: return "Không xác định";
        }
    }
    
    /**
     * Tìm kiếm khách hàng theo tên hoặc số điện thoại
     */
    public List<Customer> searchCustomers(String searchTerm) {
        List<Customer> customers = fileService.readCustomersFromFile();
        String lowerSearchTerm = searchTerm.toLowerCase();
        
        return customers.stream()
                .filter(c -> c.getName().toLowerCase().contains(lowerSearchTerm) ||
                           c.getPhone().contains(searchTerm))
                .collect(Collectors.toList());
    }
    
    /**
     * Hiển thị danh sách khách hàng
     */
    public void displayAllCustomers() {
        List<Customer> customers = getAllCustomers();
        
        System.out.println("\n👥 DANH SÁCH KHÁCH HÀNG");
        System.out.println("──────────────────────────────────────────────────────────────");
        
        if (customers.isEmpty()) {
            System.out.println("❌ Chưa có khách hàng nào");
        } else {
            for (Customer customer : customers) {
                CustomerInfo customerInfo = getCustomerInfo(customer.getCustomerId());
                long activeBookings = customerInfo.getBookings().stream()
                        .filter(b -> "CONFIRMED".equals(b.getStatus()))
                        .count();
                
                System.out.println("👤 #" + customer.getCustomerId() + " - " + customer.getName());
                System.out.println("   📞 " + customer.getPhone());
                if (customer.getEmail() != null && !customer.getEmail().isEmpty()) {
                    System.out.println("   📧 " + customer.getEmail());
                }
                System.out.println("   📅 " + customerInfo.getBookings().size() + " booking(s), " + activeBookings + " đang hoạt động");
                System.out.println();
            }
        }
        System.out.println("──────────────────────────────────────────────────────────────");
    }
    
    /**
     * Cập nhật thông tin khách hàng theo số điện thoại
     */
    public boolean updateCustomer(String phone, String newName, String newPhone, String newEmail) {
        List<Customer> customers = fileService.readCustomersFromFile();
        
        for (int i = 0; i < customers.size(); i++) {
            if (customers.get(i).getPhone().equals(phone)) {
                Customer customer = customers.get(i);
                if (newName != null && !newName.isEmpty()) {
                    customer.setName(newName);
                }
                if (newPhone != null && !newPhone.isEmpty()) {
                    customer.setPhone(newPhone);
                }
                if (newEmail != null && !newEmail.isEmpty()) {
                    customer.setEmail(newEmail);
                }
                customers.set(i, customer);
                fileService.writeCustomersToFile(customers);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Xóa khách hàng theo số điện thoại
     */
    public boolean deleteCustomer(String phone) {
        List<Customer> customers = fileService.readCustomersFromFile();
        
        for (int i = 0; i < customers.size(); i++) {
            if (customers.get(i).getPhone().equals(phone)) {
                customers.remove(i);
                fileService.writeCustomersToFile(customers);
                return true;
            }
        }
        return false;
    }
} 