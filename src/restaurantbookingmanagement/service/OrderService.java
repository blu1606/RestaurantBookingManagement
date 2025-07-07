package restaurantbookingmanagement.service;

import restaurantbookingmanagement.model.*;
import java.util.List;
import java.util.stream.Collectors;
import restaurantbookingmanagement.service.fileservice.MenuItemFileService;
import restaurantbookingmanagement.service.fileservice.OrderFileService;

/**
 * Service xử lý logic nghiệp vụ đơn hàng
 */
// Design Pattern: Dependency Injection
// Purpose: Inject MenuService for flexible and testable order logic.
public class OrderService {
    private final MenuItemFileService menuItemFileService;
    private final MenuService menuService;
    private final OrderFileService orderFileService;
    private int nextOrderId;
    
    public OrderService(MenuService menuService) {
        this.menuItemFileService = new MenuItemFileService();
        this.menuService = menuService;
        this.orderFileService = new OrderFileService();
        this.nextOrderId = 1;
        
        // Tính toán nextOrderId từ dữ liệu hiện tại
        calculateNextOrderId();
    }
    
    private void calculateNextOrderId() {
        List<Order> existingOrders = orderFileService.readOrdersFromFile();
        if (!existingOrders.isEmpty()) {
            this.nextOrderId = existingOrders.stream()
                    .mapToInt(Order::getOrderId)
                    .max()
                    .orElse(0) + 1;
        }
    }
    
    public Order createOrder(Booking booking) {
        List<Order> orders = orderFileService.readOrdersFromFile();
        Order order = new Order(nextOrderId++, booking);
        
        orders.add(order);
        orderFileService.writeOrdersToFile(orders);
        return order;
    }
    
    /**
     * Tạo order cho booking cụ thể hoặc tìm order hiện có
     */
    public Order getOrCreateOrderForBooking(Booking booking) {
        List<Order> orders = orderFileService.readOrdersFromFile();
        
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
        orderFileService.writeOrdersToFile(orders);
        return newOrder;
    }
    
    /**
     * Tính bill cho booking cụ thể
     */
    public double calculateBillForBooking(int bookingId) {
        List<Order> orders = orderFileService.readOrdersFromFile();
        
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
        List<Order> orders = orderFileService.readOrdersFromFile();
        updateOrderItemsWithMenuItems(orders);
        return orders.stream()
                .filter(order -> order.getBooking().getBookingId() == bookingId)
                .collect(Collectors.toList());
    }
    
    /**
     * Hoàn thành order (thanh toán)
     */
    public boolean completeOrder(int orderId) {
        List<Order> orders = orderFileService.readOrdersFromFile();
        Order order = findOrderById(orderId, orders);
        if (order != null) {
            order.setStatus("COMPLETED");
            orderFileService.writeOrdersToFile(orders);
            return true;
        }
        return false;
    }
    
    public boolean addItemToOrder(int orderId, String itemName, int quantity) {
        List<Order> orders = orderFileService.readOrdersFromFile();
        Order order = findOrderById(orderId, orders);
        if (order == null) {
            return false;
        }
        MenuItem item = menuService.findMenuItemByName(itemName);
        if (item == null) {
            return false;
        }
        order.addItem(item.getItemId(), quantity);
        orderFileService.writeOrdersToFile(orders);
        return true;
    }
    
    public boolean addItemToOrder(int orderId, int itemId, int quantity) {
        List<Order> orders = orderFileService.readOrdersFromFile();
        Order order = findOrderById(orderId, orders);
        if (order == null) {
            return false;
        }
        MenuItem item = menuService.findMenuItemById(itemId);
        if (item == null) {
            return false;
        }
        order.addItem(itemId, quantity);
        orderFileService.writeOrdersToFile(orders);
        return true;
    }
    
    public boolean removeItemFromOrder(int orderId, String itemName) {
        List<Order> orders = orderFileService.readOrdersFromFile();
        Order order = findOrderById(orderId, orders);
        if (order == null) {
            return false;
        }
        MenuItem item = menuService.findMenuItemByName(itemName);
        if (item == null) {
            return false;
        }
        order.removeItem(item.getItemId());
        orderFileService.writeOrdersToFile(orders);
        return true;
    }
    
    public boolean updateOrderStatus(int orderId, String status) {
        List<Order> orders = orderFileService.readOrdersFromFile();
        Order order = findOrderById(orderId, orders);
        if (order != null) {
            order.setStatus(status);
            orderFileService.writeOrdersToFile(orders);
            return true;
        }
        return false;
    }
    
    public Order findOrderById(int orderId) {
        return findOrderById(orderId, orderFileService.readOrdersFromFile());
    }
    
    private Order findOrderById(int orderId, List<Order> orders) {
        return orders.stream()
                .filter(order -> order.getOrderId() == orderId)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Cập nhật MenuItem cho các OrderItem
     */
    private void updateOrderItemsWithMenuItems(List<Order> orders) {
        for (Order o : orders) {
            for (Order.OrderItem oi : o.getItems()) {
                if (oi.getItem() == null) {
                    MenuItem mi = menuService.findMenuItemById(oi.getItemId());
                    oi.setItem(mi);
                }
            }
        }
    }
    
    public List<Order> getOrdersByBooking(Booking booking) {
        List<Order> orders = orderFileService.readOrdersFromFile();
        updateOrderItemsWithMenuItems(orders);
        return orders.stream()
                .filter(order -> order.getBooking().getBookingId() == booking.getBookingId())
                .collect(Collectors.toList());
    }
    
    public List<Order> getAllOrders() {
        List<Order> orders = orderFileService.readOrdersFromFile();
        updateOrderItemsWithMenuItems(orders);
        return orders;
    }
    
    public List<Order> getOrdersByStatus(String status) {
        return orderFileService.readOrdersFromFile().stream()
                .filter(order -> order.getStatus().equals(status))
                .collect(Collectors.toList());
    }
    
    public double getTotalRevenue() {
        return orderFileService.readOrdersFromFile().stream()
                .filter(order -> order.getStatus().equals("COMPLETED"))
                .mapToDouble(Order::getTotalAmount)
                .sum();
    }
} 