package restaurantbookingmanagement.ai.handlers;

import restaurantbookingmanagement.service.OrderService;
import restaurantbookingmanagement.service.BookingService;
import restaurantbookingmanagement.service.CustomerService;
import restaurantbookingmanagement.service.MenuService;
import restaurantbookingmanagement.service.TableService;

public class ServiceContext {
    private final OrderService orderService;
    private final BookingService bookingService;
    private final CustomerService customerService;
    private final MenuService menuService;
    private final TableService tableService;

    public ServiceContext(OrderService orderService, BookingService bookingService, CustomerService customerService, MenuService menuService, TableService tableService) {
        this.orderService = orderService;
        this.bookingService = bookingService;
        this.customerService = customerService;
        this.menuService = menuService;
        this.tableService = tableService;
    }

    public OrderService getOrderService() { return orderService; }
    public BookingService getBookingService() { return bookingService; }
    public CustomerService getCustomerService() { return customerService; }
    public MenuService getMenuService() { return menuService; }
    public TableService getTableService() { return tableService; }
} 