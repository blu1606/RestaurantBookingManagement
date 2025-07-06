package restaurantbookingmanagement.controller;

import restaurantbookingmanagement.model.*;
import restaurantbookingmanagement.service.*;
import restaurantbookingmanagement.view.*;
import java.util.List;

/**
 * Controller quản lý đơn hàng
 */
public class OrderController {
    private final OrderService orderService;
    private final ConsoleView view;
    
    public OrderController(OrderService orderService, ConsoleView view) {
        this.orderService = orderService;
        this.view = view;
    }
    
    /**
     * Hiển thị menu quản lý đơn hàng
     */
    public void handleOrderManagement() {
        Menu menu = new Menu("--- Quản lý Đơn Hàng ---", new String[]{
            "Xem danh sách đơn hàng",
            "Thêm đơn hàng",
            "Sửa đơn hàng",
            "Xóa đơn hàng",
            "Tìm kiếm đơn hàng",
        }) {
            @Override
            public void execute(int n) {
                switch (n) {
                    case 1 -> viewOrdersMenu();
                    case 2 -> addOrderMenu();
                    case 3 -> editOrderMenu();
                    case 4 -> deleteOrderMenu();
                    case 5 -> searchOrderMenu();
                    default -> view.displayError("Lựa chọn không hợp lệ.");
                }
            }
        };
        menu.run();
    }
    
    /**
     * Menu tìm kiếm đơn hàng
     */
    private void searchOrderMenu() {
        Menu menu = new Menu("--- Tìm kiếm Đơn Hàng ---", new String[]{
            "Tìm theo ID",
            "Tìm theo tên món",
            "Tìm theo giá",
            "Tìm theo mô tả",
        }) {
            @Override
            public void execute(int n) {
                switch (n) {
                    case 1 -> searchOrderById();
                    case 2 -> searchOrderByName();
                    case 3 -> searchOrderByPrice();
                    case 4 -> searchOrderByDescription();
                    default -> view.displayError("Lựa chọn không hợp lệ.");
                }
            }
        };
        menu.run();
    }
    
    /**
     * Xem danh sách đơn hàng
     */
    private void viewOrdersMenu() {
        List<Order> orders = orderService.getAllOrders();
        view.displayListOrder(orders);
    }
    
    /**
     * Thêm đơn hàng
     */
    private void addOrderMenu() {
        String name = view.getInputHandler().getStringWithCancel("Nhập tên món:");
        if (name == null) {
            view.displayMessage("Đã hủy thao tác thêm đơn hàng.");
            return;
        }
        Double price = getDoubleWithCancel("Nhập giá:");
        if (price == null) {
            view.displayMessage("Đã hủy thao tác thêm đơn hàng.");
            return;
        }
        String desc = view.getInputHandler().getStringWithCancel("Nhập mô tả:");
        if (desc == null) {
            view.displayMessage("Đã hủy thao tác thêm đơn hàng.");
            return;
        }
        orderService.addOrder(name, price, desc);
        view.displaySuccess("Đã thêm món ăn mới.");
    }
    
    /**
     * Sửa đơn hàng
     */
    private void editOrderMenu() {
        Integer id = getIntWithCancel("Nhập ID đơn hàng cần sửa:");
        if (id == null) {
            view.displayMessage("Đã hủy thao tác sửa đơn hàng.");
            return;
        }
        String name = view.getInputHandler().getStringWithCancel("Tên món mới (bỏ trống để giữ nguyên):");
        if (name == null) {
            view.displayMessage("Đã hủy thao tác sửa đơn hàng.");
            return;
        }
        Double price = getDoubleWithCancel("Giá mới (bỏ trống để giữ nguyên):");
        if (price == null) {
            view.displayMessage("Đã hủy thao tác sửa đơn hàng.");
            return;
        }
        String desc = view.getInputHandler().getStringWithCancel("Mô tả mới (bỏ trống để giữ nguyên):");
        if (desc == null) {
            view.displayMessage("Đã hủy thao tác sửa đơn hàng.");
            return;
        }
        boolean ok = orderService.updateOrder(id, name, price, desc);
        if (ok) view.displaySuccess("Đã cập nhật đơn hàng.");
        else view.displayError("Không tìm thấy đơn hàng.");
    }
    
    /**
     * Xóa đơn hàng
     */
    private void deleteOrderMenu() {
        Integer id = getIntWithCancel("Nhập ID đơn hàng cần xóa:");
        if (id == null) {
            view.displayMessage("Đã hủy thao tác xóa đơn hàng.");
            return;
        }
        boolean deleted = orderService.deleteOrder(id);
        if (deleted) view.displaySuccess("Đã xóa đơn hàng.");
        else view.displayError("Không tìm thấy đơn hàng.");
    }
    
