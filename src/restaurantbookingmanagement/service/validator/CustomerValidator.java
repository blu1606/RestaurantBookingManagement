package restaurantbookingmanagement.service.validator;

import restaurantbookingmanagement.model.Customer;

public class CustomerValidator {
    public static boolean isValid(Customer customer) {
        return customer != null &&
               customer.getName() != null && !customer.getName().trim().isEmpty() &&
               customer.getPhone() != null && !customer.getPhone().trim().isEmpty() &&
               (customer.getRole().equals("admin") || customer.getRole().equals("user") || customer.getRole().equals("guest"));
    }
    public static String validate(Customer customer) {
        if (customer == null) return "Customer is null";
        if (customer.getName() == null || customer.getName().trim().isEmpty()) return "Name is required";
        if (customer.getPhone() == null || customer.getPhone().trim().isEmpty()) return "Phone is required";
        if (customer.getEmail() != null && !customer.getEmail().isEmpty() && !customer.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) return "Invalid email format";
        if (!(customer.getRole().equals("admin") || customer.getRole().equals("user") || customer.getRole().equals("guest"))) return "Invalid role";
        return null;
    }
} 