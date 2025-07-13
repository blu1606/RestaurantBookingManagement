package restaurantbookingmanagement.service;

import restaurantbookingmanagement.model.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import restaurantbookingmanagement.utils.DebugUtil;
import restaurantbookingmanagement.service.fileservice.BookingFileService;
import restaurantbookingmanagement.service.fileservice.CustomerFileService;
import restaurantbookingmanagement.service.validator.BookingValidator;
import restaurantbookingmanagement.view.dto.BookingRequest;

// Design Pattern: Dependency Injection, State
// Purpose: Inject TableService and BookingValidator; manage Booking status using State pattern.

/**
 * Service xử lý logic nghiệp vụ đặt bàn
 */
public class BookingService {
    private final BookingFileService bookingFileService;
    private final CustomerFileService customerFileService;
    private final TableService tableService;
    private int nextBookingId;
    
    public BookingService(TableService tableService, BookingValidator bookingValidator) {
        this.bookingFileService = new BookingFileService();
        this.customerFileService = new CustomerFileService();
        this.tableService = tableService;
        this.nextBookingId = 1;
        
        // Tính toán nextBookingId từ dữ liệu hiện tại
        calculateNextBookingId();
    }
    
    private void calculateNextBookingId() {
        List<Booking> existingBookings = bookingFileService.readBookingsFromFile();
        if (!existingBookings.isEmpty()) {
            this.nextBookingId = existingBookings.stream()
                    .mapToInt(Booking::getBookingId)
                    .max()
                    .orElse(0) + 1;
        }
    }
    
    public Booking createBooking(Customer customer, int numberOfGuests, LocalDateTime bookingTime) {
        List<Table> tables = tableService.getAllTables();
        List<Booking> bookings = bookingFileService.readBookingsFromFile();
        
        // Lưu thông tin khách hàng vào customers.json và lấy customer đã được lưu
        Customer savedCustomer = saveCustomerToFile(customer);

        // Đọc lại customers từ file để đảm bảo đồng bộ
        List<Customer> customers = customerFileService.readCustomersFromFile();
        Customer realCustomer = customers.stream()
            .filter(c -> c.getPhone().equals(savedCustomer.getPhone()))
            .findFirst()
            .orElse(savedCustomer);

        // Debug: Log customer returned from saveCustomerToFile
        DebugUtil.debugPrint("🔍 DEBUG - Customer used for booking:");
        DebugUtil.debugPrint("   - Name: " + realCustomer.getName());
        DebugUtil.debugPrint("   - Phone: " + realCustomer.getPhone());
        DebugUtil.debugPrint("   - ID: " + realCustomer.getCustomerId());

        Table availableTable = tableService.findAvailableTable(numberOfGuests);
        if (availableTable == null) {
            return null; // Không có bàn phù hợp
        }

        // Kiểm tra xem bàn có bị đặt trùng thời gian không
        if (isTableBookedAtTime(availableTable, bookingTime, bookings)) {
            return null; // Bàn đã được đặt vào thời gian này
        }

        // Tạo booking mới với customer đã được lưu
        Booking booking = new Booking(nextBookingId++, realCustomer, availableTable, bookingTime, numberOfGuests);

        // Cập nhật trạng thái bàn
        availableTable.setStatus(TableStatus.RESERVED);

        // Cập nhật danh sách bàn
        updateTableInList(tables, availableTable);
        tableService.writeTablesToFile(tables);

        // Thêm booking mới vào danh sách
        bookings.add(booking);
        bookingFileService.writeBookingsToFile(bookings);

        // Cập nhật activeBookingIds cho customer
        realCustomer.addBookingId(booking.getBookingId());
        updateCustomerInList(customers, realCustomer);
        customerFileService.writeCustomersToFile(customers);

        return booking;
    }
    
    private void updateTableInList(List<Table> tables, Table updatedTable) {
        for (int i = 0; i < tables.size(); i++) {
            if (tables.get(i).getTableId() == updatedTable.getTableId()) {
                tables.set(i, updatedTable);
                break;
            }
        }
    }
    
