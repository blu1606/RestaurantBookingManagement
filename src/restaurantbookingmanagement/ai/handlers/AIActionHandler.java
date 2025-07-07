package restaurantbookingmanagement.ai.handlers;

import restaurantbookingmanagement.ai.AIResponse;
import restaurantbookingmanagement.service.*;
import restaurantbookingmanagement.view.ConsoleView;
 
public interface AIActionHandler {
    void handle(AIResponse response, OrderService orderService, BookingService bookingService, CustomerService customerService, ConsoleView view);
} 