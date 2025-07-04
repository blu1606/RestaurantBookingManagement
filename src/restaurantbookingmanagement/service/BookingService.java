package restaurantbookingmanagement.service;

import restaurantbookingmanagement.model.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import restaurantbookingmanagement.utils.DebugUtil;

/**
 * Service xử lý logic nghiệp vụ đặt bàn
 */
public class BookingService {
    private final FileService fileService;
    private int nextBookingId;
    
    public BookingService() {
        this.fileService = new FileService();
        this.nextBookingId = 1;
        
        // Khởi tạo dữ liệu mẫu nếu cần
        fileService.initializeSampleDataIfNeeded();
        
        // Tính toán nextBookingId từ dữ liệu hiện tại
        calculateNextBookingId();
    }
    
    private void calculateNextBookingId() {
        List<Booking> existingBookings = fileService.readBookingsFromFile();
        if (!existingBookings.isEmpty()) {
            this.nextBookingId = existingBookings.stream()
                    .mapToInt(Booking::getBookingId)
                    .max()
                    .orElse(0) + 1;
        }
    }
    
    public List<Table> getAllTables() {
        return new ArrayList<>(fileService.readTablesFromFile());
    }
    
    public List<Table> getAvailableTables() {
        return fileService.readTablesFromFile().stream()
                .filter(table -> table.getStatus() == TableStatus.AVAILABLE)
                .collect(Collectors.toList());
    }
    
    public List<Table> getAvailableTablesForCapacity(int capacity) {
        return fileService.readTablesFromFile().stream()
                .filter(table -> table.getStatus() == TableStatus.AVAILABLE && table.getCapacity() >= capacity)
                .collect(Collectors.toList());
    }
    
    public Table findAvailableTable(int capacity) {
        return fileService.readTablesFromFile().stream()
                .filter(table -> table.getStatus() == TableStatus.AVAILABLE && table.getCapacity() >= capacity)
                .findFirst()
                .orElse(null);
    }
    
    public Booking createBooking(Customer customer, int numberOfGuests, LocalDateTime bookingTime) {
        List<Table> tables = fileService.readTablesFromFile();
        List<Booking> bookings = fileService.readBookingsFromFile();
        // Lưu thông tin khách hàng vào customers.json và lấy customer đã được lưu
        Customer savedCustomer = saveCustomerToFile(customer);

        // Đọc lại customers từ file để đảm bảo đồng bộ
        List<Customer> customers = fileService.readCustomersFromFile();
        Customer realCustomer = customers.stream()
            .filter(c -> c.getPhone().equals(savedCustomer.getPhone()))
            .findFirst()
            .orElse(savedCustomer);

        // Debug: Log customer returned from saveCustomerToFile
        DebugUtil.debugPrint("🔍 DEBUG - Customer used for booking:");
        DebugUtil.debugPrint("   - Name: " + realCustomer.getName());
        DebugUtil.debugPrint("   - Phone: " + realCustomer.getPhone());
        DebugUtil.debugPrint("   - ID: " + realCustomer.getCustomerId());

        Table availableTable = findAvailableTable(numberOfGuests);
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
        fileService.writeTablesToFile(tables);

        // Thêm booking mới vào danh sách
        bookings.add(booking);
        fileService.writeBookingsToFile(bookings);

        // Cập nhật activeBookingIds cho customer
        realCustomer.addBookingId(booking.getBookingId());
        updateCustomerInList(customers, realCustomer);
        fileService.writeCustomersToFile(customers);

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
        List<Customer> customers = fileService.readCustomersFromFile();
        
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
            fileService.writeCustomersToFile(customers);
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
        List<Booking> bookings = fileService.readBookingsFromFile();
        List<Table> tables = fileService.readTablesFromFile();
        List<Customer> customers = fileService.readCustomersFromFile();
        
        Booking booking = findBookingById(bookingId, bookings);
        if (booking != null && booking.getStatus().equals("CONFIRMED")) {
            booking.setStatus("CANCELLED");
            
            // Cập nhật trạng thái bàn
            Table table = booking.getTable();
            table.setStatus(TableStatus.AVAILABLE);
            updateTableInList(tables, table);
            
            // Xóa bookingId khỏi activeBookingIds của customer
            Customer customer = booking.getCustomer();
            if (customer != null) {
                customer.removeBookingId(bookingId);
                updateCustomerInList(customers, customer);
                fileService.writeCustomersToFile(customers);
            }
            
            // Lưu thay đổi
            fileService.writeBookingsToFile(bookings);
            fileService.writeTablesToFile(tables);
            
            return true;
        }
        return false;
    }
    
    public Booking findBookingById(int bookingId) {
        return findBookingById(bookingId, fileService.readBookingsFromFile());
    }
    
