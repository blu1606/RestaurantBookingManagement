package restaurantbookingmanagement.service.validator;

import restaurantbookingmanagement.model.Order;

public class OrderValidator {
    public static boolean isValid(Order order) {
        return order != null &&
               order.getBooking() != null &&
               order.getItems() != null &&
               !order.getItems().isEmpty() &&
               (order.getStatus().equals("PENDING") || order.getStatus().equals("PREPARING") || order.getStatus().equals("READY") || order.getStatus().equals("COMPLETED")) &&
               order.getTotalAmount() >= 0;
    }
    public static String validate(Order order) {
        if (order == null) return "Order is null";
        if (order.getBooking() == null) return "Booking is required";
        if (order.getItems() == null || order.getItems().isEmpty()) return "Order must have at least one item";
        if (!(order.getStatus().equals("PENDING") || order.getStatus().equals("PREPARING") || order.getStatus().equals("READY") || order.getStatus().equals("COMPLETED"))) return "Invalid status";
        if (order.getTotalAmount() < 0) return "Total amount must be >= 0";
        return null;
    }
} 