package restaurantbookingmanagement;

import restaurantbookingmanagement.model.*;
import restaurantbookingmanagement.service.*;
import java.util.*;
import java.time.LocalDateTime;

public class TestFunctionality {
    public static void main(String[] args) {
        // Test tạo khách hàng
        Customer customer = new Customer(1, "Nguyen Van A", "0988888888", "a@email.com");
        System.out.println("Tạo khách hàng: " + customer);

        // Test tạo bàn
        Table table = new Table(1, 4);
        System.out.println("Tạo bàn: " + table);

        // Test tạo booking
        Booking booking = new Booking(1, customer, table, LocalDateTime.now(), 4);
        System.out.println("Tạo booking: " + booking);

        // Test tạo menu item
        MenuItem item1 = new MenuItem(1, "Phở bò", 50000, "Phở bò truyền thống");
        MenuItem item2 = new MenuItem(2, "Bún chả", 45000, "Bún chả Hà Nội");
        System.out.println("Tạo món: " + item1);
        System.out.println("Tạo món: " + item2);

        // Test tạo order và thêm món
        Order order = new Order(1, booking);
        order.addItem(item1.getItemId(), 2);
        order.addItem(item2.getItemId(), 1);
        // Gán MenuItem cho OrderItem để test toString
        for (Order.OrderItem oi : order.getItems()) {
            if (oi.getItemId() == item1.getItemId()) oi.setItem(item1);
            if (oi.getItemId() == item2.getItemId()) oi.setItem(item2);
        }
        System.out.println("Tạo đơn hàng: " + order);

        // Test cập nhật trạng thái bàn
        table.setStatus(TableStatus.OCCUPIED);
        System.out.println("Cập nhật trạng thái bàn: " + table);

        // Test cập nhật số lượng món trong order
        order.updateItemAmount(item1.getItemId(), 3);
        System.out.println("Cập nhật số lượng món: " + order);

        // Test xóa món khỏi order
        order.removeItem(item2.getItemId());
        System.out.println("Xóa món khỏi order: " + order);
    }
} 