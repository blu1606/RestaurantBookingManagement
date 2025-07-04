package restaurantbookingmanagement.service;

import restaurantbookingmanagement.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service xử lý logic nghiệp vụ đơn hàng
 */
public class OrderService {
    private final FileService fileService;
    private int nextOrderId;
    
    public OrderService() {
        this.fileService = new FileService();
        this.nextOrderId = 1;
        
        // Khởi tạo dữ liệu mẫu nếu cần
        fileService.initializeSampleDataIfNeeded();
        
        // Tính toán nextOrderId từ dữ liệu hiện tại
        calculateNextOrderId();
    }
    
    private void calculateNextOrderId() {
        List<Order> existingOrders = fileService.readOrdersFromFile();
        if (!existingOrders.isEmpty()) {
            this.nextOrderId = existingOrders.stream()
                    .mapToInt(Order::getOrderId)
                    .max()
                    .orElse(0) + 1;
        }
    }
    
    public List<MenuItem> getAllMenuItems() {
        return new ArrayList<>(fileService.readMenuItemsFromFile());
    }
    
    public MenuItem findMenuItemByName(String name) {
        return fileService.readMenuItemsFromFile().stream()
                .filter(item -> item.getName().toLowerCase().contains(name.toLowerCase()))
                .findFirst()
                .orElse(null);
    }
    
    public MenuItem findMenuItemById(int itemId) {
        return fileService.readMenuItemsFromFile().stream()
                .filter(item -> item.getItemId() == itemId)
                .findFirst()
                .orElse(null);
    }
    
    public Order createOrder(Booking booking) {
        List<Order> orders = fileService.readOrdersFromFile();
        List<Table> tables = fileService.readTablesFromFile();
        Order order = new Order(nextOrderId++, booking);
        // Gán tableId cho order
        if (booking != null && booking.getTable() != null) {
            order.setTableId(booking.getTable().getTableId());
            // Thêm orderId vào table.orderIds
            for (Table t : tables) {
                if (t.getTableId() == booking.getTable().getTableId()) {
                    t.addOrderId(order.getOrderId());
                    break;
                }
            }
            fileService.writeTablesToFile(tables);
        }
        orders.add(order);
        fileService.writeOrdersToFile(orders);
        return order;
    }
    
    /**
     * Tạo order cho booking cụ thể hoặc tìm order hiện có
     */
    public Order getOrCreateOrderForBooking(Booking booking) {
        List<Order> orders = fileService.readOrdersFromFile();
        
        // Tìm order hiện có cho booking này
        Order existingOrder = orders.stream()
                .filter(order -> order.getBooking().getBookingId() == booking.getBookingId() && 
                               !order.getStatus().equals("COMPLETED"))
                .findFirst()
                .orElse(null);
        
        if (existingOrder != null) {
            return existingOrder;
        }
        
        // Tạo order mới nếu chưa có
        Order newOrder = new Order(nextOrderId++, booking);
        orders.add(newOrder);
        fileService.writeOrdersToFile(orders);
        return newOrder;
    }
    
    /**
     * Tính bill cho booking cụ thể
     */
    public double calculateBillForBooking(int bookingId) {
        List<Order> orders = fileService.readOrdersFromFile();
        
        return orders.stream()
                .filter(order -> order.getBooking().getBookingId() == bookingId && 
                               !order.getStatus().equals("COMPLETED"))
                .mapToDouble(Order::getTotalAmount)
                .sum();
    }
    
    /**
     * Lấy tất cả orders cho booking cụ thể
     */
    public List<Order> getOrdersForBooking(int bookingId) {
        return fileService.readOrdersFromFile().stream()
                .filter(order -> order.getBooking().getBookingId() == bookingId)
                .collect(Collectors.toList());
    }
    
    /**
     * Hoàn thành order (thanh toán)
     */
    public boolean completeOrder(int orderId) {
        List<Order> orders = fileService.readOrdersFromFile();
        Order order = findOrderById(orderId, orders);
        if (order != null) {
            order.setStatus("COMPLETED");
            fileService.writeOrdersToFile(orders);
            return true;
        }
        return false;
    }
    
