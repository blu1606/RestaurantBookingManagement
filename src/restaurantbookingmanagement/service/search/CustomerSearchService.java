package restaurantbookingmanagement.service.search;

import restaurantbookingmanagement.model.Customer;
import restaurantbookingmanagement.service.fileservice.CustomerFileService;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class CustomerSearchService {
    private final CustomerFileService customerFileService;

    public CustomerSearchService() {
        this.customerFileService = new CustomerFileService();
    }

    /**
     * Tìm kiếm khách hàng theo tên hoặc số điện thoại
     */
    public List<Customer> searchCustomers(String searchTerm) {
        List<Customer> customers = customerFileService.readCustomersFromFile();
        String lowerSearchTerm = searchTerm.toLowerCase();
        return customers.stream()
                .filter(c -> c.getName().toLowerCase().contains(lowerSearchTerm) ||
                           c.getPhone().contains(searchTerm))
                .collect(Collectors.toList());
    }
} 