    /**
     * Tìm đơn hàng theo ID
     */
    private void searchOrderById() {
        Integer id = getIntWithCancel("Nhập ID đơn hàng:");
        if (id == null) {
            view.displayMessage("Đã hủy thao tác tìm kiếm đơn hàng.");
            return;
        }
        Order order = orderService.findOrderById(id);
        if (order != null) view.displayMessage(order.toString());
        else view.displayError("Không tìm thấy đơn hàng với ID này.");
    }
    
    /**
     * Tìm đơn hàng theo tên món
     */
    private void searchOrderByName() {
        String name = view.getInputHandler().getStringWithCancel("Nhập tên món:");
        if (name == null) {
            view.displayMessage("Đã hủy thao tác tìm kiếm đơn hàng.");
            return;
        }
        List<Order> orders = orderService.getAllOrders();
        boolean found = false;
        for (Order o : orders) {
            if (o.getName().toLowerCase().contains(name.toLowerCase())) {
                view.displayMessage(o.toString());
                found = true;
            }
        }
        if (!found) view.displayError("Không tìm thấy đơn hàng cho tên món này.");
    }
    
    /**
     * Tìm đơn hàng theo giá
     */
    private void searchOrderByPrice() {
        Double price = getDoubleWithCancel("Nhập giá:");
        if (price == null) {
            view.displayMessage("Đã hủy thao tác tìm kiếm đơn hàng.");
            return;
        }
        List<Order> orders = orderService.getAllOrders();
        boolean found = false;
        for (Order o : orders) {
            if (o.getPrice() == price) {
                view.displayMessage(o.toString());
                found = true;
            }
        }
        if (!found) view.displayError("Không tìm thấy đơn hàng với giá này.");
    }
    
    /**
     * Tìm đơn hàng theo mô tả
     */
    private void searchOrderByDescription() {
        String desc = view.getInputHandler().getStringWithCancel("Nhập mô tả:");
        if (desc == null) {
            view.displayMessage("Đã hủy thao tác tìm kiếm đơn hàng.");
            return;
        }
        List<Order> orders = orderService.getAllOrders();
        boolean found = false;
        for (Order o : orders) {
            if (o.getDescription().toLowerCase().contains(desc.toLowerCase())) {
                view.displayMessage(o.toString());
                found = true;
            }
        }
        if (!found) view.displayError("Không tìm thấy đơn hàng với mô tả này.");
    }
    
    /**
     * Thêm item vào order
     */
    public boolean addItemToOrder(int orderId, int itemId, int quantity) {
        return orderService.addItemToOrder(orderId, itemId, quantity);
    }
    
    /**
     * Thêm item vào order theo tên
     */
    public boolean addItemToOrder(int orderId, String itemName, int quantity) {
        return orderService.addItemToOrder(orderId, itemName, quantity);
    }
    
    /**
     * Xóa item khỏi order
     */
    public boolean removeItemFromOrder(int orderId, String itemName) {
        return orderService.removeItemFromOrder(orderId, itemName);
    }
    
    /**
     * Tìm order theo ID
     */
    public Order findOrderById(int orderId) {
        return orderService.findOrderById(orderId);
    }
    
    /**
     * Lấy hoặc tạo order cho booking
     */
    public Order getOrCreateOrderForBooking(Booking booking) {
        return orderService.getOrCreateOrderForBooking(booking);
    }
    
    /**
     * Tính bill cho booking
     */
    public double calculateBillForBooking(int bookingId) {
        return orderService.calculateBillForBooking(bookingId);
    }
    
    /**
     * Lấy orders cho booking
     */
    public List<Order> getOrdersForBooking(int bookingId) {
        return orderService.getOrdersForBooking(bookingId);
    }
    
    /**
     * Hiển thị danh sách đơn hàng
     */
    public void displayOrders() {
        List<Order> orders = orderService.getAllOrders();
        view.displayListOrder(orders);
    }
    
    /**
     * Hiển thị menu
     */
    public void displayMenu() {
        view.displayMenu(orderService.getAllMenuItems());
    }
    
    /**
     * Hàm tiện ích cho nhập int có hỗ trợ cancel
     */
    private Integer getIntWithCancel(String message) {
        String input = view.getInputHandler().getStringWithCancel(message);
        if (input == null) return null;
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            view.displayError("Giá trị không hợp lệ. Vui lòng nhập số nguyên hoặc 'cancel' để hủy.");
            return getIntWithCancel(message);
        }
    }
    
    /**
     * Hàm tiện ích cho nhập double có hỗ trợ cancel
     */
    private Double getDoubleWithCancel(String message) {
        String input = view.getInputHandler().getStringWithCancel(message);
        if (input == null) return null;
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            view.displayError("Giá trị không hợp lệ. Vui lòng nhập số thực hoặc 'cancel' để hủy.");
            return getDoubleWithCancel(message);
        }
    }
    
    /**
     * Getter cho OrderService
     */
    public OrderService getOrderService() {
        return orderService;
    }
} 