    private void updateCustomerInList(List<Customer> customers, Customer updatedCustomer) {
        for (int i = 0; i < customers.size(); i++) {
            if (customers.get(i).getCustomerId() == updatedCustomer.getCustomerId()) {
                customers.set(i, updatedCustomer);
                break;
            }
        }
    }
    
    private boolean isTableBookedAtTime(Table table, LocalDateTime bookingTime, List<Booking> bookings) {
        return bookings.stream()
                .anyMatch(booking -> booking.getTable().getTableId() == table.getTableId() &&
                                   booking.getStatus().equals("CONFIRMED") &&
                                   isTimeOverlap(booking.getBookingTime(), bookingTime));
    }
    
    private boolean isTimeOverlap(LocalDateTime time1, LocalDateTime time2) {
        // Giả sử mỗi booking kéo dài 2 giờ
        LocalDateTime endTime1 = time1.plusHours(2);
        LocalDateTime endTime2 = time2.plusHours(2);
        
        return !time1.isAfter(endTime2) && !time2.isAfter(endTime1);
    }
    
    private Customer saveCustomerToFile(Customer customer) {
        List<Customer> customers = customerFileService.readCustomersFromFile();
        
        DebugUtil.debugPrint("🔍 DEBUG - saveCustomerToFile called with:");
        DebugUtil.debugPrint("   - Name: " + customer.getName());
        DebugUtil.debugPrint("   - Phone: " + customer.getPhone());
        DebugUtil.debugPrint("   - ID: " + customer.getCustomerId());
        
        // Kiểm tra xem khách hàng đã tồn tại chưa (chỉ so sánh theo số điện thoại)
        boolean customerExists = customers.stream()
                .anyMatch(c -> c.getPhone().equals(customer.getPhone()));
        
        DebugUtil.debugPrint("🔍 DEBUG - Customer exists check: " + customerExists);
        
        if (!customerExists) {
            // Tạo customerId mới nếu cần
            Customer customerToSave = customer;
            if (customer.getCustomerId() == 0) {
                int nextCustomerId = customers.stream()
                        .mapToInt(Customer::getCustomerId)
                        .max()
                        .orElse(0) + 1;
                customerToSave = new Customer(nextCustomerId, customer.getName(), customer.getPhone());
                DebugUtil.debugPrint("✅ Creating new customer with ID: " + nextCustomerId);
            }
            customers.add(customerToSave);
            customerFileService.writeCustomersToFile(customers);
            DebugUtil.debugPrint("✅ Added new customer to file and returning: " + customerToSave.getName() + " - " + customerToSave.getPhone());
            return customerToSave; // Trả về customer đã được lưu
        } else {
            // Tìm customer đã tồn tại theo số điện thoại
            Customer existingCustomer = customers.stream()
                    .filter(c -> c.getPhone().equals(customer.getPhone()))
                    .findFirst()
                    .orElse(customer);
            DebugUtil.debugPrint("⚠️ Found existing customer: " + existingCustomer.getName() + " - " + existingCustomer.getPhone());
            return existingCustomer;
        }
    }
    
    public boolean cancelBooking(int bookingId) {
        List<Booking> bookings = bookingFileService.readBookingsFromFile();
        List<Table> tables = tableService.getAllTables();
        List<Customer> customers = customerFileService.readCustomersFromFile();
        
        Booking booking = findBookingById(bookingId, bookings);
        if (booking != null && booking.getStatus().equals("CONFIRMED")) {
            booking.transitionTo(new Booking.CancelledState());
            
            // Cập nhật trạng thái bàn
            Table table = booking.getTable();
            table.setStatus(TableStatus.AVAILABLE);
            updateTableInList(tables, table);
            
            // Xóa bookingId khỏi activeBookingIds của customer
            Customer customer = booking.getCustomer();
            if (customer != null) {
                customer.removeBookingId(bookingId);
                updateCustomerInList(customers, customer);
                customerFileService.writeCustomersToFile(customers);
            }
            
            // Lưu thay đổi
            bookings.remove(booking);
            bookingFileService.writeBookingsToFile(bookings);
            tableService.writeTablesToFile(tables);
            
            return true;
        }
        return false;
    }
    
