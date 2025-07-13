/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package restaurantbookingmanagement;

import restaurantbookingmanagement.service.*;
import restaurantbookingmanagement.view.*;
import restaurantbookingmanagement.controller.*;
import restaurantbookingmanagement.service.validator.BookingValidator;

/**
 * Main class - Entry point of the application
 * @author Blue
 */
public class RestaurantBookingManagement {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            // 1. Initialize Model (Services)
            TableService tableService = new TableService();
            MenuService menuService = new MenuService();
            BookingValidator bookingValidator = new BookingValidator();
            BookingService bookingService = new BookingService(tableService, bookingValidator);
            OrderService orderService = new OrderService(menuService, bookingService, tableService);

            // 2. Initialize View
            ConsoleView view = new ConsoleView();

            // 3. Initialize Services
            CustomerService customerService = new CustomerService(tableService, bookingValidator);
            
            // 4. Initialize Controller and inject Model, View
            MainController controller = new MainController(bookingService, orderService, customerService, view);

            // 5. Start application
            controller.run();
            
        } catch (Exception e) {
            System.err.println("Application startup error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