    private Booking findBookingById(int bookingId, List<Booking> bookings) {
        return bookings.stream()
                .filter(booking -> booking.getBookingId() == bookingId)
                .findFirst()
                .orElse(null);
    }
    
    public List<Booking> getBookingsByCustomer(Customer customer) {
        return fileService.readBookingsFromFile().stream()
                .filter(booking -> booking.getCustomer().getCustomerId() == customer.getCustomerId())
                .collect(Collectors.toList());
    }
    
    public List<Booking> getAllBookings() {
        return new ArrayList<>(fileService.readBookingsFromFile());
    }
    
    public void completeBooking(int bookingId) {
        List<Booking> bookings = fileService.readBookingsFromFile();
        List<Table> tables = fileService.readTablesFromFile();
        
        Booking booking = findBookingById(bookingId, bookings);
        if (booking != null) {
            booking.setStatus("COMPLETED");
            
            // Cập nhật trạng thái bàn
            Table table = booking.getTable();
            table.setStatus(TableStatus.AVAILABLE);
            updateTableInList(tables, table);
            
            // Lưu thay đổi
            fileService.writeBookingsToFile(bookings);
            fileService.writeTablesToFile(tables);
        }
    }
    
    /**
     * Fix các booking có customer null
     */
    public void fixBookingsWithNullCustomer() {
        List<Booking> bookings = fileService.readBookingsFromFile();
        List<Customer> customers = fileService.readCustomersFromFile();
        
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
            fileService.writeBookingsToFile(bookings);
            fileService.writeCustomersToFile(customers);
            DebugUtil.debugPrint("✅ Fixed " + bookings.stream().filter(b -> b.getCustomer() != null).count() + " bookings");
        } else {
            DebugUtil.debugPrint("✅ No bookings need fixing");
        }
    }
    
    /**
     * Add a new table to the system
     */
    public Table addTable(int capacity) {
        List<Table> tables = fileService.readTablesFromFile();
        
        // Calculate next table ID
        int nextTableId = tables.stream()
                .mapToInt(Table::getTableId)
                .max()
                .orElse(0) + 1;
        
        Table newTable = new Table(nextTableId, capacity);
        tables.add(newTable);
        
        fileService.writeTablesToFile(tables);
        return newTable;
    }
    
    /**
     * Delete a booking permanently
     */
    public boolean deleteBooking(int bookingId) {
        List<Booking> bookings = fileService.readBookingsFromFile();
        List<Table> tables = fileService.readTablesFromFile();
        
        Booking booking = findBookingById(bookingId, bookings);
        if (booking != null) {
            // Update table status if booking was confirmed
            if (booking.getStatus().equals("CONFIRMED")) {
                Table table = booking.getTable();
                table.setStatus(TableStatus.AVAILABLE);
                updateTableInList(tables, table);
                fileService.writeTablesToFile(tables);
            }
            
            // Remove booking
            bookings.remove(booking);
            fileService.writeBookingsToFile(bookings);
            return true;
        }
        return false;
    }
    
    public boolean updateTable(int id, String newCapacity, String newStatus) {
        List<Table> tables = fileService.readTablesFromFile();
        Table table = null;
        for (Table t : tables) if (t.getTableId() == id) table = t;
        if (table == null) return false;
        if (newCapacity != null && !newCapacity.isEmpty()) {
            try { table.setCapacity(Integer.parseInt(newCapacity)); } catch (Exception e) { return false; }
        }
        if (newStatus != null && !newStatus.isEmpty()) {
            try { table.setStatus(TableStatus.valueOf(newStatus)); } catch (Exception e) { return false; }
        }
        for (int i = 0; i < tables.size(); i++) if (tables.get(i).getTableId() == id) tables.set(i, table);
        fileService.writeTablesToFile(tables);
        return true;
    }
    
    public boolean deleteTable(int id) {
        List<Table> tables = fileService.readTablesFromFile();
        boolean found = false;
        for (int i = 0; i < tables.size(); i++) {
            if (tables.get(i).getTableId() == id) {
                tables.remove(i);
                found = true;
                break;
            }
        }
        if (found) fileService.writeTablesToFile(tables);
        return found;
    }
    
    public List<Table> searchTables(String keyword) {
        List<Table> tables = fileService.readTablesFromFile();
        List<Table> result = new ArrayList<>();
        for (Table t : tables) {
            if (String.valueOf(t.getTableId()).equals(keyword) || String.valueOf(t.getCapacity()).equals(keyword) || t.getStatus().name().equalsIgnoreCase(keyword)) {
                result.add(t);
            }
        }
        return result;
    }
    
    public boolean updateBooking(int id, String guestsStr, String timeStr) {
        List<Booking> bookings = fileService.readBookingsFromFile();
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
        fileService.writeBookingsToFile(bookings);
        return true;
    }
} 