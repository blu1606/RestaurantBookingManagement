package restaurantbookingmanagement.controller;

import restaurantbookingmanagement.model.*;
import restaurantbookingmanagement.service.*;
import restaurantbookingmanagement.view.*;
import java.util.List;

/**
 * Controller quản lý bàn
 */
public class TableController {
    private final TableService tableService;
    private final ConsoleView view;
    
    public TableController(TableService tableService, ConsoleView view) {
        this.tableService = tableService;
        this.view = view;
    }
    
    /**
     * Hiển thị menu quản lý bàn
     */
    public void handleTableManagement() {
        Menu menu = new Menu("--- Quản lý Bàn ---", new String[]{
            "Xem danh sách bàn",
            "Thêm bàn mới",
            "Sửa bàn",
            "Xóa bàn",
            "Tìm kiếm bàn",
        }) {
            @Override
            public void execute(int n) {
                switch (n) {
                    case 1 -> view.displayTables(tableService.getAllTables());
                    case 2 -> addTableMenu();
                    case 3 -> editTableMenu();
                    case 4 -> deleteTableMenu();
                    case 5 -> searchTableMenu();
                    default -> view.displayError("Lựa chọn không hợp lệ.");
                }
            }
        };
        menu.run();
    }
    
    /**
     * Thêm bàn mới
     */
    private void addTableMenu() {
        Integer cap = getIntWithCancel("Nhập sức chứa bàn:");
        if (cap == null) {
            view.displayMessage("Đã hủy thao tác thêm bàn.");
            return;
        }
        tableService.addTable(cap);
        view.displaySuccess("Đã thêm bàn mới.");
    }
    
    /**
     * Sửa bàn
     */
    private void editTableMenu() {
        Integer id = getIntWithCancel("Nhập ID bàn cần sửa:");
        if (id == null) {
            view.displayMessage("Đã hủy thao tác sửa bàn.");
            return;
        }
        String capStr = view.getInputHandler().getStringWithCancel("Sức chứa mới (bỏ trống để giữ nguyên):");
        if (capStr == null) {
            view.displayMessage("Đã hủy thao tác sửa bàn.");
            return;
        }
        String statusStr = view.getInputHandler().getStringWithCancel("Trạng thái mới (AVAILABLE/OCCUPIED/RESERVED/MAINTENANCE, bỏ trống để giữ nguyên):");
        if (statusStr == null) {
            view.displayMessage("Đã hủy thao tác sửa bàn.");
            return;
        }
        boolean ok = tableService.updateTable(id, capStr, statusStr);
        if (ok) view.displaySuccess("Đã cập nhật bàn.");
        else view.displayError("Không tìm thấy bàn hoặc dữ liệu không hợp lệ.");
    }
    
    /**
     * Xóa bàn
     */
    private void deleteTableMenu() {
        Integer delId = getIntWithCancel("Nhập ID bàn cần xóa:");
        if (delId == null) {
            view.displayMessage("Đã hủy thao tác xóa bàn.");
            return;
        }
        boolean ok = tableService.deleteTable(delId);
        if (ok) view.displaySuccess("Đã xóa bàn.");
        else view.displayError("Không tìm thấy bàn.");
    }
    
    /**
     * Tìm kiếm bàn
     */
    private void searchTableMenu() {
        String keyword = view.getInputHandler().getStringWithCancel("Nhập ID, sức chứa hoặc trạng thái bàn:");
        if (keyword == null) {
            view.displayMessage("Đã hủy thao tác tìm kiếm bàn.");
            return;
        }
        List<Table> result = tableService.searchTables(keyword);
        if (result.isEmpty()) view.displayError("Không tìm thấy bàn phù hợp.");
        else for (Table t : result) view.displayMessage(t.toString());
    }
    
    /**
     * Hiển thị danh sách bàn
     */
    public void displayTables() {
        view.displayTables(tableService.getAllTables());
    }
    
    /**
     * Tìm bàn khả dụng
     */
    public Table findAvailableTable(int guests) {
        return tableService.findAvailableTable(guests);
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
} 