    public Booking findBookingById(int bookingId) {
        return findBookingById(bookingId, bookingFileService.readBookingsFromFile());
    }
    
    private Booking findBookingById(int bookingId, List<Booking> bookings) {
        return bookings.stream()
                .filter(booking -> booking.getBookingId() == bookingId)
                .findFirst()
                .orElse(null);
    }
    
    public List<Booking> getBookingsByCustomer(Customer customer) {
        return bookingFileService.readBookingsFromFile().stream()
                .filter(booking -> booking.getCustomer().getCustomerId() == customer.getCustomerId())
                .collect(Collectors.toList());
    }
    
    public List<Booking> getAllBookings() {
        List<Booking> bookings = bookingFileService.readBookingsFromFile();
        List<Customer> customers = customerFileService.readCustomersFromFile();
        List<Table> tables = tableService.getAllTables();
        for (Booking booking : bookings) {
            // Ánh xạ customerId sang object
            if (booking.getCustomer() == null && booking.getCustomerId() > 0) {
                Customer customer = customers.stream()
                    .filter(c -> c.getCustomerId() == booking.getCustomerId())
                    .findFirst().orElse(null);
                booking.setCustomer(customer);
            }
            // Ánh xạ tableId sang object
            if (booking.getTable() == null && booking.getTableId() > 0) {
                Table table = tables.stream()
                    .filter(t -> t.getTableId() == booking.getTableId())
                    .findFirst().orElse(null);
                booking.setTable(table);
            }
            // Khởi tạo lại state từ status
            booking.syncStateWithStatus();
        }
        return bookings;
    }
    
    public void completeBooking(int bookingId) {
        List<Booking> bookings = bookingFileService.readBookingsFromFile();
        List<Table> tables = tableService.getAllTables();
        
        Booking booking = findBookingById(bookingId, bookings);
        if (booking != null) {
            booking.transitionTo(new Booking.CompletedState());
            
            // Cập nhật trạng thái bàn
            Table table = booking.getTable();
            table.setStatus(TableStatus.AVAILABLE);
            updateTableInList(tables, table);
            
            // Lưu thay đổi
            bookings.remove(booking);
            bookingFileService.writeBookingsToFile(bookings);
            tableService.writeTablesToFile(tables);
        }
    }
    
    /**
     * Fix các booking có customer null
     */
    public void fixBookingsWithNullCustomer() {
        List<Booking> bookings = bookingFileService.readBookingsFromFile();
        List<Customer> customers = customerFileService.readCustomersFromFile();
        
        boolean hasChanges = false;
        
        for (Booking booking : bookings) {
            if (booking.getCustomer() == null) {
                // Tạo customer mặc định
                int nextCustomerId = customers.stream()
                        .mapToInt(Customer::getCustomerId)
                        .max()
                        .orElse(0) + 1;
                
                Customer defaultCustomer = new Customer(nextCustomerId, "Khách hàng không xác định", "0000000000");
                customers.add(defaultCustomer);
                booking.setCustomer(defaultCustomer);
                hasChanges = true;
                
                DebugUtil.debugPrint("🔧 Fixed booking #" + booking.getBookingId() + " with default customer");
            }
        }
        
        if (hasChanges) {
            bookingFileService.writeBookingsToFile(bookings);
            customerFileService.writeCustomersToFile(customers);
            DebugUtil.debugPrint("✅ Fixed " + bookings.stream().filter(b -> b.getCustomer() != null).count() + " bookings");
        } else {
            DebugUtil.debugPrint("✅ No bookings need fixing");
        }
    }
    
