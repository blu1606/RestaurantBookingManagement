package restaurantbookingmanagement.controller;

import restaurantbookingmanagement.model.*;
import restaurantbookingmanagement.service.*;
import restaurantbookingmanagement.view.*;

/**
 * Controller quản lý menu items
 */
public class MenuController {
    private final OrderService orderService;
    private final ConsoleView view;
    
    public MenuController(OrderService orderService, ConsoleView view) {
        this.orderService = orderService;
        this.view = view;
    }
    
    /**
     * Hiển thị menu quản lý món ăn
     */
    public void handleMenuManagement() {
        Menu menu = new Menu("--- Quản lý Món Ăn ---", new String[]{
            "Xem danh sách món ăn",
            "Thêm món ăn",
            "Xóa món ăn",
            "Sửa món ăn",
            "Tìm kiếm món ăn",
        }) {
            @Override
            public void execute(int n) {
                switch (n) {
                    case 1 -> view.displayMenu(orderService.getAllMenuItems());
                    case 2 -> addMenuItemMenu();
                    case 3 -> deleteMenuItemMenu();
                    case 4 -> editMenuItemMenu();
                    case 5 -> searchMenuItemMenu();
                    default -> view.displayError("Lựa chọn không hợp lệ.");
                }
            }
        };
        menu.run();
    }
    
    /**
     * Thêm món ăn
     */
    private void addMenuItemMenu() {
        String name = view.getInputHandler().getStringWithCancel("Nhập tên món:");
        if (name == null) {
            view.displayMessage("Đã hủy thao tác thêm món ăn.");
            return;
        }
        Double price = getDoubleWithCancel("Nhập giá:");
        if (price == null) {
            view.displayMessage("Đã hủy thao tác thêm món ăn.");
            return;
        }
        String desc = view.getInputHandler().getStringWithCancel("Nhập mô tả:");
        if (desc == null) {
            view.displayMessage("Đã hủy thao tác thêm món ăn.");
            return;
        }
        orderService.addMenuItem(name, price, desc);
        view.displaySuccess("Đã thêm món ăn mới.");
    }
    
    /**
     * Xóa món ăn
     */
    private void deleteMenuItemMenu() {
        Integer delId = getIntWithCancel("Nhập ID món ăn cần xóa:");
        if (delId == null) {
            view.displayMessage("Đã hủy thao tác xóa món ăn.");
            return;
        }
        boolean deleted = orderService.deleteMenuItem(delId);
        if (deleted) view.displaySuccess("Đã xóa món ăn.");
        else view.displayError("Không tìm thấy món ăn với ID này.");
    }
    
    /**
     * Sửa món ăn
     */
    private void editMenuItemMenu() {
        Integer editId = getIntWithCancel("Nhập ID món ăn cần sửa:");
        if (editId == null) {
            view.displayMessage("Đã hủy thao tác sửa món ăn.");
            return;
        }
        String newName = view.getInputHandler().getStringWithCancel("Tên mới (bỏ trống để giữ nguyên):");
        if (newName == null) {
            view.displayMessage("Đã hủy thao tác sửa món ăn.");
            return;
        }
        String priceStr = view.getInputHandler().getStringWithCancel("Giá mới (bỏ trống để giữ nguyên):");
        if (priceStr == null) {
            view.displayMessage("Đã hủy thao tác sửa món ăn.");
            return;
        }
        String newDesc = view.getInputHandler().getStringWithCancel("Mô tả mới (bỏ trống để giữ nguyên):");
        if (newDesc == null) {
            view.displayMessage("Đã hủy thao tác sửa món ăn.");
            return;
        }
        boolean ok = orderService.updateMenuItem(editId, newName, priceStr, newDesc);
        if (ok) view.displaySuccess("Đã cập nhật món ăn.");
        else view.displayError("Không tìm thấy món ăn hoặc giá không hợp lệ.");
    }
    
    /**
     * Tìm kiếm món ăn
     */
    private void searchMenuItemMenu() {
        String keyword = view.getInputHandler().getStringWithCancel("Nhập tên hoặc ID món ăn:");
        if (keyword == null) {
            view.displayMessage("Đã hủy thao tác tìm kiếm món ăn.");
            return;
        }
        try {
            int id = Integer.parseInt(keyword);
            MenuItem found = orderService.findMenuItemById(id);
            if (found != null) view.displayMessage(found.toString());
            else view.displayError("Không tìm thấy món ăn với ID này.");
        } catch (NumberFormatException e) {
            MenuItem found = orderService.findMenuItemByName(keyword);
            if (found != null) view.displayMessage(found.toString());
            else view.displayError("Không tìm thấy món ăn với tên này.");
        }
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
} 