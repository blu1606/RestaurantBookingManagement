package restaurantbookingmanagement.service;

import restaurantbookingmanagement.model.*;
import java.util.List;
import java.util.stream.Collectors;
import restaurantbookingmanagement.service.fileservice.MenuItemFileService;
import restaurantbookingmanagement.service.fileservice.OrderFileService;
import restaurantbookingmanagement.view.dto.OrderRequest;
import java.util.ArrayList;

/**
 * Service xử lý logic nghiệp vụ đơn hàng
 */
// Design Pattern: Dependency Injection
// Purpose: Inject MenuService for flexible and testable order logic.
public class OrderService {
    private final MenuItemFileService menuItemFileService;
    private final MenuService menuService;
    private final OrderFileService orderFileService;
    private final BookingService bookingService;
    private final TableService tableService;
    private int nextOrderId;
    
    public OrderService(MenuService menuService, BookingService bookingService, TableService tableService) {
        this.menuItemFileService = new MenuItemFileService();
        this.menuService = menuService;
        this.orderFileService = new OrderFileService();
        this.bookingService = bookingService;
        this.tableService = tableService;
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
        // Ánh xạ lại booking cho tất cả order nếu bị null
        for (Order order : orders) {
            if (order.getBooking() == null && order.getBookingId() > 0) {
                Booking b = bookingService.findBookingById(order.getBookingId());
                order.setBooking(b);
            }
        }
        // Sửa filter: kiểm tra null
        Order existingOrder = orders.stream()
                .filter(order -> order.getBooking() != null && order.getBooking().getBookingId() == booking.getBookingId() && 
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
        // Ánh xạ lại booking, table, menu item như cũ
        for (Order order : orders) {
            if (order.getBooking() == null && order.getBookingId() > 0) {
                Booking booking = bookingService.findBookingById(order.getBookingId());
                order.setBooking(booking);
            }
            if (order.getTable() == null && order.getTableId() > 0) {
                Table table = tableService.getAllTables().stream()
                    .filter(t -> t.getTableId() == order.getTableId())
                    .findFirst().orElse(null);
                order.setTable(table);
            }
            for (Order.OrderItem oi : order.getItems()) {
                if (oi.getItem() == null && oi.getItemId() > 0) {
                    MenuItem mi = menuService.findMenuItemById(oi.getItemId());
                    oi.setItem(mi);
                }
            }
        }
        // Loại bỏ các order không có món nào
        orders.removeIf(order -> order.getItems() == null || order.getItems().isEmpty());
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
    
    /**
     * Thêm món vào order từ OrderRequest DTO (refactor cho controller mỏng)
     */
    public boolean addOrderItem(OrderRequest req, MenuService menuService) {
        Order order = getOrCreateOrderForBooking(req.getBooking());
        MenuItem item = null;
        try {
            int id = Integer.parseInt(req.getItemInput());
            item = menuService.findMenuItemById(id);
        } catch (NumberFormatException e) {
            item = menuService.findMenuItemByName(req.getItemInput());
        }
        if (item == null) return false;
        return addItemToOrder(order.getOrderId(), item.getItemId(), req.getQuantity());
    }

    public List<Order> getOrdersByCustomer(Customer customer) {
        if (customer == null) return new ArrayList<>();
        List<Integer> userBookingIds = bookingService.getBookingsByCustomer(customer).stream()
            .map(Booking::getBookingId).toList();
        return getAllOrders().stream()
            .filter(order -> userBookingIds.contains(order.getBookingId()))
            .toList();
    }
} 