package restaurantbookingmanagement.controller;

import restaurantbookingmanagement.model.*;
import restaurantbookingmanagement.service.*;
import restaurantbookingmanagement.view.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller quản lý đặt bàn
 */
public class BookingController {
    private final BookingService bookingService;
    private final ConsoleView view;
    
    public BookingController(BookingService bookingService, ConsoleView view) {
        this.bookingService = bookingService;
        this.view = view;
    }
    
    /**
     * Hiển thị menu quản lý đặt bàn
     */
    public void handleBookingManagement() {
        Menu menu = new Menu("--- Quản lý Đặt Bàn ---", new String[]{
            "Xem danh sách đặt bàn",
            "Xóa đặt bàn",
            "Sửa đặt bàn",
            "Tìm kiếm đặt bàn",
        }) {
            @Override
            public void execute(int n) {
                switch (n) {
                    case 1 -> view.displayBookings(bookingService.getAllBookings());
                    case 2 -> deleteBookingMenu();
                    case 3 -> editBookingMenu();
                    case 4 -> searchBookingMenu();
                    default -> view.displayError("Lựa chọn không hợp lệ.");
                }
            }
        };
        menu.run();
    }
    
    /**
     * Menu tìm kiếm đặt bàn
     */
    private void searchBookingMenu() {
        Menu menu = new Menu("--- Tìm kiếm Đặt Bàn ---", new String[]{
            "Tìm theo ID",
            "Tìm theo tên khách hàng",
            "Tìm theo số điện thoại khách hàng",
        }) {
            @Override
            public void execute(int n) {
                switch (n) {
                    case 1 -> searchBookingById();
                    case 2 -> searchBookingByName();
                    case 3 -> searchBookingByPhone();
                    default -> view.displayError("Lựa chọn không hợp lệ.");
                }
            }
        };
        menu.run();
    }
    
    /**
     * Xóa đặt bàn
     */
    private void deleteBookingMenu() {
        Integer id = view.getIntWithCancel("Nhập ID đặt bàn cần xóa:");
        if (id == null) {
            view.displayMessage("Đã hủy thao tác xóa đặt bàn.");
            return;
        }
        boolean deleted = bookingService.deleteBooking(id);
        if (deleted) view.displaySuccess("Đã xóa đặt bàn.");
        else view.displayError("Không tìm thấy hoặc không thể xóa đặt bàn này.");
    }
    
    /**
     * Sửa đặt bàn
     */
    private void editBookingMenu() {
        Integer id = view.getIntWithCancel("Nhập ID đặt bàn cần sửa:");
        if (id == null) {
            view.displayMessage("Đã hủy thao tác sửa đặt bàn.");
            return;
        }
        String guestsStr = view.getInputHandler().getStringWithCancel("Số khách mới (bỏ trống để giữ nguyên):");
        if (guestsStr == null) {
            view.displayMessage("Đã hủy thao tác sửa đặt bàn.");
            return;
        }
        String timeStr = view.getInputHandler().getStringWithCancel("Thời gian mới (dd/MM/yyyy HH:mm, bỏ trống để giữ nguyên):");
        if (timeStr == null) {
            view.displayMessage("Đã hủy thao tác sửa đặt bàn.");
            return;
        }
        boolean ok = bookingService.updateBooking(id, guestsStr, timeStr);
        if (ok) view.displaySuccess("Đã cập nhật đặt bàn.");
        else view.displayError("Không tìm thấy đặt bàn hoặc dữ liệu không hợp lệ.");
    }
    
    /**
     * Tìm đặt bàn theo ID
     */
    private void searchBookingById() {
        Integer id = view.getIntWithCancel("Nhập ID đặt bàn:");
        if (id == null) {
            view.displayMessage("Đã hủy thao tác tìm kiếm đặt bàn.");
            return;
        }
        Booking booking = bookingService.findBookingById(id);
        if (booking != null) view.displayMessage(booking.toString());
        else view.displayError("Không tìm thấy đặt bàn với ID này.");
    }
    
    /**
     * Tìm đặt bàn theo tên khách hàng
     */
    private void searchBookingByName() {
        String name = view.getInputHandler().getStringWithCancel("Nhập tên khách hàng:");
        if (name == null) {
            view.displayMessage("Đã hủy thao tác tìm kiếm đặt bàn.");
            return;
        }
        List<Booking> bookings = bookingService.getAllBookings();
        boolean found = false;
        for (Booking b : bookings) {
            if (b.getCustomer().getName().toLowerCase().contains(name.toLowerCase())) {
                view.displayMessage(b.toString());
                found = true;
            }
        }
        if (!found) view.displayError("Không tìm thấy đặt bàn cho tên khách này.");
    }
    
    /**
     * Tìm đặt bàn theo số điện thoại khách hàng
     */
    private void searchBookingByPhone() {
        String phone = view.getInputHandler().getStringWithCancel("Nhập số điện thoại khách hàng:");
        if (phone == null) {
            view.displayMessage("Đã hủy thao tác tìm kiếm đặt bàn.");
            return;
        }
        List<Booking> bookings = bookingService.getAllBookings();
        boolean found = false;
        for (Booking b : bookings) {
            if (b.getCustomer().getPhone().contains(phone)) {
                view.displayMessage(b.toString());
                found = true;
            }
        }
        if (!found) view.displayError("Không tìm thấy đặt bàn cho số điện thoại này.");
    }
    
    /**
     * Tạo đặt bàn
     */
    public Booking createBooking(Customer customer, int guests, LocalDateTime bookingTime) {
        return bookingService.createBooking(customer, guests, bookingTime);
    }
    
    /**
     * Xóa đặt bàn
     */
    public boolean deleteBooking(int bookingId) {
        return bookingService.deleteBooking(bookingId);
    }
    
    /**
     * Hủy đặt bàn
     */
    public boolean cancelBooking(int bookingId) {
        return bookingService.cancelBooking(bookingId);
    }
    
    /**
     * Tìm đặt bàn theo ID
     */
    public Booking findBookingById(int bookingId) {
        return bookingService.findBookingById(bookingId);
    }
    
    /**
     * Lấy danh sách đặt bàn theo khách hàng
     */
    public List<Booking> getBookingsByCustomer(Customer customer) {
        return bookingService.getBookingsByCustomer(customer);
    }
    
    /**
     * Hiển thị danh sách đặt bàn
     */
    public void displayBookings() {
        view.displayBookings(bookingService.getAllBookings());
    }
    
    /**
     * Getter cho BookingService
     */
    public BookingService getBookingService() {
        return bookingService;
    }
} 