    private void updateOrderTotalAmount(Order order) {
        double total = 0.0;
        for (Order.OrderItem oi : order.getItems()) {
            MenuItem mi = findMenuItemById(oi.getItemId());
            if (mi != null) {
                oi.setItem(mi);
                total += mi.getPrice() * oi.getAmount();
            }
        }
        order.setOrderTime(order.getOrderTime()); // Đảm bảo không bị null
        order.setStatus(order.getStatus());
        order.setTableId(order.getTableId());
        // Gán lại tổng tiền
        try {
            java.lang.reflect.Field f = order.getClass().getDeclaredField("totalAmount");
            f.setAccessible(true);
            f.set(order, total);
        } catch (Exception e) {}
    }
    
    public boolean addItemToOrder(int orderId, String itemName, int quantity) {
        List<Order> orders = fileService.readOrdersFromFile();
        Order order = findOrderById(orderId, orders);
        if (order == null) {
            return false;
        }
        MenuItem item = findMenuItemByName(itemName);
        if (item == null) {
            return false;
        }
        order.addItem(item.getItemId(), quantity);
        updateOrderTotalAmount(order);
        fileService.writeOrdersToFile(orders);
        return true;
    }
    
    public boolean addItemToOrder(int orderId, int itemId, int quantity) {
        List<Order> orders = fileService.readOrdersFromFile();
        Order order = findOrderById(orderId, orders);
        if (order == null) {
            return false;
        }
        order.addItem(itemId, quantity);
        updateOrderTotalAmount(order);
        fileService.writeOrdersToFile(orders);
        return true;
    }
    
    public boolean removeItemFromOrder(int orderId, String itemName) {
        List<Order> orders = fileService.readOrdersFromFile();
        Order order = findOrderById(orderId, orders);
        if (order == null) {
            return false;
        }
        MenuItem item = findMenuItemByName(itemName);
        if (item == null) {
            return false;
        }
        order.removeItem(item.getItemId());
        updateOrderTotalAmount(order);
        fileService.writeOrdersToFile(orders);
        return true;
    }
    
    public boolean updateOrderStatus(int orderId, String status) {
        List<Order> orders = fileService.readOrdersFromFile();
        Order order = findOrderById(orderId, orders);
        if (order != null) {
            order.setStatus(status);
            fileService.writeOrdersToFile(orders);
            return true;
        }
        return false;
    }
    
    public Order findOrderById(int orderId) {
        return findOrderById(orderId, fileService.readOrdersFromFile());
    }
    
    private Order findOrderById(int orderId, List<Order> orders) {
        return orders.stream()
                .filter(order -> order.getOrderId() == orderId)
                .findFirst()
                .orElse(null);
    }
    
    public List<Order> getOrdersByBooking(Booking booking) {
        return fileService.readOrdersFromFile().stream()
                .filter(order -> order.getBooking().getBookingId() == booking.getBookingId())
                .collect(Collectors.toList());
    }
    
    public List<Order> getAllOrders() {
        List<Order> orders = fileService.readOrdersFromFile();
        for (Order o : orders) updateOrderTotalAmount(o);
        return orders;
    }
    
    public List<Order> getOrdersByStatus(String status) {
        return fileService.readOrdersFromFile().stream()
                .filter(order -> order.getStatus().equals(status))
                .collect(Collectors.toList());
    }
    
    public double getTotalRevenue() {
        return fileService.readOrdersFromFile().stream()
                .filter(order -> order.getStatus().equals("COMPLETED"))
                .mapToDouble(Order::getTotalAmount)
                .sum();
    }
    
