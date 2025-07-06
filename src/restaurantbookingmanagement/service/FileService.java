package restaurantbookingmanagement.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import restaurantbookingmanagement.model.*;
import restaurantbookingmanagement.ai.AIAgentConnector;
import restaurantbookingmanagement.utils.DebugUtil;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.HashMap;

/**
 * Service xử lý việc đọc và ghi dữ liệu từ các file JSON
 * Đây là "Single Writer" - chỉ có Java application mới được phép ghi file
 */
public class FileService {
    private static final String DATA_DIR = "data";
    private static final String TABLES_FILE = "tables.json";
    private static final String MENU_ITEMS_FILE = "menu_items.json";
    private static final String BOOKINGS_FILE = "bookings.json";
    private static final String CUSTOMERS_FILE = "customers.json";
    
    private final Gson gson;
    private final AIAgentConnector aiAgentConnector;
    
    public FileService() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        this.aiAgentConnector = new AIAgentConnector();
        
        // Đảm bảo thư mục data tồn tại
        createDataDirectoryIfNotExists();
    }
    
    private void createDataDirectoryIfNotExists() {
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }
    
    // === TABLES OPERATIONS ===
    
    /**
     * Đọc danh sách bàn từ file
     */
    public synchronized List<Table> readTablesFromFile() {
        try {
            File file = new File(DATA_DIR, TABLES_FILE);
            if (!file.exists()) {
                return new ArrayList<>();
            }
            
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> rawTables = gson.fromJson(reader, listType);
                if (rawTables == null) return new ArrayList<>();
                List<Table> tables = new ArrayList<>();
                for (Map<String, Object> rawTable : rawTables) {
                    Table table = new Table();
                    if (rawTable.containsKey("tableId")) {
                        table.setTableId(((Number) rawTable.get("tableId")).intValue());
                    }
                    if (rawTable.containsKey("capacity")) {
                        table.setCapacity(((Number) rawTable.get("capacity")).intValue());
                    }
                    if (rawTable.containsKey("status")) {
                        table.setStatus(TableStatus.valueOf((String) rawTable.get("status")));
                    }
                    if (rawTable.containsKey("orderIds")) {
                        List<?> orderIdsRaw = (List<?>) rawTable.get("orderIds");
                        List<Integer> orderIds = new ArrayList<>();
                        for (Object d : orderIdsRaw) {
                            if (d instanceof Number) {
                                orderIds.add(((Number) d).intValue());
                            }
                        }
                        table.setOrderIds(orderIds);
                    }
                    tables.add(table);
                }
                return tables;
            }
        } catch (Exception e) {
            System.err.println("Error reading tables from file: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Ghi danh sách bàn vào file
     */
    public synchronized void writeTablesToFile(List<Table> tables) {
        try {
            File file = new File(DATA_DIR, TABLES_FILE);
            List<Map<String, Object>> jsonTables = new ArrayList<>();
            for (Table table : tables) {
                Map<String, Object> jsonTable = new HashMap<>();
                jsonTable.put("tableId", table.getTableId());
                jsonTable.put("capacity", table.getCapacity());
                jsonTable.put("status", table.getStatus().name());
                jsonTable.put("orderIds", table.getOrderIds());
                jsonTables.add(jsonTable);
            }
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(jsonTables, writer);
            }
            notifyAIAgentOfChange();
        } catch (Exception e) {
            System.err.println("Error writing tables to file: " + e.getMessage());
        }
    }
    
    // === MENU ITEMS OPERATIONS ===
    
    /**
     * Đọc danh sách món ăn từ file
     */
    public synchronized List<MenuItem> readMenuItemsFromFile() {
        try {
            File file = new File(DATA_DIR, MENU_ITEMS_FILE);
            if (!file.exists()) {
                return new ArrayList<>();
            }
            
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                Type listType = new TypeToken<List<MenuItem>>(){}.getType();
                List<MenuItem> menuItems = gson.fromJson(reader, listType);
                return menuItems != null ? menuItems : new ArrayList<>();
            }
        } catch (Exception e) {
            System.err.println("Error reading menu items from file: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Ghi danh sách món ăn vào file
     */
    public synchronized void writeMenuItemsToFile(List<MenuItem> menuItems) {
        try {
            File file = new File(DATA_DIR, MENU_ITEMS_FILE);
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(menuItems, writer);
            }
            notifyAIAgentOfChange();
        } catch (Exception e) {
            System.err.println("Error writing menu items to file: " + e.getMessage());
        }
    }
    
    // === BOOKINGS OPERATIONS ===
    
    /**
     * Đọc danh sách đặt bàn từ file
     */
    public synchronized List<Booking> readBookingsFromFile() {
        try {
            File file = new File(DATA_DIR, BOOKINGS_FILE);
            if (!file.exists()) {
                return new ArrayList<>();
            }
            
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                // Đọc dữ liệu JSON raw trước
                List<Map<String, Object>> rawBookings = gson.fromJson(reader, new TypeToken<List<Map<String, Object>>>(){}.getType());
                
                if (rawBookings == null) {
                    return new ArrayList<>();
                }
                
                // Đọc customers và tables để resolve references
                List<Customer> customers = readCustomersFromFile();
                List<Table> tables = readTablesFromFile();
                
                List<Booking> bookings = new ArrayList<>();
                for (Map<String, Object> rawBooking : rawBookings) {
                    Booking booking = new Booking();
                    
                    // Set basic fields
                    if (rawBooking.containsKey("bookingId")) {
                        booking.setBookingId(((Number) rawBooking.get("bookingId")).intValue());
                    }
                    if (rawBooking.containsKey("numberOfGuests")) {
                        booking.setNumberOfGuests(((Number) rawBooking.get("numberOfGuests")).intValue());
                    }
                    if (rawBooking.containsKey("status")) {
                        booking.setStatus((String) rawBooking.get("status"));
                    }
                    
                    // Parse booking time
                    if (rawBooking.containsKey("bookingTime")) {
                        String timeStr = (String) rawBooking.get("bookingTime");
                        try {
                            LocalDateTime bookingTime = LocalDateTime.parse(timeStr);
                            booking.setBookingTime(bookingTime);
                        } catch (Exception e) {
                            System.err.println("Error parsing booking time: " + timeStr);
                        }
                    }
                    
                    // Resolve customer reference
                    if (rawBooking.containsKey("customerId")) {
                        int customerId = ((Number) rawBooking.get("customerId")).intValue();
                        DebugUtil.debugPrint("🔍 DEBUG - Resolving customer for booking #" + booking.getBookingId() + ": customerId = " + customerId);
                        Customer customer = customers.stream()
                                .filter(c -> c.getCustomerId() == customerId)
                                .findFirst()
                                .orElse(null);
                        if (customer != null) {
                            booking.setCustomer(customer);
                            DebugUtil.debugPrint("✅ Found customer: " + customer.getName() + " (" + customer.getPhone() + ")");
                        } else {
                            Customer defaultCustomer = new Customer(customerId, "Unknown Customer", "0000000000");
                            booking.setCustomer(defaultCustomer);
                            DebugUtil.debugPrint("⚠️ Created default customer for booking #" + booking.getBookingId() + " with customerId: " + customerId);
                            DebugUtil.debugPrint("   - Available customers: " + customers.stream().map(c -> c.getCustomerId() + ":" + c.getName()).collect(java.util.stream.Collectors.joining(", ")));
                        }
                    }
                    
                    // Resolve table reference
                    if (rawBooking.containsKey("tableId")) {
                        int tableId = ((Number) rawBooking.get("tableId")).intValue();
                        Table table = tables.stream()
                                .filter(t -> t.getTableId() == tableId)
                                .findFirst()
                                .orElse(null);
                        booking.setTable(table);
                    }
                    
                    bookings.add(booking);
                }
                
                return bookings;
            }
        } catch (Exception e) {
            System.err.println("Error reading bookings from file: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Ghi danh sách đặt bàn vào file
     */
    public synchronized void writeBookingsToFile(List<Booking> bookings) {
        try {
            File file = new File(DATA_DIR, BOOKINGS_FILE);
            
            // Convert Booking objects to JSON format with IDs
            List<Map<String, Object>> jsonBookings = new ArrayList<>();
            for (Booking booking : bookings) {
                Map<String, Object> jsonBooking = new HashMap<>();
                jsonBooking.put("bookingId", booking.getBookingId());
                
                // Debug customer info
                Customer customer = booking.getCustomer();
                if (customer != null) {
                    jsonBooking.put("customerId", customer.getCustomerId());
                    DebugUtil.debugPrint("🔍 DEBUG - Writing booking #" + booking.getBookingId() + " with customer: " + customer.getName() + " (ID: " + customer.getCustomerId() + ")");
                } else {
                    jsonBooking.put("customerId", null);
                    DebugUtil.debugPrint("⚠️ DEBUG - Writing booking #" + booking.getBookingId() + " with null customer");
                }
                
                jsonBooking.put("tableId", booking.getTable() != null ? booking.getTable().getTableId() : null);
                jsonBooking.put("bookingTime", booking.getBookingTime() != null ? booking.getBookingTime().toString() : null);
                jsonBooking.put("numberOfGuests", booking.getNumberOfGuests());
                jsonBooking.put("status", booking.getStatus());
                jsonBookings.add(jsonBooking);
            }
            
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(jsonBookings, writer);
            }
            notifyAIAgentOfChange();
        } catch (Exception e) {
            System.err.println("Error writing bookings to file: " + e.getMessage());
        }
    }
    
    // === CUSTOMERS OPERATIONS ===
    
    /**
     * Đọc danh sách khách hàng từ file
     */
    public synchronized List<Customer> readCustomersFromFile() {
        try {
            File file = new File(DATA_DIR, CUSTOMERS_FILE);
            if (!file.exists()) {
                return new ArrayList<>();
            }
            
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                Type listType = new TypeToken<List<Customer>>(){}.getType();
                List<Customer> customers = gson.fromJson(reader, listType);
                return customers != null ? customers : new ArrayList<>();
            }
        } catch (Exception e) {
            System.err.println("Error reading customers from file: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Ghi danh sách khách hàng vào file
     */
    public synchronized void writeCustomersToFile(List<Customer> customers) {
        try {
            File file = new File(DATA_DIR, CUSTOMERS_FILE);
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(customers, writer);
            }
            notifyAIAgentOfChange();
        } catch (Exception e) {
            System.err.println("Error writing customers to file: " + e.getMessage());
        }
    }
    
    // === ORDERS OPERATIONS ===
    
    /**
     * Đọc danh sách đơn hàng từ file
     */
    public synchronized List<Order> readOrdersFromFile() {
        try {
            File file = new File(DATA_DIR, "orders.json");
            if (!file.exists()) {
                return new ArrayList<>();
            }
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                List<Map<String, Object>> rawOrders = gson.fromJson(reader, new TypeToken<List<Map<String, Object>>>(){}.getType());
                if (rawOrders == null) {
                    return new ArrayList<>();
                }
                List<Booking> bookings = readBookingsFromFile();
                List<MenuItem> menuItems = readMenuItemsFromFile();
                List<Order> orders = new ArrayList<>();
                for (Map<String, Object> rawOrder : rawOrders) {
                    Booking booking = null;
                    if (rawOrder.containsKey("bookingId")) {
                        int bookingId = ((Number) rawOrder.get("bookingId")).intValue();
                        booking = bookings.stream()
                            .filter(b -> b.getBookingId() == bookingId)
                            .findFirst()
                            .orElse(null);
                    }
                    if (booking == null) {
                        booking = new Booking();
                    }
                    Order order = new Order(0, booking);
                    if (rawOrder.containsKey("orderId")) {
                        order.setOrderId(((Number) rawOrder.get("orderId")).intValue());
                    }
                    if (rawOrder.containsKey("status")) {
                        order.setStatus((String) rawOrder.get("status"));
                    }
                    if (rawOrder.containsKey("orderTime")) {
                        String timeStr = (String) rawOrder.get("orderTime");
                        try {
                            LocalDateTime orderTime = LocalDateTime.parse(timeStr);
                            order.setOrderTime(orderTime);
                        } catch (Exception e) {
                            System.err.println("Error parsing order time: " + timeStr);
                        }
                    }
                    if (rawOrder.containsKey("tableId")) {
                        order.setTableId(((Number) rawOrder.get("tableId")).intValue());
                    }
                    // Parse items
                    if (rawOrder.containsKey("items")) {
                        List<Map<String, Object>> rawItems = (List<Map<String, Object>>) rawOrder.get("items");
                        List<Order.OrderItem> items = new ArrayList<>();
                        for (Map<String, Object> rawItem : rawItems) {
                            int itemId = ((Number) rawItem.get("itemId")).intValue();
                            int amount = ((Number) rawItem.get("amount")).intValue();
                            MenuItem mi = menuItems.stream().filter(m -> m.getItemId() == itemId).findFirst().orElse(null);
                            if (mi != null) {
                                Order.OrderItem orderItem = new Order.OrderItem(itemId, amount);
                                orderItem.setItem(mi);
                                items.add(orderItem);
                            }
                        }
                        order.setItems(items);
                    }
                    orders.add(order);
                }
                return orders;
            }
        } catch (Exception e) {
            System.err.println("Error reading orders from file: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Ghi danh sách đơn hàng vào file
     */
    public synchronized void writeOrdersToFile(List<Order> orders) {
        try {
            File file = new File(DATA_DIR, "orders.json");
            List<Map<String, Object>> jsonOrders = new ArrayList<>();
            for (Order order : orders) {
                Map<String, Object> jsonOrder = new HashMap<>();
                jsonOrder.put("orderId", order.getOrderId());
                jsonOrder.put("bookingId", order.getBooking() != null ? order.getBooking().getBookingId() : null);
                jsonOrder.put("orderTime", order.getOrderTime() != null ? order.getOrderTime().toString() : null);
                jsonOrder.put("status", order.getStatus());
                jsonOrder.put("totalAmount", order.getTotalAmount());
                jsonOrder.put("tableId", order.getTableId());
                // Write items
                List<Map<String, Object>> items = new ArrayList<>();
                for (Order.OrderItem oi : order.getItems()) {
                    Map<String, Object> itemObj = new HashMap<>();
                    itemObj.put("itemId", oi.getItemId());
                    itemObj.put("amount", oi.getAmount());
                    items.add(itemObj);
                }
                jsonOrder.put("items", items);
                jsonOrders.add(jsonOrder);
            }
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(jsonOrders, writer);
            }
            notifyAIAgentOfChange();
        } catch (Exception e) {
            System.err.println("Error writing orders to file: " + e.getMessage());
        }
    }
    
    // === AI AGENT NOTIFICATION ===
    
    /**
     * Thông báo cho AI Agent rằng dữ liệu đã thay đổi
     */
    private void notifyAIAgentOfChange() {
        try {
            // Gọi endpoint refresh-knowledge của AI Agent
            aiAgentConnector.notifyKnowledgeRefresh();
        } catch (Exception e) {
            System.err.println("Warning: Could not notify AI Agent of data change: " + e.getMessage());
        }
    }
    
    /**
     * Khởi tạo dữ liệu mẫu nếu các file chưa tồn tại
     */
    public void initializeSampleDataIfNeeded() {
        // Kiểm tra và tạo dữ liệu mẫu cho tables
        if (readTablesFromFile().isEmpty()) {
            List<Table> sampleTables = new ArrayList<>();
            sampleTables.add(new Table(1, 2));
            sampleTables.add(new Table(2, 4));
            sampleTables.add(new Table(3, 4));
            sampleTables.add(new Table(4, 6));
            sampleTables.add(new Table(5, 8));
            writeTablesToFile(sampleTables);
        }
        
        // Kiểm tra và tạo dữ liệu mẫu cho menu items
        if (readMenuItemsFromFile().isEmpty()) {
            List<MenuItem> sampleMenuItems = new ArrayList<>();
            sampleMenuItems.add(new MenuItem(101, "Phở Bò Tái", 50000.0, "Nước dùng hầm xương 8 tiếng, thịt bò tươi."));
            sampleMenuItems.add(new MenuItem(102, "Bún Chả Hà Nội", 45000.0, "Đặc sản nức tiếng với thịt nướng thơm lừng."));
            sampleMenuItems.add(new MenuItem(201, "Cà phê sữa đá", 25000.0, "Cà phê Robusta pha phin với sữa đặc."));
            writeMenuItemsToFile(sampleMenuItems);
        }
        
        // Kiểm tra và tạo dữ liệu mẫu cho customers
        if (readCustomersFromFile().isEmpty()) {
            List<Customer> sampleCustomers = new ArrayList<>();
            sampleCustomers.add(new Customer(1, "Nguyễn Văn A", "0123456789", "nguyenvana@email.com"));
            writeCustomersToFile(sampleCustomers);
        }
    }
} 