    /**
     * Delete a booking permanently
     */
    public boolean deleteBooking(int bookingId) {
        List<Booking> bookings = bookingFileService.readBookingsFromFile();
        List<Table> tables = tableService.getAllTables();
        Booking booking = findBookingById(bookingId, bookings);
        if (booking != null) {
            // Ánh xạ lại Table nếu đang null
            if (booking.getTable() == null && booking.getTableId() > 0) {
                Table table = tables.stream()
                    .filter(t -> t.getTableId() == booking.getTableId())
                    .findFirst().orElse(null);
                booking.setTable(table);
            }
            // Ánh xạ lại Customer nếu đang null
            if (booking.getCustomer() == null && booking.getCustomerId() > 0) {
                List<Customer> customers = customerFileService.readCustomersFromFile();
                Customer customer = customers.stream()
                    .filter(c -> c.getCustomerId() == booking.getCustomerId())
                    .findFirst().orElse(null);
                booking.setCustomer(customer);
            }
            // Update table status if booking was confirmed
            if (booking.getStatus().equals("CONFIRMED")) {
                Table table = booking.getTable();
                if (table != null) {
                    table.setStatus(TableStatus.AVAILABLE);
                    updateTableInList(tables, table);
                    tableService.writeTablesToFile(tables);
                }
            }
            // Remove booking
            bookings.remove(booking);
            bookingFileService.writeBookingsToFile(bookings);
            return true;
        }
        return false;
    }
    
    public boolean updateBooking(int id, String guestsStr, String timeStr) {
        List<Booking> bookings = bookingFileService.readBookingsFromFile();
        Booking booking = null;
        for (Booking b : bookings) if (b.getBookingId() == id) booking = b;
        if (booking == null) return false;
        if (guestsStr != null && !guestsStr.isEmpty()) {
            try { booking.setNumberOfGuests(Integer.parseInt(guestsStr)); } catch (Exception e) { return false; }
        }
        if (timeStr != null && !timeStr.isEmpty()) {
            try {
                java.time.LocalDateTime newTime = java.time.LocalDateTime.parse(timeStr, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                booking.setBookingTime(newTime);
            } catch (Exception e) { return false; }
        }
        for (int i = 0; i < bookings.size(); i++) if (bookings.get(i).getBookingId() == id) bookings.set(i, booking);
        bookingFileService.writeBookingsToFile(bookings);
        return true;
    }
    
    /**
     * Tạo booking từ BookingRequest DTO (refactor cho controller mỏng)
     */
    public Booking createBooking(BookingRequest req) {
        List<Table> tables = tableService.getAllTables();
        List<Booking> bookings = bookingFileService.readBookingsFromFile();

        // Lưu thông tin khách hàng vào customers.json và lấy customer đã được lưu
        Customer customer = new Customer(0, req.getName(), req.getPhone(), req.getEmail(), "user", "");
        Customer savedCustomer = saveCustomerToFile(customer);

        // Đọc lại customers từ file để đảm bảo đồng bộ
        List<Customer> customers = customerFileService.readCustomersFromFile();
        Customer realCustomer = customers.stream()
            .filter(c -> c.getPhone().equals(savedCustomer.getPhone()))
            .findFirst()
            .orElse(savedCustomer);

        Table availableTable = tableService.findAvailableTable(req.getGuests());
        if (availableTable == null) {
            return null; // Không có bàn phù hợp
        }

        // Kiểm tra xem bàn có bị đặt trùng thời gian không
        if (isTableBookedAtTime(availableTable, req.getBookingTime(), bookings)) {
            return null; // Bàn đã được đặt vào thời gian này
        }

        // Tạo booking mới với customer đã được lưu
        Booking booking = new Booking(nextBookingId++, realCustomer, availableTable, req.getBookingTime(), req.getGuests());

        // Cập nhật trạng thái bàn
        availableTable.setStatus(TableStatus.RESERVED);

        // Cập nhật danh sách bàn
        updateTableInList(tables, availableTable);
        tableService.writeTablesToFile(tables);

        // Thêm booking mới vào danh sách
        bookings.add(booking);
        bookingFileService.writeBookingsToFile(bookings);

        // Cập nhật activeBookingIds cho customer
        realCustomer.addBookingId(booking.getBookingId());
        updateCustomerInList(customers, realCustomer);
        customerFileService.writeCustomersToFile(customers);

        return booking;
    }
} 