    /**
     * Add a new menu item to the system
     */
    public MenuItem addMenuItem(String name, double price, String description) {
        List<MenuItem> menuItems = fileService.readMenuItemsFromFile();
        
        // Calculate next item ID
        int nextItemId = menuItems.stream()
                .mapToInt(MenuItem::getItemId)
                .max()
                .orElse(0) + 1;
        
        MenuItem newItem = new MenuItem(nextItemId, name, price, description);
        menuItems.add(newItem);
        
        fileService.writeMenuItemsToFile(menuItems);
        return newItem;
    }
    
    /**
     * Delete a menu item from the system
     */
    public boolean deleteMenuItem(int itemId) {
        List<MenuItem> menuItems = fileService.readMenuItemsFromFile();
        
        MenuItem itemToDelete = menuItems.stream()
                .filter(item -> item.getItemId() == itemId)
                .findFirst()
                .orElse(null);
        
        if (itemToDelete != null) {
            menuItems.remove(itemToDelete);
            fileService.writeMenuItemsToFile(menuItems);
            return true;
        }
        return false;
    }
    
    public boolean updateMenuItem(int id, String newName, String priceStr, String newDesc) {
        List<MenuItem> items = fileService.readMenuItemsFromFile();
        MenuItem item = null;
        for (MenuItem mi : items) {
            if (mi.getItemId() == id) {
                item = mi;
                break;
            }
        }
        if (item == null) return false;
        if (newName != null && !newName.isEmpty()) item.setName(newName);
        if (priceStr != null && !priceStr.isEmpty()) {
            try {
                item.setPrice(Double.parseDouble(priceStr));
            } catch (Exception e) {
                return false;
            }
        }
        if (newDesc != null && !newDesc.isEmpty()) item.setDescription(newDesc);
        // Ghi lại file
        fileService.writeMenuItemsToFile(items);
        return true;
    }

    // Thêm món mới cho user 
    public Order addOrder(String name, double price, String desc) {
        List<Order> orders = fileService.readOrdersFromFile();
        // Tạo order mới với thông tin món ăn đơn giản 
        Order order = new Order(nextOrderId++, null);
        // Tạo menu item tạm thời (nếu cần)
        // MenuItem item = new MenuItem(-1, name, price, desc);
        // order.addItem(item, 1); // Sửa lại cho phù hợp với itemId
        // Để đơn giản, không thêm item vào order ở đây (vì không có itemId thực)
        orders.add(order);
        fileService.writeOrdersToFile(orders);
        return order;
    }

    // Tính tổng tiền của tất cả đơn hàng (cho user)
    public double calculateTotal() {
        List<Order> orders = fileService.readOrdersFromFile();
        return orders.stream()
                .filter(order -> !order.getStatus().equals("COMPLETED"))
                .mapToDouble(Order::getTotalAmount)
                .sum();
    }

    // Xóa đơn hàng theo orderId (cho user)
    public boolean deleteOrder(int orderId) {
        List<Order> orders = fileService.readOrdersFromFile();
        Order order = findOrderById(orderId, orders);
        if (order != null) {
            orders.remove(order);
            fileService.writeOrdersToFile(orders);
            return true;
        }
        return false;
    }

    // Cập nhật thông tin món đầu tiên trong đơn hàng (dùng cho quản lý đơn hàng đơn giản)
    public boolean updateOrder(int orderId, String name, double price, String desc) {
        List<Order> orders = fileService.readOrdersFromFile();
        Order order = findOrderById(orderId, orders);
        if (order == null) return false;
        if (!order.getItems().isEmpty()) {
            int firstItemId = order.getItems().get(0).getItemId();
            MenuItem firstItem = findMenuItemById(firstItemId);
            if (firstItem != null) {
                if (name != null && !name.isEmpty()) firstItem.setName(name);
                if (price > 0) firstItem.setPrice(price);
                if (desc != null && !desc.isEmpty()) firstItem.setDescription(desc);
                fileService.writeMenuItemsToFile(fileService.readMenuItemsFromFile());
                fileService.writeOrdersToFile(orders);
                return true;
            }
        }
        return false;
    }
} 